package com.yoursweakfoe.common.test.archunit;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.NESTED_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * DDD 分层架构守护规则集（ArchUnit 预定义规则工厂）。
 *
 * <p>提供标准 DDD 五模块架构（adapter / application / domain / infrastructure / contract）
 * 的通用合规性检查规则。业务服务只需在测试类中通过
 * {@code @AnalyzeClasses(packages = "com.xxx.service")} 指定扫描包，
 * 然后引用本类的静态规则即可。
 *
 * <h3>使用示例</h3>
 *
 * <pre>{@code
 * @AnalyzeClasses(
 *         packages = "com.yoursweakfoe.application.service",
 *         importOptions = ImportOption.DoNotIncludeTests.class)
 * public class ArchitectureTest {
 *
 *     @ArchTest
 *     static final ArchRule r1 = DDDArchitectureRules.LAYERED_ARCHITECTURE;
 *
 *     @ArchTest
 *     static final ArchRule r2 = DDDArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;
 *
 *     @ArchTest
 *     static final ArchRule r3 = DDDArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
 *
 *     @ArchTest
 *     static final ArchRule r4 = DDDArchitectureRules.DOMAIN_MODEL_IS_PURE;
 *
 *     @ArchTest
 *     static final ArchRule r5a = DDDArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;
 *
 *     @ArchTest
 *     static final ArchRule r5b = DDDArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;
 *
 *     @ArchTest
 *     static final ArchRule r10b = DDDArchitectureRules.APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED;
 *
 *     @ArchTest
 *     static final ArchRule r1b = DDDArchitectureRules.INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES;
 *
 *     @ArchTest
 *     static final ArchRule r11 = DDDArchitectureRules.COMMAND_HANDLERS_ARE_TRANSACTIONAL;
 *
 *     @ArchTest
 *     static final ArchRule r12 = DDDArchitectureRules.DOMAIN_HAS_NO_PUBLIC_SETTERS;
 *
 *     @ArchTest
 *     static final ArchRule r13 = DDDArchitectureRules.QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES;
 *
 *     @ArchTest
 *     static final ArchRule c1 = DDDArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;
 * }
 * }</pre>
 *
 * <h3>规则清单</h3>
 * <ul>
 *   <li>R1 —— DDD 四层依赖方向：adapter → application → domain ← infrastructure（依赖倒置）</li>
 *   <li>R2 —— adapter 只依赖 application/contract，不得直连 domain 或 infrastructure</li>
 *   <li>R3 —— domain 不依赖 application / infrastructure / adapter / contract</li>
 *   <li>R4 —— domain.model 不得依赖 MyBatis-Plus / Spring Stereotype / JPA</li>
 *   <li>R5a —— domain Repository 必须是 interface</li>
 *   <li>R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下</li>
 *   <li>R6 —— domain 不依赖 common-security（领域模型不感知认证上下文）</li>
 *   <li>R8a —— 实现 {@code RestAdapter} 标记的类必须位于 adapter 层（定型 REST 入口角色）</li>
 *   <li>R8b —— 类名以 ControllerImpl 结尾的类必须实现 {@code RestAdapter} 标记（堵命名漂移）</li>
 *   <li>R10a —— 实现 {@code ApplicationDTO} 标记的类必须位于 application 层（定型应用层内部视图角色）</li>
 *   <li>R10b —— {@code ..application..dto..} 包下的顶层类必须实现 {@code ApplicationDTO} 标记</li>
 *   <li>R1b —— Infrastructure 对 Application 的访问仅限读端口类型锚点（QueryRepository 实现 / ApplicationDTO），收窄 R1 读侧整层例外</li>
 *   <li>R11 —— CommandHandler.handle 必须标注 @Transactional（写侧事务边界强制）</li>
 *   <li>R12 —— Domain 层禁止 public setter（守护充血模型不变量）</li>
 *   <li>R13 —— QueryHandler 禁止触碰写侧仓储（CQRS 读侧只走 QueryRepository 读端口）</li>
 *   <li>C1 —— contract 纯契约模块，不得依赖 server 的 adapter/application/domain/infrastructure</li>
 * </ul>
 */
public final class DDDArchitectureRules {

    private DDDArchitectureRules() {}

