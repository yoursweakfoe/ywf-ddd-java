# 术语表

本表是全仓库特有术语的登记处，只做一件事：从术语找到权威出处。每行给一句话身份定位；完整定义和论证由指针目标单独维护，本表不复述，因为压缩副本比原文更容易腐烂。文末的业务词汇是示例系统的通用语言本身，保留业务名是它的职责。

## 框架角色与机制

| 术语（全称） | 一句话 | canonical |
|------|------|------|
| CO（Contract Object） | Presenter 清洗后对外返回的数据对象，服务边界外的调用方唯一可见的结构 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[common-contract.md](api/common-contract.md) |
| DTO（Data Transfer Object） | 应用层内部数据视图，可携带版本号、审计等字段，不出服务边界。写侧 `XxxDTO`、读侧 `XxxViewDTO`，都实现 `ApplicationDTO` 标记接口 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、`knowledge/specs/current/patterns/discipline/coding-conventions.md` |
| CQE（Command / Query） | Command 与 Query 两类请求对象的统称，每条与一个 Handler 1:1 对应 | → 见 [common-contract.md §2](api/common-contract.md) |
| Command | 写请求标记接口：声明"执行这个操作"，携带该操作的全部输入 | → 见 [common-contract.md §2](api/common-contract.md) |
| Query | 读请求标记接口：声明"给我这份数据"，携带查询条件 | → 见 [common-contract.md §2](api/common-contract.md) |
| PageableQuery | 分页查询基础契约，record 实现：带 `pageNum`/`pageSize` 两个参数，经 `safe*()` 方法钳制到安全值，实现类不覆写。无默认分页，缺参绑定为 0 即触发 400 | → 见 [common-contract.md §2](api/common-contract.md)、`specs/current/modules/contract.md` |
| BusinessException | 业务失败的统一异常载体，携带 messageKey 与 params。领域层业务规则 if-throw 的唯一出口，禁止另立具名领域异常类 | → 见 [exception 法卷 EV-1](../../specs/current/modules/exception.md)、[common-exception.md §2](api/common-exception.md) |
| i18n messageKey | 错误文案的位点键，格式 `{aggregate}:err.{scene}`。键交给前端渲染，服务端不维护 messages.properties 文案 | → 全仓 key 登记账本在 how-to/error-handling.md；条款 EV-2 |
| DomainEvent | 领域事件标记接口，住 `domain/event/`，声明"领域内已发生一个事实"。只在进程内有效，框架不提供发布机制 | → 见 [common-ddd.md §2 领域建模基类](api/common-ddd.md) |
| IntegrationEvent | 跨服务事件契约，住 common-contract 的 `dto/event/`。出站入站收发的都是它；传输通道由业务自备，框架不指定 | → 见 [common-contract.md §2](api/common-contract.md) |
| DomainEventPublisher | 事件角色空标记：定型"在进程内发布领域事件"这一身份，不含任何机制实现 | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| IntegrationEventPublisher | 事件角色空标记：定型"把领域事实翻译为集成事件并发出"这一身份，投递可靠性由业务自持 | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| DomainEventSubscriber | 事件角色空标记：定型"在域内响应进程内领域事件"这一身份 | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| IntegrationEventSubscriber | 事件角色空标记：定型"入站消费外部集成事件"这一身份；属 driving 侧适配器，幂等消费由业务自持 | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| 标记接口（marker） | 框架的约定体系：用空接口定型身份，再拿接口本身做 ArchUnit 的类型锚点。判断依据是类型，不是包名猜测 | → 见 [theory-map「标记接口定型体系」](../explanation/theory-map.md)、[common-test.md §2](api/common-test.md) |
| Portal | Domain 层定义的外部资源访问接口，表达"本域需要什么外部能力"，不关心怎么调用 | → 见 [external-gateway 法卷 GW-1](../../specs/current/patterns/boundary/external-gateway.md)、[common-ddd.md §2](api/common-ddd.md) |
| Gateway | Infrastructure 层对 Portal 的技术实现类：发起真实外部调用，并把外部模型翻译成领域语言 | → 见 [external-gateway 法卷 GW-2](../../specs/current/patterns/boundary/external-gateway.md)、[infrastructure.md](../explanation/infrastructure.md) |
| QueryRepository（读端口） | application 层读端口标记接口。读侧绕过聚合、直查存储投影，只准经这个端口，是 R13 的落点 | → 见 [read-chain 法卷 RC-1](../../specs/current/patterns/chain/read-chain.md) |
| reconstitute | 从持久化数据重建聚合根的静态工厂方法。只被 `Converter.toDomain()` 调用；重建不是新建，不走业务构造器 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[blueprint 法卷 BP-8](../../specs/current/patterns/building-block/aggregate-blueprint.md) |
| ACL（Anti-Corruption Layer） | 防腐层：在 Gateway 内部把外部 SDK 的模型翻译成自己的领域语言，防止外部形状渗入领域模型 | → 见 [external-gateway 法卷](../../specs/current/patterns/boundary/external-gateway.md)、[infrastructure.md](../explanation/infrastructure.md) |
| Assembler | 应用层转换组件：把 Domain 转成 DTO，由 Handler 调用 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[write-chain 法卷 WC-5](../../specs/current/patterns/chain/write-chain.md) |
| Presenter | 应用层呈现组件：把 DTO 单向转成 CO，由 AppService 调用。与 Assembler 强制分离，转换和呈现不混在一个类 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[write-chain 法卷 WC-5](../../specs/current/patterns/chain/write-chain.md) |
| Handler | 一个用例的执行单元，分 `CommandHandler` 写、`QueryHandler` 读两种，与 CQE 1:1 对应 | → 见 [common-ddd.md §2 CQRS Handler 接口](api/common-ddd.md) |
| AppService（ApplicationService） | 一个聚合的应用层协调入口，一个聚合一个类：委托 Handler 执行用例，交 Presenter 转输出，返回 CO | → 见 [common-ddd.md §2 CQRS Handler 接口](api/common-ddd.md) |
| Controller | REST 接口的一对名字：contract 层定义 `XxxController` 接口，adapter 层实现 `XxxControllerImpl`，实现纯透传不含逻辑 | → 见 [common-ddd.md §2](api/common-ddd.md)、[adapter.md](../explanation/adapter.md) |
| RestAdapter | REST 入口适配器空标记，住 `adapter/rest/controller/`；ArchUnit R8a/R8b 拿它做分层锚点 | → 见 [common-ddd.md §2](api/common-ddd.md)、[common-test.md §2](api/common-test.md) |
| ScheduledAdapter | 定时任务入口空标记，住 `adapter/task/scheduler/`；R14a/R14b 的锚点 | → 见 [scheduler 法卷 SC-1](../../specs/current/patterns/chain/scheduler.md) |
| ApplicationDTO | 应用层内部视图空标记，住 `application/dto/`；ArchUnit R10a/R10b 锚点。与 CO 对偶：内部视图是它，对外视图是 CO | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[common-test.md §2](api/common-test.md) |
| Policy | 可插拔的领域规则：Strategy 模式，`isApplicable` 加行为方法成对出现；无状态、纯计算、无副作用 | → 见 [domain-policy 法卷](../../specs/current/patterns/building-block/domain-policy.md)、[common-ddd.md §2](api/common-ddd.md) |
| PageResult | 框架级分页容器 record，住 contract 的 `dto/query`，与 PageableQuery 同居；`map()` 把行逐条转成另一种分页容器 | → 见 [common-ddd.md §2](api/common-ddd.md) |
| BasicConverter | 基础设施层转换器接口：Domain ↔ PO 双向转换，手写逐字段，不用反射或代码生成 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md) |
| MybatisPersistence | 仓储支撑基类：手写 XML 持久化调用、自动触发 validate、审计字段显式填充、0 影响行按三分通道处理。自己不声明 `@Transactional` | → 见 [common-ddd.md §2 仓储支撑](api/common-ddd.md)、[optimistic-lock 法卷 OL-1](../../specs/current/patterns/collaboration/optimistic-lock.md) |
| DddMapper | 七条通用语句的 MyBatis 契约接口，住 `infrastructure/mybatis/mapper/`。逻辑删除过滤与版本条件由 SQL 文本承担，Java 侧不拼 SQL | → 见 [common-ddd.md §2 DddMapper](api/common-ddd.md)、[blueprint 法卷 BP-X1](../../specs/current/patterns/building-block/aggregate-blueprint.md) |
| AuditFieldFiller | 审计字段填充器，住 `infrastructure/mybatis/handler/`；仓储基类写库前显式调用它填创建、更新审计字段 | → 见 [common-ddd.md §2 AuditFieldFiller](api/common-ddd.md) |
| DomainService | 跨聚合协调的无状态领域服务标记接口 | → 见 [cross-aggregate 法卷 CA-1](../../specs/current/patterns/collaboration/cross-aggregate.md) |
| opt-in | common 模块的按需引入设计：依赖不强制传递，需要该能力的项目自己显式声明 | → 见 [common-cloud.md §1 / §5](api/common-cloud.md) |
| PgArrayType | common-pg 的枚举：维护 Java 数组类型到 PG 数组类型名的映射 | → 见 [common-pg.md §2](api/common-pg.md) |
| DddArchitectureRules | ArchUnit 预置规则常量类：装 R1–R14/C1 系规则；R15 已删除，编号作废不复用 | → 见 [common-test.md §2](api/common-test.md)、[architecture-rules.md](../explanation/architecture-rules.md) |
| RFC 9457 | HTTP API 错误响应标准 Problem Details：定义 type/title/status/detail/instance 五字段与 `application/problem+json` 媒体类型，取代旧标准 RFC 7807 | → 见 [common-exception.md §2](api/common-exception.md) |
| 枚举双份（contract / domain） | 同名枚举在 contract 与 domain 各存一份、各用各的，这是刻意的上下文隔离：契约形状求稳定，领域建模求自由，两边独立演化。**禁止为「去重」合并共享**。本行即该词的权威定义；sample 有真实双份可对照 | 本行 |

