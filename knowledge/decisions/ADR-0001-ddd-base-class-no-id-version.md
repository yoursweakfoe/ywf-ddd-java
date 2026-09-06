# ADR-0001 基类不持有 id/version 字段

**Status**: Accepted
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

聚合根基类（`Entity` / `AggregateRoot`）是否内置 id/version 字段。ID 类型（UUID / Long / 业务编码）在不同业务间差异大，基类一旦内置字段即强制统一。

## Decision Drivers

- 子类样板代码成本（内置字段可少写样板）
- ID 类型自由度（内置会强制统一 ID 类型）
- 继承污染（基类持有子类未必需要的字段）

## Considered Options

- **内置字段**：子类少写样板，但 ID 类型（UUID/Long/业务编码）被强制统一
- **泛型化 + 不持有**：子类自由声明

## Decision Outcome

选泛型化 + 不持有。ID 生成与业务强相关，由子类构造器自行决定。

## Consequences

- `Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自行声明 id/version 字段
- 聚合根 ID 自动生成策略归业务（基类不设默认生成器）

## Confirmation

无机械规则直接针对「基类无 id/version 字段」→ 人工评审（迁移自 ddd 旧 ADR-0001）；原记锚点：`Entity<ID>` / `AggregateRoot<ID>` 无 id/version 字段（源码查看即验）。
