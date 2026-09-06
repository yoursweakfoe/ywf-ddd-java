# ADR-0002 全量 UPDATE 而非脏检查

**Status**: Accepted
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

持久化更新策略：脏检查（只更新变化列）还是全量更新（逐列覆写）。

## Decision Drivers

- 脏检查需要变更追踪设施，增加复杂度
- `update_time` 审计字段必须始终刷新的要求
- 本框架场景下脏检查收益极低

## Considered Options

- **脏检查**：只写变化列，但需要变更追踪设施
- **全量 UPDATE**：逐列枚举覆写

## Decision Outcome

选全量 UPDATE。本框架场景下脏检查收益极低且增加复杂度（需要变更追踪设施）；全量更新保证审计字段刷新——XML 的 `updateById` 语句逐列枚举，PO 由 Converter 完整装配，null 字段真实写为 NULL。

## Consequences

- 更新语义：字段传 null 即真清列（无「null = 不更新」歧义）
- 乐观锁版本条件（`WHERE id = ? AND version = ?`）与影响行数分类链建立在 `updateById` 全量语句之上（见 ADR-0007-ddd-remove-mybatis-plus）

## Confirmation

机械背书：
- 框架：`MybatisPersistenceTest.updateDomain_success_incrementsVersion` / `updateDomain_staleVersion_throwsOptimisticLockConflict` / `updateById_staleVersion_affectedZeroRows`
- 真实例（sample）：`OptimisticLockConcurrencyTest.concurrentOrders_optimisticLock_preventsOversell`
- 原记锚点：`updateDomain` 走 `mapper.updateById` 全量 UPDATE（见各聚合 `resources/mapper/**/XxxMapper.xml`）