    /**
     * R1 —— DDD 四层依赖方向（依赖倒置）：
     * adapter → application → domain ← infrastructure。
     *
     * <p>与经典分层不同，infrastructure 通过实现 domain 的 Repository / Portal 接口
     * 「倒置」接入，任何外层都不得直接依赖 infrastructure（DI 装配由 infrastructure.config 完成）。
     *
     * <p><strong>读侧例外</strong>：CQRS 读侧绕过 domain（PO → DTO 直接投影），读端口
     * （XxxQueryRepository）定义在 application 层、基础设施层实现之（XxxQueryRepositoryImpl），
     * 构成「写侧 infrastructure → domain」的读侧镜像「infrastructure → application」。
     * 故 Application 额外允许被 Infrastructure 访问（仅限读查询场景）。
     */
    public static final ArchRule LAYERED_ARCHITECTURE =
            Architectures.layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Adapter")
                    .definedBy("..adapter..")
                    .layer("Application")
                    .definedBy("..application..")
                    .layer("Domain")
                    .definedBy("..domain..")
                    .layer("Infrastructure")
                    .definedBy("..infrastructure..")
                    .whereLayer("Adapter")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application")
                    .mayOnlyBeAccessedByLayers("Adapter", "Infrastructure")
                    .whereLayer("Domain")
                    .mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                    .whereLayer("Infrastructure")
                    .mayNotBeAccessedByAnyLayer()
                    .as("""
                            R1 DDD 四层依赖方向：adapter → application → domain ← infrastructure；
                            外层不得直接依赖 infrastructure，domain 不得反向依赖任何层；
                            读侧例外：infrastructure 读查询实现可访问 application 读端口""");

    /**
     * R1b —— Infrastructure 对 Application 的访问仅限「读端口类型锚点」（CQRS 读侧例外收窄）。
     *
     * <p>R1 为读侧放行了 Infrastructure → Application 整层访问；本规则把该例外收窄为白名单，
     * 被依赖的 application 类必须满足其一：
     * <ul>
     *   <li>实现 {@code QueryRepository}（application 层读端口接口）</li>
     *   <li>实现 {@code ApplicationDTO}（读 DTO，作为查询签名的出入参）</li>
     *   <li>是上述 ApplicationDTO 实现类的<b>嵌套类</b>（嵌套 DTO 随外层定型，见 R10b 约定）</li>
     * </ul>
     * Handler / AppService / Assembler / Presenter 等其余 application 组件对
     * infrastructure 一律不可见——防止仓储实现反向调用应用层逻辑造成循环依赖。
     *
     * <p><strong>段匹配碰撞内置排除</strong>：{@code ..application..} 按包段匹配，会同时命中
     * infrastructure 下 {@code ..repository.application..} 子包（读实现及其匿名类所在包）。
     * 目标谓词已显式排除任何位于 {@code ..infrastructure..} 下的类——infra 内部的自依赖
     * （如实现类引用自己的匿名类）不属于本规则管辖范围。若业务服务的分层包命名存在其他
     * 同类碰撞且需更精确语义，用精确根包前缀覆写本规则（参见各业务服务
     * ApplicationArchitectureTest 对 R1/R3/R6 的精确前缀写法）。
     */
    private static final String QUERY_REPOSITORY_TYPE =
            "com.yoursweakfoe.common.ddd.application.repository.application.QueryRepository";

    private static final String APPLICATION_DTO_TYPE =
            "com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO";

    /**
     * R1b 读端口类型白名单：QueryRepository 实现 / ApplicationDTO 实现 /
     * ApplicationDTO 实现类的嵌套类（嵌套 DTO 随外层定型，见 R10b 约定，字节码上不携带标记）。
     */
    private static final DescribedPredicate<JavaClass> READ_PORT_TYPE_ANCHORS =
            assignableTo(QUERY_REPOSITORY_TYPE)
                    .or(assignableTo(APPLICATION_DTO_TYPE))
                    .or(new DescribedPredicate<JavaClass>("nested class of an ApplicationDTO implementation") {
                        @Override
                        public boolean test(JavaClass candidate) {
                            JavaClass enclosing = candidate.getEnclosingClass().orElse(null);
                            return enclosing != null && enclosing.isAssignableTo(APPLICATION_DTO_TYPE);
                        }
                    });

