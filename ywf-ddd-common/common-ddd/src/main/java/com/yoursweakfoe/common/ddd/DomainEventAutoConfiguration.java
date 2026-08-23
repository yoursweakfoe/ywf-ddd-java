package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.infrastructure.event.domain.InProcessDomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * DDD 领域事件自动配置。
 *
 * <p>独立于 MyBatis-Plus，注册领域事件的默认进程内发布器 {@link InProcessDomainEventPublisher}
 * （桥接 Spring {@link ApplicationEventPublisher}）。<strong>不设任何 MyBatis 门控</strong>——
 * 事件发布是纯 Spring 能力，消费方即便不引入 MyBatis-Plus 持久化，只要引入 common-ddd
 * 即可获得领域事件发布能力。
 *
 * <p>业务侧可用自己的 {@link DomainEventPublisher} 实现替换默认（{@code @ConditionalOnMissingBean}）：
 *
 * <pre>{@code
 * @Bean
 * public DomainEventPublisher myPublisher() {
 *     return event -> { ... };  // 可靠化 / 跨进程发布
 * }
 * }</pre>
 *
 * @see DomainEventPublisher
 * @see InProcessDomainEventPublisher
 */
@AutoConfiguration
public class DomainEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public InProcessDomainEventPublisher inProcessDomainEventPublisher(
            ApplicationEventPublisher applicationEventPublisher) {
        return new InProcessDomainEventPublisher(applicationEventPublisher);
    }
}