# 规则集为什么长成这样（ArchUnit 红线图与载体分工）

> **本页=解读架的"为什么"**：`DddArchitectureRules` 规则集设计论证的现行版：红线图怎么读、五块为什么这样分、类型锚点与段匹配各管什么、空集通过这个结构性弱点为什么被容忍、缺口账上挂着什么。严格件（治理规范 TR-1~3）在 [modules/test.md](../../specs/current/modules/test.md)「规则集治理」节；规则清单的字典镜像在 [common-test.md §2](../reference/api/common-test.md)；挂载形状（代码）的唯一权威在法卷场景 1。本页零条款零形状，只讲道理，冲突时法卷赢。

## 一行红线的三层载体（本案裁定的分工）

一条架构红线今天住三处，各干各的活。**代码**：常量与 `as()` 文本，即红线本身，是执法者。`as()` 以 R 编号打头、以法卷锚收尾，失败消息自带路由，违例者不用先查号再找法。**javadoc**：每常量四栏最小契约：守护什么、怎么判一句、挂载、空转或局限。改规则的人在代码面 30 秒内就知道这条线管到哪。**本页**：论证现行版：为什么这样判、被拒方案、沿革、缺口账。治理规范（编号永不重排、载体分工、缺口登记）不住 javadoc 也不住本页，住法卷 TR 系。规范行的 home 是法卷，这是归属法规定，不是品味选择。

为什么要拆：这三层曾挤在同一支 893 行的类 javadoc 里。读者要找"为什么禁 JPA"，得先穿过"怎么判"和"挂在谁身上"；文档作者想引用论证也够不着，因为 javadoc 不出编译产物。载体错位积的债，就是"不透明"这个观感本身。项目主裁定中档深度：类头的空集警示与缺口两节连详情也迁到本页，只留一行清单。**接受的风险**：改规则的人不翻本页，就看不见红线图非全图。对冲手段：法卷条款与 ddd-review 锚点抽查。

## 怎么读一支规则：主语—判定—宾语

每条规则都是同一句话的形状：**主语**（查谁）+ **判定**（禁/须有什么依赖或属性）+ **宾语**（查什么目标）。例：R3 = 「`..domain..` 段内的类，不得依赖 application / infrastructure / adapter / contract 四段」。看懂任何一条只需回答三个问题：主语用什么认（包段？类型？命名后缀？）；什么算依赖（`consideringAllDependencies()` 下字段/方法/继承/注解全计）；命中零条时怎么办（`allowEmptyShould`，见下节）。

**段匹配是「段」精确匹配，不是子串**。`..application..` 只匹配包段名恰为 application 的包；根包叫 `sampleapplication` 的服务不会因此全民皆 application 层。但 `..domain..repository..`（两段间可夹）与 `..domain.repository..`（两段必须相邻）是两种谓词：规范布局 `domain.{agg}.repository` 里 domain 与 repository 隔着聚合段，相邻式**永不为真**。这个区别制造过本库最严重事故：旧 R4 用相邻式匹配 `..domain.model..`，对一切规范布局的类永不命中，在空集放行机制掩护下当了很久的空文（见「空转防线」节）。

**类型锚点与包段是两种识别哲学**。包段回答「你站在哪」，是位置规则。类型锚点（实现某标记接口/可赋值为某类型）回答「你是什么」，是本质规则。规则集的分寸：判角色用锚点，判位置用包段。命名后缀（`*RepositoryImpl`、`*ControllerImpl`）从不被用来猜角色，只用来保证「名实相符，不符即失败」。

## 「保留段唯一语义」不变量——全体规则的前提

全部段匹配规则敢用裸 `..domain..` / `..application..` 而**零层排除**，靠的是一条包命名不变量：`adapter / application / domain / infrastructure / contract` 五个保留段，只允许出现在其真实层的位置上；任何子包不得借用保留段表达「接口归属侧」。

这条不变量是把一笔「包命名税」赎回来的。旧惯例按「实现哪层接口」给 infrastructure 子包命名，如 `repository.domain`、`repository.application`。于是 infra 的仓储实现包**同时命中两个段**：它依赖同为 infra 层的 Mapper，R1 的分层 DSL 就判出「Domain 依赖 Infrastructure」的冤案。当时的本息清单：业务侧被迫以根包前缀本地覆写 R1/R1b/R3/R6 四件套；规则库内养着 `PURE_DOMAIN_CLASSES` 排除谓词；R1b 宾语挂着一段历史排除；R13 的段匹配纯靠坐标巧合才咬得到实现类。2026-09-05 两步退税：阶段一纯包移动，读写归属改由类名后缀加标记接口表达；阶段二删光全部排除谓词，R13 宾语切换类型锚点，业务覆写退役。共享常量就此获得首次真实挂载。迁移坐标对照表（旧 → 新）：

