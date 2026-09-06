# ADR-0024 Seata 独立构件 + 版本独立管理

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0004（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

分布式事务需求。SCA 绑定 seata 2.5.0，落后于官方 2.6.0。

## Decision Drivers

- 版本升级节奏自主权（不受 SCA 发布节奏约束）

## Considered Options

- **经 SCA 引入**：版本绑 SCA 发布节奏
- **独立引入 seata-spring-boot-starter**：版本独立管理

## Decision Outcome

选独立引入，版本独立管理 2.6.0（覆盖 SCA BOM 的 2.5.0）。

## Consequences

- Seata 升级不受 SCA 发布节奏约束

## Confirmation

无机械规则针对依赖选型 → 人工评审（迁移自 cloud 旧 ADR-0004）；原记锚点：pom 引入 `org.apache.seata:seata-spring-boot-starter`；父 DM 声明 2.6.0。
