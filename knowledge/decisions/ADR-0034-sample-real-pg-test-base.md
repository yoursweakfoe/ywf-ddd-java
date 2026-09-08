﻿# ADR-0034 sample 测试基座真 PG 化（H2 退役）

**Status**: Accepted（2026-09-08，用户裁决 Q②组合/Q③不做逃生门/Q④批准）
**关联法案**: 同 ADR-0033（TC-5 改卷 / TC-9 开卷）

## Context and Problem Statement

「test profile=H2 零基础设施」契约让测试方言永远隔一层兼容模式，且旧形状经 H2 脚本反哺产库设计；sample 只有本地开发测试/正式部署两态，无线上测试需求。

## Decision Outcome

test profile 直连真 PG 测试库 `ddd_sample_application_test`（`DB_TEST_URL/USER/PASSWORD` 覆写）；库形状权威 = db-migration（测试不建表，H2 schema.sql 与 h2 依赖从 sample 退役）；隔离双轨=容器测试 `@Transactional` 回滚 + 真 HTTP 链教例类级 `@Sql TRUNCATE … RESTART IDENTITY` 清场（独立事务提交，回滚不可覆盖）；testcontainers 逃生门不做，README 记一条前置。common 模块自身测试树不受本案辖域（框架测自己，继续 H2）。

## Consequences

- 演示契约改述：「离线逻辑零出口、一条前置全绿」（TC-5 新文案）。
- 背书：`RestEndpointIntegrationTest`/`OptimisticLockConcurrencyTest` 类级 @Sql；119/119 真 PG 绿。