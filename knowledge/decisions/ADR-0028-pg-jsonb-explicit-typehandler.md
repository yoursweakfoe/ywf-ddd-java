# ADR-0028 JSONB 需显式指定 typeHandler

**Status**: Accepted
**迁移来源**: docs/common/common-pg.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

JSONB 字段能否按 Java 类型自动路由到 JsonbTypeHandler。

## Decision Drivers

- `String.class` 已被默认 `StringTypeHandler` 占用，无法自动路由
- 显式声明避免歧义

## Considered Options

- 自动路由（技术上不可行——类型槽位已被默认 handler 占用）
- XML 语句显式指定 typeHandler（选定）

## Decision Outcome

不能自动路由。`String.class` 已被默认 `StringTypeHandler` 占用，无法自动路由到 JsonbTypeHandler；显式声明避免歧义。

## Consequences

- 每处 JSONB 参数位 / 结果位需在 XML 中显式写 `typeHandler`（手写 XML 教义下该义务可见、可 grep，见 ADR-0007-ddd-remove-mybatis-plus）

## Confirmation

机械背书：
- 框架：`JsonbTypeHandlerTest`、`JsonNodeTypeHandlerTest`（handler 行为）
- 原记锚点：JsonbTypeHandler / JsonNodeTypeHandler 需在 XML 语句中显式指定 `typeHandler`（参数位 / 结果位）
