# ADR-0025 Seata XID 透传不内置

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0005（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

分布式事务需 XID 跨服务透传。该能力放框架还是放配方。

## Decision Drivers

- 模式未稳定 → 框架不宜焊死
- 框架保持薄

## Considered Options

- 内置到 common-cloud
- 不内置，配方沉淀到 cookbook

## Decision Outcome

选不内置。HTTP 统一后透传为出站 interceptor + 入站 Filter（TX_XID header），模式未稳定，先以配方形式沉淀。

## Consequences

- 业务方需按配方自行接入；框架保持薄

## Confirmation

无机械规则针对「框架不内置某能力」→ 人工评审（迁移自 cloud 旧 ADR-0005）；原记锚点：common-cloud 无 XID 透传代码；配方见 sample-application cookbook distributed-transaction.md。
