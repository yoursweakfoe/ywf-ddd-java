# ADR-0027 自动注册而非手动配置

**Status**: Accepted
**迁移来源**: docs/common/common-pg.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

PG TypeHandler（UUID / JSONB / 数组）的注册方式。

## Decision Drivers

- 类型映射是通用的，无需每个服务重复声明
- 引入即生效（自动装配教义）

## Considered Options

- **手动 `type-handlers-package`**：每个服务重复声明
- **自动注册**：`PgTypeHandlerAutoConfiguration` 批量注册

## Decision Outcome

选自动注册。类型映射是通用的，无需每个服务重复声明。

## Consequences

- 消费方引入依赖即获得类型映射，无 per-service 配置义务

## Confirmation

无测试直接覆盖「自动注册链」本身（各 TypeHandler 行为由 `JsonbTypeHandlerTest` / `UUIDTypeHandlerTest` 等 handler 级测试群背书）→ 人工评审（迁移自 pg 旧 ADR-0001）；原记锚点：`PgTypeHandlerAutoConfiguration` 经 AutoConfiguration.imports 注册。
