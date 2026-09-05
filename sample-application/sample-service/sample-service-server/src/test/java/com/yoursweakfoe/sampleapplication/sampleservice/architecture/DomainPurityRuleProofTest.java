package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.yoursweakfoe.archproof.domain.FrameworkLeakProbe;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R4「Domain 框架中立（stereotype 豁免）」规则的自证测试 —— 证明该规则<strong>会失败</strong>、
 * <strong>真实命中规范布局</strong>，且<strong>白名单不是死闸</strong>。
 *
 * <p>背景：旧版 R4（{@code DOMAIN_MODEL_IS_PURE}，主语 {@code ..domain.model..} 相邻匹配）对规范
 * 布局 {@code domain.{agg}.model} 永不命中，是全局 {@code failOnEmptyShould=false} 掩护下的空文
 * ——{@code OrderFactory} 的 {@code @Component} 在其眼皮下通过。空文与守护在「零命中通过」上
 * 不可区分，故沿袭本仓既有负证明范式（夹具 + 定向导入 + 双向断言），做三类断言：
 * <ol>
 *   <li><strong>违例必失败</strong>：{@link FrameworkLeakProbe}（domain 包内依赖 Spring 运行时）
 *       → {@code check()} 抛 AssertionError。证明规则「会咬人」，不是恒真句式。</li>
 *   <li><strong>豁免必通过</strong>：{@link OrderFactory}（@Component）、
 *       {@link InventoryDomainService}（@Service）与迁移后的 {@link OrderRepository}
 *       （{@code domain.order.repository} 写端口）→ 通过。证明 stereotype 白名单生效、
 *       2026-09-05 包迁移后的布局确在射程内被评估（若谓词仍是旧版相邻匹配，此断言
 *       会因「根本没人被看」而虚假成立——所以配合 ① 才有证明力）。</li>
 *   <li><strong>纯净聚合必通过</strong>：{@link Order} → 通过，回归哨兵。</li>
 * </ol>
 *
 * <p>本类不新增任何产品代码依赖：夹具位于 test source root 的 {@code com.yoursweakfoe.archproof.domain}
 * 包（两个 {@code @AnalyzeClasses} 扫描根之外）；本测试类被 {@code DoNotIncludeTests} 排除于常规扫描。
 */
class DomainPurityRuleProofTest {

    @Test
    void rule_fails_when_domain_resident_class_depends_on_spring_runtime() {
        JavaClasses violating = new ClassFileImporter().importClasses(FrameworkLeakProbe.class);

        assertThatThrownBy(() ->
                DddArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE.check(violating))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("org.springframework.context.ApplicationContext")
                .hasMessageContaining("FrameworkLeakProbe");
    }

    @Test
    void rule_passes_for_stereotype_annotated_domain_classes_on_canonical_nested_layout() {
        // OrderFactory 在 domain.order.model、InventoryDomainService 在 domain.shared.service、
        // OrderRepository 在迁移后的 domain.order.repository（去 repository.domain 重侧段）——
        // 都是旧 R4 相邻谓词永远看不见的规范布局，新 R4 必须看见它们、
        // 又必须放行 stereotype 豁免（OrderRepository 顺带证明迁移后布局仍在射程且端口纯净）。
        JavaClasses annotated = new ClassFileImporter()
                .importClasses(OrderFactory.class, InventoryDomainService.class, OrderRepository.class);

        assertThatCode(() ->
                DddArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE.check(annotated))
                .doesNotThrowAnyException();
    }

    @Test
    void rule_passes_for_pure_aggregate_root() {
        JavaClasses clean = new ClassFileImporter().importClasses(Order.class);

        assertThatCode(() ->
                DddArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE.check(clean))
                .doesNotThrowAnyException();
    }
}