## 文档体系词汇（知识伞自身的法）

| 术语 | 一句话 | canonical |
|------|------|------|
| 知识伞 | 根级 `knowledge/` 目录，全仓文档的统一住所。两类知识各有法律区，执法工具链同伞收纳；伞自身不立法，裁决记在案卷 §裁决记录 | → [knowledge/README.md](../../README.md) |
| 诸区分野（三态） | 分区原则：一个目录只受一种规则管。地图区描述代码，契约区承诺行为，方法区约束干活手法，另有执法工具链。裁决快照不住独立区，住案卷 specify §裁决记录 | → `knowledge/specs/current/patterns/meta/attribution-law.md` §1 |
| 判据一句话 | 内容分区的唯一问句：「这句话能机械化执行吗？能→法卷；不能→docs」 | → [knowledge/README §2](../../README.md)、`specs/README` 宽严双份 |
| 宽严双份 | 同一规则写两份，权威只有一处：法卷是严格件，机器遵循它；docs 是宽松件，人读，禁写条款编号与精确参数表。冲突时法卷赢，改 docs | → `specs/README` §宽严双份 |
| 法卷 | `specs/current/{modules,patterns}/` 下法律文件的统称。严格件容器：每卷住条款、取证源、规范代码形状、生效登记 | → `specs/README` |
| 设计卡 | how-to 架的文档形态，宽松件：只回答"该不该用、怎么选"，不写代码形状，形状归法卷 | → [how-to/README](../how-to/README.md) |
| 镜像区 | 业务行为法的住所 `sample-application/specs/`：与框架知识区同构三分区；业务名只在这棵树里合法 | → 该区 README |
| 现行本 / 审议中 / 已归档 | 法的三种身份，一个目录一种：草案住 `changes/`，随便改；现行本住 `current/`，只在折叠生效那一刻写入；封存册住 `archive/`，入封后只进不改 | → `specs/README` 目录表 |
| 四件套 | 一份法案的四份文书：specify 定验收，plan 定技术裁量与修卷 delta，tasks 拆施工清单，implement 记执行账。模板在 `changes/_template/` | → [changes/README](../../specs/changes/README.md) |
| 归档折叠 | 修卷 delta 合入 `current/`、案卷整袋移进 `archive/` 的那个时刻，全套文档同步义务的唯一时点 | → `specs/README` 守则 2 |
| 生效登记 | 法卷末节的固定格式：逐条登记条款是 ✅ 已生效还是 ⛔ 未落地。未生效必须写明，不允许装死 | → 各法卷 §生效登记（如 [batch-write §3](../../specs/current/patterns/chain/batch-write.md)） |
| 业务不入伞 | 辖域规则：伞只装框架与脚手架层通识；业务契约与业务词不入伞，住镜像区 | → [knowledge/README](../../README.md)、`sample-application/specs/README` |
| 真实例指针位 | 教学文档引用 sample 真实类的唯一合法形式：写在代码块外，同行标注「真实例」 | → `knowledge/specs/current/patterns/meta/attribution-law.md` §3 |
| 虚构教例 / 教例家族 | 中立教学替身：Payment、Reservation、Invoice 等虚构名，用于需要聚合名但不涉及真实业务的场合。首现必须标「虚构教例，sample 未实现」 | → `knowledge/specs/current/patterns/meta/attribution-law.md` §3 |
| `{agg}` / `{Agg}` | 聚合名占位符，教学形状里代表任意聚合。C1 用真实聚合名逐个代入，核验实例化后的路径存在 | → [doc-guards C1](doc-guards.md) |
| 幽灵路径 | 文档写了但源码里不存在的目录路径，C1 专治此病 | → [doc-guards](doc-guards.md) |
| 七校验 | check-docs 跑的七道机器检查 C1–C7。退出码等于 FAIL 数，非零即返工 | → [doc-guards](doc-guards.md) |
| 登记法 | 新增、迁移、改名文档，只在 docs/README 一处登记，其余载体只放指针 | → [docs/README](../README.md) |
| 理论账本 | theory-map 的论证台账：记录外部理论被采纳或被拒绝的理由。它是通往法律的地图，本身不是法律 | → [theory-map.md](../explanation/theory-map.md) |
| §裁决记录（案卷） | 案卷级决策事件的唯一居所，记三样：门次收据、裁定与当时理由、supersede 宣告，Confirmation 收据必填。裁一条落一条，落笔不改；随案卷封入 archive，到期整册归零，册灭但论证留下。论证现行版不住这里：⑨⁺ 复写进同题解读篇，篇脚回指本节 | → [changes/README](../../specs/changes/README.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 一次设计 | 定名复合事件：修改与决策齐备，才算一次设计。修改是对现实动手，决策是对价值拍板。它的论证沉淀出口是解读架 | → [knowledge/README §2.1](../../README.md)（驱动关系图边定义） |
| 开发者＝项目主 | 立法流程的发起方与裁决方是同一个人，即项目主。AI 只起草案卷、执行施工；两道批准门硬停，等人拍板。代码只是被驱动的事实，不立案、不施工 | → [knowledge/README §2.1](../../README.md) 导语 |
| 立法十步（①–⑩） | 全流程正序定名：①立案起草 → ②③Specify 门回合 → ④裁定入稿 → ⑤⑥Plan 门回合 → ⑦按约施工 → ⑧取证回填 → ⑨折叠落实 → ⑩归档。两门回合各一箭双号；⁺ 上标 = 挂靠在该时刻的分支义务 | → [knowledge/README §2.1 锚表](../../README.md)（各步法源 = new-bill 步骤号） |
| 决策快照回指 | 解读篇页脚的固定格式：`→ 案卷 <date-slug> §裁决记录（决策快照）`。作用是论证可回溯到决策现场，读者不必翻案卷。仅活册期有效：清册抹案时整行删除，册灭论证留 | → [归属法 §4「案卷折叠时（⑨⁺）」行](../../specs/current/patterns/meta/attribution-law.md) |
| 案卷先行 | 新裁定前必查旧案：②③ 门回合拍板前，翻 `specs/archive/` 封存案卷的 §裁决记录与解读架，确认不与旧案冲突。撞案就在新案 §裁决记录宣告 supersede，旧案一字不改 | → [knowledge/README §2.1 行②③](../../README.md)、归属法 §2「裁决与当时思考」行 |
| 论证沉淀 | 立法流程的两个分支义务，用上标 ⁺ 记号：③⁺ 在 theory-map 账本落一行，与 §裁决记录同案；⑨⁺ 在折叠时刻把沉淀论证强制复写进同题解读篇，篇脚回指案卷 | → [归属法 §1/§4](../../specs/current/patterns/meta/attribution-law.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 卷宗清册制 | 案卷有到期之日：两侧 `specs/archive/` 整册归零。本体整袋删除，README 账行同裁，活面案名标识符清干净。C6 两臂执法：冻结臂防在位涂改，清册臂防废号残留与删而未尽 | → [归属法 §1「卷宗清册制」注](../../specs/current/patterns/meta/attribution-law.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 自足判据 | 清册的先验门槛：每案四栏因果，立因、取舍、被拒方案及拒因、生效边界，必须已完整住进解读架或 current/ 条文。检验标准一句话：没见过卷宗的读者能从解读架复述该案全部因果，过了才许抹 | → 归属法 §4「卷宗清册执行」行 |
| 压缩宪法 / 就近宪法 | 根 AGENTS.md 九条是法卷的压缩复述，每次交互必读；各模块目录的 AGENTS.md 对该目录就近生效，nearest-wins | → `AGENTS.md` 路由 |
| 摆架（摊／摊卡／牌面） | 模式法卷的关切分架：十个摊目录承载卷籍（路径即户口，不另设名册）；摊卡五段（验证问句／管什么／界线／摊路径／空穴）具法条正文身份，牌面=摊头三行告示零条款 | → `knowledge/specs/current/patterns/meta/pattern-taxonomy.md` meta-1/2/4/5 |
| 空穴登记 | 无卷之摊以牌面＋登记行存在；每行洞必须带证据指针（现行法原句/架构图原句/裁决记录），零证据不得占坑，首卷落地同案销穴 | → 同上 §3、meta-4 |
| 原号随身 | 条款搬家制度：正文迁卷、编号不换、出卷留墓碑互指；新铸编号=摊目录名-序号，两字母旧前缀为存量永不再增 | → 同上 meta-6、§4 搬家账 |

## 命名映射规范

目录、Maven、包名的结构映射属于可机械化的用法规范，已迁出本表：权威在 [coding-conventions 法卷 §2](../../specs/current/patterns/discipline/coding-conventions.md) 的 CC-2/CC-3，本表不复述，一词一身。

本表只留一条非机械的映射，属领域词库层的升格：订单域 Java 类名 `Order` 对应库层表 `sales_order`。原因分两头：schema 层直接用该名会撞 SQL 保留字，所以库层升格为行业惯用全称；Java 类名不随迁，保持领域语言。两头的理在 [infrastructure.md](../explanation/infrastructure.md) 的「单数之理」「类名不随迁之理」两节。

## 业务词汇（订单域通用语言）

下表是状态机词汇的语义翻译，取 Evans 的 Ubiquitous Language 之义：代码方法名就是业务语言。迁移规则、前置条件、终态守卫以业务法卷为准，本表不复载，防止与法卷漂移：状态机条款见 [order 现行册](../../../sample-application/specs/current/order.md)。

| 词汇 | 代码落点 | 业务语义（一句话） |
|------|---------|------------------|
| 下单 place | `Order.place()` | 创建订单 |
| 支付 pay | `Order.pay()` | 买家完成付款 |
| 确认 confirm | `Order.confirm()` | 商家审核通过已付款订单 |
| 发货 ship | `Order.ship(trackingNumber)` | 商家交付物流并登记单号 |
| 签收 deliver | `Order.deliver()` | 买家确认收货 |
| 完成 complete | `Order.complete()` | 订单流程走完，进入终态 |
| 取消 cancel | `Order.cancel(reason)` | 关闭订单并记录原因，同时触发库存回补补偿 |
| 扣减库存 deductStock | `Product.deductStock(quantity)` | 扣减商品可用库存 |
| 回补库存 restoreStock | `Product.restoreStock(quantity)` | 取消后归还此前占用的库存，作为独立补偿动作 |

> 状态迁移守卫集中在聚合根的 `requireStatus(...)`，用穷尽 switch 校验。这是形状条款，法源在 blueprint 卷与 order 现行册，本表不重载。