    public static final ArchRule INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat(
                            resideInAPackage("..application..")
                                    .and(not(resideInAPackage("..infrastructure..")))
                                    .and(not(READ_PORT_TYPE_ANCHORS)))
                    .allowEmptyShould(true)
                    .as("R1b Infrastructure 对 Application 的访问仅限读端口类型"
                            + "（QueryRepository 实现 / ApplicationDTO 及其嵌套类），其余 application 组件一律禁止");

    /**
     * R2 —— adapter 层（REST / MQ / 定时任务入口）只依赖 application 与 contract，
     * 不得直连 domain 或 infrastructure。
     */
    public static final ArchRule ADAPTER_ONLY_DEPENDS_ON_APPLICATION =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..domain..", "..infrastructure..")
                    .as("R2 Adapter 只依赖 Application/Contract，不得直连 Domain 或 Infrastructure");

    /**
     * R3 —— domain 层不得依赖 application / infrastructure / adapter / contract。
     *
     * <p>注意：ArchUnit 包匹配按「段」精确匹配（非子串），{@code ..application..} 只匹配
     * 包段名恰为 {@code application} 的包，不会误伤 {@code sampleapplication} 这类
     * 仅含 "application" 子串的根包。
     *
     * <p><strong>段匹配碰撞内置排除</strong>：{@code ..domain..} 按包段匹配，会同时命中
     * 其它层内部以 {@code domain} 命名的子包——本框架惯例用 {@code .domain} 后缀表达
     * 「某关注点的领域侧/领域对象」，如 infrastructure 下的
     * {@code ..repository.domain..}（仓储实现）。
     * 这些类按更深前缀归属于其所在层（受 R1b 等该层规则管辖），不属于本规则的域层主语——
     * 故主语谓词显式排除任何位于 application / infrastructure / adapter 包下的类。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that(resideInAPackage("..domain..")
                            .and(not(resideInAPackage("..infrastructure..")))
                            .and(not(resideInAPackage("..application..")))
                            .and(not(resideInAPackage("..adapter.."))))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..adapter..", "..contract..")
                    .as("R3 Domain 不依赖 application/infrastructure/adapter/contract");

    /** R4 —— Entity/ValueObject 不得依赖 MyBatis-Plus/Spring Stereotype/JPA 等基础设施技术栈。 */
    public static final ArchRule DOMAIN_MODEL_IS_PURE =
            noClasses()
                    .that()
                    .resideInAPackage("..domain.model..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.baomidou.mybatisplus..",
                            "org.springframework.stereotype..",
                            "jakarta.persistence..",
                            "javax.persistence..")
                    .as("R4 Domain 模型保持纯净：Entity/ValueObject 不得依赖 MyBatis-Plus/Spring Stereotype/JPA");

    /**
     * R5a —— domain Repository 必须是 interface。
     *
     * <p>框架/服务包可能不含任何 domain repository（空集），故允许空集通过。
     */
    public static final ArchRule DOMAIN_REPOSITORIES_MUST_BE_INTERFACES =
            classes()
                    .that()
                    .resideInAPackage("..domain.repository..")
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(true)
                    .as("R5a Domain Repository 必须是 interface（实现应放在 infrastructure.persistence..repository）");

    /**
     * R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下。
     *
     * <p>{@code ..} 通配段可匹配 {@code infrastructure.persistence.master.{agg}.repository}
     * 这类多数据源 + 按聚合分包的结构；空集时允许通过。
     */
    public static final ArchRule REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("RepositoryImpl")
                    .should()
                    .resideInAPackage("..infrastructure.persistence..repository..")
                    .allowEmptyShould(true)
                    .as("R5b 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下");

    /**
     * R8a —— 实现 {@code RestAdapter} 标记的类（adapter 层 REST 入口）必须位于 adapter 层。
     *
     * <p>{@code RestAdapter}（common-ddd/adapter/rest/controller/）为<strong>空标记</strong>，定型「REST
     * 入口适配器」角色：纯透传 ApplicationService，不含业务逻辑。本规则保证被标记的组件
     * 不泄漏到其他层。空集时允许通过（业务服务可能暂无 REST 入口——通常不会）。
     */
    public static final ArchRule REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter")
                    .should()
                    .resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .as("R8a 实现 RestAdapter 标记的类必须位于 adapter 层（REST 入口角色）");

