package com.yoursweakfoe.common.contract.dto.event.integration;

/**
 * 集成事件标记接口 —— 标识一个对象为跨服务边界事件契约（CQRS 中的 IntegrationEvent）。
 *
 * <p>IntegrationEvent 表达"已经发生、需要跨服务协作的事实"，是服务间契约的一部分，
 * 既可以是<strong>出站</strong>（本服务发布、其他服务消费），也可以是<strong>入站</strong>
 * （其他服务发布、本服务消费）。
 *
 * <p>与领域事件（DomainEvent）的区别：
 *
 * <ul>
 *   <li>DomainEvent —— 领域内部产生，仅进程内消费，不对外
 *   <li>IntegrationEvent（本接口） —— 跨服务边界，经消息中间件传输，出入站均为它
 * </ul>
 *
 * <p>实现类应命名为 {@code XxxIntegrationEvent}，位于 {@code contract/{agg}/dto/event/integration/}，
 * 如 {@code PaymentCompletedIntegrationEvent}；载荷字段视为长期对外契约，增删须考虑兼容性。
 *
 * @see com.yoursweakfoe.common.contract.dto.command.Command
 * @see com.yoursweakfoe.common.contract.dto.query.Query
 * @see com.yoursweakfoe.common.contract.dto.co.CO
 */
public interface IntegrationEvent {}
