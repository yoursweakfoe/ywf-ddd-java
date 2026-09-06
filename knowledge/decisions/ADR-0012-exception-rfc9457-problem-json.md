# ADR-0012 RFC 9457 响应格式

**Status**: Accepted
**迁移来源**: docs/common/common-exception.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

REST 错误响应采用何种格式。

## Decision Drivers

- 外部消费方可程序化处理错误响应
- 标准化（HTTP 语义）优先于自定义格式

## Considered Options

- 自定义错误响应体（原记未展开）
- RFC 9457 `application/problem+json`（选定）

## Decision Outcome

采用 RFC 9457（`application/problem+json`），`type` 当前为 `about:blank`，待错误类型文档化后替换为绝对 URI；`params`/`fieldErrors` 为合规扩展字段。

## Consequences

- 技术类异常的 detail 为稳定泛化文案（原始消息只进服务端日志，防内部信息外泄）
- `type` 字段留升级位（about:blank → 绝对 URI）

## Confirmation

机械背书：
- 框架：`GlobalRestExceptionHandlerTest`（`PROBLEM_JSON` 断言 + `businessException_returns422Rfc9457` / `constraintViolation_returns400WithFieldErrors` / `silentWriteLoss_returns500WithoutLeak` 等映射用例）
- 真实例（sample）：`RestEndpointIntegrationTest` 错误路径经同一载体
- 原记锚点：`GlobalRestExceptionHandler` 响应载体为 Spring 内建 `ProblemDetail`（`ResponseEntity<ProblemDetail>`，RFC 9457 标准成员 `type`/`title`/`status`/`detail`/`instance`），`params`/`fieldErrors` 经 `ProblemDetail` 扩展属性位（`setProperty`）注入为合规扩展成员（RFC 9457 §3.2），Content-Type 显式声明 `application/problem+json`