    /**
     * R8b —— 类名以 ControllerImpl 结尾的类必须实现 {@code RestAdapter} 标记（堵命名漂移）。
     *
     * <p>识别 REST 入口用「类型锚点」而非「名字猜测」：实现 contract 的 {@code XxxController}
     * 契约接口之外，还必须实现 {@code RestAdapter} 标记，否则视为命名漂移。
     */
    public static final ArchRule CONTROLLER_IMPL_NAMING_MUST_BE_MARKED =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("ControllerImpl")
                    .should()
                    .implement("com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter")
                    .allowEmptyShould(true)
                    .as("R8b 类名以 ControllerImpl 结尾的类必须实现 RestAdapter 标记（识别锚点用类型而非名字）");

    /**
     * R10a —— 实现 {@code ApplicationDTO} 标记的类（application 层内部视图）必须位于 application 层。
     *
     * <p>{@code ApplicationDTO}（common-ddd/application/dto/）为<strong>空标记</strong>，定型
     * 「应用层内部视图」角色：写侧 DTO（含 version）+ 读侧 DTO（投影），与 contract 层对外
     * {@code CO} 标记对偶。本规则保证被标记的组件不泄漏到其他层。空集时允许通过。
     */
    public static final ArchRule APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO")
                    .should()
                    .resideInAPackage("..application..")
                    .allowEmptyShould(true)
                    .as("R10a 实现 ApplicationDTO 标记的类必须位于 application 层（应用层内部视图角色）");

    /**
     * R10b —— {@code ..application..dto..} 包下的<strong>顶层</strong>类必须实现
     * {@code ApplicationDTO} 标记。
     *
     * <p>识别应用层内部视图用类型锚点而非包名猜测。排除嵌套类：嵌套 DTO（如
     * {@code OrderDTO.OrderItemDTO}）随外层定型，不重复标记（javadoc 约定）。
     */
    public static final ArchRule APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED =
            classes()
                    .that()
                    .resideInAPackage("..application..dto..")
                    .and(not(NESTED_CLASSES))
                    .and()
                    .areNotInterfaces()
                    .should()
                    .implement("com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO")
                    .allowEmptyShould(true)
                    .as("R10b ..application..dto.. 包下的顶层类必须实现 ApplicationDTO 标记（嵌套类除外）");

    /**
      * C1 —— contract 纯契约模块（Service 接口 + CQE + CO + 枚举），
     * 不得依赖 server 侧的 adapter / application / domain / infrastructure，
     * 也不得依赖 Spring / MyBatis 运行时基础设施（DI / Bean / 持久化）。
     *
     * <p>注意：重契约（ADR-0003）下 contract 允许携带 Spring MVC 映射注解（spring-web）、
     * Swagger 文档注解与 Jakarta 校验注解以声明 HTTP 面（映射经 adapter 实现类继承），
     * 但仅限「注解级」依赖，不得引入任何 Spring 运行时基础设施或 MyBatis。
     */
    public static final ArchRule CONTRACT_DOES_NOT_DEPEND_ON_SERVER =
            noClasses()
                    .that()
                    .resideInAPackage("..contract..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..", "..application..", "..domain..", "..infrastructure..",
                            "org.springframework.stereotype..",
                            "org.springframework.context..",
                            "org.springframework.beans..",
                            "com.baomidou.mybatisplus..")
                    .as("C1 Contract 纯契约：不得依赖 server 四层及 Spring/MyBatis 运行时基础设施");

    /**
     * R6 —— Domain 层不得依赖 common-security（领域模型不感知认证上下文）。
     *
     * <p>{@code SecurityUtil} 仅允许在 Application / Adapter 层调用（见 .agents/rules/03），
     * 领域模型不感知认证上下文，故 domain 不得依赖 common-security 的任何类。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_SECURITY =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.yoursweakfoe.common.security..")
                    .as("R6 Domain 不依赖 common-security（SecurityUtil 仅限 Application/Adapter 层）");

    /**
     * R11 —— 写侧事务边界强制：{@code CommandHandler} 实现类的 {@code handle} 方法必须标注
     * {@code @Transactional(rollbackFor = Exception.class)}。
     *
     * <p>写侧固定模式「load → 聚合行为 → save」的原子性由 Handler 入口事务保证；仓储支撑类
     * （{@code MybatisPlusPersistence}）刻意不声明事务（边界上收至应用层），因此漏标注解时
     * 多次持久化将各自提交、中途失败不回滚——本规则将该遗漏从纪律约束变为编译期后置红线。
     * 读侧 {@code QueryHandler} 刻意豁免（只读可省事务，见编码规范 03）。
     */
    public static final ArchRule COMMAND_HANDLERS_ARE_TRANSACTIONAL =
            methods()
                    .that()
                    .haveName("handle")
                    .and()
                    .areDeclaredInClassesThat()
                    .implement("com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler")
                    .should()
                    .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                    .allowEmptyShould(true)
                    .as("R11 CommandHandler.handle 必须标注 @Transactional"
                            + "（写侧事务边界由应用层保证，框架不兜底）");

