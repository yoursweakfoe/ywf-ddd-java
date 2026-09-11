package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id;

import com.yoursweakfoe.common.ddd.domain.id.Identifier;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.io.Serializable;
import java.util.UUID;

/**
 * 商品聚合身份终类型 —— {@code Product} 的专属币种（聚合根身份槽 / 写端口 ID 槽实参）。
 *
 * <p>只装箱不站岗：紧凑构造器仅 null 检查，不校验底值格式、不校验 UUID 版本位
 * （装载存量行照过 {@link #of(UUID)}，出生保证归铸造入口 {@code ProductFactory}）。
 * {@code Serializable} 仅满足持久槽既有约束（{@code MybatisPersistence} 的
 * {@code ID extends Serializable}），非序列化承诺。
 *
 * <p>法卷锚 BP-13（案卷 2026-09-typed-identifier 折叠后）；执法 R15。
 * 跨聚合引用位（如 {@code OrderItem} 的商品引用槽）亦用本币种（法卷锚 BP-14）。
 */
public record ProductId(UUID value) implements Identifier<UUID>, Serializable {

    public ProductId {
        if (value == null) {
            throw new BusinessException("product:err.idRequired");
        }
    }

    /** 装箱入口（写 Handler 一点定型位使用）——仅 null 检查，不站岗。 */
    public static ProductId of(UUID value) {
        return new ProductId(value);
    }
}
