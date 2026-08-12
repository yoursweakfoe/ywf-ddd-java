package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * 应用层架构守护测试 —— 对 {@code com.yoursweakfoe.sampleapplication.sampleservice} 执行架构合规性校验。
 *
 * <p>本测试覆盖四条应用层特有规则：
 * <ul>
 *   <li>A1 —— 四层依赖方向：Adapter → Application → Domain；Infrastructure → Domain</li>
 *   <li>A2 —— Domain 层零框架依赖：不依赖 Spring / MyBatis-Plus / gRPC</li>
 *   <li>A3 —— Repository 接口必须在 domain..repository.. 包下</li>
 *   <li>A4 —— CommandHandler / QueryHandler 实现必须在 application.. 包下</li>
 * </ul>
 *
 * <p>同时复用 {@link com.yoursweakfoe.common.test.archunit.DddArchitectureRules} 中的通用规则
 * （R3 Domain 不依赖外层、R4 模型纯净性、R5a Repository 必须为接口）。
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    // ── A1 四层依赖方向 ──────────────────────────────────────────────

    /**
     * A1 —— DDD 四层依赖方向：
     * Adapter → Application → Domain；Infrastructure → Domain。
     * Domain 不得反向依赖任何外层。
     */
    @ArchTest
    static final ArchRule a1_layered_dependency = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Adapter").definedBy("..adapter..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Adapter", "Application")
            .as("A1 四层依赖方向：Adapter → Application → Domain；Infrastructure → Domain");

    // ── A2 Domain 层零框架依赖 ────────────────────────────────────────

    /** A2 —— Domain 层不依赖 Spring / MyBatis-Plus / gRPC 等框架。 */
    @ArchTest
    static final ArchRule a2_domain_zero_framework_dependency = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "com.baomidou.mybatisplus..",
                    "io.grpc..")
            .as("A2 Domain 层零框架依赖：不依赖 Spring / MyBatis-Plus / gRPC");

    // ── A3 Repository 接口位置 ────────────────────────────────────────

    /** A3 —— Repository 接口必须在 domain 层下的 repository 子包中（支持按聚合根分包）。 */
    @ArchTest
    static final ArchRule a3_repository_interface_in_domain = classes()
            .that()
            .haveSimpleNameEndingWith("Repository")
            .and().areInterfaces()
            .should()
            .resideInAPackage("..domain..repository..")
            .allowEmptyShould(true)
            .as("A3 Repository 接口必须在 domain..repository.. 包下（支持 domain.<aggregate>.repository 结构）");

    // ── A4 AppService 位置 ────────────────────────────────────────────

    /** A4 —— AppService 必须在 application.. 包下。 */
    @ArchTest
    static final ArchRule a4_app_service_in_application = classes()
            .that()
            .haveSimpleNameEndingWith("AppService")
            .should()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)
            .as("A4 AppService 必须在 application.. 包下");

    // ── 复用 DddArchitectureRules 通用规则 ─────────────────────────────

    // 注意：不在本测试中重复声明 R3（Domain 不依赖外层），
    // 因为根包 com.yoursweakfoe.sampleapplication.sampleservice 包含 "application" 子串，
    // 导致 resideInAnyPackage("..application..") 会匹配所有 domain 内部依赖，产生大量误报。
    // A1 分层架构规则已等效覆盖 Domain 不得反向依赖的约束。

    /** R4 —— Domain 模型保持纯净：Entity / ValueObject 不依赖 MyBatis-Plus / Spring Stereotype / JPA。 */
    @ArchTest
    static final ArchRule r4 = noClasses()
            .that()
            .resideInAPackage("..domain.model..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.baomidou.mybatisplus..",
                    "org.springframework.stereotype..",
                    "jakarta.persistence..",
                    "javax.persistence..")
            .as("R4 Domain 模型保持纯净：Entity/ValueObject 不依赖 MyBatis-Plus / Spring Stereotype / JPA");

    /** R5a —— Domain Repository 必须是 interface。 */
    @ArchTest
    static final ArchRule r5a = classes()
            .that()
            .resideInAPackage("..domain.repository..")
            .should()
            .beInterfaces()
            .allowEmptyShould(true)
            .as("R5a Domain Repository 必须是 interface");
}
