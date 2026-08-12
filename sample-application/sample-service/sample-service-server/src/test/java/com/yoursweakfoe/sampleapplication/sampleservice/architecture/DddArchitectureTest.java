package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;

/**
 * DDD 分层架构守护测试 —— 对 common-ddd 框架包执行 R1-R5b 规则校验。
 *
 * <p>{@code com.yoursweakfoe.common.ddd} 包含标准 DDD 分层（domain / application / infrastructure），
 * 所有规则均可有效校验。
 *
 * <p>R1 覆写：新增 Configuration 层（{@code DddAutoConfiguration} 等根包类）以允许框架
 * 自动配置类跨层引用基础设施组件；R5b 覆写以处理框架包不含 RepositoryImpl 的空集情况。
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.common.ddd",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DddArchitectureTest {

    /**
     * R1 本地覆写：在标准四层基础上增加 Configuration 层，
     * 容纳 {@code DddAutoConfiguration} 等根包自动配置类，避免其跨层引用被误报。
     */
    @ArchTest
    static final ArchRule r1 = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Configuration").definedBy("com.yoursweakfoe.common.ddd")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Configuration")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers(
                    "Application", "Configuration")
            .as("R1 DDD 三层依赖方向 + Configuration 层");

    @ArchTest
    static final ArchRule r2 = DddArchitectureRules.CONTROLLER_ONLY_DEPENDS_ON_APPLICATION;

    @ArchTest
    static final ArchRule r3 = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    @ArchTest
    static final ArchRule r4 = DddArchitectureRules.DOMAIN_MODEL_IS_PURE;

    @ArchTest
    static final ArchRule r5a = DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    /**
     * R5b 本地覆写：框架包不含 *RepositoryImpl 类，原始规则对空集默认失败，
     * 此处添加 {@code allowEmptyShould(true)} 使规则在无匹配时仍能通过。
     */
    @ArchTest
    static final ArchRule r5b = classes()
            .that()
            .haveSimpleNameEndingWith("RepositoryImpl")
            .should()
            .resideInAPackage("..infrastructure.persistence..repository..")
            .allowEmptyShould(true)
            .as("R5b 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下");
}
