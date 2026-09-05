package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;

/**
 * 框架侧架构扫描 —— 对 common-ddd 框架包自身（扫描根 {@code com.yoursweakfoe.common.ddd}）
 * 执行架构教义校验：框架要求业务遵守的每一条，框架自己先遵守。
 *
 * <p>subject 实际非空、真正起守护作用的规则：r2（adapter 标记接口包）、r3 / r4
 * （common-ddd 自有 domain 包）。其余标记类规则（r8a / r8b / r10a / r10b / r14a / r14b）
 * 在此扫描为<strong>空集通过</strong>——挂载意义是守护「标记接口所在包结构不被破坏」
 * （common-ddd 的 adapter.rest.controller / application.dto / adapter.task.scheduler 包
 * 即标记接口之家），属 {@code DddArchitectureRules} 类头「空集通过警示」登记的教义例外。
 *
 * <p>R1 本地覆写：新增 Configuration 层（{@code MybatisDddAutoConfiguration} 等根包类）以允许框架
 * 自动配置类跨层引用基础设施组件——框架包无 adapter/contract 业务组件，且根包配置类需要合法
 * 归属层，故框架扫描必须覆写；共享 LAYERED_ARCHITECTURE 本版本起挂载于业务扫描（命名税迁移后
 * 段语义无歧义），框架侧保留本覆写。
 *
 * <p>挂载原则（本版本接线修复）：不新增、不保留结构性空转挂载——原 r5b 框架挂载已撤销
 * （common-ddd 无任何 *RepositoryImpl 类：仓储实现由各业务服务继承 {@code MybatisPersistence}
 * 支撑类自行完成，框架包内不存在该命名形态的类，挂载永空集），R5b 的真实守护点在业务扫描。
 * 原 R15（com.baomidou 全仓禁令）连同规则本体已删除：规则库不为「项目选择不用的库」立特别法。
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.common.ddd",
        importOptions = ImportOption.DoNotIncludeTests.class)
class DddArchitectureTest {

    /**
     * R1 本地覆写：在标准四层基础上增加 Configuration 层，
     * 容纳 {@code MybatisDddAutoConfiguration} 等根包自动配置类，避免其跨层引用被误报。
     *
     * <p>Application 允许被 Infrastructure 访问：与通用 R1 的依赖倒置语义对齐——
     * 应用层读端口（{@code QueryRepository}）由基础设施层实现（读侧先例）。
     */
    @ArchTest
    static final ArchRule r1 = Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Configuration").definedBy("com.yoursweakfoe.common.ddd")
            .whereLayer("Application").mayOnlyBeAccessedByLayers(
                    "Configuration", "Infrastructure")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers(
                    "Application", "Configuration")
            .as("R1 DDD 三层依赖方向 + Configuration 层");

    /** R2 —— adapter 标记接口（RestAdapter / ScheduledAdapter）不得触碰 domain / infrastructure。 */
    @ArchTest
    static final ArchRule r2 = DddArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;

    /** R3 —— 框架 domain 骨架（AggregateRoot / Repository 接口等）不依赖任何外层。 */
    @ArchTest
    static final ArchRule r3 = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    /**
     * R4 —— 框架 domain 骨架框架中立（零 Spring import，教义自证）。
     * 业务扫描挂载同一常量守护 sample 的 stereotype 白名单边界，双向分发（审计 §6.2-7 修复项）。
     */
    @ArchTest
    static final ArchRule r4 = DddArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE;

    /** R5a —— 框架写端口（domain.repository.Repository）必须是 interface（相邻段直接命中，真实开火）。 */
    @ArchTest
    static final ArchRule r5a = DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    // 框架包自身无 adapter 层组件（标记接口位于 common-ddd/adapter/..，规则按空集通过），
    // 挂载以守护「标记接口所在的包结构不被破坏」。
    @ArchTest
    static final ArchRule r8a = DddArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    @ArchTest
    static final ArchRule r8b = DddArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;

    // 框架包自身无 dto 实现类（标记接口位于 common-ddd/application/dto/，规则按空集通过）
    @ArchTest
    static final ArchRule r10a = DddArchitectureRules.APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION;

    @ArchTest
    static final ArchRule r10b = DddArchitectureRules.APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED;

    // 同上：框架包自身无 scheduler 实现类（标记接口位于 common-ddd/adapter/task/scheduler/..），
    // 挂载以守护「scheduler 包结构 = 标记接口 + 被标记实现」不被破坏。
    @ArchTest
    static final ArchRule r14a = DddArchitectureRules.SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    @ArchTest
    static final ArchRule r14b = DddArchitectureRules.SCHEDULER_PACKAGE_CLASSES_MUST_BE_MARKED;
}
