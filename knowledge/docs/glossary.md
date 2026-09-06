# 术语表

本项目特有术语的唯一登记处。本表定位 = **术语 → canonical 指针**：每行只给一句话身份定位，定义与论证在指针目标处单源维护，本表不复述（避免压缩副本与原文漂移）。业务词汇（文末）是领域通用语言本身，保留业务名。

| 术语（全称） | 一句话 | canonical |
|------|------|------|
| CO（Contract Object） | 经 Presenter 清洗后的对外安全视图，消费方唯一可见的数据结构 | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md)、[common-contract.md](reference/api/common-contract.md) |
| DTO（Data Transfer Object） | 应用层内部视图（可含 version / 审计），不出服务边界；写侧 `XxxDTO` / 读侧 `XxxViewDTO` 均实现 `ApplicationDTO` 标记（两代旧定义已合一于此） | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md)、.agents/rules/03 |
| CQE（Command / Query） | 请求对象统称，与 Handler 1:1 对应 | → 见 [common-contract.md §2](reference/api/common-contract.md) |
| Command | 「请做这件事」——写请求标记接口 | → 见 [common-contract.md §2](reference/api/common-contract.md) |
| Query | 「请给我这个」——读请求标记接口 | → 见 [common-contract.md §2](reference/api/common-contract.md) |
| PageableQuery | 分页查询契约（pageNum/pageSize + `safe*()` 钳制，record 零覆写） | → 见 [common-contract.md §2 PageableQuery API](reference/api/common-contract.md) |
| DomainEvent | 领域事件标记接口（`domain/event/`）——「领域已发生的事实」，仅进程内，框架无发布机制 | → 见 [common-ddd.md §2 领域建模基类](reference/api/common-ddd.md) |
| IntegrationEvent | 跨服务事件契约（common-contract `dto/event/`），出入站均为它，传输通道业务自持 | → 见 [common-contract.md §2](reference/api/common-contract.md) |
| DomainEventPublisher | 事件角色空标记：进程内发布领域事件的身份定型（无机制） | → 见 [common-ddd.md §2 事件角色标记](reference/api/common-ddd.md) |
| IntegrationEventPublisher | 事件角色空标记：领域事实翻译为集成事件并出站的身份定型（可靠性归业务） | → 见 [common-ddd.md §2 事件角色标记](reference/api/common-ddd.md) |
| DomainEventSubscriber | 事件角色空标记：进程内领域事件域内反应的身份定型 | → 见 [common-ddd.md §2 事件角色标记](reference/api/common-ddd.md) |
| IntegrationEventSubscriber | 事件角色空标记：外部集成事件入站消费的身份定型（driving adapter，幂等归业务） | → 见 [common-ddd.md §2 事件角色标记](reference/api/common-ddd.md) |
| Portal | Domain 层定义的外部资源访问接口（「我需要什么外部能力」） | → 见 [common-ddd.md §2 领域建模基类](reference/api/common-ddd.md) |
| Gateway | Infrastructure 层实现 Portal 的类（技术调用 + ACL 翻译，「怎么对接外部」） | → 见 [module-design/infrastructure.md §gateway/](explanation/infrastructure.md) |
| reconstitute | 从持久化数据重建聚合根的静态工厂（`Converter.toDomain()` 专用，不走业务构造器） | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md) |
| ACL（Anti-Corruption Layer） | 防腐层——Gateway 内把外部 SDK 模型翻译为领域语言，防外部概念污染 Domain | → 见 [module-design/infrastructure.md §gateway/](explanation/infrastructure.md) |
| Assembler | 应用层组件，Domain → DTO（由 Handler 调用） | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md) |
| Presenter | 应用层组件，DTO → CO 单向呈现（由 AppService 调用） | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md) |
| Handler | 用例执行单元（`CommandHandler` 写 / `QueryHandler` 读），与 CQE 1:1 | → 见 [common-ddd.md §2 CQRS Handler 接口](reference/api/common-ddd.md) |
| AppService（ApplicationService） | 聚合协调入口，一个聚合一个类，委托 Handler + Presenter，返回 CO | → 见 [common-ddd.md §2 CQRS Handler 接口](reference/api/common-ddd.md) |
| Controller | 契约接口 `XxxController`（contract 层）+ 实现 `XxxControllerImpl`（adapter 层纯透传） | → 见 [common-ddd.md §2](reference/api/common-ddd.md)、[module-design/adapter.md](explanation/adapter.md) |
| RestAdapter | REST 入口适配器空标记（`adapter/rest/controller/`），ArchUnit R8a/R8b 锚点 | → 见 [common-ddd.md §2](reference/api/common-ddd.md)、[common-test.md §2](reference/api/common-test.md) |
| ApplicationDTO | 应用层内部视图空标记（`application/dto/`），ArchUnit R10a/R10b 锚点，与 `CO` 对偶 | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md)、[common-test.md §2](reference/api/common-test.md) |
| Policy | 可插拔领域规则（Strategy 模式，`isApplicable` + 行为），无状态纯计算无副作用 | → 见 [common-ddd.md §2 领域建模基类](reference/api/common-ddd.md) |
| PageResult | 框架级分页容器 record（contract `dto/query`，与 PageableQuery 同居），`map()` 逐层转换 | → 见 [common-ddd.md §2 CQRS Handler 接口](reference/api/common-ddd.md) |
| BasicConverter | 基础设施层转换器接口（Domain ↔ PO，手写逐字段） | → 见 [common-ddd.md §2 对象转换](reference/api/common-ddd.md) |
| MybatisPersistence | 仓储支撑基类（`infrastructure/mybatis/persistence/`）：封装手写 XML 持久化 + validate 自动调用 + 审计显式填充 + 0 影响行三分通道（乐观锁冲突 / 实体消失 / 静默写丢失）；不声明 `@Transactional` | → 见 [common-ddd.md §2 仓储支撑](reference/api/common-ddd.md) |
| DddMapper | 通用七条语句契约接口（`infrastructure/mybatis/mapper/`），逻辑删除过滤与版本条件由 SQL 文本承担 | → 见 [common-ddd.md §2 DddMapper](reference/api/common-ddd.md) |
| AuditFieldFiller | 审计字段显式填充器（`infrastructure/mybatis/handler/`），基于 `MetaObject` 反射，由基类写库前显式调用 | → 见 [common-ddd.md §2 AuditFieldFiller](reference/api/common-ddd.md) |
| DomainService | 跨聚合协调的无状态领域服务标记接口 | → 见 [common-ddd.md §2 领域建模基类](reference/api/common-ddd.md) |
| Scheduler（ScheduledAdapter） | 定时任务入口（`@Scheduled`）实现 `ScheduledAdapter` 标记，透传 AppService | → 见 [cookbook/scheduled-task.md](how-to/scheduled-task.md)、[common-test.md §2 R14](reference/api/common-test.md) |
| opt-in | common 模块按需引入设计：依赖不强制传递，用到才声明 | → 见 [common-cloud.md §1 / §5](reference/api/common-cloud.md) |
| PgArrayType | common-pg 枚举：Java 数组类型 → PG 数组类型名映射 | → 见 [common-pg.md §2](reference/api/common-pg.md) |
| DddArchitectureRules | ArchUnit 预置规则常量类（R1–R14/C1 系；R15 已删除、编号作废） | → 见 [common-test.md §2](reference/api/common-test.md) |
| RFC 9457 | Problem Details for HTTP APIs（原 RFC 7807）：type/title/status/detail/instance + `application/problem+json` | → 见 [common-exception.md §2](reference/api/common-exception.md) |
| 枚举双份（contract / domain） | 同名枚举在 contract 与 domain 各存一份是**刻意的上下文隔离**（`contract/{agg}/enums/` 与 `domain/{agg}/model/`）：契约稳定与建模自由解耦，字段演进互不牵连，**禁止为「去重」合并共享**（canonical 即本行；sample 有真实双份可对照） | 本行 |

