# ADR-0029 stdout 输出，不落盘文件

**Status**: Accepted
**迁移来源**: docs/common/common-observability.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

日志输出到文件还是 stdout。

## Decision Drivers

- 容器化部署统一走 stdout + 日志采集
- 文件落盘增加运维复杂度且不利于弹性扩缩

## Considered Options

- 文件落盘
- stdout（选定）

## Decision Outcome

选 stdout。容器化部署统一走日志采集；文件落盘增加运维复杂度且不利于弹性扩缩。

## Consequences

- 库不 ship logback 配置：结构化日志走 Spring Boot 内置能力，业务 app 配 `logging.structured.format.console=logstash` 一行即可（§5 教义句，随本决策登记）

## Confirmation

无机械测试针对输出目标 → 人工评审（迁移自 observability 旧 ADR-0001）；原记锚点：结构化日志默认输出 console（Spring Boot 内置，无文件 Appender）。
