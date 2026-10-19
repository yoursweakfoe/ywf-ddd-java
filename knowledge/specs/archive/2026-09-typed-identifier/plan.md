# 施工方案与修卷 delta
> 本节回答「怎么做」与「改哪些法」，是这两类内容的唯一居所。P-x 是技术决策编号；tasks 条目只引用编号，不抄参数。AC-n 对应 specify 的验收账。

## §1 技术形状（机制选型、坐标、文件落点、参数值）

- **P-1 身份词汇**：选定 common-ddd `domain/model/` 新增纯 Java 接口 `Identifier<V>`，唯一方法 `V value()`，零 JDK 外依赖、无默认方法、无抽象基类；被拒：引入 jMolecules 制品（借词汇不借 jar，specify 已裁）、phantom 泛型 `Id<A>`（类型擦除下 A/B 互赋编译放行＝假安全）、抽象基类路线（record 无法继承类）。
- **P-2 {Agg}Id 形状**：选定 `public record {Agg}Id(V value) implements Identifier<V>, Serializable`，落位 `domain/{agg}/id/`，命名 `{Agg}Id`；紧凑构造器仅 null 检查，**不做底值合法性校验**——禁查格式（非法格式到不了此处：Web 层类型转换 400 判例在先），亦**禁查 UUID 版本位**（存量行/手工插入行经装载路径照样过 of()，查版本=炸历史数据；出生保证归铸造入口 AggregateIds，类型只装箱不站岗）（裁定 Q5）；`Serializable` 系满足 `MybatisPersistence` 持久槽既有约束，JDK 面不破域纯度；静态入口 `of(V)` 供 P-3 定型位使用。被拒：嵌套于聚合根文件（跨聚合 import 须只见 id 包，不见根类）。
- **P-3 类型化币种范围**：选定「写侧全程、读侧契约不入」——{Agg}Id 流通域 = domain（根身份槽、跨聚合引用槽）+ application 写侧（Handler 局部、DTO、DomainService 签名）；豁免位 = 契约 CQE/CO、PO、读侧（QueryRepository 端口与读 DTO）、聚合内子实体 PK，全维持原生承载。**读侧豁免的根据是宪章不是权衡**：RC-6 现行文本已定「读端口禁泄漏 domain 类型」，项目主裁定读侧独立性原则（裁定 Q6）——本案对 id 包不开任何新例外；登记残余账：读端口参数混放（两者皆裸 UUID）不受编译锁，风险定级低（读错=404 级难堪，非写坏级灾难）随此姿势接受。被拒：读侧同步类型化（正面撞 RC-6 与「读绕过 domain」教义）。
- **P-4 持久接缝**：选定 RepositoryImpl 覆写既有钩子 `toPersistenceId`（`id.value()` 一行）为唯一领域 ID→原生主键转换位，每仓储恰一处；PO / Mapper 接口 / XML 七语句 / DB schema 零修改；DddMapper javadoc「业务铸造 ID 的聚合显式插入 id」句在转换后仍逐字为真。被拒：给 DddMapper 加身份泛型（把写侧币种渗进持久契约，破 P-3 豁免面）。
- **P-5 执法与负证明**：选定 ArchUnit 新规则 **R15**（现集尾数 R14b 后第一位**废号顶位**——裁定 Q8：「占位缺失就顶上」；旧 R15 baomidou 禁令作废账原位保留）：凡 `AggregateRoot` 具体子类的 ID 泛型实参必须实现 `Identifier`，且 `Repository` 端口 ID 槽与根槽一致；负证明探针按 TransactionBoundaryProbes 四锁格式成对发行（裸类型根必咬 / 类型化根必放 / 子实体 PK 与读端口豁免位不误咬 / **端口裸槽必咬**——施工时点勘获：原「端口-根槽不一致」形状被框架 F-边界 `Repository<Domain extends Identifiable<ID>, ID>` 编译期锁死、javac 不可构造，等式臂降为裸继承/擦除逃路之防御备胎，见 implement §4），混放编译锁以 javac 期望失败取证脚本落账。被拒：只发规则不发探针（R11 先例：新谓词不过负证明即空文）。
- **P-6 示例迁移形状**：选定 sample 两聚合同步迁型作参考实现——两 `*Id` record、两根泛型实参换型、Factory/reconstitute/Converter 装配点、OrderItem 的商品引用槽、InventoryDomainService 与全部写 Handler 入口定型、测试夹具（TestOrders/OrderFixtures 等）随动；全量 mvn 绿 + check-docs 绿。被拒：只改法卷不动 sample（法失参照即空转）。
- **P-7 来源三分法定型**：法裁三来源=应用铸造（默认姿，铸造唯一入口教义不动）/ 自然键（{Agg}Id 包业务值事实，无铸造）/ DB 代铸（insert 省 id + useGeneratedKeys 通道——DddMapper 契约已留半槽；其「创建即合法」例外语义本案不立）。机制类型无感的 AC-5 证明以法卷内 Long 承载教例（`PaymentId(Long value)` 形态）纸面完成，不落 sample、不建设施。被拒：本案顺手补号段/雪花（为未存在需求建机制＝事件管线病灶翻版）。
- **P-8 计数与全表重排（裁定 Q7 定档）**：新件 `{Agg}Id` **插位为 ⑫**（domain 段、聚合根之前——身份先于根被引用，阅读序=依赖序），原 ⑫–㉒ 全体顺移 +1，**①–⑪ 不动**（插位点在其后）。总计数 22（20+2）→ **23（21+2）**。换号全表（旧件名 → 旧号 → 新号）：契约接口 ①、CO ②、Command ③、Query ④、ControllerImpl ⑤、AppService ⑥、DTO ⑦、Assembler ⑧、Presenter ⑨、CommandHandler ⑩、QueryHandler ⑪——以上十一次段原号照旧；聚合根 ⑫→⑬、状态枚举 ⑬→⑭、Repository 端口 ⑭→⑮、PO ⑮→⑯、Converter ⑯→⑰、Mapper ⑰→⑱、RepositoryImpl ⑱→⑲、手写 XML ⑲→⑳、读端口 ⑳→㉑、读实现 ㉑→㉒、契约枚举 ㉒→㉓；新件身份终类型 = 新 ⑫。映射一行注于蓝图 §生效登记（卷内自闭环考古锚）。
  - **重排波及白名单（全库 45 文件圈号勘验分类毕）**：仅「蓝图槽位引用」语境改号 = `aggregate-blueprint`（全卷含 §2 各 BP 行内嵌引用与 §4 节题）、`write-chain`（2 处：Assembler 最小契约引 ⑧、XML 模板引 ⑲）、`scheduler`（SC-6 行 2 处引 ⑭/⑰）、skills 四件（new-aggregate 全篇、scheduled-task、new-service、ddd-review）、折叠时逐线复核两处存疑件（`docs/README`、`explanation/architecture-rules` 长行）。
  - **排除判据（一字不碰）**：立法工序圈号 ①–⑩ 与 ③⁺/⑨⁺ 家族（knowledge/README、drive-relations 图源+SVG、attribution-law、glossary、archive/README、new-bill、_template/specify）、`patterns/_template/blueprint.md`（抽象界 ㊞ 占位，非实号）、业务卷与代码注释的列举号（sample order.md、DddArchitectureRules/AuditFieldFiller/AggregateIds javadoc、pom、quickstart 脚本步号）。
  - **折叠序硬约束（并行案卷协调）**：在途案 `2026-09-pattern-taxonomy`（Plan 门已过、折叠①–④待施工）将迁 patterns/ 法卷物理架（git mv 十摊）——本案**代码侧施工可即刻并行**（零卷面交集）；本案**折叠写回 current/ 必须排于该案折叠之后**，delta 锚一律按卷逻辑名记、落位时刻解析新路径；该案若撤，本约束自动解除。
  - 被拒：末号顺延挂新号不重排（案起草人默认倾向）——项目主推翻：「蓝图就改呗，又不涉及代码，维护文档不是代码库层面的大破坏，改就改了」（一致性优先于小 diff，Q7 在案）。

