package com.yoursweakfoe.common.integration.pg;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集成测试 PG 基座的三段生命周期（TC-9 框架轨；法案 2026-09-common-it-consolidation）。
 *
 * <p>① 自建——经 maintenance 库 {@code postgres} 裸连接，专属测试库缺失则 CREATE DATABASE，零人工前置；
 * ② 复位——每 JVM 首次触库时 DROP/CREATE {@code integration_test} 与 {@code integration_test_vacant}
 * 双 schema，并在前者会话内执行 fixture DDL（classpath:schema.sql），静态门闩保证恰一次；
 * ③ 清空——shutdown hook 尽力 DROP 双 schema；崩溃残留由下次启动复位兜底。
 *
 * <p>连接参数默认值是 {@code application-test.yml} 同名占位的字面镜像（本件先于 Spring 上下文运行、
 * 无法注入配置；两处漂移即回同 PR 修正——试验场纪律）。
 */
public final class PgTestSupport {

    /** 业务表所在 schema（master 源 currentSchema 指向它）。 */
    public static final String SCHEMA = "integration_test";
    /** 空 schema（second 源 currentSchema 指向它——路由脱靶教例的真实性来源）。 */
    public static final String SCHEMA_VACANT = "integration_test_vacant";

    private static final String HOST = env("DB_IT_HOST", "localhost");
    private static final String PORT = env("DB_IT_PORT", "5432");
    private static final String DB = env("DB_IT_DB", "ddd_framework_test");
    private static final String USER = env("DB_IT_USER", "ywf");
    private static final String PASSWORD = env("DB_IT_PASSWORD", "ywf-local-123");
    private static final String BASE_URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/";

    private static final AtomicBoolean READY = new AtomicBoolean();

    private PgTestSupport() {
    }

    /**
     * {@code @BeforeAll} 入口（幂等）。一切真触库的测试必须先于其 SQL 执行调用本法。
     */
    public static void ensureReady() {
        if (READY.get()) {
            return;
        }
        synchronized (PgTestSupport.class) {
            if (READY.get()) {
                return;
            }
            // 标识符不可参数化，env 覆写值先过白名单闸（防测试基建自伤式拼接注入）
            requireSafeIdentifier(DB);
            requireSafeIdentifier(USER);
            try {
                createDatabaseIfMissing();
                resetSchemas();
                READY.set(true);
                Runtime.getRuntime().addShutdownHook(new Thread(PgTestSupport::dropQuietly, "pg-it-cleanup"));
            } catch (Exception e) {
                throw new IllegalStateException(
                        "PG 集成测试基座初始化失败——postgres 容器是否在跑？(" + BASE_URL + ")", e);
            }
        }
    }

    private static void createDatabaseIfMissing() throws Exception {
        try (Connection admin = DriverManager.getConnection(BASE_URL + "postgres", USER, PASSWORD);
             Statement st = admin.createStatement()) {
            try (ResultSet rs = st.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + DB + "'")) {
                if (!rs.next()) {
                    st.execute("CREATE DATABASE " + DB);
                }
            }
        }
    }

    private static void resetSchemas() throws Exception {
        try (Connection c = DriverManager.getConnection(BASE_URL + DB + "?currentSchema=" + SCHEMA, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
            st.execute("DROP SCHEMA IF EXISTS " + SCHEMA_VACANT + " CASCADE");
            st.execute("CREATE SCHEMA " + SCHEMA);
            st.execute("CREATE SCHEMA " + SCHEMA_VACANT);
            try (InputStream in = PgTestSupport.class.getResourceAsStream("/schema.sql")) {
                if (in == null) {
                    throw new IllegalStateException("classpath:/schema.sql 缺失——fixture DDL 未随迁");
                }
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    StringBuilder buf = new StringBuilder();
                    char[] chunk = new char[4096];
                    int n;
                    while ((n = r.read(chunk)) > 0) {
                        buf.append(chunk, 0, n);
                    }
                    for (String ddl : buf.toString().split(";")) {
                        String stmt = stripComments(ddl);
                        if (!stmt.isBlank()) {
                            st.execute(stmt);
                        }
                    }
                }
            }
        }
    }

    /** 剥行注释后判空白（schema.sql 头部是纯注释行，split 后首段即注释体）。 */
    private static String stripComments(String ddl) {
        StringBuilder sb = new StringBuilder();
        for (String line : ddl.split("\n")) {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private static void dropQuietly() {
        try (Connection c = DriverManager.getConnection(BASE_URL + DB, USER, PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
            st.execute("DROP SCHEMA IF EXISTS " + SCHEMA_VACANT + " CASCADE");
        } catch (Exception ignored) {
            // 尽力而为：崩溃/网络残留下次启动复位兜底
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static void requireSafeIdentifier(String ident) {
        if (!ident.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("非白名单标识符，拒作 SQL 拼接: " + ident);
        }
    }
}
