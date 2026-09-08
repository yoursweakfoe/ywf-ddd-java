package com.yoursweakfoe.DbMigration.snapshot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Schema 事实现状取回器：每次迁移成功后，从 PostgreSQL 系统目录内省当前库的真实表结构，
 * 逐表重建 {@code <output-dir>/<schema>/<table>.sql}——等价于把全部 migration 在 schema
 * 层面「合并」出一份事实文档（omate-bi-flyway CurrentSchemaSnapshotCallback 的移植件）。
 *
 * <p>相对旧实现的三处修正（快照的立身之本是忠实于库，而非复述迁移脚本）：
 * <ul>
 *   <li><b>schema 参数化</b>：旧件写死 public；本件按配置清单逐 schema 取回
 *       （orders / products / public）。</li>
 *   <li><b>约束补全</b>：旧件漏取主键——PK 不住在 information_schema.columns 里，其快照
 *       回放会丢约束。本件经 pg_constraint + {@code pg_get_constraintdef} 把
 *       PK / UNIQUE / CHECK / FK 全部内联进 CREATE TABLE，形状对齐 pg_dump。</li>
 *   <li><b>防幽灵残留</b>：重写前清空目标目录内旧 *.sql——表被 drop 后快照不会滞留
 *       「不存在的表」。</li>
 * </ul>
 *
 * <p>时序保证：ApplicationRunner 在上下文刷新完成后才执行，而 Liquibase update 发生在
 * bean 初始化阶段——迁移失败则启动中止、本件根本不会跑；即快照只会反映「迁移成功后」
 * 的事实，绝不出半成品。
 *
 * <p>失败语义：快照失败 = 抛异常 = 进程非 0 退出（旧件 catch-log-继续会给出一份悄悄过期
 * 的毒快照；事实现状宁可缺席，不可失真）。
 *
 * <p>边界：只覆盖 BASE TABLE（列/默认值/约束/索引/表列注释）。库内当前不存在且本件
 * 不覆盖：视图、函数、序列、域、触发器、分区、RLS——它们出现时按表扩查询，刻意不预建。
 */
