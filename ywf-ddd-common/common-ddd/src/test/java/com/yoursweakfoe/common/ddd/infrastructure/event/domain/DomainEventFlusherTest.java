package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderItem;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.DomainEventOutboxStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * DomainEventFlusher 测试 —— 全链路 Outbox 规范的交付语义：
 * ① 有 Outbox 只捕获（先清后捕，不做进程内派发）；② 无 Outbox 但有事件 → fail-fast 抛错；
 * ③ 无事件（非聚合 / 空列表）静默无操作。
 */
@DisplayName("DomainEventFlusher — 全链路 Outbox 捕获（事件强制要求 Outbox，无直发降级）")
class DomainEventFlusherTest {

    /** 记录型 DomainEventOutboxStore 测试替身：验证冲刷编排（捕获语义） */
    static final class RecordingDomainEventOutboxStore implements DomainEventOutboxStore {
        final List<DomainEvent> captured = new ArrayList<>();

        @Override
        public void appendAll(List<DomainEvent> events) {
            captured.addAll(events);
        }
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<DomainEventOutboxStore> storeProvider(DomainEventOutboxStore s) {
        ObjectProvider<DomainEventOutboxStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(s);
        return provider;
    }

    private Order orderWithPlacedEvent() {
        Order order = new Order(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(new OrderItem("prod-1", 1, BigDecimal.ONE)),
                BigDecimal.TEN, "cust-1");
        order.place();
        return order;
    }

    private Order orderWithoutEvents() {
        return new Order(UUID.randomUUID(), OrderStatus.PENDING,
                List.of(new OrderItem("prod-1", 1, BigDecimal.ONE)),
                BigDecimal.TEN, "cust-1");
    }

    @Test
    @DisplayName("有 Outbox：只捕获（先清后捕），不做任何进程内派发")
    void outboxPresent_capturesOnly() {
        RecordingDomainEventOutboxStore store = new RecordingDomainEventOutboxStore();
        DomainEventFlusher flusher = new DomainEventFlusher(storeProvider(store));

        Order order = orderWithPlacedEvent();
        UUID originalEventId = order.getDomainEvents().get(0).getEventId();

        flusher.publishAndClear(order);

        assertThat(order.getDomainEvents()).isEmpty();              // 先清后捕
        assertThat(store.captured).hasSize(1);                      // 已同事务捕获（锚点由实现担保）
        assertThat(store.captured.get(0).getEventId()).isEqualTo(originalEventId);
    }

    @Test
    @DisplayName("外部事件列表（删除工厂路径）：同样只捕获入箱")
    void publishAll_outboxPresent_capturesOnly() {
        RecordingDomainEventOutboxStore store = new RecordingDomainEventOutboxStore();
        DomainEventFlusher flusher = new DomainEventFlusher(storeProvider(store));
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.ONE);

        flusher.publishAll(List.of(event));

        assertThat(store.captured).containsExactly(event);
    }

    @Test
    @DisplayName("fail-fast：无 Outbox 但有事件 → 抛 IllegalStateException（回滚业务写入）")
    void noOutbox_withEvents_throws() {
        DomainEventFlusher flusher = new DomainEventFlusher(storeProvider(null));

        assertThatThrownBy(() -> flusher.publishAndClear(orderWithPlacedEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DomainEventOutboxStore");

        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.ONE);
        assertThatThrownBy(() -> flusher.publishAll(List.of(event)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("无事件（聚合无已注册事件 / 空列表 / null）：静默无操作，不抛异常")
    void noEvents_noOp_evenWithoutOutbox() {
        DomainEventFlusher flusher = new DomainEventFlusher(storeProvider(null));

        assertThatCode(() -> flusher.publishAndClear(orderWithoutEvents()))
                .doesNotThrowAnyException();
        assertThatCode(() -> flusher.publishAll(List.of()))
                .doesNotThrowAnyException();
        assertThatCode(() -> flusher.publishAll(null))
                .doesNotThrowAnyException();
    }
}
