# ADR-0011 i18n 位点（字符串 key）而非数字错误码

**Status**: Accepted
**迁移来源**: docs/common/common-exception.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

错误码用字符串 key 还是数字。

## Decision Drivers

- 多语言扩展成本（数字码需映射表）
- 翻译责任归属（服务端 vs 前端）

## Considered Options

- **数字错误码**：紧凑，但多语言扩展需映射表
- **字符串 key**：天然支持多语言，前端直接翻译

## Decision Outcome

选字符串 key。服务端不维护 messages.properties，前端负责渲染。

## Consequences

- `messageKey` 为前端翻译 key，服务端不维护 messages.properties，由前端 `t(key, params)` 渲染（位点格式 `{aggregate}:err.{scene}`）

## Confirmation

机械背书：
- 框架：`BusinessExceptionTest.constructor_messageKeyOnly` / `getMessage_returnsMessageKey`（messageKey 字符串承载行为）
- 原记锚点：`BusinessException` 持有 `messageKey` 字符串
