package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.yoursweakfoe.archproof.domain.IdentifierProbes;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R15「聚合根身份终类型化」的自证测试 —— 证明规则<strong>会失败</strong>、
 * <strong>白名单不是死闸</strong>、<strong>豁免主语不外溢</strong>。
 *
 * <p>背景（案卷 2026-09-typed-identifier P-5）：聚合身份全域裸用底层类型（现为 UUID）时，
 * 各聚合身份在类型系统里同形，跨聚合调用位混放迟到运行时才暴露。新词汇 {@code Identifier}
 * 发行 + R15（顶位承旧 R15 废号）立闸后，按 R4/R11 先例同型补负证明——新反射谓词若无牙
 * （泛型解析写反、豁免面误收主语、加载失败静默放行），「零命中通过」与「永真」不可区分。
 *
 * <p>夹具 {@link IdentifierProbes} 位于两扫描根之外的 archproof 包，
 * 本类被 {@code DoNotIncludeTests} 排除于常规扫描。端口锁注记：可编译的端口失守形态为
 * 裸币种槽与裸类型继承（条文物件所指类型化-裸槽混放在 {@code Repository} 的 F-边界下
 * javac 即拒，夹具不可发行，见夹具类头勘验注与规则 javadoc「怎么判」备胎臂登记）。
 */
class IdentifierRuleProofTest {

    @Test
    void rule_fails_for_bare_uuid_identity_slot() {
        JavaClasses bare =
                new ClassFileImporter().importClasses(IdentifierProbes.BareUuidRoot.class);

        assertThatThrownBy(() -> DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS.check(bare))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("未实现")
                .hasMessageContaining("Identifier");
    }

    @Test
    void rule_passes_for_typed_root_and_port() {
        // 类型化根 + 槽一致的合格端口——教义合法路必须放行；若本断言变红，说明谓词写反成「永假」。
        JavaClasses typed =
                new ClassFileImporter().importClasses(
                        IdentifierProbes.TypedRoot.class, IdentifierProbes.TypedPort.class);

        assertThatCode(() -> DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS.check(typed))
                .doesNotThrowAnyException();
    }

    @Test
    void rule_ignores_exempt_slots() {
        // 子实体 PK（Entity<Long> 裸）与读端口（裸 UUID 参数）居豁免面（法卷锚 BP-15）；
        // 若本断言变红，说明主语从 AggregateRoot/Repository 锚点外溢到了 Entity/读侧标记。
        JavaClasses exempt =
                new ClassFileImporter().importClasses(
                        IdentifierProbes.BareEntityPk.class, IdentifierProbes.ExemptReadPort.class);

        assertThatCode(() -> DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS.check(exempt))
                .doesNotThrowAnyException();
    }

    @Test
    void rule_fails_for_untyped_port_slots() {
        JavaClasses bareSlot =
                new ClassFileImporter().importClasses(IdentifierProbes.BareSlotPort.class);

        assertThatThrownBy(() -> DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS.check(bareSlot))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("端口 ID 槽")
                .hasMessageContaining("Identifier");

        JavaClasses rawPort =
                new ClassFileImporter().importClasses(IdentifierProbes.RawSlotPort.class);

        assertThatThrownBy(() -> DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS.check(rawPort))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("裸类型");
    }
}
