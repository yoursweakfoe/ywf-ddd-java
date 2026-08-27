package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainEventCodec — 事件载荷编解码与身份重建")
class DomainEventCodecTest {

    private final DomainEventCodec codec = new DomainEventCodec();

    @Test
    @DisplayName("write→read 往返：业务载荷一致，且 eventId/occurredOn 被行身份重建（非重新生成）")
    void roundTrip_restoresOriginalIdentity() {
        UUID orderId = UUID.randomUUID();
        OrderPlacedEvent original = new OrderPlacedEvent(orderId, new BigDecimal("99.95"));
        Instant originalOccurrence = original.getOccurredOn();

        String payload = codec.write(original);
        // 模拟 Outbox 重投递：反序列化必然产生新实例、临时身份，随后由 codec 以行身份重建
        DomainEvent restored = codec.read(
                OrderPlacedEvent.class.getName(), payload, original.getEventId(), originalOccurrence);

        assertThat(restored).isInstanceOf(OrderPlacedEvent.class).isNotSameAs(original);
        OrderPlacedEvent typed = (OrderPlacedEvent) restored;
        assertThat(typed.getOrderId()).isEqualTo(orderId);
        assertThat(typed.getTotalAmount()).isEqualByComparingTo(new BigDecimal("99.95"));
        assertThat(typed.getEventId()).isEqualTo(original.getEventId());
        assertThat(typed.getOccurredOn()).isEqualTo(originalOccurrence);
    }

    @Test
    @DisplayName("身份重建覆盖载荷中的身份字段：行身份是唯一权威来源")
    void roundTrip_rowIdentityWinsOverPayloadIdentity() {
        OrderPlacedEvent original = new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.ONE);
        String payload = codec.write(original);

        // 重投递时行身份与载荷内身份不一致（理论上不会发生，钉死权威序仍须覆盖）
        UUID rowId = UUID.randomUUID();
        Instant rowOccurrence = Instant.parse("2020-01-01T00:00:00Z");
        DomainEvent restored = codec.read(OrderPlacedEvent.class.getName(), payload, rowId, rowOccurrence);

        assertThat(restored.getEventId()).isEqualTo(rowId);
        assertThat(restored.getOccurredOn()).isEqualTo(rowOccurrence);
    }

    @Test
    @DisplayName("read：eventType 非 DomainEvent 子类 → IllegalStateException（防任意类反序列化）")
    void read_rejectsNonDomainEventType() {
        assertThatThrownBy(() -> codec.read(
                "java.lang.String", "\"x\"", UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not a DomainEvent subclass");
    }

    @Test
    @DisplayName("read：未知类名 → IllegalStateException")
    void read_unknownClass_throws() {
        assertThatThrownBy(() -> codec.read(
                "com.nope.Missing", "{}", UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("class not found");
    }

    @Test
    @DisplayName("read：畸形载荷 → IllegalStateException（携带事件类型上下文）")
    void read_malformedPayload_throws() {
        assertThatThrownBy(() -> codec.read(
                OrderPlacedEvent.class.getName(), "{not json", UUID.randomUUID(), Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to deserialize outbox payload");
    }
}
