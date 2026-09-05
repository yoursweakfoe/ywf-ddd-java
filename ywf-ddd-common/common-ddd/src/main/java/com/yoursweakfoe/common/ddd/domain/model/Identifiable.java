package com.yoursweakfoe.common.ddd.domain.model;

/**
 * 可标识接口 —— 所有具有唯一标识的领域对象的通用契约。
 *
 * <p>在 DDD 中，实体（Entity）和聚合根（AggregateRoot）都通过唯一标识来区分，
 * 本接口抽取 {@code getId()} 方法，使得基础设施组件（如仓储实现）
 * 能够以统一方式按 ID 操作领域对象，而无需关心具体类型。
 *
 * <p>本契约只约定「身份可取」；「身份值从何而来」是系统级策略，由同包
 * {@link AggregateIds} 唯一入口定档（UUIDv7 铸造，装配宣言见其 javadoc）——
 * 配对两面共居 model 包，实现方不自行 import 铸造库。
 *
 * @param <ID> 标识类型，如 {@link java.util.UUID}、{@link Long} 等
 * @see Entity
 * @see AggregateRoot
 * @see AggregateIds
 */
public interface Identifiable<ID> {

    /**
     * 获取实体的唯一标识。
     *
     * @return 实体标识，新建但尚未持久化的实体可能返回 {@code null}
     */
    ID getId();
}