| 旧坐标 | 新坐标 |
|---|---|
| `common.ddd.domain.repository.domain.Repository` | `common.ddd.domain.repository.Repository` |
| `common.ddd.application.repository.application.QueryRepository` | `common.ddd.application.repository.QueryRepository` |
| `common.ddd.domain.event.domain.DomainEvent` | `common.ddd.domain.event.DomainEvent` |
| `common.contract.dto.event.integration.IntegrationEvent` | `common.contract.dto.event.IntegrationEvent` |
| `sampleservice.domain.{agg}.repository.domain.{Agg}Repository` | `sampleservice.domain.{agg}.repository.{Agg}Repository` |
| `sampleservice.application.{agg}.repository.application.{Agg}QueryRepository` | `sampleservice.application.{agg}.repository.{Agg}QueryRepository` |
| `sampleservice.infrastructure.persistence.master.{agg}.repository.{domain\|application}.*Impl` | `…master.{agg}.repository.*Impl`（读写四实现并级） |
| `sampleservice.contract.{agg}.adapter.rest.{Agg}Controller` | `sampleservice.contract.{agg}.adapter.rest.controller.{Agg}Controller`——与框架标记 `common.ddd.adapter.rest.controller.RestAdapter` 精确对偶（server 实现镜像同一坐标） |

坐标对偶现在也是不变量的一部分：contract 契约接口、server 实现、框架 marker 三段全同深，「镜像框架 marker 坐标」被保留段纪律接管。破坏不变量的后果是成片式的：段匹配歧义复活，要么规则成片误报，把 infra 内部自依赖判成跨层；要么为躲误报加排除，排除再掩护成片漏网。这也是该裁决郑重落案卷 §裁决记录、而不是当清理顺手改的原因。

## 五块分工：一条红线图的四象限加一页契约

规则按「问题类型」而非「层」分块：**块1 层间方向**，谁能依赖谁。**块2 领域纯净**，domain 内部不许出现什么。**块3 装配位置**，实现类与读写边界放哪。**块4 标记与命名对偶**，类型锚点 ⇄ 名字互锁。**块5 契约独立**，contract 对外纯洁性。块4 是其余各块的识别基础设施：标记接口住 common-ddd，正反向成对锁死。正向（实现标记 ⇒ 必须在某层段内）防角色泄漏；反向（包段/后缀 ⇒ 必须实现标记）防名实漂移。R13 是同一哲学在禁则方向的延伸。块5 单列，因为 C1 的辖域是 jar 边界不是层边界。

## 层间方向块（R1 / R1b / R2 / R3 / R6）

**R1（四层依赖方向）**守的是六边形脊柱：infrastructure 通过实现 domain 的端口「倒置」接入。一旦任何外层能直接依赖 infra，SQL 与 SDK 类型就沿捷径渗入业务，分层名存实亡。读侧有个结构性例外：CQRS 读端口定义在 application、由 infrastructure 实现，PO → DTO 直接投影、绕过聚合根，构成写侧倒置的镜像「infra → application」，故 Application 层额外放行 Infrastructure 访问。contract 契约接口按裁决保留 `adapter` 段，成为分层 DSL 的 Adapter 层成员；`ControllerImpl → 契约接口` 属同层访问，DSL 不禁同层，语义自洽，业务扫描绿为实证。框架扫描不复用本常量，持「+Configuration 层」覆写：common-ddd 根包自动配置类（`MybatisDddAutoConfiguration`）天生跨层装配，共享四层切分容纳不了它，且框架无 adapter/contract 业务组件。

**R1b（读端口类型白名单）**把 R1 的整层放行收窄成三个锚点：实现 `QueryRepository` 的读端口、实现 `ApplicationDTO` 的读视图、以及**前者的嵌套类**。嵌套类要单列，因为嵌套 DTO 按约定随外层定型、字节码上不携带标记接口；白名单若不放行「外层是 ApplicationDTO 实现的嵌套类」，读实现引用嵌套视图即误报。没有 R1b，整层豁免就是万能后门：仓储实现可以反手调 Handler 编排或 Presenter 呈现，application ⇄ infrastructure 循环依赖从此合法。它的历史宾语曾挂过一段 infra 排除谓词，那是命名税的利息：infra 的 `repository.application` 子包双段命中，内部自依赖被误判成跨层。迁移后排除永无命中可能，随之删除；防误报职责由「保留段唯一语义」不变量接管。

