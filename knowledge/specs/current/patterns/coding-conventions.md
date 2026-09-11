# 用法规范法卷：编码公约（框架法 · 严格件）

> **身份**：本卷是横切编码规范的唯一权威，管五类事：命名、结构映射、Lombok、时间类型、安全上下文取用。违反本卷就改代码；要修改本卷，必须走 `../../changes/` 立案。docs 一侧的复述已全部降为指针。
> **机器对账**：C1/C3/C4 扫本卷。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| CC-1 | CQE 与 Handler 保持 1:1：`XxxCommand` 对应 `CommandHandler`，`XxxQuery` 对应 `QueryHandler`。两者分住 `application/{agg}/handler/{command,query}/`。 | AGENTS 九条 2；[write-chain WC-2](write-chain.md) | ArchUnit R 系 |
| CC-2 | 类型命名走后缀制，见 §2.1。一个名字只用一个后缀、承担一个角色；禁止自创第六种载体。 | 本卷 §2.1 命名表 | 评审项 |
| CC-3 | 结构映射五条：目录名 kebab-case；groupId 限于 `com.yoursweakfoe(.application)`；Java 包名 = 目录名去连字符；artifactId kebab-case；Spring 服务名纯小写。 | 本卷 §2.2；新服务实例见 `new-service` skill | 评审项 |
| CC-4 | Repository 泛型：写端口 `XxxRepository extends Repository<Domain, ID>`，住 `domain/{agg}/repository/`。实现类继承 `MybatisPersistence`。Mapper `extends DddMapper<XxxPO>` 并标 `@Mapper`。Converter 实现 `BasicConverter`，`toDomain()` 走 `reconstitute()`。 | `MybatisPersistence`/`DddMapper` javadoc；[blueprint BP-X2](aggregate-blueprint.md) | C3 |
| CC-5 | 时间类型：持久化与领域层的时间一律 `OffsetDateTime`。当前时间必须经注入的 `Clock` 获取，禁止无参 `now()`。比较「同一瞬时」用 `isEqual()` 或 `timeLineOrder()`，因为 `equals` 会连同偏移一起比较。 | AGENTS 九条 8 | 测试可注入验证 |
| CC-6 | Lombok 分工：Domain 层禁 `@Data`，只用字段级 `@Getter`，以保住不变量与 ID 判等。PO 必须 `@Data` 且零 ORM 注解。DTO、CQE、CO 必须 `@Data @NoArgsConstructor @AllArgsConstructor`，契约字段带 Javadoc。 | AGENTS 九条 9；[blueprint BP-X2](aggregate-blueprint.md)；理由见 [theory-map](../../../docs/explanation/theory-map.md) | 评审项 |
| CC-7 | contract 模块内容有白名单：只放 Controller 契约接口、CQE、CO、枚举；只依赖 common-contract 的标记接口。东西向调用复用同一契约接口：一期走 RestClient 静态直连，Feign 为选项，需引入 common-cloud 才启用。 | [contract 卷](../modules/contract.md) | ArchUnit |
| CC-8 | 安全上下文按层取用：`SecurityUtil` 只允许在 Application 和 Adapter 层使用；Controller 优先用 `@AuthenticationPrincipal` 注入已验签 JWT；**Domain 层禁止**感知认证上下文；角色判断用 `@PreAuthorize`，角色 claim 的名称经配置键指定。 | `common-security` javadoc/配置；[security 卷](../modules/security.md) | 评审项 |
| CC-9 | 分页契约：`PageableQuery` 由 Query record 实现；页码从 **1** 起。`pageNum` 和 `pageSize` 必须显式传入，不设默认注入：缺参会落成 0，被 `@Min(1)` 拦下返回 400。页大小上限为 `MAX_PAGE_SIZE`；`safe*()` 是执行侧的第二道防线。`@Valid` 标在契约接口的方法参数上。 | contract 模块卷 + `PageableQuery` javadoc | 400 三通道测试 |

## §2 命名与映射表

本节是 canonical 版本，其他文档只准指针引用。

### 2.1 类型后缀

示例用虚构教例 Payment 家族。

| 类型 | 命名 | 示例 |
|------|------|------|
| 写请求 | `XxxCommand` | `PayCommand` |
| 读请求 | `XxxQuery` / `XxxPageQuery` | `GetPaymentQuery` |
| 契约输出 | `XxxCO` | `PaymentCO` |
| 内部视图 | `XxxDTO` | `PaymentDTO` |
| 持久化对象 | `XxxPO` | `PaymentPO` |
| Domain 外部接口 | `XxxPortal` | `PaymentPortal` |
| Infra 外部实现 | `XxxGateway` | `StripePaymentGateway` |
| 聚合根 | `Xxx extends AggregateRoot<ID>` | `Payment` |
| Repository 接口 | `XxxRepository` | `PaymentRepository` |
| Repository 实现 | `XxxRepositoryImpl` | `PaymentRepositoryImpl` |
| 读端口 / 读实现 | `XxxQueryRepository`(Impl) | `PaymentQueryRepositoryImpl` |

### 2.2 结构映射

| 层面 | 规则 | 示例 |
|------|------|------|
| 目录名 | kebab-case | `sample-application/`、`common-ddd/` |
| groupId（common） | `com.yoursweakfoe` | `com.yoursweakfoe:common-ddd` |
| groupId（业务服务） | `com.yoursweakfoe.application` | `com.yoursweakfoe.application:sample-service` |
| Java 包名 | 全小写无分隔符 | `com.yoursweakfoe.sampleapplication.sampleservice` |
| artifactId | kebab-case | `sample-service-server`、`common-exception` |
| 服务名（Spring） | 纯小写 | `service`（`spring.application.name`） |

## §3 互指索引

以下规则各有权威法卷，本卷不复述，只给指针。写侧四拍链与依赖方向 → [write-chain](write-chain.md)；读侧投影 → [read-chain](read-chain.md)；跨聚合协作 → [cross-aggregate](cross-aggregate.md)；策略 → [domain-policy](domain-policy.md)；外部集成 → [external-gateway](external-gateway.md)；异常与错误码 → [exception 卷 EV 系](../modules/exception.md)；Domain Service / Policy / Portal 的**禁止事项** → [prohibitions](prohibitions.md)。

## §4 生效登记

CC-1~9 全部 ✅。实证就是 sample 现行两个聚合；真实例的具体位置见各互指法卷内标注。