## §2 修卷 delta（对 current/ 各卷与章程的修改量；折叠时全部修订以本节为准写入）
> MODIFIED 整节替换 current/ 同名节，成对条款则整对替换；REMOVED 删节，并在归档记录留一句原因。每条标 ← AC-n。条款编号已按各卷现序取号（BP 现尾 BP-12，WC 现尾 WC-12，规则现尾 R14b）。

### ADDED
#### Requirement: 身份词汇发行（ddd 模块卷）   ← AC-2
系统 SHALL 于 common-ddd `domain/model` 发行纯 Java 身份词汇接口 `Identifier<V>`（唯一方法 `V value()`，零 JDK 外依赖），作为聚合身份终类型的唯一类型学锚点；框架不发行抽象基类、解析器或任何运行期设施。（源：`common-ddd/.../domain/model/Identifier.java` 在库；框架扫描 `DddArchitectureTest` r3/r4 绿 11/11＝域纯度不破）
##### Scenario: 域纯度不破
- GIVEN 新接口文件 ｜ WHEN R4 现行域纯度规则扫描 ｜ THEN 绿，零新增依赖边

#### Requirement: BP-13 聚合根身份终类型化（蓝图 §2）   ← AC-1
聚合根 SHALL 以专属终类型 `{Agg}Id`（`public record`，implements `Identifier<V>` 与 `Serializable`，落位 `domain/{agg}/id/`，命名 `{Agg}Id`，紧凑构造器仅 null 检查、不校验底值，形状细则住 P-2）为其 `AggregateRoot<ID>` 身份槽实参；原生类型（UUID/Long/String 直用）不再合格。R15 + 负证明探针锁死。（源：`DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS`；实形 `domain/order/id/OrderId.java`、`domain/product/id/ProductId.java`；负证明 `IdentifierRuleProofTest` 四锁 4/4）
##### Scenario: 混放编译锁
- GIVEN `OrderRepository.findById` 期望 `OrderId` ｜ WHEN 调用位传入 `ProductId` ｜ THEN 编译失败（javac 期望失败取证，实录 → implement §3）
- AND GIVEN 根为裸类型 UUID 的教例 ｜ WHEN R15 扫描 ｜ THEN 必咬

