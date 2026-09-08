package com.yoursweakfoe.dbmigration.config;

import java.sql.Connection;
import java.sql.Statement;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Liquibase 账表 schema 的引导件：账表先于任何变更集建立（鸡生蛋问题），而实证表明
 * 两个「想当然」的方案都不成立——
 * <ul>
 *   <li>SpringLiquibase（liquibase-core 5.0.3）<b>不会</b>自动创建 liquibase-schema
 *       指向的 schema：schema 缺失时启动直接 PSQLException；</li>
 *   <li>Boot 的 {@code spring.sql.init}（schema.sql）<b>不保证</b>先于 Liquibase bean
 *       执行（Boot 4.1 亦已无 3.x 的 MigrationStrategy 钩子）——实测无效。</li>
 * </ul>
 *
 * <p>本件因此自带确定性装配：bootstrap bean 幂等 {@code CREATE SCHEMA IF NOT EXISTS}，
 * 并用 {@link BeanFactoryPostProcessor} 给自动配置的 {@code liquibase} bean 显式挂
 * dependsOn——时序契约由 Bean 定义保证，不赌自动配置的内部顺序。
 *
 * <p>为何拼接而非参数化：CREATE SCHEMA 不接受占位符（DDL 标识符位）；schema 名来自
 * 配置而非用户输入，仍以标识符白名单硬校验兜底（同时挡住注入与拼写错误）。
 */
@Configuration(proxyBeanMethods = false)
public class LiquibaseSchemaBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseSchemaBootstrapConfig.class);

    /** PG 合法小写标识符（≤63 字符），兼作注入防线 */
    private static final Pattern SCHEMA_IDENT = Pattern.compile("[a-z][a-z0-9_]{0,62}");

    static final String BOOTSTRAP_BEAN = "liquibaseSchemaBootstrap";

    /** 自动配置 Liquibase bean 的 bean name（实证于 Boot 4.1 失败日志）。 */
    static final String LIQUIBASE_BEAN = "liquibase";

    @Bean(name = BOOTSTRAP_BEAN)
    InitializingBean liquibaseSchemaBootstrap(DataSource dataSource,
                                              @Value("${spring.liquibase.liquibase-schema}") String schema) {
        return () -> {
            if (!SCHEMA_IDENT.matcher(schema).matches()) {
                throw new IllegalArgumentException("非法账表 schema 名（仅小写标识符）: " + schema);
            }
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            }
            log.info("账表 schema 引导完成: {}", schema);
        };
    }

    @Bean
    static BeanFactoryPostProcessor liquibaseDependsOnSchemaBootstrap() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            if (!(beanFactory instanceof BeanDefinitionRegistry registry)
                    || !registry.containsBeanDefinition(LIQUIBASE_BEAN)) {
                return; // liquibase 被关闭/条件不满足：不越权
            }
            var definition = beanFactory.getBeanDefinition(LIQUIBASE_BEAN);
            // 未挂过 dependsOn 时 getDependsOn 返回 null（非空数组）——实证 NPE 教训
            String[] existing = definition.getDependsOn() == null ? new String[0] : definition.getDependsOn();
            for (String dep : existing) {
                if (BOOTSTRAP_BEAN.equals(dep)) {
                    return;
                }
            }
            String[] updated = new String[existing.length + 1];
            System.arraycopy(existing, 0, updated, 0, existing.length);
            updated[existing.length] = BOOTSTRAP_BEAN;
            definition.setDependsOn(updated);
        };
    }
}
