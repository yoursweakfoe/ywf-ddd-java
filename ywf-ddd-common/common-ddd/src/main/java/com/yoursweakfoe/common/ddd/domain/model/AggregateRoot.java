package com.yoursweakfoe.common.ddd.domain.model;

/**
 * 聚合根基类 —— 聚合的唯一外部访问入口和一致性边界。
 *
 * <p>职责：
 * <ul>
 *   <li>维护业务不变量（{@link #validate()} 模板方法，持久化前由仓储自动调用）
 *   <li>控制子实体生命周期
 * </ul>
 *
 * <p><strong>线程安全约束</strong>：聚合根实例设计为<strong>单请求、单线程</strong>使用
 * （一次 HTTP/RPC 请求内加载、操作、持久化），不应跨线程共享。若业务确需并发操作
 * 同一聚合根实例（极罕见），调用方须自行保证外部同步，框架不为此场景提供内置保护。
 *
 * @param <ID> 标识类型
 * @see Entity
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {

    /**
     * 校验聚合不变量。子类覆写，框架在 save/update 前自动调用。默认无校验。
     */
    public void validate() {
        // 默认无校验，子类按需覆写
    }
}
