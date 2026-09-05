package com.yoursweakfoe.common.exception.type;

/**
 * 静默写丢失 —— 持久化调用返回 0 影响行的<strong>不可能状态</strong>（INSERT / DELETE rows==0）。
 *
 * <p>合法请求走到 INSERT 必然落一行、按存在 ID 的 DELETE 必然命中——0 影响行意味着写丢失、
 * schema 事故或调用链逻辑缺陷（如 ID 体系错乱），<strong>不是业务冲突</strong>：重试无意义，
 * 需要人工介入。全局异常处理器将本异常按 500 + ERROR 日志处置（告警信号），
 * 与业务通道的 4xx 泛化文案隔离。
 *
 * <h3>与 {@link OptimisticLockConflictException} 的分界</h3>
 * <ul>
 *   <li>{@code OptimisticLockConflictException}（继承 {@link IllegalStateException}，走 409/WARN）
 *       ——「实体仍在、版本被并发推进」：<strong>可重试</strong>，属正常并发流；</li>
 *   <li>本异常（直接继承 {@link RuntimeException}，走 500/ERROR）——「语句合法执行却 0 行受影响」：
 *       <strong>重试无意义</strong>，属基础设施级事故信号。</li>
 * </ul>
 *
 * <p>刻意不继承 {@link IllegalStateException}：ISE 通道按 409 处理且只记 WARN——写丢失若混入
 * 该通道将静默躲过告警（本类存在的动机，release-audit B4）。
 *
 * <h3>范围围栏</h3>
 * <p>{@code updateDomain} 影响行数 0 且存在性探测判定「实体已消失」的路径仍抛
 * {@link IllegalStateException}（并发删除属业务竞态，409 可辩护），不升级为
 * SilentWriteLossException——分界详见 {@code MybatisPersistence.updateDomain} javadoc。
 *
 * <h3>抛出点</h3>
 * <p>由 {@code common-ddd} 模块 {@code MybatisPersistence.saveDomain()}（INSERT affected 0 rows）、
 * {@code removeDomainById()} / {@code removeDomainByIds()} 全未命中（DELETE affected 0 rows）抛出；
 * 消息保留历史核心字样 {@code affected 0 rows}（兼容按文本匹配的旧消费方）。
 */
public class SilentWriteLossException extends RuntimeException {

    public SilentWriteLossException(String message) {
        super(message);
    }

    public SilentWriteLossException(String message, Throwable cause) {
        super(message, cause);
    }
}
