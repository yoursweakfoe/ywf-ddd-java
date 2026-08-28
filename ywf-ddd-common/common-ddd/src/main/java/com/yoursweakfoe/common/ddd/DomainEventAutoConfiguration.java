package com.yoursweakfoe.common.ddd;

import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
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
 * <p><strong>扩展边界（全链路 Outbox 规范）</strong>：默认实现即唯一受支持形态，不提供跨进程扩展——
 * 本 Bean 由框架领域排空器（{@code OutboxRelay} 领域实例）在排空事务内调用，此处直发 MQ
 * 无法与「领域行标记完成」原子提交（重新打开 dual-write 窗口）。跨服务边界一律经集成 Outbox
 * （{@code IntegrationEventOutboxStore} + 框架集成排空器）。
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