**R2（adapter 纯透传）**封两条捷径：在 Controller 里调聚合根，或在 Controller 里调 Mapper。一旦允许，AppService → Handler → Presenter 的用例编排整链被跳过，DTO/CO 强制分离随之瓦解。主语段有意把 contract 的契约接口也罩进来，它们同样不得触碰 server 内部层。分工：R2 不判「adapter 是否依赖了错误的 application 内部组件」，那是 R1b 与 sample 本地锚点规则（`A4` 系）的职责。业务扫描现状不挂 R2：共享 R1 的 whereLayer 声明加 R8a/R8b 类型锚点组合已覆盖同等语义，重复挂载只在报告里制造双重错误行。

**R3（domain 零外依赖）**是依赖箭头的最后一条逆向路径。domain import 了 CO/DTO/PO/Mapper 的那一刻，聚合模型开始感知传输格式与存储格式，CQRS 边界与契约独立性同时从根部蛀空。宾语 `..contract..` 段会命中框架契约模块的包：domain 依赖框架契约类型同样违例，这是预期收紧不是误伤。诚实账：跨仓「domain → 框架 infrastructure」的依赖仍看不见，外部类不进扫描 subject；此洞由教义与 review 收口。扫描边界制造的盲区，登记之，不假装全图。

**R6（domain 不感知认证）**：domain 一旦读「当前用户」，业务规则就和请求上下文耦合，聚合无法在系统任务、测试夹具、事件回放中独立运行。createdBy 之类身份注入的正确位置是应用层下沉传入。框架扫描不挂载本规则：common-ddd 的 pom 根本不依赖 common-security，编译期已断绝，挂载属结构性空转；对依赖 security 的外部消费方，本常量照常可挂。

## 领域纯净块（R4 / R12）

**R4（框架中立）**教义的准确措辞是「零框架**运行时**依赖」：DI 上下文、AOP、事务、Web 能力禁入 domain，因为业务规则必须能在脱离 Spring 的纯 JVM 下推理与测试。豁免与不射程要分三家。`org.springframework.stereotype` 装配注解是**既定白名单**：纯元数据，不改编译与运行语义；框架 Factory 标注解、领域服务标注解就是它。BusinessException、Lombok、common-ddd 骨架类的编译依赖**不在本规则射程**，是教义明示的编译期底座。JPA 注解（jakarta/javax persistence 两族）**在禁**：实体被 `@Entity`/`@Table` 焊死在特定 ORM 上，而本框架持久化语义全部由手写 XML SQL 承担，PO 尚且零 ORM 注解，实体携带 JPA 属双重违宪。**MyBatis API 渗入 domain 是登记过的盲区**：外部包、无层段特征，任何方向规则都看不见，只能靠 R1/R3 间接防线与 review。本规则的挂载理由：框架自证，加业务承接原 sample 本地白名单教义（旧 A2 已上收删除）。

**R12（禁 public setter）**守充血模型的根：状态变迁只许经行为方法。public setter 让外部绕过聚合根的状态机守卫直接改写内部状态，把「取消订单跳过库存回补」这类事故从设计错误降级成一次自动补全。Lombok 在 domain 类上生成的 setter 字节码与手写无异，一并拦截；这条规则就是「domain 禁 `@Data`」教义的机器化。刻意不查的有三类：包私有 setter，教义允许同包 Factory 走业务构造器；字段直改即 public field，由封装惯例与 review 兜底，机器化它会误伤枚举等合法形态；返回 this 的流式 setter **查**，且命中属预期。

## 装配位置块（R5a / R5b / R11 / R13）

**R5a（写端口必是 interface）**守端口形态：domain 的 repository 段内若出现 class，即「域层内实现仓储」，倒置链条在源头断裂。它用的是**相邻**段匹配：框架端口 `domain.repository.Repository` 直接相邻、真实命中；代价是业务嵌套布局 `domain.{agg}.repository` 永不相邻，**业务扫描空集通过，登记在册**。业务写端口的 interface 定型，实际由框架扫描加 sample 本地锚点规则（`A3`，按 Repository 标记锚定）分担。保留业务侧挂载不为装样子：扁平布局（`domain.repository.*`）的外部消费方一挂载即生效。

