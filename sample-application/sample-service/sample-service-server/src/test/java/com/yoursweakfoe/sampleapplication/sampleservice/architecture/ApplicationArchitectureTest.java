package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.test.archunit.DDDArchitectureRules;

/**
 * 应用架构守护测试 —— 对 {@code com.yoursweakfoe.sampleapplication.sampleservice} 执行架构合规性校验。
 *
 * <p>R1 / R3 / R6 / A2 使用<b>精确包前缀</b>（{@code com.yoursweakfoe.sampleapplication.sampleservice.}）
 * 而非通用 {@code ..domain..} 段匹配——因为 infra 层存在 {@code repository.domain} /
 * {@code repository.application} 子包（按"实现哪个层的接口"命名），若用段匹配会把它们误判成
 * domain/application 层。其余规则（R4 / R5a / C1 / A3 / A4）不涉及该碰撞，沿用通用规则。
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    /** 本应用根包前缀（用于精确匹配顶层分层，避免误伤 infra 下的 repository.domain/application 子包） */
    private static final String BASE = "com.yoursweakfoe.sampleapplication.sampleservice.";

    // ── 精确包前缀规则（因 repository.domain / repository.application 子包命名） ──

    /** R1 —— DDD 四层依赖方向（依赖倒置）。 */
    @ArchTest
    static final ArchRule r1_layered_dependency = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Adapter").definedBy(BASE + "adapter..")
            .layer("Application").definedBy(BASE + "application..")
            .layer("Domain").definedBy(BASE + "domain..")
            .layer("Infrastructure").definedBy(BASE + "infrastructure..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .as("R1 DDD 四层依赖方向：adapter → application → domain ← infrastructure；"
                    + "读侧例外：infrastructure 读实现可访问 application 读端口");

    /** R3 —— Domain 不依赖 application/infrastructure/adapter/contract。 */
    @ArchTest
    static final ArchRule r3_domain_no_outer = noClasses()
            .that()
            .resideInAPackage(BASE + "domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    BASE + "application..",
                    BASE + "infrastructure..",
                    BASE + "adapter..",
                    BASE + "contract..")
            .as("R3 Domain 不依赖 application/infrastructure/adapter/contract");

    /** R6 —— Domain 不依赖 common-security（SecurityUtil 仅限 Application/Adapter 层）。 */
    @ArchTest
    static final ArchRule r6_domain_no_security = noClasses()
            .that()
            .resideInAPackage(BASE + "domain..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.yoursweakfoe.common.security..")
            .as("R6 Domain 不依赖 common-security（SecurityUtil 仅限 Application/Adapter 层）");

    /** A2 —— Domain 不依赖框架运行时能力（允许 @Service/@Component 等 stereotype 注解——Spring 是生态基座，注解是纯元数据）。 */
    @ArchTest
    static final ArchRule a2_domain_zero_framework_dependency = noClasses()
            .that()
            .resideInAPackage(BASE + "domain..")
            .should()
            .dependOnClassesThat(
                    resideInAPackage("com.baomidou.mybatisplus..")
                            .or(resideInAPackage("org.springframework..")
                                    .and(not(resideInAPackage("org.springframework.stereotype..")))))
            .as("A2 Domain 不依赖框架运行时能力（允许 stereotype 注解，禁 Spring 容器 / MyBatis-Plus）");

    // ── 复用 DDDArchitectureRules 通用规则（不涉及 repository.domain/application 碰撞） ──

    /** R4 —— Domain 模型保持纯净。 */
    @ArchTest
    static final ArchRule r4_domain_model_pure = DDDArchitectureRules.DOMAIN_MODEL_IS_PURE;

    /** R5a —— Domain Repository 必须是 interface。 */
    @ArchTest
    static final ArchRule r5a_repository_interfaces = DDDArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    /** C1 —— Contract 纯契约，不得依赖 server 四层及 Spring/MyBatis 运行时基础设施。 */
    @ArchTest
    static final ArchRule c1_contract_pure = DDDArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;

    // ── 应用特有规则 ───────────────────────────────────────────────────

    /** A3 —— 领域 Repository 接口（继承框架 Repository）必须在 domain..repository.. 包下（读端口 QueryRepository 除外）。 */
    @ArchTest
    static final ArchRule a3_repository_interface_in_domain = classes()
            .that()
            .areInterfaces()
            .and().areAssignableTo(Repository.class)
            .should()
            .resideInAPackage("..domain..repository..")
            .allowEmptyShould(true)
            .as("A3 领域 Repository 接口必须在 domain..repository.. 包下");

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
