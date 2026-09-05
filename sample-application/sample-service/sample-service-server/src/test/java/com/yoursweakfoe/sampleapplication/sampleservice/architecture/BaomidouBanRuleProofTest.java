package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.baomidou.archproof.BannedProbe;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.yoursweakfoe.common.test.archunit.DDDArchitectureRules;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R15 全仓 {@code com.baomidou..} 禁入规则的自证测试 —— 证明该规则<strong>会失败</strong>，而非恒真通过。
 *
 * <p>架构禁入规则的真实形态是对全仓主代码「零命中通过」（vacuous pass）——这本身无法区分
 * 「规则在守护」与「规则写错了」。本测试用两个定向导入的夹具类构造出最小违规样本：
 * {@link ProbeConsumer}（模拟违规依赖方）→ {@link BannedProbe}（位于被禁包内的占位类），
 * 断言规则对违规集失败、对真实领域类通过。
 *
 * <p>夹具不污染任何 @ArchTest：{@code BannedProbe} 位于 {@code com.baomidou} 包（不在各
 * {@code @AnalyzeClasses} 的业务根包内）；{@link ProbeConsumer} 与本类为测试类（被
 * {@code DoNotIncludeTests} 排除）。本类只验证规则语义，不新增任何产品代码依赖。
 */
class BaomidouBanRuleProofTest {

    /** 模拟违规依赖方：持有位于被禁包内的类型引用（仅存在于测试字节码）。 */
    static class ProbeConsumer {
        @SuppressWarnings("unused")
        private BannedProbe forbiddenDependency;
    }

    @Test
    void rule_fails_when_a_class_depends_on_baomidou_package() {
        JavaClasses violating = new ClassFileImporter().importClasses(ProbeConsumer.class);

        assertThatThrownBy(() -> DDDArchitectureRules.MYBATIS_PLUS_BANNED.check(violating))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("com.baomidou");
    }

    @Test
    void rule_passes_for_real_domain_classes() {
        JavaClasses clean = new ClassFileImporter().importClasses(Order.class);

        assertThatCode(() -> DDDArchitectureRules.MYBATIS_PLUS_BANNED.check(clean))
                .doesNotThrowAnyException();
    }
}
