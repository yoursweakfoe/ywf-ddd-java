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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * DDD 分层架构守护规则集（ArchUnit 预定义规则工厂）。
 *
 * <h3>本文件是什么</h3>
 *
 * <p>提供标准 DDD 五模块架构（adapter / application / domain / infrastructure / contract）
 * 的通用合规性检查规则。业务服务只需在测试类中通过
 * {@code @AnalyzeClasses(packages = "com.xxx.service")} 指定扫描包，然后引用本类的静态规则即可。
 * 规则编号（R1、R1b、R4……）是本项目的教义锚点，被
 * {@code docs/common/common-test.md §2}、{@code .agents/rules/}、各技能文档反复引用，
 * <strong>永不重排、永不复用</strong>；规则删除时编号作废（见变更记录）。
 *
 * <h3>模块地图（常量按主题分块，代码内以横幅注释分隔）</h3>
 * <ul>
 *   <li><strong>块1 层间依赖方向</strong> —— 谁能依赖谁：R1 / R1b / R2 / R3</li>
 *   <li><strong>块2 领域纯净</strong> —— domain 内部不许出现什么：R4 / R6 / R12</li>
 *   <li><strong>块3 装配位置契约</strong> —— 实现类与事务边界放哪：R5a / R5b / R11 / R13</li>
 *   <li><strong>块4 注解与标记契约</strong> —— 类型锚点（标记接口）与命名对偶：R8a / R8b / R10a / R10b / R14a / R14b</li>
 *   <li><strong>块5 契约模块</strong> —— contract 独立性：C1</li>
 * </ul>
 *
 * <h3>本仓内谁在消费这些规则（两个扫描入口）</h3>
 * <ul>
 *   <li><strong>框架扫描</strong> {@code DddArchitectureTest}
 *       （packages = {@code com.yoursweakfoe.common.ddd}）：让框架自己先过一遍教义。
 *       R1 在此被覆写为「四层 + Configuration 层」（框架自动配置类跨层装配所需）。</li>
 *   <li><strong>业务扫描</strong> {@code ApplicationArchitectureTest}
 *       （packages = {@code com.yoursweakfoe.sampleapplication.sampleservice}）：
 *       R1 / R1b / R3 / R6 使用 {@code BASE} 精确包前缀覆写（见下方「包段碰撞」），
 *       其余直接挂载本库常量。</li>
 *   <li><strong>负证明</strong> {@code DomainPurityRuleProofTest}：证明 R4 会失败而非恒真
 *       （配套夹具 {@code com.yoursweakfoe.archproof.domain.FrameworkLeakProbe}，
 *       位于两个扫描根之外，不污染任何 {@code @ArchTest}）。</li>
 * </ul>
 *
 * <h3>包段碰撞（本库谓词普遍带排除列表的根因）</h3>
 *
 * <p>本框架惯例「按实现的接口归属」为子包命名：infrastructure 下存在
 * {@code ..repository.domain..}（写仓储实现）、application 下存在 {@code ..repository.application..}
 * （读端口定义）。ArchUnit 的 {@code ..domain..} / {@code ..application..} 按<strong>包段</strong>
 * 匹配（段名精确、位置任意、不相邻不匹配），这些子包会被误认为「属于 domain / application 层」。
 * 因此凡主语取 {@code ..domain..} 的规则都必须显式排除位于 infrastructure / application /
 * adapter 段下的类（见 {@link #PURE_DOMAIN_CLASSES}）；消费方若包结构更规整可用段匹配，
 * 否则参照 sample 用精确根包前缀覆写。彻底解法（规则工厂参数化根包）登记在案的演进方向，本版本未实施。
 *
 * <h3>空集通过（vacuous pass）警示 —— 读本文件必须知道的结构性弱点</h3>
 *
 * <p>消费方测试类路径中的 {@code archunit.properties} 全局声明
 * {@code archRule.failOnEmptyShould=false}，且多数常量额外写了 {@code allowEmptyShould(true)}：
 * <strong>规则匹配不到任何类时无声通过</strong>。「零命中」与「规则写错成永不为假」在结果上不可区分
 * ——旧 R4（{@code ..domain.model..} 相邻匹配，永不命中规范布局 {@code domain.{agg}.model}）
 * 正是被此机制掩护的空文（见变更记录）。现状：
 * <ul>
 *   <li>有负证明的规则：<strong>仅 R4</strong>（DomainPurityRuleProofTest，违例必失败 +
 *       真实 stereotype 载体必通过，双向锁死）。</li>
 *   <li>其余禁则类规则（R1b / R2 / R3 / R6 / R13）的「非空转」依赖两个扫描的挂载：
 *       框架扫描的 subject 集（common-ddd 的 domain / adapter 类）恒非空，业务扫描的
 *       subject 集（sample 聚合类）随业务增长非空——subject 为空时它们退化为空集通过。</li>
 *   <li>为「包结构锚点」而挂载的空集 {@code @ArchTest}（框架扫描的 r8a / r8b / r10a / r10b /
 *       r14a / r14b）守护的是「标记接口所在包结构不被破坏」，属既定教义例外，挂载处有逐条注释。</li>
 * </ul>
 *
 * <h3>已知缺口（登记，本文件不实现）</h3>
 *
 * <p>以下教义有、规则无，全部来自 2026-09-05 发版审计 §6.3，读规则时必须知道红线图并非全图：
 * <ul>
 *   <li>Handler 必须返回 DTO（禁 CO）—— 无规则（可静态检查 handle 返回类型后缀）。</li>
 *   <li>AppService 返回 CO / 不含编排逻辑 —— 无规则（CO 返回可查，编排语义不可查）。</li>
 *   <li>时间类型统一 {@code OffsetDateTime}（禁 {@code LocalDateTime}/{@code ZonedDateTime}
 *       持久化）—— 无规则，纯靠 {@code .agents/rules/04} 纪律。</li>
 *   <li>禁止具名领域异常（统一 BusinessException）—— 无规则（可查：domain 内不得有
 *       RuntimeException 子类）。</li>
 *   <li>PO 零 ORM 注解 —— 无规则，现靠自觉（sample 达标）。</li>
 *   <li>R11 只查 {@code @Transactional} 存在性，<strong>不查</strong> javadoc 承诺的
 *       {@code rollbackFor = Exception.class} —— 语义缺口按教义登记，改规则或改注释二选一尚未裁决。</li>
 *   <li>{@code handler.command} 包内非 CommandHandler 类（{@code RetryablePlaceOrderHandler}
 *       型编排包装器）逃逸 R11 / R13 —— 无规则；1 CQE : 2 Handler 的教义回应尚未裁决。</li>
 * </ul>
 *
 * <h3>变更记录</h3>
 * <ul>
 *   <li><strong>本版本 · R15（全仓禁 {@code com.baomidou..} 禁令）删除</strong>
 *       ——规则库不为「项目选择不用的库」立特别法；多数据源库（dynamic-datasource）经 BOM 显式推荐，
 *       使用 {@code @DS} 不再触任何红线。连带删除其负证明夹具体系（sample 测试侧）。</li>
 *   <li><strong>本版本 · R4 重写</strong>：旧谓词 {@code ..domain.model..} 为相邻段匹配，
 *       对规范布局 {@code domain.{agg}.model} 永不相邻 = 空文（审计 §6.1-1）；
 *       且旧禁令连 {@code org.springframework.stereotype} 一并禁，与既有教义例外
 *       （原 sample 本地 A2 白名单）三方矛盾（审计 §6.1-2）。新 R4 主语改 {@code ..domain..}
 *       段匹配（带层排除），白名单 stereotype、禁其余 Spring 运行时与 JPA；
 *       A2 上收进本库（审计 §6.2-7），sample 本地副本随之删除。</li>
 *   <li><strong>本版本 · 接线修复</strong>：R1b 共享常量原为零挂载死规则，sample 本地副本
 *       与其谓词完全同构，现改挂载共享常量；R5b 原仅挂框架扫描（框架无 *RepositoryImpl，
 *       恒空转），现挂载业务扫描（真实命中 4 个 Impl 类）、框架扫描挂载撤销。</li>
 * </ul>
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
 *     static final ArchRule r1b =
 *             DDDArchitectureRules.INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES;
 *
 *     @ArchTest
 *     static final ArchRule r2 = DDDArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;
 *
 *     @ArchTest
 *     static final ArchRule r3 = DDDArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
 *
 *     @ArchTest
 *     static final ArchRule r4 = DDDArchitectureRules.DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE;
 *
 *     @ArchTest
 *     static final ArchRule r5a = DDDArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;
 *
 *     @ArchTest
 *     static final ArchRule r5b = DDDArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;
 *
 *     @ArchTest
 *     static final ArchRule r8a = DDDArchitectureRules.REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;
 *
 *     @ArchTest
 *     static final ArchRule r8b = DDDArchitectureRules.CONTROLLER_IMPL_NAMING_MUST_BE_MARKED;
 *
 *     @ArchTest
 *     static final ArchRule r10a = DDDArchitectureRules.APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION;
 *
 *     @ArchTest
 *     static final ArchRule r10b = DDDArchitectureRules.APPLICATION_DTO_PACKAGE_CLASSES_MUST_BE_MARKED;
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
 *     static final ArchRule r14a = DDDArchitectureRules.SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER;
 *
 *     @ArchTest
 *     static final ArchRule r14b = DDDArchitectureRules.SCHEDULER_PACKAGE_CLASSES_MUST_BE_MARKED;
 *
 *     @ArchTest
 *     static final ArchRule c1 = DDDArchitectureRules.CONTRACT_DOES_NOT_DEPEND_ON_SERVER;
 * }
 * }</pre>
 *
 * <h3>规则清单（速览，详解见各常量 javadoc）</h3>
 * <ul>
 *   <li>R1 —— DDD 四层依赖方向：adapter → application → domain ← infrastructure（依赖倒置）</li>
 *   <li>R1b —— Infrastructure 对 Application 的访问仅限读端口类型锚点（QueryRepository 实现 /
 *       ApplicationDTO），收窄 R1 读侧整层例外</li>
 *   <li>R2 —— adapter 只依赖 application/contract，不得直连 domain 或 infrastructure</li>
 *   <li>R3 —— domain 不依赖 application / infrastructure / adapter / contract</li>
 *   <li>R4 —— domain 框架中立：禁 Spring 运行时依赖（stereotype 装配注解唯一豁免）与 JPA 注解</li>
 *   <li>R5a —— domain Repository 必须是 interface</li>
 *   <li>R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下</li>
 *   <li>R6 —— domain 不依赖 common-security（领域模型不感知认证上下文）</li>
 *   <li>R8a —— 实现 {@code RestAdapter} 标记的类必须位于 adapter 层（定型 REST 入口角色）</li>
 *   <li>R8b —— 类名以 ControllerImpl 结尾的类必须实现 {@code RestAdapter} 标记（堵命名漂移）</li>
 *   <li>R10a —— 实现 {@code ApplicationDTO} 标记的类必须位于 application 层（应用层内部视图角色）</li>
 *   <li>R10b —— {@code ..application..dto..} 包下的顶层类必须实现 {@code ApplicationDTO} 标记</li>
 *   <li>R11 —— CommandHandler.handle 必须标注 @Transactional（写侧事务边界强制）</li>
 *   <li>R12 —— Domain 层禁止 public setter（守护充血模型不变量）</li>
 *   <li>R13 —— QueryHandler 禁止触碰写侧仓储（CQRS 读侧只走 QueryRepository 读端口）</li>
 *   <li>R14a —— 实现 {@code ScheduledAdapter} 标记的类必须位于 adapter 层（定时任务入口角色）</li>
 *   <li>R14b —— {@code ..adapter..scheduler..} 包下非接口类必须实现 {@code ScheduledAdapter} 标记</li>
 *   <li>C1 —— contract 纯契约模块，不得依赖 server 四层及 Spring 运行时基础设施</li>
 * </ul>
 */
