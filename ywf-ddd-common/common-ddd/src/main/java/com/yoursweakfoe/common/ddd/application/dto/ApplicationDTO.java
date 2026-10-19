package com.yoursweakfoe.common.ddd.application.dto;

/**
 * 应用层内部视图对象标记接口 —— 标识 application 层承载用例数据的 DTO。
 *
 * <p>位于 {@code application/{aggregate}/dto/}，两类 DTO 均实现本标记：
 * <ul>
 *   <li>写侧 DTO（如 {@code OrderDTO}）—— 写操作用例执行后聚合根状态的完整投影，可含乐观锁版本</li>
 *   <li>读侧 DTO（如 {@code OrderViewDTO}）—— 读侧投影（PO → DTO 直接投影，绕过 domain），纯读视图</li>
 * </ul>
 *
 * <p>本接口为<strong>空标记</strong>：价值在「标识应用层内部视图身份」（供架构规则/ArchUnit
 * 识别与约束），而非约束字段形状——字段集因业务用例而异，抽成接口字段只会退化成
 * {@code Object} / {@code Map}。与 common-contract 的 {@code CO} 标记对偶：
 * DTO 是 application 层<strong>内部</strong>视图（可含 version / 审计字段），CO 是<strong>对外</strong>
 * 契约输出（经 Presenter 清洗后暴露）。
 *
 * <h3>命名说明</h3>
 * <p>业务类保持自身命名（{@code XxxDTO} / {@code XxxViewDTO} / 嵌套项如 {@code OrderItemDTO}），
 * 仅顶层 DTO 类实现本标记（嵌套类随外层定型，不重复标记）。
 *
 * @see com.yoursweakfoe.common.contract.dto.co.CO
 * @see com.yoursweakfoe.common.ddd.application.assembler.BasicAssembler
 * @see com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter
 */
public interface ApplicationDTO {
}
