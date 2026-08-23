package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件自动配置。
 *
 * <p>默认注册的拦截器（顺序即执行优先级）：
 * <ol>
 *   <li>{@link PaginationInnerInterceptor} — 物理分页（AUTO 模式，运行时自动识别数据库方言）</li>
 *   <li>{@link OptimisticLockerInnerInterceptor} — 乐观锁（仅对含 {@code @Version} 字段的实体生效）</li>
 *   <li>{@link BlockAttackInnerInterceptor} — 防全表 UPDATE/DELETE（无 WHERE 时阻断，始终开启）</li>
 * </ol>
 *
 * <p><strong>各拦截器的生效条件：</strong>
 * <ul>
 *   <li>分页 — 仅当 Mapper 方法传入 {@code Page} 参数时触发，普通查询不受影响</li>
 *   <li>乐观锁 — 仅当实体含 {@code @Version} 字段时触发，无该字段的实体不受影响</li>
 *   <li>防全表攻击 — 对所有无 WHERE 的 UPDATE/DELETE 生效；默认开启，
 *       可经 {@code ywf.ddd.mybatisplus.block-attack-enabled=false} 关闭（数据修复等场景），
 *       确需全表操作时请改用原生 MyBatis 编写 SQL</li>
 * </ul>
 *
 * <p><strong>分页上限：</strong>默认不限制；可经 {@code ywf.ddd.mybatisplus.pagination-max-limit}
 * 设单页条数上限（正值），超过上限的 pageSize 会被钳制，防止超大分页拖垮数据库。</p>
 *
 * <p><strong>数据变动记录：</strong>不注册 {@code DataChangeRecorderInnerInterceptor}。
 * 数据变更日志由业务代码在具体场景中手动记录（意图更明确，避免无差别 SELECT 开销）。
 *
 * <p>如果业务项目需要完全自定义拦截器组合，可自行声明
 * {@code MybatisPlusInterceptor} Bean 覆盖此默认配置（{@code @ConditionalOnMissingBean}）。
 *
 */
@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusPluginConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisPlusProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 分页（AUTO 模式：运行时自动识别数据库方言，仅传入 Page 参数时触发）
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor();
        if (properties.paginationMaxLimit() != null && properties.paginationMaxLimit() > 0) {
            pagination.setMaxLimit(properties.paginationMaxLimit());
        }
        interceptor.addInnerInterceptor(pagination);
        // 2. 乐观锁（仅对含 @Version 字段的实体生效）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 防全表更新/删除（无 WHERE 的 UPDATE/DELETE 直接抛异常，默认开启，可 opt-out）
        if (properties.blockAttackEnabled()) {
            interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        }
        return interceptor;
    }
}
