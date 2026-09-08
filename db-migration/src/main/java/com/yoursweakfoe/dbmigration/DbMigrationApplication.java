package com.yoursweakfoe.DbMigration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * db-migration 一次性迁移 Job 入口（执行模型移植自 omate-bi-flyway）。
 *
 * <p>执行序列：
 * <ol>
 *   <li>Spring Boot 启动，自动装配 DataSource 与 Liquibase（spring-boot-starter-liquibase）；</li>
 *   <li>Liquibase bean 初始化阶段自动执行 update：应用
 *       {@code classpath:db/changelog/} 下未执行的变更集（纯 SQL formatted）；</li>
 *   <li>{@link com.yoursweakfoe.DbMigration.snapshot.SchemaSnapshotRunner}（ApplicationRunner，
 *       恰在上下文刷新完成后运行 = AFTER_MIGRATE 语义）从库内自省取回 schema 事实现状，
 *       写入 {@code db/current/}；</li>
 *   <li>{@code SpringApplication.exit} 关闭上下文，进程按退出码自然结束——
 *       迁移或快照任一失败即非 0 退出，供 CI/K8s Job 短路。</li>
 * </ol>
 *
 * <p>设计要点：不引入 web、不驻留进程；数据源零默认值（主配置 fail-fast），
 * 本地画像经 dev profile 显式激活。
 */
@SpringBootApplication
public class DbMigrationApplication {

    public static void main(String[] args) {
        // exit 的退出码（含任一 bean 启动失败的异常路径）透传给容器/流水线运行时
        System.exit(SpringApplication.exit(SpringApplication.run(DbMigrationApplication.class, args)));
    }
}
