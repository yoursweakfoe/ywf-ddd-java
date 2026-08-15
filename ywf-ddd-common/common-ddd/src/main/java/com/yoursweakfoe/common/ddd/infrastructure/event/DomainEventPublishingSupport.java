package com.yoursweakfoe.common.ddd.infrastructure.event;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 领域事件发布支撑 —— 封装「先清后发」的领域事件发布契约。
 *
 * <p>从 {@code MybatisPersistence} 抽出，承担事件发布的单一职责：聚合根持久化成功后，
 * 由本类发布其已注册的领域事件（先清空事件列表再发布，保证即使监听器抛异常也不会重复发布）。
 *
 * <p>事件发布者缺失（容器中无 {@link DomainEventPublisher} Bean）时，丢弃事件并记录警告，不抛异常。
 */
@Slf4j
public class DomainEventPublishingSupport {

    private final DomainEventPublisher publisher;

    public DomainEventPublishingSupport(ObjectProvider<DomainEventPublisher> publisherProvider) {
        this.publisher = publisherProvider.getIfAvailable();
    }

    /**
     * 发布聚合根已注册的领域事件（先清后发）。
     *
     * <p>若 domain 不是聚合根（无事件暂存），静默无操作。
     */
    public void publishAndClear(Identifiable<?> domain) {
        if (publisher == null) {
            if (domain instanceof AggregateRoot<?> ar && !ar.getDomainEvents().isEmpty()) {
                log.warn(
                        "DomainEventPublisher not available, {} event(s) discarded for entity ID: {}",
                        ar.getDomainEvents().size(),
                        domain.getId());
            }
            return;
        }
        if (domain instanceof AggregateRoot<?> aggregateRoot) {
            List<DomainEvent> events = aggregateRoot.getDomainEvents();
            if (!events.isEmpty()) {
                List<DomainEvent> snapshot = List.copyOf(events);
                aggregateRoot.clearDomainEvents();
                publisher.publishAll(snapshot);
            }
        }
    }

    /**
     * 发布外部构造的领域事件列表（按 ID 删除的事件工厂路径使用）。
     *
     * <p>与 {@link #publishAndClear(Identifiable)} 一致的容错语义：
     * publisher 缺失时丢弃事件并记录警告，不抛异常。
     */
    public void publishAll(List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        if (publisher == null) {
            log.warn("DomainEventPublisher not available, {} event(s) discarded", events.size());
            return;
        }
        publisher.publishAll(events);
    }
}
