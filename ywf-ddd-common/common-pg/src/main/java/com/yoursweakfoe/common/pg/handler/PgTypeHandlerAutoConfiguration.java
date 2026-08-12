package com.yoursweakfoe.common.pg.handler;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.type.BaseTypeHandler;
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
 * <p>业务侧无需再手动配置 {@code mybatis-plus.type-handlers-package}。
 */
@AutoConfiguration
@ConditionalOnClass(BaseTypeHandler.class)
public class PgTypeHandlerAutoConfiguration implements ConfigurationCustomizer {

    private static final String TYPE_HANDLER_PACKAGE =
            "com.yoursweakfoe.common.pg.handler";

    @Override
    public void customize(MybatisConfiguration configuration) {
        configuration.getTypeHandlerRegistry().register(TYPE_HANDLER_PACKAGE);
    }
}
