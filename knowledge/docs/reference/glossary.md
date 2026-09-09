# 术语表

本项目特有术语的唯一登记处。本表定位 = **术语 → canonical 指针**：每行只给一句话身份定位，定义与论证在指针目标处单源维护，本表不复述（避免压缩副本与原文漂移）。业务词汇（文末）是领域通用语言本身，保留业务名。

## 框架角色与机制

| 术语（全称） | 一句话 | canonical |
|------|------|------|
| CO（Contract Object） | 经 Presenter 清洗后的对外安全视图，消费方唯一可见的数据结构 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[common-contract.md](api/common-contract.md) |
| DTO（Data Transfer Object） | 应用层内部视图（可含 version / 审计），不出服务边界；写侧 `XxxDTO` / 读侧 `XxxViewDTO` 均实现 `ApplicationDTO` 标记 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、`knowledge/specs/current/patterns/coding-conventions.md` |
| CQE（Command / Query） | 请求对象统称，与 Handler 1:1 对应 | → 见 [common-contract.md §2](api/common-contract.md) |
| Command | 「请做这件事」——写请求标记接口 | → 见 [common-contract.md §2](api/common-contract.md) |
| Query | 「请给我这个」——读请求标记接口 | → 见 [common-contract.md §2](api/common-contract.md) |
| PageableQuery | 分页查询契约（pageNum/pageSize + `safe*()` 钳制，record 零覆写；缺参绑定 0 → 400，无默认分页值） | → 见 [common-contract.md §2](api/common-contract.md)、`specs/current/modules/contract.md` |
| BusinessException | 业务失败统一载体（messageKey + params），领域 if-throw 的唯一出口；禁止具名领域异常 | → 见 [exception 法卷 EV-1](../../specs/current/modules/exception.md)、[common-exception.md §2](api/common-exception.md) |
| i18n messageKey | 错误码位点 `{aggregate}:err.{scene}`——前端渲染键，服务端不维护 messages.properties | → 全仓 key 登记账本在 how-to/error-handling.md；条款 EV-2 |
| DomainEvent | 领域事件标记接口（`domain/event/`）——「领域已发生的事实」，仅进程内，框架无发布机制 | → 见 [common-ddd.md §2 领域建模基类](api/common-ddd.md) |
| IntegrationEvent | 跨服务事件契约（common-contract `dto/event/`），出入站均为它，传输通道业务自持 | → 见 [common-contract.md §2](api/common-contract.md) |
| DomainEventPublisher | 事件角色空标记：进程内发布领域事件的身份定型（无机制） | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| IntegrationEventPublisher | 事件角色空标记：领域事实翻译为集成事件并出站的身份定型（可靠性归业务） | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| DomainEventSubscriber | 事件角色空标记：进程内领域事件域内反应的身份定型 | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| IntegrationEventSubscriber | 事件角色空标记：外部集成事件入站消费的身份定型（driving adapter，幂等归业务） | → 见 [common-ddd.md §2 事件角色标记](api/common-ddd.md) |
| 标记接口（marker） | 空接口定型角色 + ArchUnit 类型锚点（非包名猜测）的框架约定体系 | → 见 [theory-map「标记接口定型体系」](../explanation/theory-map.md)、[common-test.md §2](api/common-test.md) |
| Portal | Domain 层定义的外部资源访问接口（「我需要什么外部能力」） | → 见 [external-gateway 法卷 GW-1](../../specs/current/patterns/external-gateway.md)、[common-ddd.md §2](api/common-ddd.md) |
| Gateway | Infrastructure 层实现 Portal 的类（技术调用 + ACL 翻译） | → 见 [external-gateway 法卷 GW-2](../../specs/current/patterns/external-gateway.md)、[infrastructure.md](../explanation/infrastructure.md) |
| QueryRepository（读端口） | application 层读端口标记接口：读侧绕过聚合的唯一合法路径（R13） | → 见 [read-chain 法卷 RC-1](../../specs/current/patterns/read-chain.md) |
| reconstitute | 从持久化数据重建聚合根的静态工厂（`Converter.toDomain()` 专用，不走业务构造器） | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[blueprint 法卷 BP-8](../../specs/current/patterns/aggregate-blueprint.md) |
| ACL（Anti-Corruption Layer） | 防腐层——Gateway 内把外部 SDK 模型翻译为领域语言 | → 见 [external-gateway 法卷](../../specs/current/patterns/external-gateway.md)、[infrastructure.md](../explanation/infrastructure.md) |
| Assembler | 应用层组件，Domain → DTO（由 Handler 调用） | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[write-chain 法卷 WC-5](../../specs/current/patterns/write-chain.md) |
| Presenter | 应用层组件，DTO → CO 单向呈现（由 AppService 调用） | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[write-chain 法卷 WC-5](../../specs/current/patterns/write-chain.md) |
| Handler | 用例执行单元（`CommandHandler` 写 / `QueryHandler` 读），与 CQE 1:1 | → 见 [common-ddd.md §2 CQRS Handler 接口](api/common-ddd.md) |
| AppService（ApplicationService） | 聚合协调入口，一个聚合一个类，委托 Handler + Presenter，返回 CO | → 见 [common-ddd.md §2 CQRS Handler 接口](api/common-ddd.md) |
| Controller | 契约接口 `XxxController`（contract 层）+ 实现 `XxxControllerImpl`（adapter 层纯透传） | → 见 [common-ddd.md §2](api/common-ddd.md)、[adapter.md](../explanation/adapter.md) |
| RestAdapter | REST 入口适配器空标记（`adapter/rest/controller/`），ArchUnit R8a/R8b 锚点 | → 见 [common-ddd.md §2](api/common-ddd.md)、[common-test.md §2](api/common-test.md) |
| ScheduledAdapter | 定时任务入口空标记（`adapter/task/scheduler/`），R14a/R14b 锚点 | → 见 [scheduler 法卷 SC-1](../../specs/current/patterns/scheduler.md) |
| ApplicationDTO | 应用层内部视图空标记（`application/dto/`），ArchUnit R10a/R10b 锚点，与 `CO` 对偶 | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md)、[common-test.md §2](api/common-test.md) |
| Policy | 可插拔领域规则（Strategy 模式，`isApplicable` + 行为），无状态纯计算无副作用 | → 见 [domain-policy 法卷](../../specs/current/patterns/domain-policy.md)、[common-ddd.md §2](api/common-ddd.md) |
| PageResult | 框架级分页容器 record（contract `dto/query`，与 PageableQuery 同居），`map()` 逐层转换 | → 见 [common-ddd.md §2](api/common-ddd.md) |
| BasicConverter | 基础设施层转换器接口（Domain ↔ PO，手写逐字段） | → 见 [common-ddd.md §2 对象转换](api/common-ddd.md) |
| MybatisPersistence | 仓储支撑基类：手写 XML 持久化 + validate 自动调用 + 审计显式填充 + 0 影响行三分通道；不声明 `@Transactional` | → 见 [common-ddd.md §2 仓储支撑](api/common-ddd.md)、[optimistic-lock 法卷 OL-1](../../specs/current/patterns/optimistic-lock.md) |
| DddMapper | 通用七条语句契约接口（`infrastructure/mybatis/mapper/`），逻辑删除过滤与版本条件由 SQL 文本承担 | → 见 [common-ddd.md §2 DddMapper](api/common-ddd.md)、[blueprint 法卷 BP-X1](../../specs/current/patterns/aggregate-blueprint.md) |
| AuditFieldFiller | 审计字段显式填充器（`infrastructure/mybatis/handler/`），由基类写库前显式调用 | → 见 [common-ddd.md §2 AuditFieldFiller](api/common-ddd.md) |
| DomainService | 跨聚合协调的无状态领域服务标记接口 | → 见 [cross-aggregate 法卷 CA-1](../../specs/current/patterns/cross-aggregate.md) |
| opt-in | common 模块按需引入设计：依赖不强制传递，用到才声明 | → 见 [common-cloud.md §1 / §5](api/common-cloud.md) |
| PgArrayType | common-pg 枚举：Java 数组类型 → PG 数组类型名映射 | → 见 [common-pg.md §2](api/common-pg.md) |
| DddArchitectureRules | ArchUnit 预置规则常量类（R1–R14/C1 系；R15 已删除、编号作废） | → 见 [common-test.md §2](api/common-test.md) |
| RFC 9457 | Problem Details for HTTP APIs（原 RFC 7807）：type/title/status/detail/instance + `application/problem+json` | → 见 [common-exception.md §2](api/common-exception.md) |
| 枚举双份（contract / domain） | 同名枚举在 contract 与 domain 各存一份是**刻意的上下文隔离**（`contract/{agg}/enums/` 与 `domain/{agg}/model/`）：契约稳定与建模自由解耦，**禁止为「去重」合并共享**（canonical 即本行；sample 有真实双份可对照） | 本行 |

