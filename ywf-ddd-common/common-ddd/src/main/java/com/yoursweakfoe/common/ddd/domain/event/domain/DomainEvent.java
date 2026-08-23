package com.yoursweakfoe.common.ddd.domain.event.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import lombok.Getter;

/**
 * 领域事件基类 —— 所有领域事件的抽象父类。
 *
 * <p>领域事件用于表达“领域中发生了某件有意义的事”，是聚合间解耦通信的核心机制。 聚合根通过
 * {@code AggregateRoot#registerEvent(DomainEvent)}
 * 注册事件，仓储层在持久化成功后自动发布。
 *
 * <p><strong>边界</strong>：领域事件<strong>仅在进程内</strong>消费，受众是 application 层的域内反应监听器
 * （{@code @EventListener}），不对外、不做跨服务序列化。需要跨服务通知时，由 application 层 Publisher
 * 将领域事件翻译为集成事件（IntegrationEvent）后投递 MQ。
 *
 * <p>每个事件包含：
 *
 * <ul>
 *   <li>{@link #eventId} — 事件唯一标识，用于幂等和去重
 *   <li>{@link #occurredOn} — 事件发生时间戳
 * </ul>
 *
 * <p>领域事件应当是<strong>不可变的</strong>，一旦创建不可修改。
 *
 * <p>
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * public class OrderPlacedEvent extends DomainEvent {
 *     private final UUID orderId;
 *     private final BigDecimal totalAmount;
 *
 *     public OrderPlacedEvent(UUID orderId, BigDecimal totalAmount) {
 *         super();
 *         this.orderId = orderId;
 *         this.totalAmount = totalAmount;
 *     }
 *     // getters ...
 * }
 * }</pre>
 *
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateRoot
 * @see DomainEventPublisher
 */
public abstract class DomainEvent {

    /** 事件唯一标识，用于幂等处理和事件去重 */
    @Getter
    private final UUID eventId;

    /** 事件发生的时间戳 */
    @Getter
    private final Instant occurredOn;

    /** 默认构造器 —— 自动生成 eventId 和 occurredOn。 */
    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
    }

    // ==================== 手写 toString / equals / hashCode（不使用 Lombok） ====================
    //
    // toString：Lombok @ToString 输出声明类名（DomainEvent），而 getClass().getSimpleName()
    //         是运行时多态的，始终输出实际子类名（如 OrderPlacedEvent），子类无需额外注解。
    //
    // equals：Lombok 生成 instanceof + canEqual() 模式（为继承链设计）。
    //        领域事件身份永远由 eventId 唯一决定，子类不应覆写 equals，
    //        getClass() 严格比较更简单直接，且天然禁止“父类实例 equals 子类实例”。

    @Override
    public String toString() {
        return "%s{eventId=%s, occurredOn=%s}".formatted(getClass().getSimpleName(), getEventId(), getOccurredOn());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEvent that = (DomainEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
