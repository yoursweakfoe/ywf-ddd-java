# 用法规范法卷：编码公约（框架法 · 严格件）

> **身份**：本卷是横切编码规范（命名 / 结构映射 / Lombok / 时间 / 安全上下文取用）的唯一权威；违反本卷=修代码，修卷走 `../../changes/`。前身 `.agents/rules/03` 升格入典（法案 `2026-10-rules-codification`——规范出工作台、入法典）。docs 侧复述均降为指针。
> **机器对账**：C1/C3/C4 扫本卷。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| CC-1 | CQE 与 Handler 1:1：`XxxCommand`→`CommandHandler`、`XxxQuery`→`QueryHandler`，分住 `application/{agg}/handler/{command,query}/` | AGENTS 九条 2；[write-chain WC-2](write-chain.md) | ArchUnit R 系 |
| CC-2 | 类型命名后缀制（下表）；一名一后缀一角色，禁止自创第六种载体 | 本卷 §2.1 命名表 | 评审项 |
| CC-3 | 结构映射：目录 kebab-case、groupId `com.yoursweakfoe(.application)`、Java 包名=目录去连字符、artifactId kebab-case、Spring 服务名纯小写 | 本卷 §2.2（迁自术语表，法律位在此）；新服务实例见 `new-service` skill | 评审项 |
| CC-4 | Repository 泛型：写端口 `XxxRepository extends Repository<Domain, ID>`（`domain/{agg}/repository/`）；实现继承 `MybatisPersistence`；Mapper `extends DddMapper<XxxPO>` 标 `@Mapper`；Converter 实现 `BasicConverter`，`toDomain()` 走 `reconstitute()` | `MybatisPersistence`/`DddMapper` javadoc；[blueprint BP-X2](aggregate-blueprint.md) | C3 |
| CC-5 | 时间律：持久化与领域时间一律 `OffsetDateTime`；当前时间经注入 `Clock`，禁无参 `now()`；「同一瞬时」比较用 `isEqual()`/`timeLineOrder()`（`equals` 含偏移） | ADR-0006 判例；AGENTS 九条 8 | 测试可注入验证 |
| CC-6 | Lombok 矩阵：Domain 禁 `@Data` 仅字段级 `@Getter`（保不变量、ID 判等）；PO 必 `@Data` 零 ORM 注解；DTO / CQE / CO 必 `@Data @NoArgsConstructor @AllArgsConstructor`（契约字段带 Javadoc） | AGENTS 九条 9；[blueprint BP-X2](aggregate-blueprint.md)；理由账本 → [theory-map](../../../docs/explanation/theory-map.md) | 评审项 |
| CC-7 | contract 模块内容白名单：仅 Controller 契约接口 + CQE + CO + 枚举；仅依赖 common-contract 标记接口；东西向复用同一契约（一期 RestClient 静态直连，Feign 经 common-cloud opt-in） | ADR-0010 重契约判例；[contract 卷](../modules/contract.md) | ArchUnit |
| CC-8 | 安全上下文取用层：`SecurityUtil` 只许 Application/Adapter 层；Controller 优先 `@AuthenticationPrincipal` 注入已验签 JWT；**Domain 层禁止**感知认证上下文；角色判断用 `@PreAuthorize`（角色 claim 名经配置键） | `common-security` javadoc/配置；[security 卷](../modules/security.md) | 评审项 |
| CC-9 | 分页契约细则：`PageableQuery` 实现于 Query record；页码自 **1**；`pageNum`/`pageSize` 须显式传入（缺参→0→`@Min(1)`→400，无默认注入）；上限 `MAX_PAGE_SIZE`；`safe*()` 为执行侧第二道防线；`@Valid` 标于契约接口方法参数 | contract 模块卷 + `PageableQuery` javadoc；binding 案卷 `archive/2026-09-pagequery-default-claim` | 400 三通道测试 |

## §2 命名与映射表（本卷为 canonical，他处只准指针）

### 2.1 类型后缀（虚构教例示范，Payment 家族）

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

## §3 互指索引（这些规则已有正身法卷，本卷不复述）

写侧四拍链与依赖方向 → [write-chain](write-chain.md)；读侧投影 → [read-chain](read-chain.md)；跨聚合协作 → [cross-aggregate](cross-aggregate.md)；策略 → [domain-policy](domain-policy.md)；外部集成 → [external-gateway](external-gateway.md)；异常与错误码 → [exception 卷 EV 系](../modules/exception.md)；Domain Service / Policy / Portal 的**禁止面** → [prohibitions](prohibitions.md)。

## §4 生效登记

CC-1~9 ✅（sample 现行两聚合即活体实证；真实例映射位见互指卷内标注）。