public final class DDDArchitectureRules {

    private DDDArchitectureRules() {}

    // ═══════════════════════════════════════════════════════════════════════
    // 块1 · 层间依赖方向 —— 谁能依赖谁（R1 / R1b / R2 / R3）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R1 —— DDD 四层依赖方向（依赖倒置）：adapter → application → domain ← infrastructure。
     *
     * <p><strong>守护什么</strong>：依赖箭头只能沿上述方向出现——外层不得直连 infrastructure
     * （防止「业务逻辑写进技术层」），domain 不得被任何外层反向依赖以外的方式污染
     * （防止「repository/portal 被绕过、直用 Mapper」）。
     *
     * <p><strong>为什么重要</strong>：依赖倒置是六边形脊柱。infrastructure 通过实现 domain 的
     * Repository / Portal 接口「倒置」接入；一旦允许任何外层直接依赖 infrastructure，
     * 技术细节（SQL、SDK 类型）就会沿捷径渗入业务，分层名存实亡。
     *
     * <p><strong>怎么判</strong>：{@code layeredArchitecture().consideringAllDependencies()}
     * 按四层包段（{@code ..adapter..} 等）划层，声明每层「允许被谁访问」；任何跨层字段/方法/
     * 继承/注解依赖反向即违例。
     *
     * <p><strong>读侧例外</strong>：CQRS 读端口（XxxQueryRepository）定义在 application、
     * 由 infrastructure 实现（PO → DTO 直接投影，绕过聚合根），构成写侧倒置的读侧镜像
     * 「infrastructure → application」，故 Application 额外允许被 Infrastructure 访问。
     * 该整层放行由 R1b 收窄到类型锚点白名单。
     *
     * <p><strong>局限与本仓挂载状态（已退役为纯下发件）</strong>：本常量在本仓两个扫描中
     * <strong>均未挂载</strong>——框架扫描覆写「+Configuration 层」（根包自动配置类跨层装配），
     * 业务扫描覆写精确 {@code BASE} 前缀（infrastructure 下 {@code ..repository.domain..}
     * 子包会被 {@code ..domain..} 段匹配吞入 Domain 层，其依赖 PO/Mapper 即成「Domain 依赖
     * Infrastructure」冤案）。挂载一份在任一扫描都必然失败或空转，故仅对包命名无碰撞的
     * 外部消费方下发。教义见 .agents/rules/02。
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