#### Requirement: BP-14 跨聚合引用槽类型化（蓝图 §2）   ← AC-1
domain 层跨聚合引用 ID 槽（字段、方法参数、集合与 Map 键）SHALL 使用目标聚合的 `{OtherAgg}Id`；跨聚合 import 仅准入目标 `id` 包。（源：`domain/order/model/OrderItem.java` productId=`ProductId`、`domain/shared/service/InventoryDomainService.java` 签名；`OrderTest`/`InventoryDomainServiceTest` 绿）

#### Requirement: BP-15 类型化豁免面（蓝图 §2）   ← AC-3
契约层 CQE/CO、PO、读侧端口与读 DTO、聚合内子实体 PK SHALL 维持原生类型承载，`{Agg}Id` 不入这些槽位；读侧不入的根据 = RC-6 读侧 domain 独立性宪章（读端口禁泄漏 domain 类型，现行法，本案不为其开 id 包例外——裁定 Q6）。（源：`application/order/repository/OrderQueryRepository.java` 与 `OrderViewDTO` 裸 UUID 签名零动、`OrderPO` 原生 id、contract 模块 diff-zero；探针锁三 `IdentifierProbes.BareEntityPk`/`ExemptReadPort` 不误咬绿）

#### Requirement: WC-13 写侧入口身份定型（写链卷 §1）   ← AC-1
写 Handler（含批量与 Scheduler 入口）SHALL 在调用任何 domain 接口前，把 CQE 携入的全部裸 ID 经 `{Agg}Id.of(...)` 一点定型；禁止任何隐式自动转换（全局 Converter、AOP、Jackson 直灌 domain）代劳此步。（源：`PayOrderHandler.java:34` 恰一行 `OrderId.of(command.getOrderId())`、`PlaceOrderHandler.java:68/91` 批量行项定型；全局 Jackson/Converter 零增设=contract 与 config diff-zero）
##### Scenario: 一点定型
- GIVEN `PayOrderCommand` 携 `UUID orderId` ｜ WHEN Handler 体 ｜ THEN 恰见一行 `OrderId.of(command.getOrderId())`，其后链路全为 `OrderId`

#### Requirement: BP-16 身份来源三分法（蓝图 §2）   ← AC-5
聚合身份来源 SHALL 分三档：①应用铸造（默认姿，经框架铸造唯一入口，创建即合法不破）；②自然键（`{Agg}Id` 包业务值事实，无铸造）；③DB 代铸（insert 省 id + useGeneratedKeys 通道；其「创建即合法」例外语义未立，首用者另案）。`{Agg}Id` 之 V 准任意单值可比较原生类型，机制不绑定 UUID。（源：`Identifier<V>` 泛型位＋R15 反射臂只验接口不验承载（`AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS`）；`OrderFactory` 源① `OrderId.of(AggregateIds.mint())`；源③ 半槽=DddMapper javadoc「DB 自增省 id 列 + useGeneratedKeys」契约在库；Long 承载教例为纸面同形证明，见 P-7）

