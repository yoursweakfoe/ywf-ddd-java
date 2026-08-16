package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Identifiable;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 领域事件冲刷器 —— 在聚合持久化成功后，把聚合根暂存的领域事件「先清后发」地冲刷出去。
 *
 * <p>从 {@code MybatisPersistence} 抽出，承担事件冲刷的单一职责：聚合根持久化成功后，
 * 由本类快照其已注册的领域事件、清空暂存、再逐个发布（先清后发，保证即使监听器抛异常也不会重复发布）。
 *
 * <p><strong>边界</strong>：本包（{@code infrastructure.event.domain}）仅负责<strong>领域事件</strong>的
 * 进程内投递（Spring {@code ApplicationEventPublisher}）。集成事件（IntegrationEvent）的收发不在此包：
 * 出站由 application 层 {@code Publisher} 投递（依赖 common-mq），入站由 adapter 层 {@code Consumer} 接收。
 *
 * <p>事件发布者缺失（容器中无 {@link DomainEventPublisher} Bean）时，丢弃事件并记录警告，不抛异常。
 */
@Slf4j
public class DomainEventFlusher {

    private final DomainEventPublisher publisher;

    public DomainEventFlusher(ObjectProvider<DomainEventPublisher> publisherProvider) {
        this.publisher = publisherProvider.getIfAvailable();
    }

    /**
     * 冲刷聚合根已注册的领域事件（先清后发）。
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
     * 冲刷外部构造的领域事件列表（按 ID 删除的事件工厂路径使用）。
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