**R5b（实现类物理位置）**：实现类住在哪个包，是它对层的宣言。仓储实现被丢进 application 或 domain，「打开一个聚合目录看到该聚合在该层的全部代码」的自包含结构即破，Mapper/PO 的引用面随之漂移。主语取类名后缀 `RepositoryImpl`；读端口实现同后缀，读实现也必须在 infra；通配宾语段容纳「多数据源 + 按聚合分包」的层级。它的真实守护点在业务扫描：sample 两聚合读写共 4 个实现，实数命中（真实例指针位）。框架扫描曾经的挂载已撤销：框架根本没有该命名形态的类，实现由各服务继承 `MybatisPersistence` 支撑类完成，挂着只是给报告添一行恒真空转。局限与锁链：只认后缀，改名即逃逸。命名教义（编码公约卷命名表）是这条规则的主语来源，两者互为前提。

**R11（写侧事务边界）**把一种静默事故变回红线。本框架的仓储支撑类**刻意不声明事务**，边界上收应用层；漏标注解没有任何编译错误，只有数据会说话：多次持久化各自提交，中途失败不回滚。规则查 methods 级：名为 `handle`、声明类实现 `CommandHandler` 标记 ⇒ 必须带 `@Transactional`。主语用标记接口而非包位置，仍是锚点哲学：实现端口的类才是 Handler，包名会骗人。读侧 QueryHandler 刻意豁免，只读可省。**教义承诺的最后一口缝在 2026-09 被堵上**（账 A 销账）：法卷 BP-10/WC-2 从来要求 `@Transactional(rollbackFor = Exception.class)`，旧规则却只查注解存在性，裸标注解照过；而 Spring 默认回滚规则**不覆盖受检异常**。本仓异常族全是 runtime，默认规则够用；缝在 Portal/SDK/IO 冒出 checked 的那一刻，届时半途提交且构建全绿。现谓词＝「注解存在 ∧ `rollbackFor` 显式声明」。严格度定档为**存在性、不判值**：`Throwable.class` 等等价或更严的显式写法放行；逐值白名单不做，ArchUnit 属性值多形态解析脆，值级执法要补另案。四锁负证明见「空转防线」节。残余逃逸面一条：不实现接口的编排包装器天然罩不到（账 B，见本页「缺口账」）。

**R13（读写隔离）**禁 QueryHandler 依赖任何写端口类型。读侧固定模式是查询完全绕过 domain：读端口 → infra 实现 → PO 直接投影读 DTO。读 Handler 图省事注入写仓储，读路径就背上聚合重建成本，还为「读通道偷偷调写行为」开门。这条规则最近一次改造是个教义案例：**从段匹配切换为类型锚点**。旧宾语 `..domain..repository..` 能咬到实现类，纯靠旧坐标 `…repository.domain` 的巧合，infra 的包名恰好含 domain 段。2026-09-05 迁移消灭该子包后，规则**静默失去识别力**却照常绿灯。「靠巧合命中的规则，坐标一变就无声退役」比从不命中更危险。故宾语切换为「可赋值为 Repository 标记」：业务写端口接口、其实现、未来任何新形态全覆盖，布局无关。读写分途由两个互不继承的 marker 保证，读端口不继承写端口，白名单不误咬。局限：依赖图深度 1，经包装器间接持有不命中（同 R11 账）。

## 标记—命名对偶块（R8 / R10 / R14）

三个角色、三对双向锁，同一套设计语言。

- **R8a/R8b（REST 入口）**：实现 `RestAdapter` ⇒ 必须在 adapter 段；`*ControllerImpl` 结尾 ⇒ 必须实现标记。标记语义 = 纯透传入口：透传 AppService，零业务。被标记类出现在 application，是入口逻辑内移；Impl 不实现标记是反向漂移，等于自造入口角色绕过定型。
- **R10a/R10b（应用层内部视图）**：实现 `ApplicationDTO` ⇒ 必须在 application 段；`..application..dto..` 包下顶层类 ⇒ 必须实现标记。守的是 DTO/CO 强制分离：内部视图携 version、逻辑删除位这类敏感字段，漂到别层即泄漏内部性。R10b 排除嵌套类与接口：嵌套视图随外层定型、不重复标记；dto 包可声明多态视图接口本身。
- **R14a/R14b（时间驱动入口）**：实现 `ScheduledAdapter` ⇒ 必须在 adapter 段；业务 `..adapter..scheduler..` 包下非接口类 ⇒ 必须实现标记。时间只是另一种协议，driving adapter 的职责与 REST 同构。主语刻意收窄到业务入口，不反向约束框架内部。

