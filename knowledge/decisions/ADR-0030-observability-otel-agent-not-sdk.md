# ADR-0030 OTel Agent 而非 SDK

**Status**: Accepted
**迁移来源**: docs/common/common-observability.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

链路追踪用 Agent 挂载还是引入 SDK 依赖。

## Decision Drivers

- 零代码侵入
- 未挂载时零副作用

## Considered Options

- 引入 OTel SDK 依赖
- OTel Java Agent 部署时挂载（选定）

## Decision Outcome

选 Agent 挂载。零代码侵入；未挂 Agent 时 classpath 无任何 OTel 类型、MDC 不含 `trace_id`/`span_id` 键（日志照常输出，无任何副作用）。

## Consequences

- 链路追踪能力随部署形态出现/消失，应用代码与构建产物不感知

## Confirmation

无机械测试针对依赖缺席 → 人工评审（迁移自 observability 旧 ADR-0002）；原记锚点：本包无任何 OTel Maven 依赖（`mvn dependency:tree` 检查）。
