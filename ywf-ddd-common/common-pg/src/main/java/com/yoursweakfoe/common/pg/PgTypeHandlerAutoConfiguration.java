package com.yoursweakfoe.common.pg;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.BaseTypeHandler;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * PostgreSQL TypeHandler 自动配置。
 *
 * <p>引入 {@code common-pg} 依赖后自动将
 * {@code com.yoursweakfoe.common.pg.handler}
 * 包下的所有 TypeHandler 注册到 MyBatis TypeHandlerRegistry，
 * 实现 PO 实体类中 UUID / JSONB / 数组等字段的零配置自动映射。
 *
 * <p>业务侧无需再手动配置 {@code mybatis.type-handlers-package}。
 */
@AutoConfiguration
@ConditionalOnClass(BaseTypeHandler.class)
public class PgTypeHandlerAutoConfiguration implements ConfigurationCustomizer {

    private static final String TYPE_HANDLER_PACKAGE =
            "com.yoursweakfoe.common.pg.handler";

    @Override
    public void customize(Configuration configuration) {
        configuration.getTypeHandlerRegistry().register(TYPE_HANDLER_PACKAGE);
    }
}
