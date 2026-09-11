# 用法规范法卷：编码公约（框架法 · 严格件）

> **身份**：本卷是横切编码规范的唯一权威，在册管三类事：CQE-Handler 对应制、类型命名与后缀、Lombok 与时间分工。结构映射（CC-3）、契约白名单（CC-7）、安全取用（CC-8）、分页契约（CC-9）四宗条文已原号随身迁出（案卷 2026-09-pattern-taxonomy 搬家账），本卷留墓碑互指。违反本卷就改代码；要修改本卷，必须走 `../../../changes/` 立案。docs 一侧的复述已全部降为指针。
> **机器对账**：C1/C3/C4 扫本卷。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| CC-1 | CQE 与 Handler 保持 1:1：`XxxCommand` 对应 `CommandHandler`，`XxxQuery` 对应 `QueryHandler`。两者分住 `application/{agg}/handler/{command,query}/`。 | AGENTS 九条 2；[write-chain WC-2](../chain/write-chain.md) | ArchUnit R 系 |
| CC-2 | 类型命名走后缀制，见 §2.1。一个名字只用一个后缀、承担一个角色；禁止自创第六种载体。 | 本卷 §2.1 命名表 | 评审项 |
| CC-3 | 本条正身已迁入 [../meta/layering.md](../meta/layering.md)，仍号 CC-3（原号随身）；本行墓碑互指，§2.2 表身随迁该卷 §2 | 案卷 2026-09-pattern-taxonomy 搬家账 1 | layering.md CC-3 |
| CC-4 | Repository 泛型：写端口 `XxxRepository extends Repository<Domain, ID>`，住 `domain/{agg}/repository/`。实现类继承 `MybatisPersistence`。Mapper `extends DddMapper<XxxPO>` 并标 `@Mapper`。Converter 实现 `BasicConverter`，`toDomain()` 走 `reconstitute()`。 | `MybatisPersistence`/`DddMapper` javadoc；[blueprint BP-X2](../building-block/aggregate-blueprint.md) | C3 |
| CC-5 | 时间类型：持久化与领域层的时间一律 `OffsetDateTime`。当前时间必须经注入的 `Clock` 获取，禁止无参 `now()`。比较「同一瞬时」用 `isEqual()` 或 `timeLineOrder()`，因为 `equals` 会连同偏移一起比较。 | AGENTS 九条 8 | 测试可注入验证 |
| CC-6 | Lombok 分工：Domain 层禁 `@Data`，只用字段级 `@Getter`，以保住不变量与 ID 判等。PO 必须 `@Data` 且零 ORM 注解。DTO、CQE、CO 必须 `@Data @NoArgsConstructor @AllArgsConstructor`，契约字段带 Javadoc。 | AGENTS 九条 9；[blueprint BP-X2](../building-block/aggregate-blueprint.md)；理由见 [theory-map](../../../../docs/explanation/theory-map.md) | 评审项 |
| CC-7 | 本条正身已迁入 [../building-block/aggregate-blueprint.md](../building-block/aggregate-blueprint.md)（表末行），仍号 CC-7（原号随身）；本行墓碑互指 | 案卷 2026-09-pattern-taxonomy 搬家账 2 | blueprint CC-7 行 |
| CC-8 | 本条正身已迁入 [../security/security-chain.md](../security/security-chain.md)，仍号 CC-8（原号随身）；本行墓碑互指 | 案卷 2026-09-pattern-taxonomy 搬家账 4 | security-chain.md CC-8 |
| CC-9 | 本条正身已迁入 [../chain/read-chain.md](../chain/read-chain.md)（§1 表末行），仍号 CC-9（原号随身）；本行墓碑互指 | 案卷 2026-09-pattern-taxonomy 搬家账 2 | read-chain CC-9 行 |

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

表身已随 CC-3 正身迁出 → [../meta/layering.md](../meta/layering.md) §2（原号随身，案卷 2026-09-pattern-taxonomy 搬家账 1）。本节留指针。

## §3 互指索引

以下规则各有权威法卷，本卷不复述，只给指针。写侧四拍链与依赖方向 → [write-chain](../chain/write-chain.md)；读侧投影 → [read-chain](../chain/read-chain.md)；跨聚合协作 → [cross-aggregate](../collaboration/cross-aggregate.md)；策略 → [domain-policy](../building-block/domain-policy.md)；外部集成 → [external-gateway](../boundary/external-gateway.md)；异常与错误码 → [exception 卷 EV 系](../../modules/exception.md)；Domain Service / Policy / Portal 的**禁止事项** → [prohibitions](prohibitions.md)。

## §4 生效登记

CC-1/2/5/6 ✅ 在册生效，实证就是 sample 现行两个聚合；CC-3/7/8/9 原号随身迁出（墓碑互指四行，案卷 2026-09-pattern-taxonomy 搬家账）。真实例的具体位置见各互指法卷内标注。
