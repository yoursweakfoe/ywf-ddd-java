package com.yoursweakfoe.common.ddd.infrastructure.mybatisplus.persistence;

/**
 * 乐观锁版本冲突 —— {@code updateDomain} 在「实体仍然存在但版本不匹配」时抛出。
 *
 * <p>继承 {@link IllegalStateException} 以保持既有捕获链兼容：
 * 全局异常处理器将其按状态冲突翻译为 HTTP 409，历史代码按
 * {@code IllegalStateException} 捕获的行为不受影响。
 *
 * <h3>与「实体消失」的语义分界</h3>
 * <p>{@code updateDomain} 影响行数为 0 时存在两种可能原因，框架经存在性探测分类后抛出不同异常：
 * <ul>
 *   <li><b>实体仍存在</b>（并发修改导致版本不匹配）→ 本异常——调用方可安全重试</li>
 *   <li><b>实体已被删除 / ID 不存在</b> → 普通 {@link IllegalStateException}
 *       （消息含 {@code entity not found}）——重试无意义，不应被重试器吞掉</li>
 * </ul>
 *
 * <h3>消息兼容性</h3>
 * <p>消息保留历史核心字样 {@code affected 0 rows}：未迁移到类型判断的旧消费方
 * （如字符串匹配重试器）行为不变；新代码应一律按本类型捕获，不依赖消息文本。
 *
 * @see MybatisPlusPersistence#updateDomain(Object)
 */
public class OptimisticLockConflictException extends IllegalStateException {

    public OptimisticLockConflictException(String message) {
        super(message);
    }

    public OptimisticLockConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
