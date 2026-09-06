# ADR-0013 IllegalStateException → 409

**Status**: Accepted
**迁移来源**: docs/common/common-exception.md §6 · 旧 ADR-0003（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

乐观锁版本冲突（UPDATE 影响行数 0 且实体仍在）如何映射 HTTP 状态。

## Decision Drivers

- 冲突语义与 HTTP 状态的标准对应（409 Conflict）
- 与业务规则违反（422）的通道分离

## Considered Options

- 原记未列备选（决策直接给出 409 通道预留）

## Decision Outcome

`IllegalStateException` → 409 Conflict——409 通道即为乐观锁冲突预留（`OptimisticLockConflictException` 继承自它，「实体消失」的普通 ISE 同走此通道）；状态机非法转换属业务规则违反，聚合根抛 `BusinessException` 走缺省 422，不占用 409。

## Consequences

- 409 = 乐观锁/存在性冲突通道，422 = 业务规则违反通道，两通道由异常类型区分
- 乐观锁三分通道（成功 / 冲突 / 静默写失）中 ISE 承载「冲突与消失」两类，分类链见 ADR-0007-ddd-remove-mybatis-plus 与 ADR-0002-ddd-full-update-no-dirty-check

## Confirmation

机械背书：
- 框架：`GlobalRestExceptionHandlerTest.illegalState_returns409`（ISE→409 + detail 稳定泛化不回显原始消息）、`businessException_returns422Rfc9457`（422 缺省通道对照）
- 真实例（sample）：聚合状态守卫（如 `Order.pay()`）抛 `BusinessException`（`order:err.*`）→ 422
- 原记锚点：`GlobalRestExceptionHandler` 处理 `IllegalStateException` 返回 409
