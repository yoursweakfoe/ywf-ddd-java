package com.yoursweakfoe.common.ddd.infrastructure.event.outbox;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.List;

/**
 * Outbox 捕获契约（SPI）—— 框架对领域事件可靠性的唯一担保面。
 *
 * <p><strong>框架领地边界</strong>：本契约只回答一个问题——「聚合状态已提交 ⇒ 事件必然已落库」。
 * 实现必须保证 {@link #appendAll} 与业务写入共享同一事务边界（由调用方在事务内调用、
 * 实现不自行开启新事务）；业务回滚则事件随行回滚。
 *
 * <p><strong>框架不提供任何缺省实现</strong>——真实业务会按查询效率 / 单表洁净拆出多张
 * 消息表（按领域、甚至按业务环节），各表列结构与处理机制互不相通，通用缺省表是伪需求。
 * 业务侧提供本接口的 Bean 即激活捕获路径（{@code DomainEventFlusher} 自动接入）；
 * 未提供时事件回退直发路径（提交后进程内派发，at-most-once）。
 * 表结构参考 {@code common-ddd/src/main/resources/sql/ddd_outbox.example.sql}。
 *
 * <p><strong>投递不是框架领地</strong>：入箱之后的扫描、认领、派发、重试、死信由业务侧的
 * 排空器（relay）或生态成熟方案（MQ 事务消息 / CDC 尾日志 / Modulith 事件发布注册表等）
 * 承担——投递语义由投递拓扑决定，框架不做通用化假设、不内置投递器。
 * 业务实现本接口时可自由选择表结构（单表 / 按聚合分表 / 按业务环节分表）、列映射与
 * 序列化方式。
 *
 * <p><strong>身份契约（捕获与投递之间唯一的跨边界约定）</strong>：
 * {@link DomainEvent#getEventId()} 是事件的幂等键。无论落库形态如何，重投递必须保持
 * eventId 稳定——消费端按它去重（at-least-once 是 Outbox 模式的固有语义）。
 * 载荷序列化可复用 {@link DomainEventCodec}（信封四元组：eventId / eventType / payload /
 * occurredOn，身份跨重投稳定）。
 *
 * <p>线程安全要求：实现必须支持多线程并发调用（多个请求线程可能同时入箱）。
 */
public interface OutboxStore {

    /**
     * 批量捕获领域事件（必须在当前业务事务内完成写入，不自行管理事务）。
     *
     * <p>事件以何种形态落库（JSON 载荷 / 拆列 / 分表路由）由实现自行决定。
     *
     * @param events 待捕获事件（非空列表）
     */
    void appendAll(List<DomainEvent> events);
}