这对偶体系的总账：识别一律用类型锚点；名字规则只负责「漂移即失败」，不负责「猜测角色」。R8/R10/R14 在框架扫描为空集通过是**教义例外**：挂载守护的是「标记接口之家不被搬走」，即 common-ddd 里标记接口的包结构本身；消费方一旦落地实现类，即自动收紧为真实守护。

## 契约独立块（C1）

contract jar 是东西向消费方的唯一依赖。它一旦 import server 内部类型或容器运行时，消费方被迫拖入整套实现，即依赖传染；「按契约编程」降级为「按实现编程」，server 发版节奏绑架所有下游。宾语取四层段加 Spring 运行时三段（stereotype/context/beans）。两条裁决要说清，防后来者「顺手收紧」。**重契约例外**：contract 允许且**应当**携带 HTTP 映射注解、Swagger 文档注解与 Jakarta 校验注解。契约 = 完整 REST 定义，映射经 adapter 实现类继承。这是裁决后的语义不是疏漏，故只禁容器运行时三段、不禁 spring-web。**MyBatis 不列宾语段**：contract 无任何理由 import 它，三段禁令已覆盖其全部合法注入路径。主语的 `..adapter..` 段不排除契约自家 `adapter.rest.controller` 子包：C1 禁的是「向外依赖 adapter 层类型」，契约接口命名自带该段是坐标对偶的教义要求，不构成违例。挂载在业务扫描：框架扫描根里没有 contract 段包，框架契约模块属独立扫描域，暂不在两扫描根内（账）。

## 挂载账（谁真的在守）

入口类全在 sample-server 测试树。框架扫描从 sample classpath 扫 common-ddd 包：框架要求业务遵守的每一条，框架自己先过一遍。扫描入口是 `ApplicationArchitectureTest`（业务扫描根）、`DddArchitectureTest`（框架扫描根，持 R1 覆写版）；负证明入口三枚，配对定向导入夹具住 `archproof` 系包（两扫描根外）：`DomainPurityRuleProofTest`（R4）、`TransactionBoundaryRuleProofTest`（R11）、`IdentifierRuleProofTest`（R15）。规则 × 入口 × 非空转来源：

| 规则 | 框架扫描 | 业务扫描 | 真实开火来源 |
|---|---|---|---|
| R1 | 覆写版（+Configuration） | 共享挂载 | 业务分层类齐全 |
| R1b | 不挂（infra→app 零依赖，挂了空转） | 共享 | 读实现真实引用读端口 |
| R2 | 共享 | 不挂（见 R2 节分工） | 框架 adapter 标记包恒非空 |
| R3 / R4 | 共享 | 共享 | 双方 domain 包真实非空 |
| R5a | 共享（真实命中） | 共享（**空集登记**） | 框架端口相邻命中 |
| R5b | 已撤销（结构性空转） | 共享 | 4 个实现类实数命中 |
| R6 | 不挂（pom 已断绝） | 共享 | 业务 subject 真实非空 |
| R8/R10/R14 系 | 共享（空集=教义例外） | 共享 | 业务实现类实数命中 |
| R11 / R12 / R13 | 不挂（无实现类，挂了空转） | 共享 | Command/Query Handler 真实非空 |
| R15 | 不挂（框架扫描无具体聚合根，挂即空转） | 业务侧挂载 | 迁型实弹：迁型前恰咬 4 处 → 迁型后 0 咬；负证明四锁 |
| C1 | 不挂（扫描根无 contract 段） | 共享 | 契约类恒非空 |

## 空转防线与缺口账

**空集通过是接受了的结构性弱点**。消费方测试类路径的 `archunit.properties` 全局声明 failOnEmptyShould=false，多数禁则另写 `allowEmptyShould(true)`。后果：零命中与规则写错成永真，结果不可区分。为什么不改成强制非空：扫描根各异，框架扫描天然没有业务组件；强制非空会把每一条「教义例外」挂载变成构建噪音。弱点接受的代价由三件套对冲：**负证明**、**挂载账**、**诚实登记**。

