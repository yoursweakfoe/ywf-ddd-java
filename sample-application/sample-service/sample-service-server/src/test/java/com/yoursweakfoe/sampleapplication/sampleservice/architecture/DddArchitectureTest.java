package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.test.archunit.DDDArchitectureRules;

/**
 * DDD 分层架构守护测试 —— 对 common-ddd 框架包执行 R1-R5b 规则校验。
 *
 * <p>{@code com.yoursweakfoe.common.ddd} 包含标准 DDD 分层（domain / application / infrastructure），
 * 所有规则均可有效校验。
 *
 * <p>R1 覆写：新增 Configuration 层（{@code MybatisDddAutoConfiguration} 等根包类）以允许框架
 * 自动配置类跨层引用基础设施组件。
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.common.ddd",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DddArchitectureTest {

    /**
     * R1 本地覆写：在标准四层基础上增加 Configuration 层，
     * 容纳 {@code MybatisDddAutoConfiguration} 等根包自动配置类，避免其跨层引用被误报。
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
    static final ArchRule r2 = DDDArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;

    @ArchTest
    static final ArchRule r3 = DDDArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    @ArchTest
    static final ArchRule r4 = DDDArchitectureRules.DOMAIN_MODEL_IS_PURE;

    @ArchTest
    static final ArchRule r5a = DDDArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    @ArchTest
    static final ArchRule r5b = DDDArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;

    // 框架包自身无 adapter 层组件（标记接口位于 common-ddd/adapter/..，规则按空集通过），
    // 挂载以守护「标记接口所在的包结构不被破坏」。
    @ArchTest
    static final ArchRule r8a = DDDArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    @ArchTest
    static final ArchRule r8b = DDDArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;

    @ArchTest
    static final ArchRule r9a = DDDArchitectureRules.EVENT_CONSUMERS_ARE_MARKED_AND_IN_ADAPTER;

    @ArchTest
    static final ArchRule r9b = DDDArchitectureRules.EVENT_CONSUMER_PACKAGE_CLASSES_MUST_BE_MARKED;
}
