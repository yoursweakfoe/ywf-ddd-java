# 术语表

本项目中使用的特有术语及其精确定义。

| 术语 | 全称 | 含义 |
|------|------|------|
| CO | Contract Object | 契约输出对象。对内部 DTO 清洗后的外部安全视图，定义在 contract 模块，是消费方唯一可见的数据结构 |
| DTO | Data Transfer Object | 应用层内部视图。可含审计字段、version、内部评分等，不出服务边界 |
| CQE | Command / Query | 请求对象的统称。Command=写，Query=读，与 Handler 1:1 对应 |
| Command | — | 写操作请求对象，实现 `common-contract` 的 `Command` 标记接口 |
| Query | — | 读操作请求对象，实现 `common-contract` 的 `Query` 标记接口 |
| PageableQuery | — | 分页查询对象，实现 `PageableQuery` 接口（自带 pageNum/pageSize） |
| DomainEvent | — | 领域事件标记接口（common-ddd，`domain/event/`）。表达「领域已发生的事实」，仅进程内产生与消费，不跨服务序列化 |
| IntegrationEvent | — | 集成事件标记接口（common-contract，`dto/event/`）。跨服务事件契约，出入站均为它；业务实现类位于 `contract/{agg}/dto/event/`，传输通道业务自持 |
| DomainEventPublisher | — | application 层事件角色空标记（common-ddd，`application/event/publisher/`）：领域事件进程内发布的身份定型，框架无机制 |
| IntegrationEventPublisher | — | application 层事件角色空标记（common-ddd，`application/event/publisher/`）：领域事实翻译为集成事件并出站的身份定型，投递可靠性策略归业务 |
| DomainEventSubscriber | — | application 层事件角色空标记（common-ddd，`application/event/subscriber/`）：进程内领域事件域内反应的身份定型（Spring 事件监听路线） |
| IntegrationEventSubscriber | — | adapter 层事件角色空标记（common-ddd，`adapter/event/subscriber/`）：外部集成事件入站消费的身份定型，与 REST / 定时任务入口同构，消费端幂等归业务 |
| Portal | — | Domain 层定义的外部资源访问接口（如支付、存储）。语义："我需要什么外部能力" |
| Gateway | — | Infrastructure 层实现 Portal 的类。包含技术调用 + ACL 模型翻译。语义："怎么对接外部" |
| reconstitute | — | 从持久化数据重建完整领域对象的静态工厂方法。Converter.toDomain() 必须调用此方法（不走业务构造器） |
| ACL | Anti-Corruption Layer | 防腐层。Gateway 内将外部 SDK 模型翻译为领域模型，防止外部概念污染 Domain |
| Assembler | — | Application 层组件，Domain → DTO 转换。由 Handler 调用 |
| Presenter | — | Application 层组件，DTO → CO 转换。由 AppService 调用 |
| Handler | — | 用例执行单元。CommandHandler（写）或 QueryHandler（读），与 CQE 1:1 对应 |
| AppService | — | 聚合协调入口。一个聚合一个类，委托 Handler + Presenter，实现类位于服务侧 `application/{agg}/service/`；实现 common-ddd 的 `ApplicationService` 标记接口 |
| DTO | — | Application 层内部视图对象（`application/{agg}/dto/`），写侧/读侧均实现 `ApplicationDTO` 标记接口（common-ddd），与 contract 层 `CO` 标记对偶（内部可含 version/审计，对外经 Presenter 清洗） |
| Controller | — | Adapter 层 web 组件（@RestController），实现 contract 接口与 `RestAdapter` 标记接口（common-ddd），spring-web 注解声明 REST 路径，纯透传 AppService |
| RestAdapter | — | common-ddd 空标记接口，定型「REST 入口适配器」角色（Ports & Adapters 的 driving adapter），供 ArchUnit R8a/R8b 识别与约束 |
| ApplicationDTO | — | common-ddd 空标记接口（`application/dto/`），定型「应用层内部视图」角色（写侧 DTO + 读侧 DTO），供 ArchUnit R10a/R10b 识别；与 contract 层 `CO` 标记对偶 |
| Policy | — | 可插拔领域规则（Strategy 模式）。无状态、纯计算、无副作用 |
| PageResult | — | 框架级分页容器（record），定义在 contract 层（与 PageableQuery 同居），隔离底层分页形态（手写 XML 的 LIMIT/OFFSET + COUNT 双语句），提供 map() 支持逐层转换 |
| BasicConverter | — | Infrastructure 层转换器接口（Domain ↔ PO），手动实现（富领域模型需 reconstitute） |
| MybatisPersistence | — | common-ddd 提供的仓储支撑基类（`infrastructure/mybatis/persistence/`），组合持有 `DddMapper`，封装手写 XML 持久化 + 乐观锁冲突分类 + validate 自动调用 + 审计显式填充；不声明 `@Transactional`（事务边界上收至 Handler） |
| DddMapper | — | common-ddd 框架级通用 Mapper 接口（`infrastructure/mybatis/mapper/`），定义每聚合手写 XML 必须实现的 7 条语句契约（insert / updateById / selectById / selectByIds / deleteById / deleteByIds / existsById）；逻辑删除过滤与版本条件由 SQL 文本自身承担 |
| AuditFieldFiller | — | common-ddd 审计字段填充器（`infrastructure/mybatis/handler/`），基于 MyBatis 核心 `MetaObject` 按字段名反射；由 `MybatisPersistence` 在写库前**显式调用**：INSERT 填 createAt + updateAt（+ 可选 createdBy/updatedBy），UPDATE 刷新 updateAt |
| DomainService | — | Domain 层标记接口（common-ddd），跨聚合协调的无状态服务。实现类标注 @Service 由组件扫描注册 |
| Scheduler | — | Adapter 层组件，定时任务入口（@Scheduled），透传 AppService |
| opt-in | — | common 模块设计原则：业务服务按需引入，不强制全量依赖 |
| PgArrayType | — | common-pg 枚举，定义 Java 数组类型与 PG 数组类型名的映射（如 INTEGER → `integer[]`） |
| DDDArchitectureRules | — | common-test 中的 ArchUnit 规则常量类，提供 DDD 分层守护规则集 |
| RFC 9457 | Problem Details for HTTP APIs | HTTP 错误响应标准（原 RFC 7807），定义 type/title/status/detail/instance 字段 + `application/problem+json` 媒体类型 |
| 枚举双份（contract / domain） | — | 同名枚举在 contract 与 domain 各存一份是**刻意的上下文隔离**（如 `contract/order/enums/OrderStatus` 与 `domain/order/model/OrderStatus`）：对外契约的稳定性与对内建模的自由度解耦，两边字段演进互不牵连。禁止为「去重」而合并共享 |

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
