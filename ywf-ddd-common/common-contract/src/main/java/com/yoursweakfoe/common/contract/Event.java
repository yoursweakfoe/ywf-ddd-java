package com.yoursweakfoe.common.contract;

/**
 * 应用层事件标记接口 —— 标识一个对象为外部事件通知（CQE 中的 Event）。
 *
 * <p>Event 表达"已经发生的事实"，系统需要对此作出响应。
 * 与领域事件（DomainEvent）的区别：
 *
 * <ul>
 *   <li>DomainEvent —— 领域内部产生，聚合根注册，进程内发布
 *   <li>Event（本接口） —— 外部进来，如 MQ 消息、其他微服务通知、Webhook 回调
 * </ul>
 *
 * <p>实现类应命名为 {@code XxxEvent}，如 {@code PaymentCompletedEvent}。
 *
 * @see Command
 * @see Query
 */
public interface Event {}
