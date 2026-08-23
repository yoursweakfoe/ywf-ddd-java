package com.yoursweakfoe.common.ddd.infrastructure.mybatis.handler;

/**
 * 当前操作用户提供者 SPI —— 为审计字段（createdBy / updatedBy）提供「谁在操作」。
 *
 * <p>框架<strong>不实现</strong>本接口、<strong>不引入</strong> security 依赖：
 * 业务的当前登录人来源（JWT claims / ThreadLocal 上下文 / 网关透传头）由业务侧或
 * common-security 模块提供适配实现。{@link BasicAutoFillHandler} 经
 * {@code ObjectProvider} 注入本接口——容器中存在实现 Bean 时才填充操作人字段，
 * 不存在时静默跳过（不影响时间字段填充）。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li><b>返回 null = 不填充</b>：匿名 / 无身份场景（如机器身份、定时任务）返回 {@code null}，
 *       填充器跳过操作人字段，不写 null 覆盖已有值。</li>
 *   <li><b>类型宽松</b>：返回类型不做死——账号用 {@code String}、工号用 {@code Long}
 *       均可，只要与 PO 中 {@code createdBy} / {@code updatedBy} 字段的声明类型一致。</li>
 *   <li><b>无状态、无副作用</b>：仅读取当前身份，不改任何业务状态。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Component
 * public class JwtCurrentUserProvider implements CurrentUserProvider {
 *     @Override
 *     public Object currentUser() {
 *         // 从安全上下文取当前登录人；匿名返回 null
 *         return SecurityUtil.getCurrentUserOrNull();
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface CurrentUserProvider {

    /**
     * 获取当前操作人标识。
     *
     * @return 操作人标识（账号字符串 / 工号数值等），无身份时返回 {@code null}
     */
    Object currentUser();
}