    /**
     * R12 —— Domain 层禁止 public setter：名称匹配 {@code setXxx} 的方法不得为 public。
     *
     * <p>充血模型的根基是「状态变迁只通过行为方法」——setter 允许外部绕过聚合根的状态机守卫
     * 与不变量校验直接改写内部状态。本规则把禁止清单中的该条目机器化（含 Lombok @Data 在
     * domain 类上生成的 setter，一并拦截）。返回 {@code this} 的流式风格 setter 同样命中，
     * 属预期行为。
     */
    public static final ArchRule DOMAIN_HAS_NO_PUBLIC_SETTERS =
            noMethods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideInAPackage("..domain..")
                    .and()
                    .haveNameMatching("set[A-Z].*")
                    .should()
                    .bePublic()
                    .allowEmptyShould(true)
                    .as("R12 Domain 层禁止 public setter（状态变迁只经行为方法，保护聚合不变量）");

    /**
     * R13 —— CQRS 读写隔离强制：{@code QueryHandler} 实现类不得依赖写侧仓储
     * （{@code ..domain..repository..} 包下的任何类型）。
     *
     * <p>读侧固定模式要求查询完全绕过 domain：只经 application 层 {@code QueryRepository}
     * 读端口做 PO → 读 DTO 投影。若读处理器图方便注入写侧 Repository 加载聚合根，
     * 读写分离即在事实上瓦解。本规则同时覆盖接口与实现类（{@code ..domain..repository..}
     * 段匹配也会命中 infrastructure 下 {@code ..repository.domain..} 子包的仓储实现类），
     * 误依赖实现类同样被拦截。
     */
    public static final ArchRule QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES =
            noClasses()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.application.handler.query.QueryHandler")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain..repository..")
                    .allowEmptyShould(true)
                    .as("R13 QueryHandler 禁止触碰写侧仓储（CQRS 读侧只走 QueryRepository 读端口）");

    /**
     * R14a —— 实现 {@code ScheduledAdapter} 标记的类（adapter 层定时任务入口）必须位于 adapter 层。
     *
     * <p>{@code ScheduledAdapter}（common-ddd/adapter/task/scheduler/）为<strong>空标记</strong>，
     * 定型「时间驱动入口」角色：{@code @Scheduled} 触发 → 透传 ApplicationService。
      * 与 REST（R8）并列的 driving adapter。空集时允许通过
     * （当前示例应用无定时任务实现，标记为框架预留，模板见 cookbook/scheduled-task.md）。
     */
    public static final ArchRule SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter")
                    .should()
                    .resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .as("R14a 实现 ScheduledAdapter 标记的类必须位于 adapter 层（定时任务入口角色）");

    /**
     * R14b —— <strong>业务服务</strong> {@code ..adapter..scheduler..} 包下的<strong>非接口</strong>类必须实现
     * {@code ScheduledAdapter} 标记。
     *
     * <p>与 R8b 同理：识别定时任务入口用类型锚点而非包名猜测。排除接口自身
     * （标记接口位于 {@code ..adapter.task.scheduler..}，接口不实现自己）。
     *
     * <p><strong>主语收窄至 {@code ..adapter..scheduler..}</strong>：本规则只约束业务服务的时间驱动
     * <strong>入口</strong>（driving adapter，经 AppService 驱动用例）。与 R8b 同一逻辑，
     * ArchUnit 守护业务分层，不反向约束框架内部结构。
     */
    public static final ArchRule SCHEDULER_PACKAGE_CLASSES_MUST_BE_MARKED =
            classes()
                    .that()
                    .resideInAPackage("..adapter..scheduler..")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .implement("com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter")
                    .allowEmptyShould(true)
                    .as("R14b 业务 ..adapter..scheduler.. 包下的类必须实现 ScheduledAdapter 标记");
}
