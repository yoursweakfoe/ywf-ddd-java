# 03 — 编码规范

## CQRS 模式

- 三类请求对象：`Command`（写）、`Query`（读）、`IntegrationEvent`（跨服务事件契约）
- 每个 CQE 对应一个 Handler（1:1），实现 `CommandHandler<C, R>` 或 `QueryHandler<Q, R>`
- Handler 位于 `application/{aggregate}/handler/`

## 写侧固定模式

```
CommandHandler.handle(command):
  1. load：repository.findById(id) → 聚合根
  2. 行为：aggregate.doSomething(command)（业务规则在聚合根内）
  3. save：repository.update(aggregate)
  4. 转换：assembler.toDTO(aggregate) → 返回 DTO
```

- Handler 标注 `@Transactional(rollbackFor = Exception.class)`
- 同事务跨聚合写入是**已定档决策**（强一致 fail-fast，理由与边界见 `docs/references.md`）：Handler 允许在同一事务内协调多个聚合的持久化，不拆事件补偿
- Handler 返回 **DTO**（不是 CO）

## 读侧固定模式

```
QueryHandler.handle(query):
  1. orderQueryRepository.findPage(...) → PO 直接投影读 DTO（绕过 domain）
  2. 返回 DTO 或 PageResult<DTO>
```

- 读侧**完全绕过 domain**：读端口（application 层 `XxxQueryRepository`）+ infra 实现直接 PO → 读 DTO 投影，不 reconstitute 聚合根、不建领域读模型
- 可省略 `@Transactional`（只读）

## AppService 模式

```java
// 有返回值
public OrderCO payOrder(PayOrderCommand command) {
    return orderPresenter.present(payOrderHandler.handle(command));
}
// 无返回值
public void cancelOrder(CancelOrderCommand command) {
    cancelOrderHandler.handle(command);
}
```

- 一个聚合一个 AppService（`@Service`）
- AppService 返回 **CO**（通过 Presenter）

## DTO / CO 强制分离

| | DTO（内部视图） | CO（契约输出） |
|--|--|--|
| 归属 | application 层内部 | contract 模块 |
| 生产者 | Handler（通过 Assembler） | AppService（通过 Presenter） |
| 可包含 | 审计字段、version、内部评分 | 仅消费方需要的字段 |

- Assembler：Domain → DTO（Handler 调用）
- Presenter：DTO → CO（AppService 调用）

## 异常策略

- 统一使用 `BusinessException(messageKey, params)`
- 错误码格式：`"{aggregate}:err.{场景}"`（如 `"order:err.invalidTransition"`）
- Domain 层通过显式 `if + throw new BusinessException(key)` 抛出
- **禁止**定义具名领域异常（如 `InsufficientStockException`）
- Domain 层**不设** `exception/` 包

## 领域事件（全链路 Outbox 可靠性规范）

- 聚合根内 `registerEvent(new XxxEvent(...))`（暂存）
- Repository 持久化成功后经 `DomainEventFlusher` 冲刷（先清后捕），**强制**经
  `DomainEventOutboxStore` 与业务同事务入箱 `ddd_domain_event_outbox`（提交 ⇒ 落库，
  跨崩溃不丢）；聚合注册了事件但无 `DomainEventOutboxStore` Bean 时 fail-fast 抛错
  回滚业务写入——要么不用事件，要么带上 Outbox，**不存在直发降级路径**
- 入箱后由框架排空器 `OutboxRelay`（领域实例）在自有事务内认领 → 派发 → 标记完成
  （at-least-once，失败退避重投，超限转死信）
