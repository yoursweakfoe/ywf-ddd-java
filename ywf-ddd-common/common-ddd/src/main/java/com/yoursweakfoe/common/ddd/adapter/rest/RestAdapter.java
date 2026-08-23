package com.yoursweakfoe.common.ddd.adapter.rest;

/**
 * REST 入口适配器标记接口 —— 标识 adapter 层实现 contract Controller 契约接口的 REST 端点。
 *
 * <p>位于 {@code adapter/rest/}，实现类为 {@code @RestController}（如 {@code OrderControllerImpl}
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
 *   <tr><td>{@code XxxController}（contract）</td><td>contract/adapter/rest</td><td>HTTP 面：方法签名 + 映射注解 + 文档注解（重契约，ADR-0003）</td></tr>
 *   <tr><td>{@code RestAdapter}（本接口）</td><td>common-ddd/adapter/rest</td><td>角色身份：adapter 层 REST 入口，供架构规则定位（与 contract 接口互补，不重复）</td></tr>
 * </table>
 *
 * <p>实现类同时实现两者：{@code OrderControllerImpl implements OrderController, RestAdapter}——
 * contract 接口定义「对外长什么样」，本标记声明「这是 adapter 层 REST 入口」。
 *
 * <h3>命名说明</h3>
 * <p>不命名为 {@code Controller}：与 contract 层业务契约接口（{@code XxxController}）及 Spring
 * {@code @Controller} 概念过宽/易混淆；{@code RestAdapter} 精确表达「REST 驱动适配器」角色。
 *
 * @see com.yoursweakfoe.common.ddd.application.service.ApplicationService
 * @see com.yoursweakfoe.common.ddd.adapter.event.consumer.IntegrationEventConsumer
 */
public interface RestAdapter {
}
