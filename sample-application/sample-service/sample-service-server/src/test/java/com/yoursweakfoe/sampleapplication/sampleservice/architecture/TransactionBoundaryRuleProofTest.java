package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.yoursweakfoe.archproof.application.TransactionBoundaryProbes;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R11「@Transactional ∧ 显式 rollbackFor」的自证测试 —— 证明增强后的规则<strong>会失败</strong>、
 * <strong>白名单不是死闸</strong>、<strong>主语锚点不外溢</strong>。
 *
 * <p>背景（R11 严格化，2026-09「修码就法」裁决）：教义（BP-10/WC-2）承诺 {@code @Transactional(rollbackFor = Exception.class)}，
 * 旧 R11 只查注解存在性——裸标注释照样过闸，而 Spring 默认回滚规则对受检异常不回滚，
 * 半途提交是一口真实的缝。规则升级为「属性显式存在」检查后，按 R4 空文教训同型补负证明：
 * 新谓词若无牙（属性名拼错、注解类型误配、stream 判据写反），「零命中通过」与「永假」不可区分。
 *
 * <p>夹具 {@link TransactionBoundaryProbes} 位于两扫描根之外的 archproof 包，
 * 本类被 {@code DoNotIncludeTests} 排除于常规扫描。
 */
class TransactionBoundaryRuleProofTest {

    @Test
    void rule_fails_for_bare_transactional_annotation() {
        JavaClasses bare =
                new ClassFileImporter().importClasses(TransactionBoundaryProbes.BareTransactionalProbe.class);

        assertThatThrownBy(() -> DddArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL.check(bare))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("rollbackFor");
    }

    @Test
    void rule_fails_when_annotation_missing() {
        JavaClasses missing =
                new ClassFileImporter().importClasses(TransactionBoundaryProbes.NoTransactionalProbe.class);

        assertThatThrownBy(() -> DddArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL.check(missing))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void rule_passes_for_explicit_rollback_for_declaration() {
        JavaClasses declared =
                new ClassFileImporter().importClasses(TransactionBoundaryProbes.RollbackDeclaredProbe.class);

        assertThatCode(() -> DddArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL.check(declared))
                .doesNotThrowAnyException();
    }

    @Test
    void rule_ignores_classes_that_are_not_command_handlers() {
        // 非 CommandHandler 实现、同名 handle、裸标注解——主语锚点必须放它过去；
        // 若本断言变红，说明主语从标记接口漂移到了方法名/包名，锚点哲学失守。
        JavaClasses outsider =
                new ClassFileImporter().importClasses(TransactionBoundaryProbes.NonHandlerProbe.class);

        assertThatCode(() -> DddArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL.check(outsider))
                .doesNotThrowAnyException();
    }
}
