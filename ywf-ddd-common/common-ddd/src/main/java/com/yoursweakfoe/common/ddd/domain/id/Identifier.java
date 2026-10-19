package com.yoursweakfoe.common.ddd.domain.id;

/**
 * 身份词汇 —— 聚合身份终类型的唯一类型学锚点。
 *
 * <p>框架只发行角色词汇、不载实现：本接口仅一个 {@link #value()} 访问器，无默认方法、
 * 无抽象基类、无任何运行期设施，零 JDK 外依赖（domain 纯度教义不破）。本架以段名
 * <code>id</code> 自立，与业务落位 <code>domain/{agg}/id/</code> 段名镜像对偶
 * （案卷 2026-09-identifier-shelf）。业务聚合铸造的专属币种即它的 record 实现——
 *
 * <pre>{@code
 * public record PaymentId(UUID value) implements Identifier<UUID>, Serializable {
 *
 *     public static PaymentId of(UUID value) {
 *         if (value == null) {
 *             throw new BusinessException("payment:err.idRequired");   // 统一位点通道，禁硬编码文案
 *         }
 *         return new PaymentId(value);
 *     }
 * }
 * }</pre>
 *
 * <p><strong>只装箱、不站岗</strong>：{@code of()} / 紧凑构造器仅允许 null 检查，不校验底值
 * （禁格式复校验、禁 UUID 版本位校验——存量行经装载路径照样过 {@code of()}）——
 * 出生保证归铸造入口 {@link com.yoursweakfoe.common.ddd.domain.model.AggregateIds}，类型层只负责编译隔离墙
 * （法卷锚 BP-13／案卷 2026-09-typed-identifier §裁决记录 Q5）。
 *
 * <p><strong>收益面</strong>：{@code {Agg}Id} 置入 {@link com.yoursweakfoe.common.ddd.domain.model.AggregateRoot} 身份槽后，他聚合身份
 * 混入本聚合调用位＝编译失败（混放编译锁）。底层类型由各聚合自定（UUID / Long / String 皆可，
 * 机制不绑定 UUID，§裁决记录 Q4）；{@code Serializable} 仅满足
 * {@code MybatisPersistence} 既有持久槽约束（{@code ID extends Serializable}），非序列化承诺。
 *
 * <p><strong>豁免面（币种不入的槽位）</strong>：契约层 CQE/CO、PO、读端口与读 DTO、
 * 聚合内子实体 PK 维持原生承载（法卷锚 BP-15／案卷 2026-09-typed-identifier）。
 *
 * @param <V> 底层身份值类型（任意单值可比较原生类型）
 * @see com.yoursweakfoe.common.ddd.domain.model.Identifiable
 * @see com.yoursweakfoe.common.ddd.domain.model.AggregateIds
 */
public interface Identifier<V> {

    /**
     * 取出底层身份值（本词汇承载的全部信息，转换/出站仅此一位）。
     *
     * @return 装箱的原生承载值（如 {@link java.util.UUID}、{@link Long}）
     */
    V value();
}
