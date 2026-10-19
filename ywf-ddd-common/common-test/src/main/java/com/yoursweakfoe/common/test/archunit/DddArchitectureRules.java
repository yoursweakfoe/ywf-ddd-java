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
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import com.tngtech.archunit.library.Architectures;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DDD 分层架构守护规则集（ArchUnit 预定义规则工厂）。
 *
 * <p><strong>载体分工（法卷 TR-1~3 → {@code knowledge/specs/current/modules/test.md}
 * 「规则集治理」节）</strong>：本 javadoc 只载挂载最小契约——每规则「守护什么 /
 * 怎么判一句 / 挂载 / 空转或局限」；设计论证、被拒方案、缺口裁决账与沿革的现行版
 * canonical 住 {@code knowledge/docs/explanation/architecture-rules.md}（解读架）。
 * 论证勿回流本文件，亦勿在此剥空挂载契约。
 *
 * <p><strong>编号纪律（TR-1，2026-09 顶位裁定后现文）</strong>：规则编号（R1、R1b、R4……）是
 * 教义锚点，法卷、字典镜像（{@code docs/reference/api/common-test.md} §2）与各文档以编号互指——
 * 编号永不重排；规则删除时编号作废并留一行作废记录（作废账见解读篇「缺口账」节），
 * <strong>废号准后续规则顶位</strong>（顶位＝新语义接管，旧作废账原位保留）——编号是教义锚点
 * 不是历史文物（裁定 Q8／案卷 2026-09-typed-identifier；R15 现行语义＝聚合根身份终类型化，
 * 旧 R15（baomidou 全仓禁令）作废账保留）。
 *
 * <h3>模块地图（常量按主题分块，代码内以横幅注释分隔）</h3>
 * <ul>
 *   <li><strong>块1 层间依赖方向</strong> —— 谁能依赖谁：R1 / R1b / R2 / R3</li>
 *   <li><strong>块2 领域纯净</strong> —— domain 内部不许出现什么：R4 / R6 / R12</li>
 *   <li><strong>块3 装配位置契约</strong> —— 实现类与读写边界放哪：R5a / R5b / R11 / R13</li>
 *   <li><strong>块4 注解与标记契约</strong> —— 类型锚点（标记接口）与命名对偶双向锁：
 *       R8a / R8b / R10a / R10b / R14a / R14b / R15</li>
 *   <li><strong>块5 契约模块</strong> —— contract 独立性：C1</li>
 * </ul>
 *
 * <h3>扫描挂载（三入口，均在 sample-server 测试树 architecture 包）</h3>
 * <ul>
 *   <li><strong>业务扫描</strong> {@code ApplicationArchitectureTest}（根 = sample 服务包）：
 *       全部共享常量、零本地谓词覆写；类内 {@code A3} / {@code A4} / {@code A4b}
 *       为 sample 自有锚点规则。</li>
 *   <li><strong>框架扫描</strong> {@code DddArchitectureTest}（根 = common-ddd 包）：
 *       R2 / R3 / R4 / R5a 真实开火；R1 持「+Configuration 层」本地覆写（根包自动配置类
 *       跨层装配需合法归属层）；标记系规则空集通过＝教义例外，守护「标记接口之家」
 *       包结构不被搬走。</li>
 *   <li><strong>负证明</strong> {@code DomainPurityRuleProofTest}：证明 R4 会失败而非恒真
 *       （三锁）；{@code TransactionBoundaryRuleProofTest}：证明 R11 会失败而非恒真（四锁：
 *       裸标／漏标必咬、显式标注必放、非 Handler 不咬）；{@code IdentifierRuleProofTest}：
 *       证明 R15 会失败而非恒真（四锁：裸类型根必咬、类型化根必放、子实体 PK 与读端口
 *       豁免位不误咬、端口 ID 槽失守必咬）。夹具居 {@code FrameworkLeakProbe}、
 *       {@code IdentifierProbes} 等 archproof 包，在两扫描根之外。</li>
 * </ul>
 *
 * <h3>安全必读（一行索引，详情全在解读篇——红线图非全图）</h3>
 * <ul>
 *   <li><strong>空集通过（vacuous pass）</strong>：消费方 {@code archunit.properties} 全局
 *       {@code archRule.failOnEmptyShould=false} ＋多数常量另写 {@code allowEmptyShould(true)}
 *       ——零命中与永真不可区分；逐规则空转账与对冲机制 → 解读篇「空转防线与缺口账」节。</li>
 *   <li><strong>已知缺口</strong>（教义有、规则无）：Handler 返回 DTO ／ AppService 返回 CO ／
 *       时间类型统一 ／ 具名领域异常 ／ PO 零 ORM 注解 ／
 *       包装器逃逸 R11·R13——未立因与待裁决账 → 解读篇同节。</li>
 *   <li><strong>「保留段唯一语义」不变量</strong>：{@code adapter / application / domain /
 *       infrastructure / contract} 五段只允许出现在其真实层位置，读写与接口归属由类名后缀＋
 *       标记接口表达——本文件全部裸段谓词成立的前提；破坏＝规则成片误报或成片漏网。
 *       完整论证与迁移坐标对照 → 解读篇同名节。</li>
 * </ul>
 *
 * <p>消费方挂载形状（代码）唯一权威＝法卷场景 1；规则清单字典镜像＝
 * {@code docs/reference/api/common-test.md} §2。使用示例与变更记录不住本文件
 * （沿革住解读篇「沿革」节，法不考古）。
 */
public final class DddArchitectureRules {

    private DddArchitectureRules() {}

