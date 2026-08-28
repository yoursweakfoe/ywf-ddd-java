package com.yoursweakfoe.sampleapplication.sampleservice.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO;
import com.yoursweakfoe.common.ddd.application.event.outbox.IntegrationEventOutboxStore;
import com.yoursweakfoe.common.ddd.application.repository.application.QueryRepository;
import com.yoursweakfoe.common.ddd.application.service.ApplicationService;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
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

    /**
     * R1b —— 收窄 Infrastructure 对 Application 的访问白名单：读端口类型锚点 + 集成事件捕获端口。
     *
     * <p>与 common-test 通用规则（{@code INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES}）
     * 同构的本地覆写，仅扩展一类锚点：全链路 Outbox 的应用层捕获端口
     * {@code IntegrationEventOutboxStore}（application/event/outbox）由基础设施参考实现
     * {@code JdbcIntegrationEventOutboxStore}（infrastructure/event/outbox）实现——依赖倒置，
     * 与读侧 {@code QueryRepository} 先例同构（框架自身 DddArchitectureTest 同款先例），
     * 捕获装配 {@code OutboxReferenceConfig} 的 @Bean 签名亦引用该端口类型。
     * Handler / AppService / Assembler / Presenter 等其余 application 组件对
     * infrastructure 一律不可见。
     */
    @ArchTest
    static final ArchRule r1b_infra_access_application_only_ports = noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat(
                    resideInAPackage("..application..")
                            .and(not(resideInAPackage("..infrastructure..")))
                            .and(not(assignableTo(QueryRepository.class)
                                    .or(assignableTo(ApplicationDTO.class))
                                    .or(assignableTo(IntegrationEventOutboxStore.class))
                                    .or(nestedClassOfApplicationDtoImpl()))))
            .allowEmptyShould(true)
            .as("R1b Infrastructure 对 Application 的访问仅限读端口类型与集成事件捕获端口"
                    + "（QueryRepository 实现 / ApplicationDTO 及其嵌套类 / 捕获端口），其余一律禁止");

    /** ApplicationDTO 实现类的嵌套类（嵌套 DTO 随外层定型，见 R10b 约定，字节码上不携带标记）。 */
    private static DescribedPredicate<JavaClass> nestedClassOfApplicationDtoImpl() {
        return new DescribedPredicate<>("nested class of an ApplicationDTO implementation") {
            @Override
            public boolean test(JavaClass candidate) {
                JavaClass enclosing = candidate.getEnclosingClass().orElse(null);
                return enclosing != null && enclosing.isAssignableTo(ApplicationDTO.class);
            }
        };
    }

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

    /** R7a —— 域内反应监听器必须实现 DomainEventListener 标记。 */
    @ArchTest
    static final ArchRule r7a_event_listeners_marked = DDDArchitectureRules.EVENT_LISTENERS_ARE_MARKED;

    /** R7b —— 集成事件出站捕获器必须实现 IntegrationEventCapture 标记。 */
    @ArchTest
    static final ArchRule r7b_event_captures_marked = DDDArchitectureRules.EVENT_CAPTURES_ARE_MARKED;

    /** R7c —— AppService 不得直接依赖集成事件出站捕获器。 */
    @ArchTest
    static final ArchRule r7c_app_service_no_direct_capture =
            DDDArchitectureRules.APP_SERVICE_DOES_NOT_DEPEND_ON_EVENT_CAPTURE;

    /** R8a —— 实现 RestAdapter 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r8a_rest_entries_marked_in_adapter =
            DDDArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;

    /** R8b —— 类名以 ControllerImpl 结尾的类必须实现 RestAdapter 标记。 */
    @ArchTest
    static final ArchRule r8b_controller_impl_must_be_marked =
            DDDArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;

    /** R9a —— 实现 IntegrationEventConsumer 标记的类必须位于 adapter 层。 */
    @ArchTest
    static final ArchRule r9a_event_consumers_marked_in_adapter =
            DDDArchitectureRules.EVENT_CONSUMERS_ARE_MARKED_AND_IN_ADAPTER;

    /** R9b —— ..event.consumer.. 包下的类必须实现 IntegrationEventConsumer 标记。 */
    @ArchTest
    static final ArchRule r9b_event_consumer_package_marked =
            DDDArchitectureRules.EVENT_CONSUMER_PACKAGE_CLASSES_MUST_BE_MARKED;

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
