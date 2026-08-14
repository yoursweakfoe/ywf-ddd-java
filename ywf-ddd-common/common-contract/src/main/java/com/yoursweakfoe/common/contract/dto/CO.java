package com.yoursweakfoe.common.contract.dto;

/**
 * 客户端对象标记接口 —— 标识一个对象为出站返回 DTO（Client Object）。
 *
 * <p>CO 是应用层向调用方返回的契约输出对象，由 Presenter 从 DTO 呈现而来，
 * 与 Command（写意图）/ Query（读请求）/ Event（外部通知）共同构成 CQRS 契约类型体系。
 *
 * <p>实现类应命名为 {@code XxxCO}。
 *
 * <p>基础设施层可基于此接口做统一处理（日志脱敏、序列化增强、统一响应包装等）。
 *
 * @see Command
 * @see Query
 * @see Event
 */
public interface CO {}
