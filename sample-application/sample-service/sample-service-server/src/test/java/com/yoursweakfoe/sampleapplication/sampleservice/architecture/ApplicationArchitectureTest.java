package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;

/**
 * 应用架构守护测试 —— 对 {@code com.yoursweakfoe.sampleapplication.sampleservice} 执行架构合规性校验。
 *
 * <p>复用 {@link DddArchitectureRules} 通用规则
 * （R1 分层方向 / R3 Domain 不依赖外层 / R4 模型纯净 / R5a Repository 接口 / C1 Contract 纯契约），
 * 并补充本应用特有规则：
 * <ul>
 *   <li>A2 —— Domain 层零框架依赖：不依赖 Spring / MyBatis-Plus</li>
 *   <li>A3 —— Repository 接口必须在 domain..repository.. 包下</li>
 *   <li>A4 —— AppService 必须在 application.. 包下</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    // ── 复用 DddArchitectureRules 通用规则 ─────────────────────────────

    /** R1 —— DDD 四层依赖方向（依赖倒置）。 */
    @ArchTest
    static final ArchRule r1_layered_dependency = DddArchitectureRules.LAYERED_ARCHITECTURE;

    /** R3 —— Domain 不依赖 application/infrastructure/adapter/contract。 */
    @ArchTest
    static final ArchRule r3_domain_no_outer = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    /** R4 —— Domain 模型保持纯净。 */
    @ArchTest
    static final ArchRule r4_domain_model_pure = DddArchitectureRules.DOMAIN_MODEL_IS_PURE;

    /** R5a —— Domain Repository 必须是 interface。 */
    @ArchTest
    static final ArchRule r5a_repository_interfaces = DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    /** C1 —— Contract 纯契约，不得依赖 server 四层及 Spring/MyBatis 运行时基础设施。 */
    @ArchTest
    static final ArchRule c1_contract_pure = DddArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;

    // ── 应用特有规则 ───────────────────────────────────────────────────

    /** A2 —— Domain 层零框架依赖：不依赖 Spring / MyBatis-Plus。 */
    @ArchTest
    static final ArchRule a2_domain_zero_framework_dependency = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "com.baomidou.mybatisplus..")
            .as("A2 Domain 层零框架依赖：不依赖 Spring / MyBatis-Plus");

    /** A3 —— Repository 接口必须在 domain..repository.. 包下。 */
    @ArchTest
    static final ArchRule a3_repository_interface_in_domain = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should()
            .resideInAPackage("..domain..repository..")
            .allowEmptyShould(true)
            .as("A3 Repository 接口必须在 domain..repository.. 包下");

    /** A4 —— AppService 必须在 application.. 包下。 */
    @ArchTest
    static final ArchRule a4_app_service_in_application = classes()
            .that()
            .haveSimpleNameEndingWith("AppService")
            .should()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)
            .as("A4 AppService 必须在 application.. 包下");
}