#### Requirement: BP-17 非聚合持久对象豁免（蓝图 §2）   ← AC-5
纯查询表、配置表、关联中间表及无合适单值代理主键的老表 SHALL 可被业务包声明为非聚合持久对象：不强套聚合蓝图套件、不为过铸造假主键；治理归读端口与 CRUD 旁路条款。反面向：欲入写侧聚合链路者，先须具备三档合法身份之一。（源：R15 主语谓词只罩 `AggregateRoot` 具体子类与 `Repository` 端口、豁免位零误伤实证 `IdentifierRuleProofTest.rule_passes_exempt_positions`＋sample 读侧/PO 全绿）

### MODIFIED
#### Requirement: BP-6 契约 ID 承载条款行（蓝图 §2）   ← AC-3   <!-- 折叠时整行替换 current/ BP-6 -->
契约层聚合 ID 引用一律底层原生值（现档 UUID，判据 B12 教义不变）；写侧 domain/DTO 槽位由 BP-13/14 接管；PO 维持原生值；String 形态仍只许出现在 JSON wire 出口（CO 字段与 Presenter toString 位）。
#### Requirement: 蓝图全卷槽位号重排（§1 表 + §2 各 BP 行内嵌引用 + §4 节题 + §5）   ← AC-6/AC-7   <!-- 折叠时按 P-8 映射机械改号，逐文件以 P-8 白名单为准 -->
新件 `{Agg}Id` 插位为 **⑫**，原 ⑫–㉒ 顺移 +1（⑬–㉓），①–⑪ 不动；全套计数宣称 22（20+2）改 23（21+2）（C2 盯防，含 BP-1「建满 23 个文件」、BP-3 建序区间、§1 表题范围记号）。
#### Requirement: 蓝图 §生效登记 换号映射行   ← AC-7   <!-- 本节内追加一行 -->
一行记「本卷 2026-09 换号：旧 ⑫–㉒ → 新 ⑬–㉓，映射全表见案卷 2026-09-typed-identifier」（纯文本指针，案卷清册后本卷映射行为考古权威）。
#### Requirement: 蓝图 §4.⑬/⑮/⑰/⑲ 逐件走查（换号后新题）   ← AC-1/AC-4   <!-- 各节整替换：变化仅泛型实参、新件形状节、Converter 装配一行、toPersistenceId 覆写示例 -->
新节 4.⑫ `{Agg}Id`（record 形状 + of() 装箱不站岗注）；教例 Payment 家族：`AggregateRoot<PaymentId>`、`Repository<Payment, PaymentId>`、Converter 装配 `PaymentId.of(po.getId())`、RepositoryImpl `protected Serializable toPersistenceId(PaymentId id) { return id.value(); }`。
#### Requirement: 蓝图 §5 服务骨架通式   ← AC-7   <!-- 整节替换 -->
包结构通式补 `{agg}/id/` 位（⑫）；计数随 §1；节内槽位引用按映射顺移。
#### Requirement: 写链卷 2 处槽位引用行（§2.6 Assembler 注、§2.10 XML 注）   ← AC-7   <!-- 行内改号：⑧→⑨、⑲→⑳ -->
机械换号，条款正文语义零变。
#### Requirement: scheduler 卷 SC-6 取证行 2 处   ← AC-7   <!-- 行内改号：⑭→⑮、⑰→⑱ -->
机械换号（SC-6 调用链步骤 ①–⑥ 为工序号，不碰）。
#### Requirement: 写链 §2.5/§2.8 形状   ← AC-1   <!-- 各节整替换 -->
四拍示例 Handler 入口补 of() 定型行；聚合根形状换 `PaymentId`。
#### Requirement: 跨聚合 CA-7 条款行与 §2.2/§2.3 形状   ← AC-1   <!-- 条款行整替换 + 两节整替换 -->
签名 `Collection<UUID>` → `Collection<{Agg}Id>`；示例 Map 键同步换型。
#### Requirement: 批量 §2.2 形状   ← AC-1   <!-- 整节替换 -->
`List<UUID>` 契约输入维持（BP-6 不变），注释改「入口 of() 定型后进 Handler 链路」。
#### Requirement: 防腐 GW 相关 §2.2/§2.3 形状   ← AC-1   <!-- 各节整替换 -->
Portal 接口引用 ID 参数用目标 `{RefAgg}Id`（domain 侧），Gateway 实现内 `value()` 出站还原原生值传外部 SDK。
#### Requirement: 应用对象 AO-8 条款行与 §2.3 RecordDTO   ← AC-1   <!-- 条款行整替换 + 节整替换 -->
防腐入口定型目标由 UUID 改「目标 `{Agg}Id`」。
#### Requirement: ddd 模块卷 场景 1/场景 2 仓储实现段   ← AC-2/AC-4   <!-- 各场景整替换 -->
聚合根示例换 `PaymentId`；RepositoryImpl 示例含 toPersistenceId 覆写形。
#### Requirement: test 模块卷 构造示例   ← AC-6   <!-- 节内整替换 -->
`UUID.randomUUID()` 构造位改 `{Agg}Id.of(UUID.randomUUID())` 形态。
#### Requirement: 编码公约 §2.1 后缀表与 §2.2 结构映射   ← AC-7   <!-- 两节整替换（本卷为命名 canonical） -->
增行：`{Agg}Id` = 聚合身份终类型 record，落位 `domain/{agg}/id/`。
#### Requirement: test 模块卷 TR-1 编号纪律条（规则集治理节）   ← AC-1/AC-7   <!-- 折叠时整行替换 TR-1 规范句：「永不复用」改「废号可顶」 -->
TR-1 编号纪律句改为：编号永不重排；规则删除时编号作废、留一行作废记录，**废号准后续规则顶位**（顶位=新语义接管，旧作废账原位不改，作废账处补一行顶位承接注）。编号是教义锚点不是历史文物（裁定 Q8）。R15 现行语义 = 聚合根身份终类型化（BP-13），旧 R15（baomidou 全仓禁令）作废账保留。（源：`DddArchitectureRules` 类头编号纪律段现文（随本案更新）＋顶位注；作废账 → `docs/explanation/architecture-rules.md`「废止也是账」段顶位承接句）