- DomainEvent 不可变（所有字段 final）
- 域内反应监听器（DomainEventListener）位于 `application/{agg}/event/listener/`：
  投递发生在**排空器事务内**——一律 `@EventListener`；带数据库写入的副作用用普通
  `@Transactional`（REQUIRED，加入排空事务，「内部反应 + 集成入箱 + 标记完成」原子提交）；
  **禁用 `REQUIRES_NEW` 与 `@Async`**（撕碎原子性，重试产生双份副作用）；
  监听器不做非事务副作用（HTTP / 直发 MQ），对外通知一律经集成 Outbox 捕获；
  薄编排：接事件 → 加载聚合 → 委托 DomainService / Publisher；逻辑按 `eventId` 幂等
- 集成事件（跨服务）：Publisher（`application/{agg}/event/publisher/`）翻译为 contract
  中的 IntegrationEvent，经 `IntegrationEventOutboxStore` 同事务捕获入
  `ddd_integration_event_outbox`（行 id = 未来 MQ messageId，`source_event_id` = 源领域
  事件 eventId）；**Publisher 不投 MQ**——投递由框架集成排空器经 `IntegrationEventSender`
  完成

## 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 写请求 | `XxxCommand` | `PlaceOrderCommand` |
| 读请求 | `XxxQuery` / `XxxPageQuery` | `GetOrderQuery` |
| 契约输出 | `XxxCO` | `OrderCO` |
| 内部视图 | `XxxDTO` | `OrderDTO` |
| 持久化对象 | `XxxPO` | `OrderPO` |
| 领域事件 | `XxxEvent` | `OrderPlacedEvent` |
| 集成事件 | `XxxIntegrationEvent` | `OrderPlacedIntegrationEvent` |
| 域内反应监听器 | `XxxDomainEventListener` | `OrderDomainEventListener` |
| Domain 外部接口 | `XxxPortal` | `PaymentPortal` |
| Infra 外部实现 | `XxxGateway` | `AlipayPaymentGateway` |
| 聚合根 | `Xxx extends AggregateRoot<ID>` | `Order` |
| Repository 接口 | `XxxRepository` | `OrderRepository` |
| Repository 实现 | `XxxRepositoryImpl` | `OrderRepositoryImpl` |

## Repository 泛型

- 接口：`Repository<Domain, ID>`（domain 层）
- 实现：继承 `MybatisPlusPersistence<Mapper, PO, Domain, ID>`（infrastructure 层），
  构造器注入 `Mapper` + `ObjectProvider<DomainEventOutboxStore>` + Converter；
  领域 ID 与 PO 主键类型不一致时覆写 `toPersistenceId(ID)`
- Converter：实现 `BasicConverter<Domain, PO>`，`toDomain()` 使用 `reconstitute()`

## Domain Service（跨聚合协调）

- 位置：`domain/shared/service/{Xxx}DomainService.java`
- 实现 `DomainService` 标记接口，标注 `@Service` 由 Spring 组件扫描自动注册（Spring 是生态基座，标注注解即标准做法；领域层允许 stereotype 注解，见 A2 规则）
- 职责：协调多个聚合的操作（不归属于任何单一聚合）
- 可调用 Repository，可修改实体状态
- 示例：`InventoryDomainService`（下单时跨 Product 聚合扣库存）

## Policy（领域策略）

- 位置：`domain/{agg}/policy/`
- 实现 `Policy<C>` 接口（`isApplicable(C context)`）
- 纯计算/决策，**无状态、无副作用、不修改任何对象**
- 由 Domain Service 收集结果后操作实体
- 三种组合：互斥型（@Order）、叠加型、精准路由型（Map）

## Portal / Gateway（外部资源访问）

- Domain 层接口：`domain/{agg}/portal/{Xxx}Portal.java`（继承 `Portal` 标记接口）
- Infrastructure 层实现：`infrastructure/gateway/{Xxx}Gateway.java`
- Gateway 职责：技术调用 + ACL 模型翻译 + 容错（超时/降级/重试）
- 禁止将外部 SDK 类型泄漏到 Domain（必须翻译为领域语言）

## 时间类型约定