    // ═══════════════════════════════════════════════════════════════════════
    // 块1 · 层间依赖方向 —— 谁能依赖谁（R1 / R1b / R2 / R3）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R1 —— DDD 四层依赖方向（依赖倒置）：adapter → application → domain ← infrastructure。
     *
     * <p><strong>守护</strong>：跨层依赖只准沿声明方向——外层直连 infra＝技术细节沿捷径渗入业务，
     * 分层名存实亡；读侧例外（infra 实现 application 读端口的「倒置镜像」）整层放行，由 R1b 收窄。
     * <strong>怎么判</strong>：{@code layeredArchitecture} 按四层包段划层、
     * {@code consideringAllDependencies()} 全形态计入；契约接口依裁决保留 {@code adapter} 段
     * 成为 Adapter 层成员，{@code ControllerImpl→契约接口} 属同层访问、DSL 不禁（实证自洽）。
     * <strong>挂载</strong>：业务扫描 r1（命名税根治后转正首挂）；框架扫描持「+Configuration 层」覆写版。
     * <strong>前提</strong>：「保留段唯一语义」不变量（见类头安全必读）。完整论证 → 解读篇「层间方向块」节。
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
                            读侧例外：infrastructure 读查询实现可访问 application 读端口；法卷锚 WC-1""");

    /** R1b 白名单锚点①：application 层读端口接口（CQRS 读侧的入口类型）。 */
    private static final String QUERY_REPOSITORY_TYPE =
            "com.yoursweakfoe.common.ddd.application.repository.QueryRepository";

    /** R1b 白名单锚点②：应用层内部视图标记（读 DTO 的定型接口）。 */
    private static final String APPLICATION_DTO_TYPE =
            "com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO";

    /**
     * R1b 白名单三锚点：QueryRepository 实现 ／ ApplicationDTO 实现 ／ ApplicationDTO 实现类的
     * 嵌套类（嵌套视图按 R10b 约定随外层定型、字节码不携带标记，须单列放行，否则读实现误报）。
     * 放行理据 → 解读篇「层间方向块」R1b 段。
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
     * <p><strong>守护</strong>：把 R1 的整层豁免收窄为白名单（三锚点见
     * {@link #READ_PORT_TYPE_ANCHORS}）；Handler / AppService / Assembler / Presenter 对
     * infrastructure 一律不可见——否则读侧捷径沦为通用后门，application ⇄ infrastructure
     * 循环依赖合法化。<strong>怎么判</strong>：主语 infra 段，宾语 = application 段减白名单
     * （旧 infra 排除谓词系命名税利息，已随迁移退役、由「保留段唯一语义」不变量接管防误报——
     * 账 → 解读篇）。<strong>挂载</strong>：业务扫描 r1b；框架扫描不挂（infra→app 零依赖，挂载即空转）。
     * <strong>局限</strong>：{@code allowEmptyShould} 静默通过；无负证明夹具，靠业务 subject 实数命中背书。
     */
    public static final ArchRule INFRA_ACCESS_TO_APPLICATION_ONLY_FOR_READ_PORT_TYPES =
            noClasses()
                    .that()
                    .resideInAPackage("..infrastructure..")
                    .should()
                    .dependOnClassesThat(
                            resideInAPackage("..application..")
                                    .and(not(READ_PORT_TYPE_ANCHORS)))
                    .allowEmptyShould(true)
                    .as("R1b Infrastructure 对 Application 的访问仅限读端口类型"
                            + "（QueryRepository 实现 / ApplicationDTO 及其嵌套类），其余 application 组件一律禁止；法卷锚 RC-6");

    /**
     * R2 —— adapter 层（REST / MQ / 定时任务入口）只依赖 application 与 contract，
     * 不得直连 domain 或 infrastructure。
     *
     * <p><strong>守护</strong>：入口层纯透传教义（禁令卷）——Controller 直连聚合/Repository 或
     * Mapper/PO 都跳过 AppService → Handler → Presenter 用例编排，DTO/CO 分离随之瓦解。
     * <strong>怎么判</strong>：主语 {@code ..adapter..} 段（contract 契约接口有意被罩——本就
     * 不得触碰 server 内部层），任何依赖形态均计；不判「adapter 依赖了错误的 application
     * 内部组件」——那是 R1b 与 sample 本地 {@code A4} 账下职责。
     * <strong>挂载</strong>：框架扫描 r2（adapter 标记接口包恒非空）；业务扫描不挂
     * （共享 R1 + R8a/R8b 锚点组合已覆盖同等语义）。
     */
    public static final ArchRule ADAPTER_ONLY_DEPENDS_ON_APPLICATION =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..domain..", "..infrastructure..")
                    .as("R2 Adapter 只依赖 Application/Contract，不得直连 Domain 或 Infrastructure；法卷锚 禁令卷 §3");

    /**
     * R3 —— domain 层不得依赖 application / infrastructure / adapter / contract。
     *
     * <p><strong>守护</strong>：依赖箭头的最后一条逆向路径——domain import CO/DTO/PO/Mapper，
     * 聚合即感知传输与存储格式，CQRS 边界与契约独立性（C1）从根部蛀空。
     * <strong>怎么判</strong>：主语裸 {@code ..domain..} 段——「保留段唯一语义」不变量成立后
     * 命中当且仅当真 domain 层（旧三连排除谓词全部删除，冤案链 → 解读篇）；包匹配按「段」
     * 精确、非子串，{@code sampleapplication} 类根包不误伤。<strong>局限</strong>：宾语
     * {@code ..contract..} 段命中框架契约模块＝预期收紧非误伤；跨仓 domain→框架 infra 依赖
     * 不受扫描边界可见（盲区，教义与 code review 收口）。
     * <strong>挂载</strong>：框架扫描 r3 ＋ 业务扫描 r3（BASE 覆写版随命名迁移退役）。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..adapter..", "..contract..")
                    .as("R3 Domain 不依赖 application/infrastructure/adapter/contract；法卷锚 WC-1");

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
     * 装配注解为唯一豁免。（旧版 {@code DOMAIN_MODEL_IS_PURE} 相邻段空文案已重写，沿革账 → 解读篇「沿革」节。）
     *
     * <p><strong>守护</strong>：业务规则必须能在脱离 Spring 的纯 JVM 下推理与测试（教义准确措辞=
     * 零框架<strong>运行时</strong>依赖；禁令卷）；stereotype 豁免＝纯元数据的既定白名单（Factory /
     * 领域服务标注解即此）；JPA 注解把实体焊死特定 ORM——持久化语义全由手写 XML SQL 承担、
     * PO 尚且零 ORM 注解，实体携带 {@code @Entity}/{@code @Table} 属双重违宪。
     * <strong>不在射程（勿当全图）</strong>：异常体系 / Lombok / common-ddd 骨架的编译依赖
     * （教义编译期底座）；MyBatis API 渗入 domain（外部包、无层段特征，任何方向规则看不见——
     * R1/R3 间接防线＋review 收口，登记账）。<strong>怎么判</strong>：主语裸 {@code ..domain..}
     * 段匹配，扁平/嵌套布局皆命中（与旧相邻式空文的本质区别）；宾语 {@link #SPRING_RUNTIME_OR_JPA}。
     * <strong>空转防线</strong>：全库第一个获<strong>负证明</strong>的禁则（{@code DomainPurityRuleProofTest}
     * 三锁：违例必失败／豁免与迁移布局必通过／纯净聚合哨兵——合读才有证明力；R11 收紧后同年有
     * {@code TransactionBoundaryRuleProofTest} 四锁同行）。
     * <strong>挂载</strong>：框架扫描 r4（教义自证）＋业务扫描 r4（原 sample 本地 A2 白名单教义上收承接）。
     */
    public static final ArchRule DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat(SPRING_RUNTIME_OR_JPA)
                    .as("R4 Domain 框架中立：禁 org.springframework 运行时依赖（org.springframework.stereotype 装配注解唯一豁免）"
                            + "与 JPA 持久化注解（jakarta/javax.persistence）；法卷锚 禁令卷 §1")
                    .because("domain 必须能在脱离 Spring 的纯 JVM 下推理与测试（禁令卷「Domain 层禁止」）；"
                            + "stereotype 是纯元数据的既定白名单，JPA 注解会绑定特定 ORM——"
                            + "持久化语义全部由手写 XML SQL 承担，PO 尚且零 ORM 注解，实体更不例外");

    /**
     * R6 —— Domain 层不得依赖 common-security（领域模型不感知认证上下文）。
     *
     * <p><strong>守护</strong>：{@code SecurityUtil} 仅准 Application / Adapter 层调用（禁令卷）——
     * domain 读「当前用户」即把业务规则耦合到请求上下文，聚合无法在系统任务、测试夹具、
     * 事件回放中独立运行；createdBy/updatedBy 类身份注入必须下沉应用层完成。
     * <strong>怎么判</strong>：主语裸 {@code ..domain..} 段（不变量保证段语义唯一，与业务侧
     * 旧覆写版主语集合已完全重合），宾语 common-security 包。
     * <strong>挂载</strong>：业务扫描 r6（BASE 覆写随迁移退役）；框架扫描不挂——common-ddd 的
     * pom 不依赖 common-security（编译层已断绝，挂载＝结构性空转）；依赖 security 的
     * 外部消费方（common-cloud 场景）照常可挂。
     */
    public static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_SECURITY =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.yoursweakfoe.common.security..")
                    .as("R6 Domain 不依赖 common-security（SecurityUtil 仅限 Application/Adapter 层）；法卷锚 CC-8");

    /**
     * R12 —— Domain 层禁止 public setter：名称匹配 {@code setXxx} 的方法不得为 public。
     *
     * <p><strong>守护</strong>：充血模型之根——状态变迁只经行为方法；setter 允许外部绕过
     * 聚合根状态机守卫与不变量校验直改内部状态（{@code setStatus} 跳过 {@code cancel()} 的
     * 补偿即事故）。Lombok 生成的 setter 字节码与手写无异、一并拦截——domain 禁 {@code @Data}
     * 教义的机器化。<strong>怎么判</strong>：{@code noMethods}，主语 {@code ..domain..} 段声明类
     * ＋方法名 {@code set[A-Z].*} 须非 public；返回 {@code this} 的流式 setter 命中＝预期。
     * <strong>局限</strong>：包私有 setter 不查（同包 Factory 走业务构造器的合法路）；
     * {@code public field} 直改不查（封装惯例与 review 兜底）。
     * <strong>挂载</strong>：业务扫描 r12（真实聚合恒非空）；框架扫描不挂（骨架类集小、
     * review 背书，挂载属重复守护）。
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
                    .as("R12 Domain 层禁止 public setter（状态变迁只经行为方法，保护聚合不变量）；法卷锚 禁令卷 §1/CC-6");

    // ═══════════════════════════════════════════════════════════════════════
    // 块3 · 装配位置契约 —— 实现类与读写边界放哪（R5a / R5b / R11 / R13）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R5a —— domain Repository 必须是 interface。
     *
     * <p><strong>守护</strong>：依赖倒置的端口形态——domain 的 repository 段内任何类型成 class
     * （含枚举/记录）＝「域层内实现仓储」，实现与接口同居、倒置链条在源头断裂（禁令卷「依赖倒置」）。
     * <strong>怎么判</strong>：主语 {@code ..domain.repository..}——<strong>相邻</strong>段匹配：
     * 框架侧 {@code domain.repository.Repository} 直接相邻、真实命中；业务规范嵌套布局
     * （domain→repository 隔聚合段）永不相邻——<strong>业务扫描空集通过，诚实登记</strong>，
     * 业务写端口的 interface 定型实际由框架扫描＋sample 本地 {@code A3} 锚点（按
     * {@code Repository} 标记锚定）分担。<strong>挂载</strong>：框架扫描（真实开火）＋
     * 业务扫描（保留挂载，扁平布局消费方即时生效）。
     */
    public static final ArchRule DOMAIN_REPOSITORIES_MUST_BE_INTERFACES =
            classes()
                    .that()
                    .resideInAPackage("..domain.repository..")
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(true)
                    .as("R5a Domain Repository 必须是 interface（实现应放在 infrastructure.persistence..repository）；法卷锚 CC-4");

    /**
     * R5b —— 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下。
     *
     * <p><strong>守护</strong>：实现类的物理位置即其对层的宣言——实现被丢进 application /
     * domain，「打开一个聚合目录看到该聚合在该层全部代码」的自包含结构（禁令卷「按聚合自包含」）
     * 即破，Mapper/PO 引用面随之漂移。<strong>怎么判</strong>：主语简单名后缀
     * {@code RepositoryImpl}（读侧实现同后缀，一并命中——读实现也必须在 infra）；宾语通配段
     * 容纳多数据源＋按聚合分包（迁移后读写实现并级，身份只由后缀与所实现标记表达）。
     * <strong>局限</strong>：只认后缀，改名即逃逸——命名教义（编码公约卷命名表）是本规则的
     * 主语来源，两者互为锁链。<strong>挂载</strong>：业务扫描 r5b（sample 两聚合读写共 4 个
     * 实现实数命中；真实例指针位）；框架扫描挂载已<strong>撤销</strong>（common-ddd 无该
     * 命名形态——实现由消费方继承 {@code MybatisPersistence} 完成，挂载属结构性空转）。
     */
    public static final ArchRule REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("RepositoryImpl")
                    .should()
                    .resideInAPackage("..infrastructure.persistence..repository..")
                    .allowEmptyShould(true)
                    .as("R5b 仓储实现（*RepositoryImpl）必须位于 infrastructure.persistence..repository 包下；法卷锚 CC-4");

    /** R11 判定的事务注解 FQN（Spring 声明式事务）。 */
    private static final String TRANSACTIONAL_TYPE =
            "org.springframework.transaction.annotation.Transactional";

    /**
     * R11 判定（2026-09「修码就法」严格化裁决）：{@code @Transactional} 存在 ∧ {@code rollbackFor} 属性
     * <strong>显式声明</strong>。严格度=存在性、不判值（{@code Throwable.class} 等等价或更严
     * 写法放行；缺注解违例由 and 前段报告，本段只对「裸标」补位）。裸标即 Spring 默认回滚
     * 规则的 checked-exception 缝——教义（BP-10/WC-2）不接受这口缝。
     */
    private static final ArchCondition<JavaMethod> TRANSACTIONAL_WITH_EXPLICIT_ROLLBACK_FOR =
            ArchConditions.<JavaMethod>beAnnotatedWith(TRANSACTIONAL_TYPE)
                    .and(new ArchCondition<JavaMethod>("@Transactional 上显式声明 rollbackFor") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                            method.getAnnotations().stream()
                                    .filter(a -> TRANSACTIONAL_TYPE.equals(a.getRawType().getName()))
                                    .filter(a -> !a.tryGetExplicitlyDeclaredProperty("rollbackFor").isPresent())
                                    .forEach(a -> events.add(SimpleConditionEvent.violated(
                                            method,
                                            method.getFullName() + " 裸标 @Transactional 未显式声明 "
                                                    + "rollbackFor（Spring 默认规则对受检异常不回滚，半途提交缝）")));
                        }
                    });

    /**
     * R11 —— 写侧事务边界强制：{@code CommandHandler} 实现类的 {@code handle} 方法必须标注
     * {@code @Transactional} <strong>且显式声明 {@code rollbackFor}</strong>。
     *
     * <p><strong>守护</strong>：写侧「load → 聚合行为 → save」的原子性由 Handler 入口事务保证，
     * 而仓储支撑类（{@code MybatisPersistence}）<strong>刻意不声明事务</strong>（边界上收应用层）——
     * 漏标注解＝多次持久化各自提交、中途失败不回滚，且零编译错误；裸标 {@code @Transactional}
     * 同样是缝：Spring 默认回滚规则<strong>不覆盖受检异常</strong>（BusinessException/MyBatis 系
     * runtime 在默认覆盖内，checked 经 Portal/SDK 冒出即半途提交）。本规则把两类静默事故都变回
     * 机器红线。<strong>怎么判</strong>：{@code methods} 级，名 {@code handle} ＋声明类实现
     * {@code CommandHandler} 标记（锚点而非包位置）⇒ {@link #TRANSACTIONAL_WITH_EXPLICIT_ROLLBACK_FOR}
     * （注解存在 ∧ 属性显式声明）；读侧 {@code QueryHandler} 刻意豁免（只读可省事务）。
     * <strong>严格度定档（2026-09 严格化裁决）</strong>：存在性检查、<strong>不判值</strong>——
     * {@code Throwable.class} 等等价或更严的显式写法放行，属性值白名单不做（值级执法另案）。
     * <strong>负证明</strong>：{@code TransactionBoundaryRuleProofTest}（四锁：裸标必咬／漏标必咬／
     * 显式标注必放／非 Handler 不咬）。<strong>残余逃逸面（登记）</strong>：包内不实现接口的编排
     * 包装器罩不到（待裁决账 B）——账全文 → 解读篇「缺口账」节。
     * <strong>挂载</strong>：业务扫描 r11（真实 CommandHandler 非空）；框架扫描不挂（只定义接口）。
     */
    public static final ArchRule COMMAND_HANDLERS_ARE_TRANSACTIONAL =
            methods()
                    .that()
                    .haveName("handle")
                    .and()
                    .areDeclaredInClassesThat()
                    .implement("com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler")
                    .should(TRANSACTIONAL_WITH_EXPLICIT_ROLLBACK_FOR)
                    .allowEmptyShould(true)
                    .as("R11 CommandHandler.handle 必须标注 @Transactional 且显式声明 rollbackFor"
                            + "（写侧事务边界由应用层保证，框架不兜底；严格度=存在性、不判值；法卷锚 BP-10/WC-2）");

    /**
     * R13 —— CQRS 读写隔离强制：{@code QueryHandler} 实现类不得依赖任何 {@code Repository}
     * 类型（写侧仓储，以类型锚点识别）。
     *
     * <p><strong>守护</strong>：读侧固定模式要求查询完全绕过 domain（读端口 → infra 实现 →
     * PO 直接投影读 DTO，不 reconstitute 聚合根）——读 Handler 注入写仓储＝读路径背上聚合
     * 重建成本，且为读通道偷调写行为开门。<strong>怎么判</strong>：主语＝实现
     * {@code QueryHandler} 标记的类；宾语＝{@code assignableTo(Repository 标记)} <strong>类型锚点
     * ——布局无关</strong>。旧宾语为段匹配 {@code ..domain..repository..}，其对实现类的识别力
     * 纯靠旧坐标巧合、命名迁移后<strong>静默失效</strong>（教训：巧合命中的规则坐标一变即无声
     * 退役，比从不命中更危险），账 → 解读篇「装配位置块」节。读写 marker 互不继承，
     * 读端口及其实现不被误咬。<strong>局限</strong>：依赖图深度 1，经包装器间接持有不命中
     * （同 R11 账 B）。<strong>挂载</strong>：业务扫描 r13（QueryHandler 非空）；框架扫描不挂（无实现）。
     */
    public static final ArchRule QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES =
            noClasses()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.application.handler.query.QueryHandler")
                    .should()
                    .dependOnClassesThat(
                            assignableTo("com.yoursweakfoe.common.ddd.domain.repository.Repository"))
                    .allowEmptyShould(true)
                    .as("R13 QueryHandler 禁止依赖任何 Repository 类型（CQRS 读侧只走 QueryRepository 读端口）；法卷锚 RC-2/BP-11");

    // ═══════════════════════════════════════════════════════════════════════
    // 块4 · 标记与命名契约 —— 类型锚点与命名后缀互为对偶（R8 / R10 / R14 / R15）
    //
    // 共同设计：框架以「空标记接口」定型角色（RestAdapter / ApplicationDTO /
    // ScheduledAdapter）。每对规则双向锁死：正向——实现标记 ⇒ 必须在某层包段内；
    // 反向——包段/命名后缀 ⇒ 必须实现标记。识别一律用类型锚点而非名字猜测，
    // 名字规则只负责「漂移即失败」，不负责「猜测角色」。R13 是同一哲学在禁则方向的延伸；
    // R15 是其向身份维的延伸——身份槽以 Identifier 词汇定型（泛型实参，ArchUnit 不建模、
    // 走反射解析）。
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * R8a（正向）—— 实现 {@code RestAdapter} 标记的类（REST 入口）必须位于 adapter 层。
     *
     * <p><strong>守护</strong>：{@code RestAdapter} 为空标记，定型「纯透传 AppService、零业务」
     * 的 REST 入口角色；被标记组件泄漏到其他层（在 application 实现一个 RestAdapter＝入口逻辑
     * 内移）即 R2 透传教义失守。<strong>坐标对偶</strong>：契约接口 ↔ server 实现 ↔ 框架标记
     * 三段全同深，由「保留段唯一语义」不变量接管（对照表 → 解读篇「保留段唯一语义」节）。
     * <strong>挂载</strong>：框架扫描（空集＝教义例外，守护标记接口之家的包结构）＋
     * 业务扫描（真实 ControllerImpl 非空，有效守护）。
     */
    public static final ArchRule REST_ENTRIES_ARE_MARKED_AND_IN_ADAPTER =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter")
                    .should()
                    .resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .as("R8a 实现 RestAdapter 标记的类必须位于 adapter 层（REST 入口角色）；法卷锚 BP-4");

    /**
     * R8b（反向）—— 类名以 ControllerImpl 结尾的类必须实现 {@code RestAdapter} 标记。
     *
     * <p><strong>守护</strong>：名实相符——写了 Impl 却不实现标记＝自造入口角色绕过 REST 面
     * 定型（可能没有透传、可能绕过 AppService）。角色识别用类型锚点而非名字猜测，命名规则
     * 只负责「不符即失败」。<strong>挂载</strong>：框架扫描（无 *ControllerImpl，空集通过——
     * 守护命名契约本身）＋业务扫描（sample 两聚合 ControllerImpl 恒命中，有效）。
     */
    public static final ArchRule CONTROLLER_IMPL_NAMING_MUST_BE_MARKED =
            classes()
                    .that()
                    .haveSimpleNameEndingWith("ControllerImpl")
                    .should()
                    .implement("com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter")
                    .allowEmptyShould(true)
                    .as("R8b 类名以 ControllerImpl 结尾的类必须实现 RestAdapter 标记（识别锚点用类型而非名字）；法卷锚 BP-4");

    /**
     * R10a（正向）—— 实现 {@code ApplicationDTO} 标记的类（应用层内部视图）必须位于
     * application 层。
     *
     * <p><strong>守护</strong>：{@code ApplicationDTO} 为空标记，定型「应用层内部视图」
     * （写侧 DTO 含 version ＋读侧投影，与 contract 对外 {@code CO} 对偶）；被标记类泄漏到
     * 其他层＝version/deleted 等内部性出现在不该出现的位置，DTO/CO 强制分离（编码公约卷）瓦解。
     * <strong>挂载</strong>：框架扫描（空集＝教义例外，同 R8a）＋业务扫描（读写 DTO 非空，有效）。
     */
    public static final ArchRule APPLICATION_DTOS_ARE_MARKED_AND_IN_APPLICATION =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO")
                    .should()
                    .resideInAPackage("..application..")
                    .allowEmptyShould(true)
                    .as("R10a 实现 ApplicationDTO 标记的类必须位于 application 层（应用层内部视图角色）；法卷锚 BP-9");

    /**
     * R10b（反向）—— {@code ..application..dto..} 包下的<strong>顶层</strong>类必须实现
     * {@code ApplicationDTO} 标记。
     *
     * <p><strong>守护</strong>：dto 包里未标记的类（忘标的裸 POJO、混进的工具类）在包名上
     * 冒充视图，Assembler/Presenter 类型链断层。<strong>排除</strong>：嵌套类（随外层定型、
     * 不重复标记——R1b 白名单第三锚点即为此在）与接口（dto 包可声明多态视图接口本身）。
     * 段匹配历史良性碰撞（infra 旧 {@code repository.application} 子包）已随不变量根除（账 → 解读篇）。
     * <strong>挂载</strong>：框架扫描（空集＝教义例外，同 R8a）＋业务扫描（dto 包非空，有效）。
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
                    .as("R10b ..application..dto.. 包下的顶层类必须实现 ApplicationDTO 标记（嵌套类除外）；法卷锚 WC-4");

    /**
     * R14a（正向）—— 实现 {@code ScheduledAdapter} 标记的类（定时任务入口）必须位于
     * adapter 层。
     *
     * <p><strong>守护</strong>：{@code ScheduledAdapter} 为空标记，定型「时间驱动入口」
     * （{@code @Scheduled} 触发 → 透传 AppService）——时间只是另一种协议，driving adapter
     * 职责与 REST（R8）同构；被标记类出现在 application/domain＝调度逻辑写进业务层。
     * <strong>挂载</strong>：框架扫描（空集＝教义例外，同 R8a）＋业务扫描（现无实现亦空集，
     * 模板见 {@code docs/how-to/scheduled-task.md}，消费方落地即自动收紧）。
     */
    public static final ArchRule SCHEDULED_ENTRIES_ARE_MARKED_AND_IN_ADAPTER =
            classes()
                    .that()
                    .implement("com.yoursweakfoe.common.ddd.adapter.task.scheduler.ScheduledAdapter")
                    .should()
                    .resideInAPackage("..adapter..")
                    .allowEmptyShould(true)
                    .as("R14a 实现 ScheduledAdapter 标记的类必须位于 adapter 层（定时任务入口角色）；法卷锚 SC-1");

    /**
     * R14b（反向）—— <strong>业务服务</strong> {@code ..adapter..scheduler..} 包下的<strong>非接口</strong>
     * 类必须实现 {@code ScheduledAdapter} 标记。
     *
     * <p><strong>守护</strong>：与 R8b 同理（命名/包位置 ⇄ 类型锚点对偶）——scheduler 包里
     * 不实现标记的类在冒充时间入口；排除接口自身（标记接口居该包，接口不实现自己）。
     * 主语刻意收窄至业务时间驱动<strong>入口</strong>（driving adapter），不反向约束框架内部结构。
     * <strong>挂载</strong>：两侧现为空集/仅标记包＝教义例外（同 R14a），实现落地自动收紧。
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
                    .as("R14b 业务 ..adapter..scheduler.. 包下的类必须实现 ScheduledAdapter 标记；法卷锚 SC-1");

    /** R15 主语锚点①：聚合根基类 FQN（框架基类自身抽象，天然出主语集）。 */
    private static final String AGGREGATE_ROOT_TYPE =
            "com.yoursweakfoe.common.ddd.domain.model.AggregateRoot";

    /** R15 主语锚点②：写侧仓储端口标记接口 FQN（标记自身以全名排除——端口臂主语是业务端口）。 */
    private static final String REPOSITORY_TYPE =
            "com.yoursweakfoe.common.ddd.domain.repository.Repository";

    /** R15 宾语锚点：身份词汇接口 FQN（common-test 不编译依赖 common-ddd，判定全程按名）。 */
    private static final String IDENTIFIER_TYPE =
            "com.yoursweakfoe.common.ddd.domain.id.Identifier";

    /**
     * R15 主语：{@code AggregateRoot} 的<strong>具体</strong>子类 ∪ {@code Repository} 的
     * 端口接口（标记自身除外）。{@code Entity} 子实体 PK、{@code QueryRepository} 读端口
     * 均不入主语——那是 BP-15 豁免面的锚点形状（读端口不罩 Repository 标记，天然在外）。
     */
    private static final DescribedPredicate<JavaClass> IDENTITY_SLOT_SUBJECTS =
            new DescribedPredicate<JavaClass>("AggregateRoot 具体子类 或 Repository 端口接口") {
                @Override
                public boolean test(JavaClass javaClass) {
                    if (javaClass.isInterface()) {
                        return javaClass.isAssignableTo(REPOSITORY_TYPE)
                                && !REPOSITORY_TYPE.equals(javaClass.getFullName());
                    }
                    return javaClass.isAssignableTo(AGGREGATE_ROOT_TYPE)
                            && !javaClass.getModifiers().contains(JavaModifier.ABSTRACT);
                }
            };

    /**
     * R15 判定：ArchUnit 不建模泛型实参（字节码只存擦除后签名），故本条件走标准反射——
     * 按二进制名加载类、解析 {@code getGenericSuperclass() / getGenericInterfaces()} 上的
     * 泛型实参（含中间抽象父的类型变量代入）。类加载失败与裸类型继承一律报违例，
     * 拒绝静默放行（案卷 P-5「新谓词不过负证明即空文」同一纪律）。
     */
    private static final ArchCondition<JavaClass> ID_SLOTS_ARE_TYPED =
            new ArchCondition<>("聚合根身份槽实现 Identifier 且端口 ID 槽与根槽一致") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    String violation = inspectIdentitySlots(javaClass.getName());
                    if (violation != null) {
                        events.add(SimpleConditionEvent.violated(
                                javaClass, javaClass.getFullName() + " —— " + violation));
                    }
                }
            };

    /**
     * R15 —— 聚合根身份终类型化（顶位规则）：凡 {@code AggregateRoot} 具体子类，其
     * {@code AggregateRoot<ID>} 泛型实参必须实现 {@code Identifier}；{@code Repository} 端口
     * ID 槽同规且与根槽一致。
     *
     * <p><strong>顶位注记（裁定 Q8／案卷 2026-09-typed-identifier）</strong>：本 R15 顶位自
     * 旧 R15（com.baomidou 全仓禁令，规则本体已删）——废号顶位＝新语义接管，旧作废账原位
     * 保留于解读篇「缺口账」节，编号纪律见类头 TR-1。
     *
     * <p><strong>守护</strong>：聚合身份必须住专属币种 <code>{Agg}Id implements
     * Identifier&lt;V&gt;</code>（法卷锚 BP-13）——全域裸用 UUID 时各聚合身份在类型系统里
     * 同形，他聚合 ID 混入本聚合调用位编译器无从察觉、错误迟到运行时才暴露；本规则把
     * 「混放即编译失败」的词汇层防线经 R15 锁死在架构面（跨聚合引用槽同法，法卷锚 BP-14）。
     * <strong>怎么判</strong>：主语＝{@link #IDENTITY_SLOT_SUBJECTS}；判定＝{@link #ID_SLOTS_ARE_TYPED}
     * （反射解析泛型实参——ArchUnit 谓词模型看不见 generics，这是本集唯一走反射臂的规则，
     * 挂载最小契约不破、classfile 主语不破）。一致臂实弹勘验（施工时点）：
     * {@code Repository<Domain extends Identifiable<ID>, ID>} 的 F-边界已令端口-根槽混放在
     * javac 即拒（含裸类型根变体，实证于案卷取证件）——一致臂为边界放宽/字节码手改场景的
     * 反射备胎，无编译期夹具可锁，据实登记于「空转或局限」。<strong>挂载</strong>：仅业务扫描
     * r15（真实聚合恒非空）；框架扫描<strong>不挂</strong>——common-ddd 无具体聚合根，挂载＝
     * 结构性空转（禁则见 {@code DddArchitectureTest} 类头挂载原则）。
     * <strong>空转或局限</strong>：{@code allowEmptyShould} 随消费方零聚合时空转（全局
     * {@code failOnEmptyShould=false} 同档）；主语按名锚定框架 FQN——业务在扫描根外自建
     * 骨架不在射程（全集共同盲区）；中间抽象父的类型变量代入止于泛型实参直取，
     * 嵌套参数化（如 {@code Base<Map<String,T>>}）不深入解析（登记账）。<strong>负证明</strong>：
     * {@code IdentifierRuleProofTest}（四锁：裸类型根必咬／类型化根必放／豁免位不误咬／
     * 端口 ID 槽失守必咬）。
     */
    public static final ArchRule AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS =
            classes()
                    .that(IDENTITY_SLOT_SUBJECTS)
                    .should(ID_SLOTS_ARE_TYPED)
                    .allowEmptyShould(true)
                    .as("R15 聚合根 ID 泛型实参必须实现 Identifier 且 Repository 端口 ID 槽与根槽一致；法卷锚 BP-13/BP-14");

    /** 反射解析单类身份槽；返回 {@code null} 即合格，非 {@code null} 为违例描述。 */
    private static String inspectIdentitySlots(String binaryName) {
        Class<?> clazz;
        try {
            clazz = Class.forName(binaryName, false, DddArchitectureRules.class.getClassLoader());
        } catch (Throwable loadFailure) {
            return "类无法加载、身份槽无从解析（" + loadFailure + "）——拒绝静默放行";
        }
        return clazz.isInterface() ? inspectRepositoryPort(clazz) : inspectAggregateRoot(clazz);
    }

    /** 根臂：沿继承链上溯取 {@code AggregateRoot<…>} 的 ID 实参，须实现 {@code Identifier}。 */
    private static String inspectAggregateRoot(Class<?> root) {
        Type idSlot = resolveRootIdSlot(root);
        if (idSlot == null) {
            return "裸类型继承 AggregateRoot（未声明 ID 泛型实参，身份槽无从判定）";
        }
        return requireIdentifier(idSlot, "聚合根身份槽");
    }

    /**
     * 端口臂：ID 槽（第 2 泛型实参）须实现 {@code Identifier}；端口-根槽一致为 F-边界备胎臂
     * （混放形态 javac 即拒，见规则 javadoc「怎么判」勘验注）。
     */
    private static String inspectRepositoryPort(Class<?> port) {
        ParameterizedType declaration = findRepositoryParameterization(port);
        if (declaration == null) {
            return "裸类型继承 Repository（端口未声明 <Domain, ID> 泛型实参，ID 槽失守）";
        }
        Type domainSlot = declaration.getActualTypeArguments()[0];
        Type idSlot = declaration.getActualTypeArguments()[1];
        if (idSlot instanceof TypeVariable<?> variable) {
            return "端口 ID 槽为未代入类型变量 " + variable.getName() + "（非终类型，币种无从锚定）";
        }
        String violation = requireIdentifier(idSlot, "Repository 端口 ID 槽");
        if (violation != null) {
            return violation;
        }
        Class<?> domain = erase(domainSlot);
        if (domain != null && isAggregateRootSubtype(domain)) {
            Type rootSlot = resolveRootIdSlot(domain);
            if (rootSlot != null && !rootSlot.getTypeName().equals(idSlot.getTypeName())) {
                return "端口 ID 槽 " + idSlot.getTypeName() + " 与根身份槽 " + rootSlot.getTypeName() + " 不一致";
            }
        }
        return null;
    }

    /** 自具体根沿 {@code getGenericSuperclass()} 上溯至 {@code AggregateRoot<…>} 取 ID 实参（类型变量逐层代入）；裸继承返回 {@code null}。 */
    private static Type resolveRootIdSlot(Class<?> start) {
        Map<String, Type> bindings = new HashMap<>();
        Class<?> current = start;
        while (current != null && !Object.class.equals(current)) {
            Type superType = current.getGenericSuperclass();
            if (superType instanceof ParameterizedType parameterized) {
                Class<?> raw = (Class<?>) parameterized.getRawType();
                if (AGGREGATE_ROOT_TYPE.equals(raw.getName())) {
                    return substitute(parameterized.getActualTypeArguments()[0], bindings);
                }
                TypeVariable<?>[] variables = raw.getTypeParameters();
                Type[] arguments = parameterized.getActualTypeArguments();
                for (int i = 0; i < arguments.length; i++) {
                    bindings.put(variables[i].getName(), substitute(arguments[i], bindings));
                }
                current = raw;
            } else if (superType instanceof Class<?> superclass) {
                if (AGGREGATE_ROOT_TYPE.equals(superclass.getName())) {
                    return null;
                }
                current = superclass;
            } else {
                return null;
            }
        }
        return null;
    }

    /** 在端口接口图中广度优先寻 {@code Repository<…>} 的参数化声明；仅以裸类型触及时返回 {@code null}。 */
    private static ParameterizedType findRepositoryParameterization(Class<?> port) {
        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(port);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            for (Type iface : current.getGenericInterfaces()) {
                if (iface instanceof ParameterizedType parameterized) {
                    Class<?> raw = (Class<?>) parameterized.getRawType();
                    if (REPOSITORY_TYPE.equals(raw.getName())) {
                        return parameterized;
                    }
                    queue.add(raw);
                } else if (iface instanceof Class<?> rawInterface
                        && !REPOSITORY_TYPE.equals(rawInterface.getName())) {
                    queue.add(rawInterface);
                }
            }
        }
        return null;
    }

    /** 类型变量代入（bindings 逐层累积；未绑定即原样返回，由调用侧判非具体形态）。 */
    private static Type substitute(Type type, Map<String, Type> bindings) {
        Type current = type;
        int guard = 0;
        while (current instanceof TypeVariable<?> variable && guard++ < 16) {
            Type mapped = bindings.get(variable.getName());
            if (mapped == null) {
                return current;
            }
            current = mapped;
        }
        return current;
    }

    /** ID 槽合格判据：擦除后类型须实现 {@code Identifier}（按名传递闭包，不依赖 common-ddd 在编译类路径）。 */
    private static String requireIdentifier(Type idSlot, String slotLabel) {
        Class<?> erasure = erase(idSlot);
        if (erasure == null) {
            return slotLabel + "实参 " + idSlot.getTypeName() + " 非可解析具体类型，无法判定币种";
        }
        if (!implementsIdentifier(erasure)) {
            return slotLabel + "实参 " + erasure.getName() + " 未实现 " + IDENTIFIER_TYPE
                    + "（聚合身份须为终类型 {Agg}Id，法卷锚 BP-13）";
        }
        return null;
    }

    private static Class<?> erase(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return null;
    }

    private static boolean isAggregateRootSubtype(Class<?> candidate) {
        for (Class<?> c = candidate; c != null; c = c.getSuperclass()) {
            if (AGGREGATE_ROOT_TYPE.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean implementsIdentifier(Class<?> start) {
        Deque<Class<?>> stack = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        stack.add(start);
        while (!stack.isEmpty()) {
            Class<?> current = stack.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (IDENTIFIER_TYPE.equals(current.getName())) {
                return true;
            }
            stack.addAll(Arrays.asList(current.getInterfaces()));
            if (current.getSuperclass() != null) {
                stack.add(current.getSuperclass());
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 块5 · 契约模块 —— contract 的对外纯洁性（C1）
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * C1 —— contract 纯契约模块（Service 接口 + CQE + CO + 枚举），不得依赖 server 侧的
     * adapter / application / domain / infrastructure，也不得依赖 Spring 运行时基础设施
     * （DI / Bean / 持久化）。
     *
     * <p><strong>守护</strong>：contract jar 是东西向消费方的<strong>唯一</strong>依赖（禁令卷）——
     * 一旦 import server 内部类型或容器运行时，消费方被迫拖入整套实现（依赖传染），
     * 「按契约编程」降级为「按实现编程」，server 发版节奏绑架所有下游。
     * <strong>怎么判</strong>：主语 {@code ..contract..} 段；宾语 = 四层段 + stereotype/context/
     * beans 三个 Spring 运行时段；宾语 {@code ..adapter..} 段<strong>不</strong>排除契约自家
     * {@code contract.{agg}.adapter.rest.controller} 子包——禁的是<em>向外依赖</em> adapter 层
     * 类型，契约命名自带该段（与框架 marker 坐标对偶，见 R8a）是教义要求、不构成违例。
     * <strong>重契约例外（既定教义，勿再收紧）</strong>：contract <em>允许且应当</em>携带
     * HTTP 映射注解（{@code @RequestMapping} 族）、Swagger 与 Jakarta 校验注解——契约＝完整
     * REST 定义、映射经 adapter 实现类继承，故只禁容器运行时三段而<strong>不禁</strong>
     * {@code org.springframework.web..}（裁决账 → 解读篇「契约独立块」节）；MyBatis 未列宾语段
     * （无理由 import，三段禁令已覆盖其全部合法注入路径）。
     * <strong>挂载</strong>：业务扫描 c1（契约类恒非空）；框架扫描不挂（扫描根无 contract 段包；
     * common-contract 模块属独立扫描域，登记账）。
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
                      .as("C1 Contract 纯契约：不得依赖 server 四层及 Spring 运行时基础设施（stereotype/context/beans 三段）；法卷锚 禁令卷 §5/CC-7");
}