    /** R1b 白名单锚点①：application 层读端口接口（CQRS 读侧的入口类型）。 */
    private static final String QUERY_REPOSITORY_TYPE =
            "com.yoursweakfoe.common.ddd.application.repository.application.QueryRepository";

    /** R1b 白名单锚点②：应用层内部视图标记（读 DTO 的定型接口）。 */
    private static final String APPLICATION_DTO_TYPE =
            "com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO";

    /**
     * R1b 读端口类型白名单：QueryRepository 实现 / ApplicationDTO 实现 /
     * ApplicationDTO 实现类的嵌套类。
     *
     * <p>嵌套 DTO（如 {@code OrderDTO.OrderItemDTO}）按 R10b 约定随外层定型、字节码上
     * 不携带标记接口，故白名单必须单独放行「外层是 ApplicationDTO 实现」的嵌套类，
     * 否则读实现引用嵌套 DTO 即误报。
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

    /**
     * R1b —— Infrastructure 对 Application 的访问仅限「读端口类型锚点」（CQRS 读侧例外收窄）。
     *
     * <p><strong>守护什么</strong>：R1 为读侧放行了 Infrastructure → Application <em>整层</em>
     * 访问；本规则把该豁免收窄为白名单，被依赖的 application 类必须满足其一：
     * <ul>
     *   <li>实现 {@code QueryRepository}（application 层读端口接口）</li>
     *   <li>实现 {@code ApplicationDTO}（读 DTO，作为查询签名的出入参）</li>
     *   <li>是上述 ApplicationDTO 实现类的<b>嵌套类</b>（嵌套 DTO 随外层定型，见 R10b 约定）</li>
     * </ul>
     * Handler / AppService / Assembler / Presenter 等其余 application 组件对
     * infrastructure 一律不可见。
     *
     * <p><strong>为什么重要</strong>：没有它，仓储实现可以反向调用应用层逻辑（Handler 编排、
     * Presenter 呈现），形成 application ⇄ infrastructure 循环依赖，读侧捷径沦为通用后门。
     *
     * <p><strong>怎么判</strong>：主语 {@code ..infrastructure..} 段；宾语取
     * {@code ..application..} 段再减去两类白名单与「实际位于 infrastructure 下的类」。
     *
     * <p><strong>段匹配碰撞内置排除</strong>：{@code ..application..} 按包段匹配，会同时命中
     * infrastructure 下 {@code ..repository.application..} 子包（读实现及其匿名类所在包，
     * 如 {@code infrastructure.persistence.master.order.repository.application}）。那是
     * infra 的内部自依赖（实现类引用自己的匿名类），不属于跨层访问，故宾语显式
     * {@code and(not(resideInAPackage("..infrastructure..")))} 排除。
     *
     * <p><strong>挂载</strong>：业务扫描 {@code ApplicationArchitectureTest#r1b}（sample 本地
     * 精确覆写版历史上与本常量谓词完全同构，已改为直接挂载本常量）。框架扫描不挂载：
     * common-ddd 的 infrastructure 对 application 零依赖，挂载即空转。
     *
     * <p><strong>局限</strong>：{@code allowEmptyShould(true)} + 全局 failOnEmptyShould=false
     * ——无 infra 类的扫描包下静默通过；无负证明夹具（白名单三锚点的「会失败」性质目前仅靠
     * 业务扫描真实 subject 背书）。
     */
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
     *
     * <p><strong>守护什么</strong>：入口层是纯透传协议适配（.agents/rules/04「Adapter 层禁止」）。
     * 直连 domain = 在 Controller 里调聚合根/Repository；直连 infrastructure = 在 Controller 里
     * 用 Mapper/PO。两者都跳过 AppService→Handler→Presenter 的用例编排，DTO/CO 分离随之瓦解。
     *
     * <p><strong>怎么判</strong>：主语 {@code ..adapter..} 段，宾语 {@code ..domain..} /
     * {@code ..infrastructure..} 段，任何依赖形态（继承/字段/参数/返回/注解）均计。
     *
     * <p><strong>局限</strong>：contract 的 {@code ...contract.{agg}.adapter.rest..} 契约接口子包
     * 同样命中 {@code ..adapter..} 主语——这是有意让契约接口也受本规则约束（它们本就不得触碰
     * server 层）；不判「adapter 是否依赖了错误的 application 内部组件」——那是 R1b/A4 的职责。
     *
     * <p><strong>挂载</strong>：框架扫描 {@code DddArchitectureTest#r2}（subject = common-ddd 的
     * adapter 标记接口包，恒非空）；业务扫描现状未挂载（adapter 类经 R1 本地覆写 + R8a/R8b
     * 类型锚点组合约束）。
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
     * 纯 domain 主语：{@code ..domain..} 段匹配，但排除任何同时位于
     * application / infrastructure / adapter 包段之下的类。
     *
     * <p>排除的必要性（本框架包命名惯例的直接后果）：「按实现的接口归属」命名子包，
     * infrastructure 下存在 {@code ..repository.domain..}（写仓储实现所在包）、
     * application 下理论上存在 {@code ...domain..} 型子包——这些类的包名含 {@code domain}
     * 段，但它们<strong>按更深前缀归属于其所在层</strong>，受各自层的规则管辖（R1b 等），
     * 不是域层主语。不排除则：infra 的 RepositoryImpl 依赖 Mapper（同为 infra 类，合法）
     * 会被误判成「Domain 依赖 Infrastructure」。
     *
     * <p>共享方：R3 与 R4。两者都需要「真正的 domain 层类」作主语。
     * 反面参照：R6 / R12 的主语<strong>刻意不做</strong>这组排除——它们约束的是「包名带
     * domain 的一切类不得做什么」，把 infra 同名子包也纳入安全网是保守增强而非冤案
     * （被排除逻辑仅适用于「宾语含层名」的 R3/R4 场景）。
     */
    private static final DescribedPredicate<JavaClass> PURE_DOMAIN_CLASSES =
            resideInAPackage("..domain..")
                    .and(not(resideInAPackage("..infrastructure..")))
                    .and(not(resideInAPackage("..application..")))
                    .and(not(resideInAPackage("..adapter..")));

    /**
     * R3 —— domain 层不得依赖 application / infrastructure / adapter / contract。
     *
     * <p><strong>守护什么</strong>：依赖箭头的最后一条逆向路径。domain 是圆心，向内零依赖；
     * 一旦 domain import 了 CO / DTO / PO / Mapper，聚合模型开始感知传输格式与存储格式，
     * CQRS 边界与契约独立性（C1）同时被从根部蛀空。
     *
     * <p><strong>怎么判</strong>：主语 {@link #PURE_DOMAIN_CLASSES}（含 repository.domain
     * 碰撞排除，原因见该谓词 javadoc）；宾语四个层段。注意 ArchUnit 包匹配按「段」
     * 精确匹配（非子串），{@code ..application..} 只匹配包段名恰为 application 的包，
     * 不会误伤 {@code sampleapplication} 这类仅含 "application" 子串的根包。
     *
     * <p><strong>局限</strong>：宾语未排除 contract 子包碰撞（contract 的
     * {@code adapter.rest} 子包命中 {@code ..adapter..} 宾语段时，domain 依赖它=依赖契约
     * 接口，本就违例，无需排除）。跨仓 domain→框架 infrastructure 的依赖依赖「框架包不在
     * 业务扫描根内」而不可见——该盲区由异常移包等具体修复收口，非本规则职责。
     *
     * <p><strong>挂载</strong>：框架扫描 {@code DddArchitectureTest#r3}；业务扫描用
     * {@code BASE} 精确前缀本地覆写版（同一教义、更严主语，见 ApplicationArchitectureTest）。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that(PURE_DOMAIN_CLASSES)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..adapter..", "..contract..")
                    .as("R3 Domain 不依赖 application/infrastructure/adapter/contract");

    // ═══════════════════════════════════════════════════════════════════════
    // 块2 · 领域纯净 —— domain 内部不许出现什么（R4 / R6 / R12）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R4 宾语：Spring 运行时依赖（stereotype 装配注解包豁免）∪ JPA 持久化注解。
     * 三段 or 短路：{@code (spring 且非 stereotype) 或 jakarta.jpa 或 javax.jpa}。
     */
    private static final DescribedPredicate<JavaClass> SPRING_RUNTIME_OR_JPA =
            resideInAPackage("org.springframework..")
                    .and(not(resideInAPackage("org.springframework.stereotype..")))
                    .or(resideInAPackage("jakarta.persistence.."))
                    .or(resideInAPackage("javax.persistence.."));

    /**
     * R4 —— Domain 框架中立：禁 Spring 运行时依赖与 JPA 注解，{@code org.springframework.stereotype}
     * 装配注解为唯一豁免。（本版本重写；旧版 {@code DOMAIN_MODEL_IS_PURE} 为永空转的相邻段匹配，
     * 见类头变更记录。）
     *
     * <p><strong>守护什么</strong>：domain 层（含规范布局 {@code domain.{agg}.model} 下的
     * 聚合根/实体/值对象/工厂，也含 {@code domain.{agg}.service} 等领域服务）的每个类，
     * 都不得依赖 Spring 容器运行时能力（DI 上下文、AOP、事务、Web……）与 JPA 注解
     * （{@code jakarta/javax.persistence..}）。唯一豁免：stereotype 装配注解
     * （{@code @Service}/{@code @Component} 等）——纯元数据，不改变类的编译与运行语义。
     *
     * <p><strong>为什么重要</strong>：domain 是六边形的圆心，教义（.agents/rules/02/04）要求
     * 「零框架<strong>运行时</strong>依赖」——业务规则必须能在脱离 Spring 的纯 JVM 下推理与测试。
     * stereotype 豁免是已定档的教义例外（框架 Factory 标 {@code @Component}、领域服务标
     * {@code @Service} 即此白名单），除此之外的 Spring import 意味着容器能力渗入业务核心。
     * JPA 注解则把实体焊死在特定 ORM 上——本项目持久化语义全部由手写 XML SQL 承担
     * （PO 零 ORM 注解），domain 携带 {@code @Entity}/{@code @Table} 属双重违宪。
     *
     * <p><strong>怎么判</strong>：主语 {@link #PURE_DOMAIN_CLASSES}——{@code ..domain..} 段匹配
     * 加层排除，<em>段任意位置</em>即可命中，{@code domain.model}（扁平布局）与
     * {@code domain.{agg}.model}（规范嵌套布局）皆在射程内；这正是与旧版相邻匹配
     * {@code ..domain.model..}（对嵌套布局永不为真 = 空文）的本质区别。宾语
     * {@link #SPRING_RUNTIME_OR_JPA}。
     *
     * <p><strong>豁免的边界（读到这里的读者最易误解处）</strong>：本规则只禁「Spring 与 JPA」。
     * domain 对 BusinessException（common-exception）、Lombok、common-ddd 骨架类
     * （AggregateRoot / DomainService 接口）的编译依赖<strong>不在本规则射程</strong>——
     * 它们是教义明示的编译期底座，「零框架依赖」的准确措辞是「零框架运行时依赖」。
     * MyBatis API（org.apache.ibatis / org.mybatis）渗入 domain 同样不在射程（外部包、
     * 无层段特征，任何方向规则都看不见）——靠 R1/R3 的间接防线与 code review。
     *
     * <p><strong>空转防线</strong>：本规则是全库唯一有<strong>负证明</strong>的禁则——
     * {@code DomainPurityRuleProofTest}（sample 测试）：
     * ① 依赖 {@code ApplicationContext} 的 domain 包夹具必须使 {@code check()} 失败（证明会咬）；
     * ② 真实 {@code OrderFactory}（@Component）/ {@code InventoryDomainService}（@Service）
     * 必须通过（证明白名单不是死闸、规范嵌套布局确在射程）。其余通过路径：框架扫描 +
     * 业务扫描的真实非空 subject。
     *
     * <p><strong>挂载</strong>：框架扫描 {@code DddArchitectureTest#r4}（common-ddd 自有
     * domain 包零 Spring import，教义自证）+ 业务扫描 {@code ApplicationArchitectureTest#r4}
     * （承接原 sample 本地 A2 白名单教义，A2 副本已删）。
     */
    public static final ArchRule DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE =
            noClasses()
                    .that(PURE_DOMAIN_CLASSES)
                    .should()
                    .dependOnClassesThat(SPRING_RUNTIME_OR_JPA)
                    .as("R4 Domain 框架中立：禁 org.springframework 运行时依赖（org.springframework.stereotype 装配注解唯一豁免）"
                            + "与 JPA 持久化注解（jakarta/javax.persistence）")
                    .because("domain 必须能在脱离 Spring 的纯 JVM 下推理与测试（.agents/rules/04「Domain 层禁止」）；"
                            + "stereotype 是纯元数据的既定白名单，JPA 注解会绑定特定 ORM——"
                            + "持久化语义全部由手写 XML SQL 承担，PO 尚且零 ORM 注解，实体更不例外");

    /**
     * R6 —— Domain 层不得依赖 common-security（领域模型不感知认证上下文）。
     *
     * <p><strong>守护什么</strong>：{@code SecurityUtil} 仅允许在 Application / Adapter 层调用
     * （.agents/rules/03）。domain 一旦读「当前用户」，业务规则就与请求上下文耦合——
     * 聚合根无法在系统任务、测试夹具、事件回放中独立运行，createdBy/updatedBy 之类的
     * 身份注入必须下沉到应用层完成。
     *
     * <p><strong>怎么判</strong>：主语 {@code ..domain..} 段匹配且<strong>刻意不做层排除</strong>
     * （见 {@link #PURE_DOMAIN_CLASSES} javadoc 的反面参照）：infrastructure 的
     * {@code ..repository.domain..} 子包一并进安全网——仓储实现同样不该感知认证上下文，
     * 纳入是保守增强。
     *
     * <p><strong>挂载状态（本仓已退役为纯下发件）</strong>：本常量在本仓两个扫描均未挂载且
     * 无挂载点——框架扫描中 common-ddd 的 pom 不依赖 common-security（编译层已断绝，
     * 挂载永空转，违背「不留空转挂载」原则）；业务扫描用 {@code BASE} 精确前缀本地覆写版
     * （{@code ApplicationArchitectureTest#r6_domain_no_security}）。对依赖 common-security
     * 的外部消费方（common-cloud 场景）下发本常量。
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
     * R12 —— Domain 层禁止 public setter：名称匹配 {@code setXxx} 的方法不得为 public。
     *
     * <p><strong>守护什么</strong>：充血模型的根基是「状态变迁只通过行为方法」——setter 允许
     * 外部绕过聚合根的状态机守卫与不变量校验直接改写内部状态（{@code order.setStatus(CANCELLED)}
     * 跳过 {@code cancel()} 的库存回补即事故）。
     *
     * <p><strong>怎么判</strong>：{@code noMethods}  declared in {@code ..domain..} 段主语，
     * 方法名匹配 {@code set[A-Z].*} 即须非 public。含 Lombok {@code @Data} 在 domain 类上
     * 生成的 setter（字节码层面与手写无异），一并拦截——这正是 domain 禁 {@code @Data}
     * 教义的机器化。返回 {@code this} 的流式 setter 同样命中，属预期行为。
     *
     * <p><strong>主语不做层排除（与 R3/R4 相反的取舍）</strong>：infra 的
     * {@code ..repository.domain..} 子包里若有人写 public setter，恰是「把仓储当 DTO 用」的
     * 坏味道，纳入安全网是保守增强（论证同 R6）。
     *
     * <p><strong>局限</strong>：包私有 setter 不查（教义允许同包 Factory 使用业务构造器）；
     * 字段直改（{@code public field}）不查——后者由 review 与封装惯例兜底。
     *
     * <p><strong>挂载</strong>：业务扫描 {@code ApplicationArchitectureTest#r12}（真实聚合
     * 恒非空 subject）。框架扫描不挂载：common-ddd 的 domain 骨架（AggregateRoot 等）
     * 由业务侧继承，其自身类集小且经 review 背书，挂载属重复守护。
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

    // ═══════════════════════════════════════════════════════════════════════
    // 块3 · 装配位置契约 —— 实现类与读写边界放哪（R5a / R5b / R11 / R13）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R5a —— domain Repository 必须是 interface。
     *
     * <p><strong>守护什么</strong>：依赖倒置的端口形态——{@code ..domain.repository..} 段下的
     * 任何类型若成 class（含枚举/记录），即出现「域层内实现仓储」，infrastructure 的实现
     * 与接口同居，倒置链条断裂（.agents/rules/02「依赖倒置」）。
     *
     * <p><strong>怎么判</strong>：主语 {@code ..domain.repository..}——注意这是<strong>相邻</strong>
     * 段匹配：{@code domain.{agg}.repository.domain} 命中（domain→repository 相邻成立），
     * 而 infra 的 {@code infrastructure...repository.domain} 中 repository→domain 顺序相反、
     * domain→repository 不相邻，<strong>天然不命中</strong>——本规则无需层排除即无碰撞。
     * 空集允许通过（部分扫描包可能不含 domain repository，如某些框架子包）。
     *
     * <p><strong>挂载</strong>：框架扫描 + 业务扫描均挂载，两边 subject 恒非空
     * （common-ddd 的 Repository 接口 / sample 的 OrderRepository、ProductRepository）。
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
     * <p><strong>守护什么</strong>：实现类的物理位置即其层的宣言。把 OrderRepositoryImpl 丢进
     * {@code application/} 或 {@code domain/} 包，「打开一个聚合目录看到该聚合在该层的全部代码」
     * 的自包含结构（.agents/rules/02「按聚合自包含」）即被破坏，Mapper/PO 的引用面也随之漂移。
     *
     * <p><strong>怎么判</strong>：主语为简单名后缀 {@code RepositoryImpl}（读侧
     * {@code XxxQueryRepositoryImpl} 同后缀，一并命中——读实现也必须在 infra）；宾语
     * {@code ..infrastructure.persistence..repository..}，通配段可匹配
     * {@code infrastructure.persistence.master.{agg}.repository.{domain|application}}
     * 这类多数据源 + 按聚合 + 按读写分包的结构。空集允许通过。
     *
     * <p><strong>挂载（本版本接线修复）</strong>：业务扫描 {@code ApplicationArchitectureTest#r5b}
     * ——sample 恰有 4 个真实 Impl（order/product × 写/读），subject 非空、守护有效。
     * 框架扫描原挂载已<strong>撤销</strong>：common-ddd 没有任何 *RepositoryImpl 类
     * （仓储实现由消费方继承 {@code MybatisPersistence} 支撑类完成），挂载属结构性空转
     * （审计 §6.2-6 的「未挂业务、反挂框架」正是修复对象——框架侧空转而业务侧 Impl 放错层
     * 可过关）。
     *
     * <p><strong>局限</strong>：只认名字后缀，改名（{@code OrderRepoImpl}/{@code OrderStore}）
     * 即逃逸——命名教义（rules/03 命名规范表）是本规则的主语来源，两者互为锁链。
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
     * R11 —— 写侧事务边界强制：{@code CommandHandler} 实现类的 {@code handle} 方法必须标注
     * {@code @Transactional}。
     *
     * <p><strong>守护什么</strong>：写侧固定模式「load → 聚合行为 → save」的原子性由 Handler
     * 入口事务保证。仓储支撑类（{@code MybatisPersistence}）<strong>刻意不声明事务</strong>
     * （边界上收应用层，见 rules/04 与 R11 的挂载点分工）——漏标注解时多次持久化各自提交、
     * 中途失败不回滚（下单扣了库存没落订单），且不会有任何编译错误。本规则把该事故从
     * 纪律约束变回机器红线。
     *
     * <p><strong>怎么判</strong>：{@code methods} 级——名 {@code handle} 且声明类实现
     * {@code CommandHandler} 标记接口 → 须标 {@code org.springframework.transaction.annotation.Transactional}。
     * 读侧 {@code QueryHandler} 刻意豁免（只读可省事务，.agents/rules/03）。
     *
     * <p><strong>已知逃逸面（登记，勿再当全图）</strong>：
     * ① 只查注解存在性，<strong>不查</strong>教义要求的 {@code rollbackFor = Exception.class}
     * （属性级检查在审计 §6.3-13 登记为待裁决）；
     * ② 主语锚点是「实现 CommandHandler 接口」而非「位于 handler.command 包」——包内不实现
     * 接口的编排包装器（{@code RetryablePlaceOrderHandler} 型，1 CQE : 2 Handler 首例）
     * 天然罩不到（缺口登记见类头「已知缺口」）。
     *
     * <p><strong>挂载</strong>：业务扫描 {@code ApplicationArchitectureTest#r11}（真实
     * CommandHandler 实现非空 subject）。框架扫描不挂载：common-ddd 只定义
     * {@code CommandHandler} 接口本身，无实现类，挂载即空转。
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
     * R13 —— CQRS 读写隔离强制：{@code QueryHandler} 实现类不得依赖写侧仓储
     * （{@code ..domain..repository..} 段下的任何类型）。
     *
     * <p><strong>守护什么</strong>：读侧固定模式要求查询完全绕过 domain：application 层
     * {@code QueryRepository} 读端口 → infra 实现 → PO 直接投影读 DTO，不 reconstitute
     * 聚合根。若读 Handler 图方便注入写侧 Repository 加载聚合，「读写分离」即名存实亡——
     * 读路径背上聚合重建成本，且为读侧偷偷调用写行为（聚合上的 markAsRead 类方法）开了门。
     *
     * <p><strong>怎么判</strong>：主语 = 实现 {@code QueryHandler} 标记的类（类型锚点，
     * 不靠包名猜测）；宾语 = {@code ..domain..repository..} 段组合。
     *
     * <p><strong>段匹配碰撞内置语义</strong>：宾语按段匹配，
     * 除写端口接口（{@code domain.{agg}.repository.domain.XxxRepository}）外，还会命中
     * infrastructure 下 {@code ..repository.domain..} 子包的<strong>实现类</strong>
     * （{@code ...repository.domain.OrderRepositoryImpl}）——这是<strong>刻意过咬</strong>：
     * QueryHandler 直连实现类与直连接口是同一违例的两种形态，两端都必须拦截。此处与
     * R3/R4 主语排除的逻辑相反：主语排除是为了不冤枉「包名带 domain 的非域类」，
     * 宾语过咬是为了不漏掉「任何形态的写仓储」——一个防误报、一个防漏报，勿混淆。
     *
     * <p><strong>挂载</strong>：业务扫描 {@code ApplicationArchitectureTest#r13}。框架扫描
     * 不挂载：common-ddd 无 QueryHandler 实现，挂载即空转。
     *
     * <p><strong>局限</strong>：QueryHandler 经 Handler 包装器间接持有写仓储时，字段类型
     * 不直接出现在读 Handler 上即不命中（依赖图深度 = 1）；包装器缺口同 R11 登记。
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

    // ═══════════════════════════════════════════════════════════════════════
    // 块4 · 标记与命名契约 —— 类型锚点与命名后缀互为对偶（R8 / R10 / R14）
    //
    // 共同设计：框架以「空标记接口」定型角色（RestAdapter / ApplicationDTO /
    // ScheduledAdapter）。每对规则双向锁死：正向——实现标记 ⇒ 必须在某层包段内；
    // 反向——包段/命名后缀 ⇒ 必须实现标记。识别一律用类型锚点而非名字猜测，
    // 名字规则只负责「漂移即失败」，不负责「猜测角色」。
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R8a（正向）—— 实现 {@code RestAdapter} 标记的类（REST 入口）必须位于 adapter 层。
     *
     * <p>{@code RestAdapter}（common-ddd/adapter/rest/controller/）为<strong>空标记</strong>，
     * 定型「REST 入口适配器」角色：纯透传 ApplicationService，不含业务逻辑。本规则防止被
     * 标记的组件泄漏到其他层（在 application 里实现一个 RestAdapter = 入口层逻辑内移，
     * R2 的透传教义随之失守）。空集允许通过。
     *
     * <p><strong>挂载</strong>：框架扫描（当前 subject 空集——common-ddd 只有标记接口本身，
     * 接口不实现自己；挂载守护「标记接口所在包结构不被破坏」，属空转警示节所述教义例外）
     * + 业务扫描（真实 ControllerImpl 非空 subject，有效守护）。
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
     * R8b（反向）—— 类名以 ControllerImpl 结尾的类必须实现 {@code RestAdapter} 标记。
     *
     * <p>堵命名漂移：契约接口是 {@code XxxController}（contract 模块），实现端按教义叫
     * {@code XxxControllerImpl}。有人写了 Impl 却不实现标记 = 自造入口角色绕过 REST 面定型
     * （可能没有透传、可能绕过了 AppService）。识别 REST 入口用「类型锚点」而非「名字猜测」，
     * 名字规则只保证「名实相符」，不符即失败。
     *
     * <p><strong>挂载</strong>：框架扫描（common-ddd 无 *ControllerImpl，空集通过——守护的是
     * 命名契约本身）+ 业务扫描（sample 的 Order/ProductControllerImpl 恒命中 subject，有效）。
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
     * R10a（正向）—— 实现 {@code ApplicationDTO} 标记的类（应用层内部视图）必须位于
     * application 层。
     *
     * <p>{@code ApplicationDTO}（common-ddd/application/dto/）为<strong>空标记</strong>，定型
     * 「应用层内部视图」角色：写侧 DTO（含 version）+ 读侧 DTO（投影），与 contract 层对外
     * {@code CO} 标记对偶。被标记类泄漏到其他层 = 内部视图（version/deleted 等敏感字段）
     * 出现在不该出现的位置，DTO/CO 强制分离（rules/03「DTO / CO 强制分离」）瓦解。
     * 空集允许通过。
     *
     * <p><strong>挂载</strong>：框架扫描（subject 空集——common-ddd 只定义标记接口，守护包结构，
     * 教义例外同 R8a）+ 业务扫描（sample 读写 DTO 非空 subject，有效守护）。
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
     * R10b（反向）—— {@code ..application..dto..} 包下的<strong>顶层</strong>类必须实现
     * {@code ApplicationDTO} 标记。
     *
     * <p>识别应用层内部视图用类型锚点而非包名猜测：dto 包里没实现标记的类（忘标的裸 POJO、
     * 混进来的工具类）在包名上冒充 DTO，Assembler/Presenter 的类型链就出现断层。
     * 排除嵌套类：嵌套 DTO（如 {@code OrderDTO.OrderItemDTO}）随外层定型、不重复标记
     * （javadoc 约定）；排除接口：dto 包下可能声明多态视图接口本身。
     *
     * <p><strong>段匹配语义注意</strong>：{@code ..application..dto..} 两段任意位置匹配，
     * infra 下若有人建 {@code ...application...dto..} 同名子包同样入网——与 R1b 的
     * {@code ..repository.application..} 碰撞同理，但此处后果良性（该位置本就该放 DTO）。
     *
     * <p><strong>挂载</strong>：框架扫描（subject 空集，守护包结构，教义例外同 R8a）+
     * 业务扫描（sample dto 包非空 subject，有效守护）。
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
     * R14a（正向）—— 实现 {@code ScheduledAdapter} 标记的类（定时任务入口）必须位于
     * adapter 层。
     *
     * <p>{@code ScheduledAdapter}（common-ddd/adapter/task/scheduler/）为<strong>空标记</strong>，
     * 定型「时间驱动入口」角色：{@code @Scheduled} 触发 → 透传 ApplicationService。与 REST
     * （R8）并列的 driving adapter——时间只是另一种协议，入口层职责不变。被标记类若在
     * application/domain 里出现，即有人把「调度逻辑」写进业务层。空集允许通过。
     *
     * <p><strong>挂载</strong>：框架扫描（subject 空集——common-ddd 只有标记接口，守护包结构，
     * 教义例外同 R8a）+ 业务扫描（当前无 Scheduler 实现亦空集，模板见 cookbook/scheduled-task.md，
     * 消费方一旦落地即自动生效）。
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
     * R14b（反向）—— <strong>业务服务</strong> {@code ..adapter..scheduler..} 包下的<strong>非接口</strong>
     * 类必须实现 {@code ScheduledAdapter} 标记。
     *
     * <p>与 R8b 同理（命名/包位置 ⇄ 类型锚点对偶）：scheduler 包里没实现标记的类在冒充时间
     * 入口。排除接口自身（标记接口位于 {@code ..adapter.task.scheduler..}，接口不实现自己）。
     *
     * <p><strong>主语收窄至 {@code ..adapter..scheduler..}</strong>：本规则只约束业务服务的时间
     * 驱动<strong>入口</strong>（driving adapter，经 AppService 驱动用例）。与 R8b 同一逻辑，
     * ArchUnit 守护业务分层，不反向约束框架内部结构。
     *
     * <p><strong>挂载</strong>：框架扫描 + 业务扫描（两边当前均为空集/仅标记接口所在包，
     * 教义例外同 R14a；消费方落地 Scheduler 时自动收紧为真实守护）。
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

    // ═══════════════════════════════════════════════════════════════════════
    // 块5 · 契约模块 —— contract 的对外纯洁性（C1）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * C1 —— contract 纯契约模块（Service 接口 + CQE + CO + 枚举），不得依赖 server 侧的
     * adapter / application / domain / infrastructure，也不得依赖 Spring 运行时基础设施
     * （DI / Bean / 持久化）。
     *
     * <p><strong>守护什么</strong>：contract jar 是东西向消费方的<strong>唯一</strong>依赖
     * （.agents/rules/02「contract 模块」）。它一旦 import 了 server 内部类型或容器运行时，
     * 消费方就被迫拖入整套实现（依赖传染），「按契约编程」降级为「按实现编程」，
     * server 的发版节奏将绑架所有下游。
     *
     * <p><strong>怎么判</strong>：主语 {@code ..contract..} 段；宾语 = 四层段 +
     * stereotype/context/beans 三个 Spring 运行时段。
     *
     * <p><strong>重契约例外（ADR-0003，勿再收紧）</strong>：contract <em>允许且应当</em>
     * 携带 HTTP 映射注解（spring-web 的 {@code @RequestMapping} 族）、Swagger 文档注解与
     * Jakarta 校验注解——契约 = 完整 REST 定义，映射经 adapter 实现类继承。故本规则只禁
     * 「容器运行时三段」而<strong>不禁</strong> {@code org.springframework.web..}，这是
     * 裁决后的语义而非疏漏。MyBatis 未列入宾语段：contract 无任何理由 import 它，
     * 而 spring 三段禁令已覆盖其全部合法注入路径。
     *
     * <p><strong>挂载</strong>：业务扫描 {@code ApplicationArchitectureTest#c1}（sample contract
     * 类恒非空 subject）。框架扫描不挂载：common-ddd 无 contract 段包，挂载即空转
     * （common-contract 模块属独立扫描域，不在两扫描根内）。
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
                             "org.springframework.beans..")
                     .as("C1 Contract 纯契约：不得依赖 server 四层及 Spring 运行时基础设施（stereotype/context/beans 三段）");
}