- 框架统一使用 `OffsetDateTime`（带时区偏移，跨地域无歧义）
- PO 中 `createAt` / `updateAt` 声明为 `OffsetDateTime`
- `BasicAutoFillHandler` 自动填充（INSERT 填 createAt + updateAt，UPDATE 填 updateAt）
- Domain 层时间字段与 PO 保持一致（Converter 直接透传）

## 分页查询

- 分页 Query 实现 `PageableQuery` 接口（继承自 `Query`）
- 页码从 **1** 开始（与 MyBatis-Plus Page 一致）
- 默认每页 20，上限 1000（`MAX_PAGE_SIZE`）
- 契约层通过 `@Min(1)` / `@Max(MAX_PAGE_SIZE)` 声明约束，Handler 层 `@Validated` 触发校验
- 读侧查询实现（`XxxQueryRepositoryImpl`）内置防御性 clamp（pageNum≥1，1≤pageSize≤1000），未校验参数不会产生非法分页
- 读侧 Repository（application 层 `XxxQueryRepository`）返回 `PageResult<读 DTO>`（PO → DTO 直接投影）
- Handler 返回 `PageResult<DTO>`，AppService 返回 `PageResult<CO>`

## Adapter（REST）

- Adapter 层 web 入口为 `@RestController`（实现 contract 接口），REST 路径通过 spring-web 注解标注（`@RequestMapping` / `@PostMapping` / `@GetMapping`），位于 Controller 上
- 东西向服务间调用复用同一契约接口，消费方经 Feign 调用提供方 REST 端点（JWT 由 common-cloud 的 RequestInterceptor 自动透传，下游自验签）
- web 入口纯透传 AppService，禁止业务判断、禁止修改 Command/Query 内容

## SecurityUtil 使用层归属

- `SecurityUtil.getJwt()` / `getClaim()` / `getString()` / `getStringList()` 仅允许在 **Application 层**（Handler）和 **Adapter 层** 调用；身份字段不预定义，按名字自取（公司 JWT 字段命名 / 数量无规范）
- Controller 层优先使用 `@AuthenticationPrincipal Jwt` 注入已验签的 JWT
- **Domain 层禁止**调用 SecurityUtil（领域模型不感知认证上下文）
- 角色/权限判断优先使用 `@PreAuthorize("hasRole('xxx')")` 方法级注解（角色 claim 名经 `ywf.security.roles-claim` 配置）

## 虚拟线程

- 项目已启用 JDK 21 虚拟线程（`spring.threads.virtual.enabled: true`）
- Tomcat 请求处理、Spring 异步任务、定时任务均运行在虚拟线程上
- **禁止引入 `synchronized` 块**（会导致虚拟线程 pinning，载体线程被钉住）
- 需要互斥时使用 `java.util.concurrent.locks.ReentrantLock` 替代
- ThreadLocal 在虚拟线程下正常工作；身份上下文的 ThreadLocal 由 Spring Security 链内 `SecurityContextHolderFilter` 统一管理，业务代码无需手工清理

## Lombok 使用约定

- Domain 层（聚合根/实体/值对象）：**禁止** `@Data`，仅用字段级 `@Getter`（禁止 setter，保护不变量）
- Domain Events：**禁止** `@Data`，仅用 `@Getter`（事件不可变）
- Infrastructure 层 PO：**必须** `@Data`（MyBatis-Plus 反射需要 setter）
- Application 层 DTO：**必须** `@Data @NoArgsConstructor @AllArgsConstructor`
- Contract 层 CQE/CO：**必须** `@Data @NoArgsConstructor @AllArgsConstructor`，字段级 Javadoc 注释

> `@Data` 禁止用于 Domain 层的原因：
> 1. `equals()/hashCode()` 基于所有字段 —— 聚合根相等性必须基于 ID
> 2. setter 破坏领域不变量（绕过状态机 / 业务校验）

→ 详见 `docs/application/cookbook/`
