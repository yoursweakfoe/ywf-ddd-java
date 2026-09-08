﻿# ADR-0033 PG 原生形状法与 schema 命名法（含审计默认字段改时态）

**Status**: Accepted（2026-09-08，用户三连裁决 + Q① 改判）
**关联法案**: knowledge/specs/changes/2026-09-pg-native-shape-and-real-testdb（折叠入库 BP-S1~S3）

## Context and Problem Statement

旧形（create_at/update_at/is_delete/version INT/id VARCHAR(36)/复数双名）系 H2 能力天花板下的迁就；工具与测试的局限不得影响业务设计。同时 AuditProperties 默认 `createAt`/`updateAt` 为畸形缩写英文，与 PG 原生规范 `created_at`（属性 createdAt）不符。

## Decision Outcome

1. 库表形状唯一权威 = `db-migration` 变更集：`id UUID DEFAULT uuidv7()`（PG18 内建）、`created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`、`created_by/updated_by UUID`、`is_deleted`、`version BIGINT`。
2. schema 命名法：聚合边界 = schema 边界、领域词单数（product.product）；SQL 保留字冲突升级行业 UB 术语（order→sales_order），禁引号/前缀逃逸；与 Java 包名单数同构，拆分时整 schema 平移。
3. 外部工具账表住独立 schema（Liquibase 账表→`liquibase`，bootstrap 幂等自建；Seata TC 优先分库、undo_log 随业务连接落 public）。
4. **公共 API 破坏性变更**：`AuditProperties` 时间缺省 `createAt/updateAt` → `createdAt/updatedAt`（操作人缺省不变）；消费方零桥接。sample 类名保留 Order/Product（`sales_order` 仅库层）。

## Consequences

- sample/common 测试树形状随换（一次改卷全账）；旧库/旧文档引用由法案折叠统一清账。
- 背书：`0001-init-schema.sql` 双库 SHA256 同形实证；common `mvn install` 绿；sample 真 PG `Tests run: 119, Failures: 0`。