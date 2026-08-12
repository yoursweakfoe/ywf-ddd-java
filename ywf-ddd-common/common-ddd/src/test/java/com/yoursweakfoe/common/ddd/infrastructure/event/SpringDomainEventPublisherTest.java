package com.yoursweakfoe.common.ddd.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import com.yoursweakfoe.common.ddd.domain.event.DomainEvent;
import com.yoursweakfoe.common.ddd.infrastructure.event.SpringDomainEventPublisher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private SpringDomainEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SpringDomainEventPublisher(applicationEventPublisher);
    }

    @Test
    void publish_delegatesToSpringPublisher() {
        DomainEvent event = new TestDomainEvent();
        publisher.publish(event);
        verify(applicationEventPublisher).publishEvent(event);
    }

    @Test
    void publishAll_iteratesAndPublishes() {
        DomainEvent event1 = new TestDomainEvent();
        DomainEvent event2 = new TestDomainEvent();
        publisher.publishAll(List.of(event1, event2));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(event1, event2);
    }

    @Test
    void publish_null_silentlyIgnored() {
        assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publishAll_null_silentlyIgnored() {
        assertThatCode(() -> publisher.publishAll(null)).doesNotThrowAnyException();
        verifyNoInteractions(applicationEventPublisher);
    }

    /** Minimal test event */
    private static class TestDomainEvent extends DomainEvent {}
}
