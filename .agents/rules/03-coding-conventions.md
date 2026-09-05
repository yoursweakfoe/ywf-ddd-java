# 03 — 编码规范

## CQRS 模式

- 两类请求对象：`Command`（写）、`Query`（读）
- 每个 CQE 对应一个 Handler（1:1），实现 `CommandHandler<C, R>` 或 `QueryHandler<Q, R>`
- Handler 位于 `application/{aggregate}/handler/command/`（写）/ `handler/query/`（读）

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

## 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 写请求 | `XxxCommand` | `PlaceOrderCommand` |
| 读请求 | `XxxQuery` / `XxxPageQuery` | `GetOrderQuery` |
| 契约输出 | `XxxCO` | `OrderCO` |
| 内部视图 | `XxxDTO` | `OrderDTO` |
| 持久化对象 | `XxxPO` | `OrderPO` |
| Domain 外部接口 | `XxxPortal` | `PaymentPortal` |
| Infra 外部实现 | `XxxGateway` | `AlipayPaymentGateway` |
| 聚合根 | `Xxx extends AggregateRoot<ID>` | `Order` |
| Repository 接口 | `XxxRepository` | `OrderRepository` |
| Repository 实现 | `XxxRepositoryImpl` | `OrderRepositoryImpl` |

## Repository 泛型

- 接口 `Repository<Domain, ID>`（`domain/{agg}/repository/domain/`）；实现继承 `MybatisPersistence<Mapper, PO, Domain, ID>`（`infrastructure/persistence/**/repository/domain/`）
- Mapper `XxxMapper extends DddMapper<XxxPO>`（标注 `@Mapper`）；PO 纯 `@Data` POJO 零 ORM 注解；Converter 实现 `BasicConverter<Domain, PO>`，`toDomain()` 使用 `reconstitute()`
- → 构造器注入清单、`toPersistenceId` 覆写、七语句契约见 `docs/common/common-ddd.md` §2 与 `docs/application/cookbook/new-aggregate.md`

## Domain Service（跨聚合协调）

- 位置 `domain/shared/service/`，实现 `DomainService` 标记接口并标注 `@Service`（A2 stereotype 白名单例外）
- 协调多个聚合的操作（不归属任何单一聚合），可调用 Repository、可修改实体状态
- → 边界论证与示例（`InventoryDomainService`）见 `docs/application/module-design/domain.md`

## Policy（领域策略）

- 位置 `domain/{agg}/policy/`，实现 `Policy<C>`（`isApplicable(C context)`）
- 纯计算/决策：**无状态、无副作用、不修改任何对象**；由 Domain Service 收集结果后操作实体
- → 三种组合形态（互斥型/叠加型/精准路由型）见 `docs/application/cookbook/policy-pattern.md`

## Portal / Gateway（外部资源访问）

- Domain 接口 `domain/{agg}/portal/{Xxx}Portal.java`（继承 `Portal` 标记接口）；infra 实现 `infrastructure/gateway/{Xxx}Gateway.java`
- Gateway 职责：技术调用 + ACL 模型翻译 + 容错（超时/降级/重试）；禁止将外部 SDK 类型泄漏到 Domain
- → 完整流程与模板见 `docs/application/cookbook/gateway.md`

## 时间类型约定

- 框架统一 `OffsetDateTime`；当前时间一律经注入的 `Clock` Bean 获取，禁止无参 `OffsetDateTime.now()`
- 「同一瞬时」比较用 `isEqual()` / `timeLineOrder()`（`equals` 要求偏移亦相等）
- → 类型对照、`AuditFieldFiller` 审计填充与选型论证见 `docs/common/common-ddd.md` §2 / §ADR-0006

## 分页查询

- 分页 Query 实现 `PageableQuery`；页码从 **1** 开始，默认每页 20，上限 1000（`MAX_PAGE_SIZE`）
- `@Valid` 标注于契约接口方法参数触发校验；分页约束 `@Min(1)` / `@Max(MAX_PAGE_SIZE)` 声明于契约 Query
- 读端口（application 层 `XxxQueryRepository`）返回 `PageResult<读 DTO>`（PO → DTO 直接投影，实现内置防御性 clamp）；Handler 返回 `PageResult<DTO>`，AppService 返回 `PageResult<CO>`
- → `LIMIT / OFFSET` 换算与实现细节见 `docs/common/common-contract.md` 与 `docs/application/cookbook/read-path.md`

## Adapter（REST）

- web 入口 `@RestController`（实现 contract Controller 契约接口）；REST 路径/文档/校验注解声明于契约接口（重契约，单一事实源），ControllerImpl 继承承载
- 东西向一期为 RestClient 直连（静态地址），复用同一契约接口；Feign 经 common-cloud opt-in（JWT 由 RequestInterceptor 自动透传，下游自验签）
- web 入口纯透传 AppService，禁止业务判断、禁止修改 Command/Query 内容
- → 细则见 `docs/application/module-design/adapter.md`

## SecurityUtil 使用层归属

- `SecurityUtil.getJwt()` / `getClaim()` 等仅允许在 **Application 层（Handler）** 和 **Adapter 层** 调用；身份字段不预定义，按名字自取（公司 JWT 字段命名 / 数量无规范）
- Controller 层优先使用 `@AuthenticationPrincipal Jwt` 注入已验签的 JWT；**Domain 层禁止**调用 SecurityUtil（领域模型不感知认证上下文）
- 角色/权限判断优先使用 `@PreAuthorize("hasRole('xxx')")` 方法级注解（角色 claim 名经 `ywf.security.roles-claim` 配置）
- → API 清单与配置细则见 `docs/common/common-security.md`

## 虚拟线程

- 项目已启用 JDK 21 虚拟线程（`spring.threads.virtual.enabled: true`）
- Tomcat 请求处理、Spring 异步任务、定时任务均运行在虚拟线程上
- **禁止引入 `synchronized` 块**（会导致虚拟线程 pinning，载体线程被钉住）
- 需要互斥时使用 `java.util.concurrent.locks.ReentrantLock` 替代
- ThreadLocal 在虚拟线程下正常工作；身份上下文的 ThreadLocal 由 Spring Security 链内 `SecurityContextHolderFilter` 统一管理，业务代码无需手工清理

## Lombok 使用约定

- Domain 层（聚合根/实体/值对象）：**禁止** `@Data`，仅用字段级 `@Getter`（禁止 setter，保护不变量）
- Infrastructure 层 PO：**必须** `@Data`（MyBatis 结果映射需要 setter）、零 ORM 注解（表名 / 版本条件 / 逻辑删除过滤等语义全部由手写 XML 承担）
- Application 层 DTO：**必须** `@Data @NoArgsConstructor @AllArgsConstructor`
- Contract 层 CQE/CO：**必须** `@Data @NoArgsConstructor @AllArgsConstructor`，字段级 Javadoc 注释

> `@Data` 禁止用于 Domain 层的原因：
> 1. `equals()/hashCode()` 基于所有字段 —— 聚合根相等性必须基于 ID
> 2. setter 破坏领域不变量（绕过状态机 / 业务校验）

→ 详见 `docs/application/cookbook/`
