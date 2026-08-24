package com.yoursweakfoe.common.ddd.domain.model;

import com.yoursweakfoe.common.ddd.domain.event.domain.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根基类 —— 聚合的唯一外部访问入口和一致性边界。
 *
 * <p>职责：
 * <ul>
 *   <li>维护业务不变量（{@link #validate()} 模板方法，持久化前自动调用）
 *   <li>管理领域事件（{@link #registerEvent(DomainEvent)}，持久化后统一发布）
 *   <li>控制子实体生命周期
 * </ul>
 *
 * <p>事件机制：业务方法内 registerEvent → 仓储持久化成功 → DomainEventPublisher 发布 → clearDomainEvents。
 *
 * <p><strong>序列化注意</strong>：{@code domainEvents} 为 {@code transient} 字段，
 * Java 序列化（Redis / Session / 分布式缓存）后已注册事件将丢失。
 * 请确保在 {@code registerEvent()} 后尽快调用仓储的 save/update 完成事件发布，
 * 避免在事件未发布前将聚合根序列化。
 *
 * <p><strong>线程安全约束</strong>：{@code domainEvents} 使用非线程安全的 {@code ArrayList}。
 * 聚合根实例设计为<strong>单请求、单线程</strong>使用（一次 HTTP/RPC 请求内加载、操作、持久化），
 * 不应跨线程共享。若业务确需并发操作同一聚合根实例（极罕见），
 * 调用方须自行保证外部同步，框架不为此场景提供内置保护。
 *
 * @param <ID> 标识类型
 * @see Entity
 * @see DomainEvent
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {

    /**
     * 领域事件暂存列表（持久化成功后由基础设施发布并清空）。
     *
     * <p>非线程安全 —— 依赖「单请求单实例」约束（见类级 Javadoc）。
     */
    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    // ==================== 领域事件管理 ====================

    /**
     * 注册领域事件（不能为 null），持久化成功后统一发布。
     */
    protected void registerEvent(DomainEvent event) {
        if (event == null) {
            // 框架契约违反（编程错误），非业务规则违反——用标准未受检异常 IllegalArgumentException。
            // 边界：业务规则违反仍抛 BusinessException（i18n 位点；domain 经 common-ddd 传递依赖可用），
            // 框架契约错误（如 null 事件）用 JDK 标准异常，二者不混用。
            throw new IllegalArgumentException("Domain event must not be null");
        }
        this.domainEvents.add(event);
    }

    /** 获取已注册事件（不可变快照） */
    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    /** 清空事件（由仓储层在发布后调用，业务代码不应直接调用） */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // ==================== 聚合不变量校验 ====================

    /**
     * 校验聚合不变量。子类覆写，框架在 save/update 前自动调用。默认无校验。
     */
    public void validate() {
        // 默认无校验，子类按需覆写
    }
}
