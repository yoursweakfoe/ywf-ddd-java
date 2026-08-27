package com.yoursweakfoe.common.ddd.infrastructure.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.domain.event.publisher.DomainEventPublisher;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import com.yoursweakfoe.common.ddd.fixtures.model.Order;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderItem;
import com.yoursweakfoe.common.ddd.fixtures.model.OrderStatus;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxStore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("DomainEventFlusher — 领地收缩后的交付语义（Outbox 只捕获；直发路径提交后派发）")
class DomainEventFlusherTest {

    /** 记录型 OutboxStore 测试替身：框架不提供缺省实现，测试自持最小替身验证编排 */
    static final class RecordingOutboxStore implements OutboxStore {
        final List<DomainEvent> captured = new ArrayList<>();

        @Override
        public void appendAll(List<DomainEvent> events) {
            captured.addAll(events);
        }
    }

    private DomainEventPublisher publisher;
    private RecordingOutboxStore store;

    @SuppressWarnings("unchecked")
    private ObjectProvider<DomainEventPublisher> pubProvider(DomainEventPublisher p) {
        ObjectProvider<DomainEventPublisher> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(p);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<OutboxStore> storeProvider(OutboxStore s) {
        ObjectProvider<OutboxStore> provider = mock(ObjectProvider.class);
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

    /** 手动触发已注册的 afterCommit 回调（模拟 Spring 事务提交点） */
    private static void fireAfterCommit() {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
    }

    @BeforeEach
    void setUp() {
        store = new RecordingOutboxStore();
        publisher = mock(DomainEventPublisher.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        try {
            TransactionSynchronizationManager.clearSynchronization();
        } catch (IllegalStateException ignored) {
            // 已清空
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("Outbox 路径·只捕获：事件入箱即止，不做任何进程内派发（投递归业务排空器）")
    void outboxPath_capturesOnly_neverPublishes() {
        DomainEventFlusher flusher = new DomainEventFlusher(pubProvider(publisher), storeProvider(store));

        Order order = orderWithPlacedEvent();
        UUID originalEventId = order.getDomainEvents().get(0).getEventId();

        flusher.publishAndClear(order);

        assertThat(order.getDomainEvents()).isEmpty();          // 先清后发契约不变
        assertThat(store.captured).hasSize(1);                  // 已同步捕获（同事务锚点由实现担保）
        assertThat(store.captured.get(0).getEventId()).isEqualTo(originalEventId);

        // 框架到此为止：即使触发提交回调，也不做任何派发
        fireAfterCommit();
        verify(publisher, never()).publish(any());
        verify(publisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("外部事件列表（删除工厂路径）：同样只捕获入箱")
    void publishAll_outboxPath_capturesOnly() {
        DomainEventFlusher flusher = new DomainEventFlusher(pubProvider(publisher), storeProvider(store));
        OrderPlacedEvent event = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.ONE);

        flusher.publishAll(List.of(event));

        assertThat(store.captured).containsExactly(event);
        verify(publisher, never()).publish(any());
        verify(publisher, never()).publishAll(any());
    }

    @Test
    @DisplayName("直发降级路径（业务未提供 OutboxStore）·事务内：afterCommit 才派发")
    void legacyPath_withinTx_publishesOnlyAfterCommit() {
        DomainEventFlusher flusher = new DomainEventFlusher(pubProvider(publisher), storeProvider(null));

        flusher.publishAndClear(orderWithPlacedEvent());
        verify(publisher, never()).publishAll(any());

        fireAfterCommit();
        // 直发路径走 DomainEventPublisher.publishAll（接口默认方法），mock 记录为单次 publishAll 调用
        verify(publisher).publishAll(any());
    }

    @Test
    @DisplayName("直发降级路径·无事务：立即派发")
    @SuppressWarnings("unchecked")
    void legacyPath_noTx_publishesImmediately() {
        DomainEventFlusher flusher = new DomainEventFlusher(pubProvider(publisher), storeProvider(null));
        TransactionSynchronizationManager.setActualTransactionActive(false);

        flusher.publishAndClear(orderWithPlacedEvent());

        ArgumentCaptor<List<DomainEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(publisher).publishAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("管线全缺失（publisher 与 outbox 均无）：丢弃事件不抛异常")
    void pipelineAbsent_discardsSilently() {
        DomainEventFlusher flusher = new DomainEventFlusher(pubProvider(null), storeProvider(null));

        assertThatCode(() -> flusher.publishAndClear(orderWithPlacedEvent()))
                .doesNotThrowAnyException();
    }
}
