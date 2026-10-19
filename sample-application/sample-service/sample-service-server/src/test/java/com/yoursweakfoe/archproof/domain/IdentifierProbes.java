package com.yoursweakfoe.archproof.domain;

import com.yoursweakfoe.common.ddd.application.repository.QueryRepository;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.ddd.domain.model.Entity;
import com.yoursweakfoe.common.ddd.domain.id.Identifier;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * R15「聚合根身份终类型化」负证明夹具（虚构教例，Payment 家族中性命名——common 树教例族规）。
 *
 * <p>位于 {@code com.yoursweakfoe.archproof.domain} 包——两个 {@code @AnalyzeClasses}
 * 扫描根之外，仅由 {@code IdentifierRuleProofTest} 定向导入，不污染常规架构扫描。
 * 四形态各证一锁：裸类型根必咬（UUID 直住身份槽＝同形事故之源）／类型化根与合格端口必放
 * （白名单不是死闸）／豁免位不误咬（子实体 PK 与读端口维持原生承载，法卷锚 BP-15）／
 * 端口 ID 槽失守必咬（裸币种槽与裸类型端口两形态）。
 *
 * <p>施工勘验注（案卷 2026-09-typed-identifier P-5 定档时补记）：条文物件所指
 * 「端口-根槽不一致」形态（{@code Repository<类型化根, 错误Id>}）在框架既有
 * {@code Repository<Domain extends Identifiable<ID>, ID>} 的 F-边界下 javac 即拒、
 * 不存在可编译夹具（含裸类型根变体，均实证）——规则一致臂作反射备胎保留（案卷报告在账），
 * 本件端口锁以<strong>可编译的端口失守形态</strong>（裸币种槽／裸类型继承）锁其牙。
 */
public final class IdentifierProbes {

    private IdentifierProbes() {}

    /** 类型化币种教例（正确形状：of() 仅 null 检查——只装箱不站岗，法卷锚 BP-13）。 */
    public record PaymentProbeId(UUID value) implements Identifier<UUID>, Serializable {

        public static PaymentProbeId of(UUID value) {
            if (value == null) {
                throw new BusinessException("payment:err.idRequired");
            }
            return new PaymentProbeId(value);
        }
    }

    /** 裸类型根（{@code AggregateRoot<UUID>}）——R15 必咬（现 sample 聚合的迁移前形态）。 */
    public static class BareUuidRoot extends AggregateRoot<UUID> {
        private UUID id;

        @Override
        public UUID getId() {
            return id;
        }
    }

    /** 类型化根（{@code AggregateRoot<PaymentProbeId>}）——必放（教义合法路）。 */
    public static class TypedRoot extends AggregateRoot<PaymentProbeId> {
        private PaymentProbeId id;

        @Override
        public PaymentProbeId getId() {
            return id;
        }
    }

    /** 合格端口（ID 槽＝根槽＝币种）——必放（端口臂不是死闸之证明）。 */
    public interface TypedPort extends Repository<TypedRoot, PaymentProbeId> {}

    /** 豁免位①：子实体 PK 裸类型（{@code Entity<Long>}）——不在主语，不误咬。 */
    public static class BareEntityPk extends Entity<Long> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }
    }

    /** 豁免位②：读端口（QueryRepository 标记 + 裸 UUID 参数，P-3 读侧豁免/BP-15）——不误咬。 */
    public interface ExemptReadPort extends QueryRepository {

        Optional<String> findById(UUID id);

        List<String> findPendingIds(UUID customerId);
    }

    /** 端口失守形态①：ID 槽裸币种（镜像未迁移 sample 的 {@code Repository<Order, UUID>} 形状）——必咬。 */
    public interface BareSlotPort extends Repository<BareUuidRoot, UUID> {}

    /** 端口失守形态②：裸类型继承 Repository（无 ID 泛型实参）——必咬（拒绝静默放行）。 */
    @SuppressWarnings("rawtypes")
    public interface RawSlotPort extends Repository {}
}
