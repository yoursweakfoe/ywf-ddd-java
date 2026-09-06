# ADR-0008 标记接口不含泛型

**Status**: Accepted
**迁移来源**: docs/common/common-contract.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

Query / Command 契约标记是否需要携带返回类型泛型。Query 定义在 contract（对外），Handler 在 application（内部）。

## Decision Drivers

- 类型信息内聚 vs 内外类型耦合
- contract（对外）与 Handler（内部）的边界独立性

## Considered Options

- **带泛型 `Query<R>`**：类型信息内聚，但 contract（对外）与 Handler（内部）产生类型耦合
- **纯标记 `Query`**：返回类型由 Service 方法签名定义

## Decision Outcome

选纯标记。Query 定义在 contract（对外），Handler 在 application（内部），绑定泛型会导致内外类型耦合。

## Consequences

- 返回类型不可从标记接口推断，需看 Service/Handler 方法签名
- 与 ADR-0005-ddd-query-pure-marker 互为镜像（契约侧决策与框架侧决策分属两模块）

## Confirmation

无机械规则直接针对「标记接口无泛型参数」→ 人工评审（迁移自 contract 旧 ADR-0001）；原记锚点：`Query.java` / `Command.java` 无泛型参数。
