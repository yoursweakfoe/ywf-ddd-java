package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.ddd.application.service.ApplicationService;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
import com.yoursweakfoe.common.test.archunit.DDDArchitectureRules;

/**
 * 业务侧架构扫描 —— 对 {@code com.yoursweakfoe.sampleapplication.sampleservice} 执行架构合规性校验。
 *
 * <p><strong>本地覆写规则（仅这四条）</strong>：R1 / R3 / R6 使用<b>精确包前缀</b>
 * （{@code com.yoursweakfoe.sampleapplication.sampleservice.}）而非通用 {@code ..domain..} 段匹配——
 * 因为 infra 层存在 {@code repository.domain} / {@code repository.application} 子包
 * （按"实现哪个层的接口"命名），若用段匹配会把它们误判成 domain/application 层主语。
 * R1 覆写另因：共享 LAYERED_ARCHITECTURE 的四层段划分会把 {@code ...repository.domain.OrderRepositoryImpl}
 * 同时吞入 Domain 与 Infrastructure 两「层」，其依赖 Mapper/PO 即成「Domain 依赖 Infrastructure」冤案。
 * 其余规则（R1b / R4 / R5a / R5b / C1 / R8 / R10 / R11 / R12 / R13 / R14 / A3 / A4）直接挂载
 * common-test 共享常量或本类独有教义。
 *
 * <p>历史本地副本去向（本版本收口，勿再复制粘贴）：
 * <ul>
 *   <li>A2（domain 禁 Spring 运行时、白名单 stereotype）→ 已上收进共享库并重写为 R4
 *       {@code DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE}（见下方 r4 挂载），本类不再保有副本。</li>
 *   <li>R1b 本地实现 → 谓词与共享常量完全同构（当年即照着共享常量抄的），现直接挂载共享常量。</li>
 *   <li>R15（com.baomidou 全仓禁令）→ 规则本体已删除（规则库不为「项目选择不用的库」立特别法；
 *       dynamic-datasource 经 BOM 显式推荐，{@code @DS} 不再触任何红线）。</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ApplicationArchitectureTest {

    /** 本应用根包前缀（用于精确匹配顶层分层，避免误伤 infra 下的 repository.domain/application 子包） */
    private static final String BASE = "com.yoursweakfoe.sampleapplication.sampleservice.";

    // ── 精确包前缀覆写（因 repository.domain / repository.application 子包命名碰撞） ──

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

    /**
     * R1b —— 收窄 Infrastructure 对 Application 的访问白名单：仅读端口类型锚点
     * （QueryRepository 实现 / ApplicationDTO 及其嵌套类）。
     *
     * <p>共享常量即为本仓 sample 场景设计（其 {@code not(resideInAPackage("..infrastructure.."))}
     * 已内置处理 {@code repository.application} 子包碰撞），此前本地副本与共享谓词逐段同构——
     * 现直接挂载共享常量，消除「共享规则无人接线、无声腐烂」的缺口（审计 §6.2-5）。
     * Handler / AppService / Assembler / Presenter 等其余 application 组件对
     * infrastructure 一律不可见。
     */
    @ArchTest
    static final ArchRule r1b_infra_access_application_only_ports =
            DDDArchitectureRules.INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES;

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

    // ── 复用 DDDArchitectureRules 共享规则 ──

    /**
     * R4 —— Domain 框架中立：禁 Spring 运行时依赖（org.springframework.stereotype 装配注解
     * 唯一豁免）与 JPA 持久化注解。原 sample 本地 A2 白名单教义已上收为共享规则并双扫描分发；
     * sample 的 {@code OrderFactory}（@Component）/ {@code InventoryDomainService}（@Service）
     * 经 stereotype 白名单放行，规则对其真实命中 subject 而非空转
     * （负证明见 {@link DomainPurityRuleProofTest}）。
     */
    @ArchTest
    static final ArchRule r4_domain_framework_neutral =
            DDDArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE;

    /** R5a —— Domain Repository 必须是 interface。 */
    @ArchTest
    static final ArchRule r5a_repository_interfaces =
            DDDArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    /**
     * R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下。
     * 本版本新挂载（审计 §6.2-6 接线修复）：sample 的 order/product × 写/读 共 4 个 Impl
     * 真实命中 subject——此前该规则只挂在框架扫描（框架无 Impl 类、恒空转），
     * 业务服务把 Impl 放错层可过关。
     */
    @ArchTest
    static final ArchRule r5b_repository_impl_in_infra =
            DDDArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;

    /** C1 —— Contract 纯契约，不得依赖 server 四层及 Spring 运行时基础设施（重契约例外见常量 javadoc）。 */
    @ArchTest
    static final ArchRule c1_contract_pure = DDDArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;

    /** R8a —— 实现 RestAdapter 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r8a_rest_entries_marked_in_adapter =
            DDDArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    /** R8b —— 类名以 ControllerImpl 结尾的类必须实现 RestAdapter 标记。 */
    @ArchTest
    static final ArchRule r8b_controller_impl_must_be_marked =
            DDDArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;

    /** R14a —— 实现 ScheduledAdapter 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r14a_scheduled_entries_marked_in_adapter =
            DDDArchitectureRules.SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    /** R14b —— ..scheduler.. 包下的类必须实现 ScheduledAdapter 标记。 */
    @ArchTest
    static final ArchRule r14b_scheduler_package_marked =
            DDDArchitectureRules.SCHEDULER_PACKAGE_CLASSES_MUST_BE_MARKED;

    /** R10a —— 实现 ApplicationDTO 标记的类必须位于 application 层。 */
    @ArchTest
    static final ArchRule r10a_application_dtos_marked_in_application =
            DDDArchitectureRules.APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION;

    /** R10b —— ..application..dto.. 包下的顶层类必须实现 ApplicationDTO 标记。 */
    @ArchTest
    static final ArchRule r10b_application_dto_package_marked =
            DDDArchitectureRules.APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED;

    /** R11 —— CommandHandler.handle 必须标注 @Transactional（写侧事务边界强制）。 */
    @ArchTest
    static final ArchRule r11_command_handlers_transactional =
            DDDArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL;

    /** R12 —— Domain 层禁止 public setter（守护充血模型不变量）。 */
    @ArchTest
    static final ArchRule r12_domain_no_public_setters =
            DDDArchitectureRules.DOMAIN_HAS_NO_PUBLIC_SETTERS;

    /** R13 —— QueryHandler 禁止触碰写侧仓储（CQRS 读写隔离）。 */
    @ArchTest
    static final ArchRule r13_query_handlers_no_write_repository =
            DDDArchitectureRules.QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES;

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