## 文档体系词汇（知识伞自身的法）

| 术语 | 一句话 | canonical |
|------|------|------|
| 知识伞 | 根级 `knowledge/`：三类知识三个法律区 + 执法工具链同伞收纳，伞自身不立法 | → [knowledge/README.md](../../README.md) |
| 诸区分野（四态） | 一区一法律：地图 / 契约（法律）/ 判例卷宗 / 方法（+ 执法工具链） | → `knowledge/specs/current/patterns/attribution-law.md` §1 |
| 判据一句话 | 「这句话能机械化执行吗？能→法卷；不能→docs」——内容归属的唯一问句 | → [knowledge/README §2](../../README.md)、`specs/README` 宽严双份 |
| 宽严双份 | 法卷=严格件（唯一权威），docs=宽松件（语感与指针，禁条款编号与精确参数表）；冲突法卷赢 | → `specs/README` §宽严双份 |
| 法卷 | `specs/current/{modules,patterns}/`——条款 + 取证源 + 规范形状 + 生效登记的严格件容器 | → `specs/README` |
| 设计卡 | how-to 架形态：只答「该不该用、怎么选」，零形状代码 | → [how-to/README](../how-to/README.md) |
| 镜像区 | 业务行为法住 `sample-application/specs/`（同构三分区）；业务名只在本树合法 | → 该区 README |
| 现行本 / 审议中 / 已归档 | 法的三层身份：`current/` / `changes/` / `archive/`，一名一身份 | → `specs/README` 目录表 |
| 三件套 | 一份法案 = proposal → spec-delta → tasks（模板 `changes/_template/`） | → [changes/README](../../specs/changes/README.md) |
| 归档折叠 | delta 合入 current、案卷进 archive 的那一刻——文档同步义务唯一时点 | → `specs/README` 守则 2 |
| 生效登记 | 法卷末节：条款 ✅ 生效 / ⛔ 未落地在册声明（未生效不装死） | → 各法卷 §生效登记（如 [batch-write §3](../../specs/current/patterns/batch-write.md)） |
| 业务不入伞 | 契约天然记生意：业务法与业务词不入知识伞（D4 教义第二兑现） | → [knowledge/README](../../README.md)、`sample-application/specs/README` |
| 真实例指针位 | 教学文档引用 sample 真实类的唯一形式：代码块外 + 「真实例」标注同行 | → `knowledge/specs/current/patterns/attribution-law.md` §3 |
| 虚构教例 / 教例家族 | 中立教学位：Payment / Reservation / Invoice 等虚构系，首现必标「虚构教例，sample 未实现」 | → `knowledge/specs/current/patterns/attribution-law.md` §3 |
| `{agg}` / `{Agg}` | 聚合名占位符，C1 对真实聚合逐个实例化对源码核验 | → [doc-guards C1](doc-guards.md) |
| 幽灵路径 | 文档写了源码不存在的目录层级——C1 治的头号病 | → [doc-guards](doc-guards.md) |
| 七校验 | check-docs 机器闸（C1–C7），退出码=FAIL 数，非零即返工 | → [doc-guards](doc-guards.md) |
| 登记法 | 新增/迁移文档仅登记 docs/README 一处，其余载体只放指针 | → [docs/README](../README.md) |
| 理论账本 | 模式采纳/未采纳的论证台账（通往法律的地图，本身不是法律） | → [theory-map.md](../explanation/theory-map.md) |
| ADR（判例） | 决策事件件：**历元编号**（全局唯一限于当期；清册改元自 0001 重启）、append-only、推翻=新立案+supersede；载事件、当时思考快照与 Confirmation 授权——**论证现行版不住这里**（⑦⁺ 复写解读架，篇脚回指快照，ADR-0036）；本体到期随册彻底抹除（ADR-0037 清册制） | → [decisions/README](../../decisions/README.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 一次设计 | 定名复合事件：**修改（对现实动手）+ 决策（对价值拍板）齐备才算一次设计**；设计的论证沉淀出口=解读架 | → [knowledge/README §2.1](../../README.md)（驱动关系图边定义） |
| 开发者＝项目主 | 立法流程的发起方与裁决方同人（AI 只递案卷，批准门硬停等拍板）；代码不立案、不施工，只是被驱动的事实 | → [knowledge/README §2.1](../../README.md) 导语 |
| 立法八步（①–⑧） | 流程正序定名：立案起草→送审问决策→拍板沉淀→裁定落稿→按约施工→结果回填→折叠落实→归档 | → [knowledge/README §2.1 锚表](../../README.md)（各步法源= new-bill 步骤号） |
| 决策快照回指 | 解读篇页脚强制 `→ ADR-NNN（决策快照）`——流可溯源、读者不必考古（2026-09 `adr-argument-sedimentation`）；**仅活册期有效**，首期清册时整行删除（行灭理存，ADR-0037） | → [归属法 §4「案卷折叠时（⑦⁺）」行](../../specs/current/patterns/attribution-law.md) |
| 判例先行 | ③ 拍板前必翻 decisions/ 旧案确认无撞案；撞案=新立 ADR + supersede，旧案正文一字不动 | → [knowledge/README §2.1 行③](../../README.md)、[decisions/README](../../decisions/README.md) |
| 论证沉淀 | 判例→解读架的两个分支义务（上标 ⁺ 记号）：③⁺ theory-map 账本行（与 ADR 同案）；⑦⁺ 沉淀论证（现行版）**强制复写**入同题解读篇、篇脚回指 `ADR-NNN（决策快照）`（ADR-0036 起：卷宗只留事件与当时思考，论证现行版住解读架） | → [归属法 §1/§4](../../specs/current/patterns/attribution-law.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 卷宗清册制 | `decisions/` 与两侧 archive 到期**彻底抹除**：本体整袋删 + 账行同裁 + 活面标识符净空；C6 两臂执法（冻结臂守在位、清册臂守归零自洽） | → [归属法 §1「卷宗清册制」注](../../specs/current/patterns/attribution-law.md)、[knowledge-system.md](../explanation/knowledge-system.md) |
| 清册即改元 | 一次清册划一个历元：两册空壳交付、ADR 编号自 0001 重启、跨世同号法不救济；仪式产物（清册 bill 自身）不豁免于下期仪式 | → 归属法 §1 同注 |
| 自足判据 | 清册先验门槛：每案四栏因果（立因/取舍/被拒方案及拒因/生效边界）须全文住进解读架或 current/ 条文——**没见过卷宗的读者能从解读架复述全部因果，才许抹** | → 归属法 §4「卷宗清册执行」行 |
| 压缩宪法 / 就近宪法 | 根 AGENTS.md 九条每次必读；模块目录 AGENTS.md nearest-wins | → `AGENTS.md` 路由 |

## 命名映射规范

已迁出：目录/Maven/包名的结构映射属可机械化的用法规范，canonical 归 [coding-conventions 法卷 §2](../../specs/current/patterns/coding-conventions.md)（CC-2/CC-3）。本表不复述（归属法：一词一身）。

领域词库层升格映射（UB 术语位，非机械形状故记于此）：订单域 Java 类名 `Order` ↔ 库层表 `sales_order`——schema 名撞 SQL 保留字，库层升格行业惯用全称、Java 类名不随迁；两理分见 [infrastructure.md](../explanation/infrastructure.md)「单数之理」「类名不随迁之理」（2026-09 案 Q⑤ 定案，本映射句系当年承诺「文档一句话写映射」之补记兑现）。

## 业务词汇（订单域通用语言）

状态机词汇的**语义翻译**（Evans：Ubiquitous Language——代码方法名即业务语言）；**迁移规则、前置条件与终态守卫的现行法是业务法卷**，本表不复载（防与法卷漂移）：状态机条款见 [order 现行册](../../../sample-application/specs/current/order.md)。

| 词汇 | 代码落点 | 业务语义（一句话） |
|------|---------|------------------|
| 下单 place | `Order.place()` | 创建订单 |
| 支付 pay | `Order.pay()` | 买家完成付款 |
| 确认 confirm | `Order.confirm()` | 商家审核通过已付款订单 |
| 发货 ship | `Order.ship(trackingNumber)` | 商家交付物流并登记单号 |
| 签收 deliver | `Order.deliver()` | 买家确认收货 |
| 完成 complete | `Order.complete()` | 订单闭环终态 |
| 取消 cancel | `Order.cancel(reason)` | 关闭订单并记录原因，触发库存回补补偿 |
| 扣减库存 deductStock | `Product.deductStock(quantity)` | 库存减少 |
| 回补库存 restoreStock | `Product.restoreStock(quantity)` | 取消后归还占用量（补偿原子化） |

> 状态迁移守卫集中在聚合根 `requireStatus(...)`（穷尽性 switch）——这是形状条款，法源见 blueprint 卷与 order 现行册。
