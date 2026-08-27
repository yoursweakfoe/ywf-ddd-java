package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.mq;

import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.IntegrationEventSender;
import com.yoursweakfoe.common.ddd.infrastructure.event.outbox.OutboxEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 集成事件投递者日志占位实现 —— common-mq 模块建设前 {@link IntegrationEventSender} 的样例接入。
 *
 * <p>框架集成排空器认领 {@code ddd_integration_event_outbox} 行后调用本实现投递；
 * 本实现仅打印日志（不真正投 MQ），投递「成功」后由排空器标记该行完成。
 * 接入 RocketMQ / Kafka 时，以真实 sender（{@code @ConditionalOnMissingBean} 顶替）把
 * {@code envelope.messageId()} 置消息头供消费端幂等去重，{@code payload} 作为消息体。
 *
 * <p><strong>失败语义</strong>：真实 sender 投递失败应抛异常，排空器据此重试 / 转死信；
 * 本占位实现不抛异常（视为恒成功）。
 */
@Slf4j
@Component
public class LoggingIntegrationEventSender implements IntegrationEventSender {

    @Override
    public void send(OutboxEnvelope envelope) {
        log.info("[MQ placeholder] sending integration event: messageId={}, eventType={}, payload={}",
                envelope.messageId(), envelope.eventType(), envelope.payload());
    }
}
