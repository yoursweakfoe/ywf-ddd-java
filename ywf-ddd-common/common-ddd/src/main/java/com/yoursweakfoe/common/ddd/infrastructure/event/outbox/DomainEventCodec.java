package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * 领域事件编解码器 —— Outbox 持久化路径上的序列化 / 反序列化 / 身份重建（reconstitution）。
 *
 * <h3>载荷格式自持</h3>
 * <p>编解码器持有<strong>自建的专用 {@link JsonMapper}</strong>，不复用消费方容器中的应用级
 * ObjectMapper：outbox 载荷是持久化锚点，其格式必须由框架单方面钉死，不随消费方的序列化
 * 偏好（命名策略 / 包含策略 / 模块注册）漂移——否则一次应用配置变更即可使历史在箱事件
 * 不可重放。字段可见性设为 {@code ANY}：载荷按<strong>声明字段</strong>捕获（而非依赖 getter
 * 惯例），事件字段无论有无 getter 均完整入箱。
 *
 * <h3>身份重建（为何需要反射）</h3>
 * <p>{@link DomainEvent} 的默认构造器在实例化时自动生成 {@code eventId}/{@code occurredOn}。
 * 反序列化必然经过构造器，产生的是临时身份；若不重建，同一条 outbox 记录每次重投递都会
 * 携带不同的 eventId，消费端按 eventId 幂等去重的契约即告失效。本类在反序列化后以
 * outbox 行身份（权威来源）覆盖临时身份，保证：
 * <em>outbox 行 id == 原始 eventId == 每次投递到达消费端的 eventId</em>。
 *
 * <p>覆盖经反射完成（与 Jackson / JPA 水合对象的标准手法同源）——{@link DomainEvent} 的
 * 字段在源码层面保持 {@code private final}（领域事件不可变是硬约束），基础设施层的重建
 * 不构成对业务代码的可变入口。
 *
 * <h3>消费方事件的兼容性要求</h3>
 * <p>重放依赖 Jackson 经事件的唯一业务构造器实例化（参数名绑定）。请保证事件满足其一：
 * <ul>
 *   <li>编译启用 {@code -parameters}（spring-boot-starter-parent 默认开启，无需额外配置）</li>
 *   <li>或提供 {@code protected} 无参构造器</li>
 * </ul>
 *
 * <p>线程安全：{@link JsonMapper} 线程安全，本类可在多线程间共享。
 */
public class DomainEventCodec {

    /** 身份字段反射句柄（静态缓存：setAccessible 一次，跨调用复用） */
    private static final Field EVENT_ID_FIELD = identityField("eventId");
    private static final Field OCCURRED_ON_FIELD = identityField("occurredOn");

    /** 自持载荷格式：字段可见性 ANY（完整捕获声明字段，不依赖 getter 惯例） */
    private final JsonMapper mapper = JsonMapper.builder()
            .changeDefaultVisibility(vc -> vc.withFieldVisibility(JsonAutoDetect.Visibility.ANY))
            .build();

    /** 序列化事件为 JSON 载荷。 */
    public String write(DomainEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event, e);
        }
    }

    /**
     * 反序列化并以 outbox 行身份重建事件。
     *
     * @param eventType  具体类全限定名（须为 {@link DomainEvent} 子类）
     * @param payload    JSON 载荷
     * @param eventId    outbox 行主键（= 原始 eventId，权威身份来源）
     * @param occurredOn 原始发生时间
     */
    public DomainEvent read(String eventType, String payload, UUID eventId, Instant occurredOn) {
        Class<? extends DomainEvent> eventClass = resolveEventClass(eventType);
        DomainEvent event;
        try {
            event = mapper.readValue(payload, eventClass);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to deserialize outbox payload for event type: " + eventType, e);
        }
        restoreIdentity(event, eventId, occurredOn);
        return event;
    }

    // ==================== 内部实现 ====================

    private static Class<? extends DomainEvent> resolveEventClass(String eventType) {
        Class<?> type;
        try {
            type = Class.forName(eventType, true, DomainEvent.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Outbox eventType class not found: " + eventType, e);
        }
        if (!DomainEvent.class.isAssignableFrom(type)) {
            throw new IllegalStateException("Outbox eventType is not a DomainEvent subclass: " + eventType);
        }
        @SuppressWarnings("unchecked")
        Class<? extends DomainEvent> eventClass = (Class<? extends DomainEvent>) type;
        return eventClass;
    }

    /** 以 outbox 行身份覆盖反序列化产生的临时身份（见类 Javadoc「身份重建」）。 */
    private static void restoreIdentity(DomainEvent event, UUID eventId, Instant occurredOn) {
        try {
            EVENT_ID_FIELD.set(event, eventId);
            OCCURRED_ON_FIELD.set(event, occurredOn);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to restore outbox event identity for: " + event, e);
        }
    }

    private static Field identityField(String name) {
        try {
            Field field = DomainEvent.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