### REMOVED
（无——裸 UUID 教义全部以改写承接，无整条废止。）

## §3 波及面与回退
- **代码**：common-ddd +1 文件（Identifier）+ javadoc 指针随动（Identifiable/AggregateIds 配对句、MybatisPersistence 钩子注）；common-test +R15 +探针组；sample main 两聚合套件与全部写 Handler/DomainService/DTO/Converter、测试随动（样本 119 测试 + archproof 全量）；**契约模块、PO、XML、db-migration、common-pg/security/cloud 零动**（AC-3/AC-4 取证面）。
- **文档与工具**：槽位换号波及按 P-8 白名单执行——skills 四件（new-aggregate 全篇重号+套件数+新件模板、scheduled-task/new-service 引用行、ddd-review 检查项挂 R15 并重号）、写链/scheduler 卷各 2 引用行、折叠前逐线复核存疑两件（docs/README、explanation/architecture-rules 长行内圈号语境）；另 glossary 术语行、docs 宽松件同题节（common-ddd api 篇、聚合蓝图设计卡）、theory-map 账本行（③⁺）、折叠日解读篇复写（⑨⁺）；AGENTS.md 九条速查不涉（未触其面）。**Q8 顶位波及**：glossary §DddArchitectureRules 行「编号作废不复用」措辞改顶位语义、解读篇 architecture-rules 旧 R15 作废段补一行顶位承接注、`ywf-ddd-common/AGENTS.md` 局部守则「R1-R14/C1 系；R15 已删」行随 R15 顶位改「R1-R15/C1 系」。
- **并行案卷协调（硬约束，P-8）**：代码施工即刻可行、与 `2026-09-pattern-taxonomy` 案零交集；本案折叠写回 current/ 排于该案折叠完成后（卷路径以其迁都结果为锚，delta 按卷逻辑名解析）；该案若撤，约束解除、按当时 current/ 布局落位。
- **执法**：C2 计数宣称改动齐（23）；C1 {agg} 模板实例化新句可扫描；C3 符号在册。
- **回退**：未折叠前 current/ 一字未动，撤本案=回退全部施工 commit + 删案卷目录即净；已折叠后要推翻=新案 supersede（旧案在位不改）。
