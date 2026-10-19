package com.yoursweakfoe.common.ddd.domain.model;

/**
 * 可标识接口 —— 所有具有唯一标识的领域对象的通用契约。
 *
 * <p>在 DDD 中，实体（Entity）和聚合根（AggregateRoot）都通过唯一标识来区分，
 * 本接口抽取 {@code getId()} 方法，使得基础设施组件（如仓储实现）
 * 能够以统一方式按 ID 操作领域对象，而无需关心具体类型。
 *
 * <p>本契约只约定「身份可取」；「身份值从何而来」是系统级策略，由同包
 * {@link AggregateIds} 唯一入口定档（UUIDv7 铸造，装配宣言见其 javadoc）；
 * 「身份以何类型被取走」是终类型策略——聚合根的身份槽应为专属币种
 * <code>{Agg}Id implements {@link com.yoursweakfoe.common.ddd.domain.id.Identifier}&lt;V&gt;</code>
 * （他聚合 ID 混入调用位＝编译失败；
 * of()／紧凑构造器仅 null 检查、只装箱不站岗，出生保证归铸造入口 {@link AggregateIds}，
 * 法卷锚 BP-13／案卷 2026-09-typed-identifier）——币种词汇住邻架
 * <code>domain.id</code>（与业务落位 <code>domain/{agg}/id/</code> 段名镜像对偶，
 * 案卷 2026-09-identifier-shelf），「身份可取／从何而来」两面共居 model 包，
 * 实现方不自行 import 铸造库。
 *
 * @param <ID> 标识类型：聚合根槽位用 <code>{Agg}Id</code> 终类型
 *             （{@link com.yoursweakfoe.common.ddd.domain.id.Identifier} 实现）；
 *             聚合内子实体 PK 维持原生承载（如 {@link java.util.UUID}、{@link Long}，法卷锚 BP-15）
 * @see Entity
 * @see AggregateRoot
 * @see AggregateIds
 * @see com.yoursweakfoe.common.ddd.domain.id.Identifier
 */
public interface Identifiable<ID> {

    /**
     * 获取实体的唯一标识。
     *
     * @return 实体标识，新建但尚未持久化的实体可能返回 {@code null}
     */
    ID getId();
}
