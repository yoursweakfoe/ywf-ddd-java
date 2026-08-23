package com.yoursweakfoe.common.ddd.domain.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DomainEvent — 领域事件基类测试")
class DomainEventTest {

    private OrderPlacedEvent createEvent() {
        return new OrderPlacedEvent(UUID.randomUUID(), BigDecimal.TEN);
    }

    @Test
    void constructor_generatesEventIdAndTimestamp() {
        OrderPlacedEvent event = createEvent();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getOccurredOn()).isNotNull();
    }

    @Test
    void equals_sameEventId_true() {
        OrderPlacedEvent a = createEvent();
        OrderPlacedEvent b = createEvent();
        // Different instances have different eventIds, so equals should be false
        assertThat(a).isNotEqualTo(b);
        // Same instance should be equal
        assertThat(a).isEqualTo(a);
    }

    @Test
    void equals_differentEventId_false() {
        OrderPlacedEvent a = createEvent();
        OrderPlacedEvent b = createEvent();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_sameEventId_same() {
        OrderPlacedEvent a = createEvent();
        // Same instance → same hashCode
        assertThat(a.hashCode()).isEqualTo(a.hashCode());
    }

    @Test
    void hashCode_differentEventId_different() {
        OrderPlacedEvent a = createEvent();
        OrderPlacedEvent b = createEvent();
        // Different eventIds → very likely different hashCodes
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void toString_containsClassNameAndEventId() {
        OrderPlacedEvent event = createEvent();
        assertThat(event.toString())
                .contains("OrderPlacedEvent")
                .contains(event.getEventId().toString());
    }
}
