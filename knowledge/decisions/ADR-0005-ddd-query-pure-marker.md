# ADR-0005 CQRS 契约：Query 纯标记

**Status**: Accepted（2026-08 修订）
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0005（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

Handler 接口的契约设计：Query 是否携带返回类型泛型。Query 定义在 contract（对外），Handler 在 application（内部）。

## Decision Drivers

- contract 与 internal 类型的耦合风险
- 返回类型信息的归属（标记接口 vs 方法签名）

## Considered Options

- 带返回类型泛型（原记未展开，与 ADR-0008-contract-marker-interface-no-generic 同题论证）
- 纯标记（无泛型）

## Decision Outcome

Query 为纯标记（无泛型，避免 contract 与 internal 类型耦合，返回类型由 Service 方法签名定义）。

## Consequences

- 返回类型信息只存在于 Service/Handler 方法签名
- CommandHandler / QueryHandler 标记成为 ArchUnit 规则的类型锚点（读写分途的识别基础）

## Confirmation

无机械规则直接针对「Query 无泛型」→ 人工评审（迁移自 ddd 旧 ADR-0005）；原记锚点：`QueryHandler` / `CommandHandler` 接口签名。配套挂载：ArchUnit R11 `COMMAND_HANDLERS_ARE_TRANSACTIONAL`、R13 `QUERY_HANDLERS_DO_NOT_TOUCH_WRITE_REPOSITORIES` 以这两个标记为类型锚点（标记的使用与守边在双端扫描真实开火）。