## 命名映射规范

本项目中目录、Maven 坐标、Java 包名的对应关系（历史原因存在风格差异，新建服务应遵循此映射）：

| 层面 | 规则 | 示例 |
|------|------|------|
| 目录名 | kebab-case | `sample-application/`、`common-ddd/` |
| groupId（common） | `com.yoursweakfoe` | `com.yoursweakfoe:common-ddd` |
| groupId（业务服务） | `com.yoursweakfoe.application` | `com.yoursweakfoe.application:sample-service` |
| Java 包名 | 全小写无分隔符 | `com.yoursweakfoe.sampleapplication.sampleservice` |
| artifactId | kebab-case | `sample-service-server`、`common-exception` |
| 服务名（Spring） | 纯小写 | `service`（`spring.application.name`） |

> **新建服务约定**：groupId 统一用 `com.yoursweakfoe.application`，Java 包名取目录名去连字符（`my-new-service` → `com.yoursweakfoe.mynewservice`）。

## 业务词汇（订单域通用语言）

状态机背后的业务语义。代码中的方法名即此处词汇的落地（Evans：Ubiquitous Language）。

| 词汇 | 代码落点 | 业务含义 | 前置条件 / 迁移 |
|------|---------|---------|----------------|
| 下单 place | `Order.place()` | 创建订单，初始 PENDING | 订单项非空、客户 ID 必填、总金额 > 0 |
| 支付 pay | `Order.pay()` | 买家完成付款，PENDING → PAID | 仅待支付订单可支付 |
| 确认 confirm | `Order.confirm()` | 商家审核通过已付款订单，PAID → CONFIRMED | 商家操作 |
| 发货 ship | `Order.ship(trackingNumber)` | 商家交付物流并登记单号，CONFIRMED → SHIPPED | 必填物流单号 |
| 签收 deliver | `Order.deliver()` | 买家确认收货，SHIPPED → DELIVERED | — |
| 完成 complete | `Order.complete()` | 订单闭环（终态），DELIVERED → COMPLETED | 终态不可再迁移 |
| 取消 cancel | `Order.cancel(reason)` | 关闭订单并记录原因（终态），触发库存回补补偿 | 仅 PENDING/PAID 可取消；已发货不可取消 |
| 扣减库存 deductStock | `Product.deductStock(quantity)` | 库存减少 | 数量为正且库存充足，否则拒绝下单 |
| 回补库存 restoreStock | `Product.restoreStock(quantity)` | 取消后归还占用量（由 `CancelOrderHandler` 同事务直调，补偿原子化） | 数量为正 |

> 状态迁移守卫集中在聚合根 `requireStatus(...)`（穷尽性 switch），新增枚举值时编译器强制处理。