@Component
public class SchemaSnapshotRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaSnapshotRunner.class);

    /** schema 名白名单：小写标识符。既挡路径注入（resolve 前先验），也挡拼写错误静默产出空目录。 */
    private static final Pattern SCHEMA_NAME = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    private static final String TABLES_SQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            ORDER BY table_name
            """;

    /** 列渲染对齐 pg_dump 惯用名；PG_DESCRIPTION 取列注释（objsubid = 列序号）。 */
    private static final String COLUMNS_SQL = """
            SELECT c.column_name,
                   CASE WHEN c.data_type = 'USER-DEFINED'            THEN c.udt_name
                        WHEN c.data_type = 'character varying'       THEN 'varchar'  || COALESCE('(' || c.character_maximum_length || ')', '')
                        WHEN c.data_type = 'character'               THEN 'char'     || COALESCE('(' || c.character_maximum_length || ')', '')
                        WHEN c.data_type = 'numeric'                 THEN 'numeric'  || COALESCE('(' || c.numeric_precision || ',' || c.numeric_scale || ')', '')
                        WHEN c.data_type = 'timestamp with time zone'    THEN 'timestamptz'
                        WHEN c.data_type = 'timestamp without time zone' THEN 'timestamp'
                        ELSE c.data_type
                   END AS rendered_type,
                   c.is_nullable,
                   c.column_default,
                   pgd.description AS col_comment
            FROM information_schema.columns c
            LEFT JOIN pg_catalog.pg_description pgd
                   ON pgd.objoid = (quote_ident(c.table_schema) || '.' || quote_ident(c.table_name))::regclass
                  AND pgd.objsubid = c.ordinal_position
            WHERE c.table_schema = ? AND c.table_name = ?
            ORDER BY c.ordinal_position
            """;

    /**
     * 表约束：PK→UNIQUE→CHECK→FK 固定序（diff 稳定）；pg_get_constraintdef 给原生完整定义。
     * 排除 contype 'n'/'t'（NOT NULL 约束对象）：它们在 CREATE TABLE 里不可回放，
     * 且列定义的 is_nullable 已忠实表达同一事实——双份=噪音。
     */
    private static final String CONSTRAINTS_SQL = """
            SELECT conname, pg_get_constraintdef(oid) AS condef
            FROM pg_catalog.pg_constraint
            WHERE conrelid = (quote_ident(?) || '.' || quote_ident(?))::regclass
              AND contype NOT IN ('n', 't')
            ORDER BY CASE contype WHEN 'p' THEN 0 WHEN 'u' THEN 1 WHEN 'c' THEN 2 WHEN 'f' THEN 3 ELSE 4 END,
                     conname
            """;

    private static final String INDEXES_SQL = """
            SELECT indexdef
            FROM pg_catalog.pg_indexes
            WHERE schemaname = ? AND tablename = ?
            ORDER BY indexname
            """;

    private static final String TABLE_COMMENT_SQL = """
            SELECT obj_description((quote_ident(?) || '.' || quote_ident(?))::regclass, 'pg_class')
            """;

    private final DataSource dataSource;
    private final List<String> schemas;
    private final Path outputRoot;

    public SchemaSnapshotRunner(DataSource dataSource,
                                @Value("${ywf.migration.snapshot.schemas}") List<String> schemas,
                                @Value("${ywf.migration.snapshot.output-dir}") String outputDir) {
        this.dataSource = dataSource;
        this.schemas = schemas;
        this.outputRoot = Path.of(outputDir).toAbsolutePath().normalize();
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException, IOException {
        int total = 0;
        log.info(">>>>> 开始取回 schema 事实现状（快照根目录: {}）", outputRoot);
        try (Connection conn = dataSource.getConnection()) {
            String database = conn.getCatalog();
            for (String schema : schemas) {
                total += snapshotSchema(conn, database, schema);
            }
        }
        log.info("<<<<< 事实现状取回完成：{} 个 schema 共 {} 张表", schemas.size(), total);
    }

    private int snapshotSchema(Connection conn, String database, String schema) throws SQLException, IOException {
        if (!SCHEMA_NAME.matcher(schema).matches()) {
            throw new IllegalArgumentException("非法 schema 名（仅允许小写标识符）: " + schema);
        }
        List<String> tables = list(conn, TABLES_SQL, schema);
        Path dir = outputRoot.resolve(schema);
        Files.createDirectories(dir);
        purgeStaleSnapshots(dir);
        for (String table : tables) {
            writeTableSnapshot(conn, database, schema, table, dir);
        }
        log.info("  [{}] {} 张表: {}", schema, tables.size(), tables);
        return tables.size();
    }

    /** 只清本工具产出的 *.sql；README 等人写文件不碰。 */
    private void purgeStaleSnapshots(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".sql"))
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException e) {
                         throw new UncheckedIoException("清理旧快照失败: " + p, e);
                     }
                 });
        }
    }

    private void writeTableSnapshot(Connection conn, String database, String schema, String table, Path dir)
            throws SQLException, IOException {
        List<Column> columns = readColumns(conn, schema, table);
        List<String> constraints = readConstraints(conn, schema, table);
        List<String> indexes = list(conn, INDEXES_SQL, schema, table);
        String tableComment = readTableComment(conn, schema, table);

        StringBuilder s = new StringBuilder();
        s.append("-- ============================================================================\n");
        s.append("-- ⚠ 自动生成 · schema 事实现状快照（SchemaSnapshotRunner），禁止手改\n");
        s.append("-- 源库: ").append(database).append(" / schema: ").append(schema).append(" / 表: ").append(table).append('\n');
        s.append("-- 性质: 迁移成功后的库内事实，供 review 对照与漂移检测；演进历史权威在 db/changelog/\n");
        s.append("-- ============================================================================\n\n");

        s.append("CREATE TABLE ").append(schema).append('.').append(table).append(" (\n");
        List<String> lines = new ArrayList<>();
        for (Column c : columns) {
            lines.add("    " + c.render());
        }
        for (String condef : constraints) {
            lines.add("    CONSTRAINT " + condef);
        }
        s.append(String.join(",\n", lines)).append("\n);\n");

        if (tableComment != null && !tableComment.isBlank()) {
            s.append('\n').append(commentOn("TABLE", schema + "." + table, tableComment)).append('\n');
        }
        for (Column c : columns) {
            if (c.comment() != null && !c.comment().isBlank()) {
                s.append(commentOn("COLUMN", schema + "." + table + "." + c.name(), c.comment())).append('\n');
            }
        }
        if (!indexes.isEmpty()) {
            s.append('\n');
            for (String indexdef : indexes) {
                s.append(indexdef).append(";\n");
            }
        }

        Files.writeString(dir.resolve(table + ".sql"), s.toString(), StandardCharsets.UTF_8);
        log.info("    ✓ {}.{}.sql", schema, table);
    }

    private List<Column> readColumns(Connection conn, String schema, String table) throws SQLException {
        List<Column> cols = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(COLUMNS_SQL)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cols.add(new Column(
                            rs.getString("column_name"),
                            rs.getString("rendered_type"),
                            "YES".equals(rs.getString("is_nullable")),
                            rs.getString("column_default"),
                            rs.getString("col_comment")));
                }
            }
        }
        return cols;
    }

    private List<String> readConstraints(Connection conn, String schema, String table) throws SQLException {
        List<String> cons = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(CONSTRAINTS_SQL)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cons.add(rs.getString("conname") + " " + rs.getString("condef"));
                }
            }
        }
        return cons;
    }

    private String readTableComment(Connection conn, String schema, String table) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(TABLE_COMMENT_SQL)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private List<String> list(Connection conn, String sql, String... params) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString(1));
                }
            }
        }
        return rows;
    }

    private static String commentOn(String objectKind, String qualifiedName, String text) {
        return "COMMENT ON " + objectKind + " " + qualifiedName + " IS '" + text.replace("'", "''") + "';";
    }

    /** 一条列定义（名称/渲染类型/可空/默认值/注释），render 出 CREATE TABLE 内联行。 */
    private record Column(String name, String type, boolean nullable, String defaultVal, String comment) {
        String render() {
            StringBuilder line = new StringBuilder(name).append(' ').append(type);
            if (!nullable) {
                line.append(" NOT NULL");
            }
            if (defaultVal != null) {
                line.append(" DEFAULT ").append(defaultVal);
            }
            return line.toString();
        }
    }

    /** 把 purge 的 IOException 包成运行时异常穿透 lambda；最终仍走 run() 的非 0 退出路径。 */
    private static final class UncheckedIoException extends RuntimeException {
        UncheckedIoException(String message, IOException cause) {
            super(message, cause);
        }
    }
}
