package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.yoursweakfoe.common.ddd.application.service.ApplicationService;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import com.yoursweakfoe.common.test.archunit.DddArchitectureRules;

/**
 * 业务侧架构扫描 —— 对 {@code com.yoursweakfoe.sampleapplication.sampleservice} 执行架构合规性校验。
 *
 * <p><strong>历史：BASE 覆写四件套已随包命名税迁移退役，全部回归共享挂载。</strong>
 * 旧版本因框架惯例「按实现的接口归属为子包命名」（{@code repository.domain} /
 * {@code repository.application} 借用保留段），R1 / R1b / R3 / R6 不得不以精确包前缀
 * （{@code com.yoursweakfoe.sampleapplication.sampleservice.}）本地覆写，否则段匹配会把
 * infra 子包误判成 domain/application 层主语。2026-09-05 迁移（阶段一）根除了保留段借用、
 * 规则合并（阶段二）删除了全部排除谓词后，本地覆写失去存在理由：
 * <ul>
 *   <li>R1b —— 上一版本已与共享常量逐段同构，直接挂载；</li>
 *   <li>R1 / R3 / R6 —— 本版本删除本地 BASE 版，挂载
 *       {@code DddArchitectureRules.LAYERED_ARCHITECTURE} /
 *       {@code DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS} /
 *       {@code DOMAIN_DOES_NOT_DEPEND_ON_SECURITY}（共享常量就此获得本仓首次真实挂载）。</li>
 * </ul>
 * 契约接口（{@code contract.{agg}.adapter.rest.controller}，与框架
 * {@code RestAdapter} 坐标精确对偶）按段匹配成为 R1 的 Adapter 层成员——
 * {@code ControllerImpl→契约接口} 属同层访问，分层 DSL 不禁同层，语义自洽（实证于本扫描绿）。
 *
 * <p>其余历史本地副本去向（勿再复制粘贴）：
 * <ul>
 *   <li>A2（domain 禁 Spring 运行时、白名单 stereotype）→ 已上收进共享库并重写为 R4
 *       {@code DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE}（见下方 r4 挂载），本类不再保有副本。</li>
 *   <li>R15（com.baomidou 全仓禁令）→ 规则本体已删除（规则库不为「项目选择不用的库」立特别法；
 *       dynamic-datasource 经 BOM 显式推荐，{@code @DS} 不再触任何红线）。</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    // ── 共享规则挂载（2026-09-05 命名税迁移后零本地谓词） ──

    /**
     * R1 —— DDD 四层依赖方向（依赖倒置）。本版本由 BASE 本地覆写转正为共享挂载：
     * 迁移后包名不再存在会让段匹配两层歧义的保留段借用（见类头与规则库类头不变量）。
     */
    @ArchTest
    static final ArchRule r1_layered_dependency = DddArchitectureRules.LAYERED_ARCHITECTURE;

    /**
     * R1b —— 收窄 Infrastructure 对 Application 的访问白名单：仅读端口类型锚点
     * （QueryRepository 实现 / ApplicationDTO 及其嵌套类）。
     *
     * <p>共享常量即本仓 sample 场景的定型形态（其旧内置的 infra 排除已随命名迁移删除），
     * Handler / AppService / Assembler / Presenter 等其余 application 组件对
     * infrastructure 一律不可见。
     */
    @ArchTest
    static final ArchRule r1b_infra_access_application_only_ports =
            DddArchitectureRules.INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES;

    /** R3 —— Domain 不依赖 application/infrastructure/adapter/contract。本版本 BASE 覆写退役。 */
    @ArchTest
    static final ArchRule r3_domain_no_outer =
            DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    /** R6 —— Domain 不依赖 common-security（SecurityUtil 仅限 Application/Adapter 层）。本版本 BASE 覆写退役。 */
    @ArchTest
    static final ArchRule r6_domain_no_security =
            DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_SECURITY;

    /**
     * R4 —— Domain 框架中立：禁 Spring 运行时依赖（org.springframework.stereotype 装配注解
     * 唯一豁免）与 JPA 持久化注解。原 sample 本地 A2 白名单教义已上收为共享规则并双扫描分发；
     * sample 的 {@code OrderFactory}（@Component）/ {@code InventoryDomainService}（@Service）
     * 经 stereotype 白名单放行，规则对其真实命中 subject 而非空转
     * （负证明见 {@link DomainPurityRuleProofTest}）。
     */
    @ArchTest
    static final ArchRule r4_domain_framework_neutral =
            DddArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE;

    /** R5a —— Domain Repository 必须是 interface。 */
    @ArchTest
    static final ArchRule r5a_repository_interfaces =
            DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    /**
     * R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下。
     * 上一版本新挂载（审计 §6.2-6 接线修复）：sample 的 order/product × 写/读 共 4 个 Impl
     * 真实命中 subject（迁移后并级于 {@code ...master.{agg}.repository}，规则宾语照常命中）。
     */
    @ArchTest
    static final ArchRule r5b_repository_impl_in_infra =
            DddArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;

    /** C1 —— Contract 纯契约，不得依赖 server 四层及 Spring 运行时基础设施（重契约例外见常量 javadoc）。 */
    @ArchTest
    static final ArchRule c1_contract_pure = DddArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;

    /** R8a —— 实现 RestAdapter 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r8a_rest_entries_marked_in_adapter =
            DddArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    /** R8b —— 类名以 ControllerImpl 结尾的类必须实现 RestAdapter 标记。 */
    @ArchTest
    static final ArchRule r8b_controller_impl_must_be_marked =
            DddArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;

    /** R14a —— 实现 ScheduledAdapter 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r14a_scheduled_entries_marked_in_adapter =
            DddArchitectureRules.SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    /** R14b —— ..scheduler.. 包下的类必须实现 ScheduledAdapter 标记。 */
    @ArchTest
    static final ArchRule r14b_scheduler_package_marked =
            DddArchitectureRules.SCHEDULER_PACKAGE_CLASSES_MUST_BE_MARKED;

    /** R10a —— 实现 ApplicationDTO 标记的类必须位于 application 层。 */
    @ArchTest
    static final ArchRule r10a_application_dtos_marked_in_application =
            DddArchitectureRules.APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION;

    /** R10b —— ..application..dto.. 包下的顶层类必须实现 ApplicationDTO 标记。 */
    @ArchTest
    static final ArchRule r10b_application_dto_package_marked =
            DddArchitectureRules.APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED;

    /** R11 —— CommandHandler.handle 必须标注 @Transactional（写侧事务边界强制）。 */
    @ArchTest
    static final ArchRule r11_command_handlers_transactional =
            DddArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL;

    /** R12 —— Domain 层禁止 public setter（守护充血模型不变量）。 */
    @ArchTest
    static final ArchRule r12_domain_no_public_setters =
            DddArchitectureRules.DOMAIN_HAS_NO_PUBLIC_SETTERS;

    /** R13 —— QueryHandler 禁止依赖任何 Repository 类型（CQRS 读写隔离，类型锚点识别）。 */
    @ArchTest
    static final ArchRule r13_query_handlers_no_write_repository =
            DddArchitectureRules.QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES;

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

    /** A4 —— 应用服务（实现 {@code ApplicationService} 标记）必须在 application.. 包下。 */
    @ArchTest
    static final ArchRule a4_app_service_in_application = classes()
            .that()
            .implement(ApplicationService.class)
            .should()
            .resideInAPackage("..application..")
            .allowEmptyShould(true)
            .as("A4 应用服务（implements ApplicationService）必须在 application.. 包下");

    /** A4b —— 类名以 AppService 结尾的类必须实现 {@code ApplicationService} 标记（堵命名漂移）。 */
    @ArchTest
    static final ArchRule a4b_app_service_naming_must_be_marked = classes()
            .that()
            .haveSimpleNameEndingWith("AppService")
            .should()
            .implement(ApplicationService.class)
            .allowEmptyShould(true)
            .as("A4b 类名以 AppService 结尾的类必须实现 ApplicationService 标记（识别锚点用类型而非名字）");
}