**负证明是空转防线的最高档，现役三把**。R4 是**第一个**获此待遇的禁则，夹具为 `DomainPurityRuleProofTest` 三锁。① 违例必失败：domain 包内依赖 Spring 运行时的定向夹具必须让 `check()` 抛错，证明规则会咬人。② 豁免必通过：真实 stereotype 标注类与迁移后的写端口必须通过，证明白名单不是死闸、规范嵌套布局确在射程。此断言单独无证明力：谓词若是旧版相邻式，「根本没人被看」也照样绿，**必须与 ① 合读**。③ 纯净聚合根必通过，充当回归哨兵。R11 属性收紧落地**当日补四锁**，夹具为 `TransactionBoundaryRuleProofTest`：裸标必咬、漏标必咬、显式标注必放、非 Handler 不咬。R15 顶位接管**当日同补四锁**，夹具为 `IdentifierRuleProofTest`：裸类型根必咬、类型化根必放、豁免位不误伤、端口裸槽必咬——第四锁原形「端口-根槽不一致」经 F 边界勘获系 javac 不可构造，改定见 [typed-identifier.md](typed-identifier.md)。新谓词不过负证明就仍是可能的空文；R4 事故学到的教训不在这里重演。其余规则的非空转，依赖上表挂载账的实数来源背书，暂无负证明夹具。这是现状不是裁决，补证按案另立。

**已知缺口**（教义有、规则无；类头只余一行清单，详情在此）：

| 缺口 | 现状与原因 |
|---|---|
| Handler 必须返回 DTO（禁 CO） | 可静态查 handle 返回类型，未实现；本 case 只登记不施工 |
| AppService 返回 CO / 不含编排逻辑 | CO 返回可查；「编排语义」本质不可机械化，永久归 review |
| 时间类型统一 `OffsetDateTime` | 无规则，纯靠编码公约卷纪律。可查禁型（LocalDateTime/ZonedDateTime），未裁决 |
| 禁具名领域异常 | 可查：domain 内异常只准 `BusinessException`＋i18n 位点，具名子类即违例；未实现 |
| PO 零 ORM 注解 | 无规则，自觉 + sample 达标 |
| 包装器逃逸（`handler.command` 包内不实现接口的编排包装器） | **待裁决账 B**：1 CQE : 2 Handler 已有首例，R11/R13 主语锚点罩不到。补包位置规则，还是教义承认包装器合法，未拍板 |

缺口的纪律：登记 ≠ 永挂，每条要补的立各案。**废止也是账**：R15（全仓禁某 ORM 增强库）已删，因为规则库不为「项目选择不用的库」立特别法。依赖品味随选型变化，特别法会变成陈旧负债；经 BOM 显式推荐的多数据源库触它纯属误伤。其负证明夹具同案撤销。废号不空坑：2026-09 按「占位缺失就顶上」之裁，新 R15（聚合根身份终类型化）已顶位接管此号，旧作废账原位保留（→ 案卷 2026-09-typed-identifier §裁决记录 Q8）。

## 沿革（为什么今天是这个样子）

现行各规则的形状，全部是事故与审计的对价。R4 由「相邻式空文 + 白名单三方矛盾」审计重写。R1b 曾为零挂载死规则，sample 养着同构本地副本；接线修复后，共享常量才第一次被真实执行。R5b 曾错挂框架侧恒真空转、业务侧裸奔，审计纠偏。R13 从巧合段匹配升级为类型锚点，教训见其节。R1/R3/R6 曾因包命名税被迫本地覆写，税根治后覆写退役。R11 从「注解存在」升级为「注解 ∧ rollbackFor 显式存在」，并即日补四锁负证明（账 A 销账，缺口账只余账 B）；同一时点，全部 `as()` 消息改挂法卷锚条款号。包命名税迁移本身分两阶段：先纯搬家，后删排除谓词，全程 git rename 保历史。沿革细目不在本页展开：法不考古，卷宗已历清册归零，此处只留「现状为什么如此」的因果；再往前，git 史非知识依赖。

## 一把尺子

十五支规则加一支契约禁令，其实只有三句话。**位置用段匹配判，本质用类型锚点判，巧合命中一律换锚点**。**能空转的规则必须说得清凭什么不空转**：负证明、实数命中、或登记教义例外，不许「大概有用」。**覆盖不到就记账**：缺口表、盲区段、废止案都入账；承认红线图非全图，好过假装全图。载体分工是第四句：**红线住代码、契约住 javadoc、道理住解读架、规范住法卷**。一句话有它自己的 home，读者就不用穿过八百行找它。

---
*本页属 `explanation/` 解读架（地图区）：治理条款的权威是法卷 [modules/test.md](../../specs/current/modules/test.md)「规则集治理」节，冲突时法卷赢（宽严双份）。本页论证是决策快照的沉淀现行版：快照冻结，本页随法演化。*
