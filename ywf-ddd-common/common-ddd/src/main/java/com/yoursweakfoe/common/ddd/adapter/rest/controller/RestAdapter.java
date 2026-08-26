package com.yoursweakfoe.common.ddd.adapter.rest.controller;

/**
 * REST 入口适配器标记接口 —— 标识 adapter 层实现 contract Controller 契约接口的 REST 端点。
 *
 * <p>位于 {@code adapter/rest/controller/}——包结构采用<strong>「协议伞 / 角色」两级式</strong>：
 * {@code rest} 为协议伞（HTTP 面），{@code controller} 为角色段；与
 * {@code task.scheduler}、{@code event.consumer} 完全对称，三类入口在目录树上等距对齐。
 * 实现类为 {@code @RestController}（如 {@code OrderControllerImpl}
 * {@code implements OrderController, RestAdapter}）。本标记将这类组件显式定型为
 * <strong>REST 入口适配器</strong>（Ports &amp; Adapters 中的 driving adapter）：纯透传
 * （协议参数 → Command/Query 包装 → 调用 ApplicationService → 返回 CO），不含业务逻辑。
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识 REST 入口角色」（供架构规则/ArchUnit 识别），
 * 而非约束方法签名——端点方法集已由 contract 层的 {@code XxxController} 契约接口完整承载
 * （HTTP 映射 + 方法签名 + 文档注解的单一事实源），本标记不重复声明。
 *
 * <h3>与 contract Controller 契约接口的分工</h3>
 * <table>
 *   <tr><th>接口</th><th>归属</th><th>承载</th></tr>
 *   <tr><td>{@code XxxController}（contract）</td><td>contract/{agg}/adapter/rest</td><td>HTTP 面：方法签名 + 映射注解 + 文档注解（重契约，ADR-0003）</td></tr>
 *   <tr><td>{@code RestAdapter}（本接口）</td><td>common-ddd/adapter/rest/controller</td><td>角色身份：adapter 层 REST 入口，供架构规则定位（与 contract 接口互补，不重复）</td></tr>
 * </table>
 *
 * <p>实现类同时实现两者：{@code OrderControllerImpl implements OrderController, RestAdapter}——
 * contract 接口定义「对外长什么样」，本标记声明「这是 adapter 层 REST 入口」。
 *
 * <h3>三类入口包结构对照（伞 / 角色两级式）</h3>
 * <table>
 *   <tr><th>协议伞</th><th>角色段</th><th>标记接口</th><th>驱动源</th><th>架构规则</th></tr>
 *   <tr><td>{@code rest}</td><td>{@code controller}</td><td>{@code RestAdapter}（本接口）</td><td>HTTP 请求</td><td>R8a / R8b</td></tr>
 *   <tr><td>{@code task}</td><td>{@code scheduler}</td><td>{@code ScheduledAdapter}</td><td>时间类调度</td><td>R14a / R14b</td></tr>
 *   <tr><td>{@code event}</td><td>{@code consumer}</td><td>{@code IntegrationEventConsumer}</td><td>MQ 消息</td><td>R9a / R9b</td></tr>
 * </table>
 *
 * @see com.yoursweakfoe.common.ddd.application.service.ApplicationService
 * @see com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter
 * @see com.yoursweakfoe.common.ddd.adapter.event.consumer.IntegrationEventConsumer
 */
public interface RestAdapter {
}
