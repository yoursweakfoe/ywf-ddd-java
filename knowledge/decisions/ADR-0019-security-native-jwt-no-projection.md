# ADR-0019 身份不投影：原生 Jwt + 按名字自取

**Status**: Accepted
**迁移来源**: docs/common/common-security.md §6 · 旧 ADR-0006（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

公司 JWT 字段命名无规范（`uid`/`uname`）、字段数量不定（可能只有 userId、可能带部门分部、可能无用户名）。若框架投影成固定 record（如 `CurrentUser(userId, username, roles)`），字段一多一少就失配。

## Decision Drivers

- 字段数量不定 → 固定投影必然失配
- 机制与字段分离：框架抽象验签/建身份，字段命名可插拔按需读取

## Considered Options

- 投影固定 record（如 `CurrentUser(userId, username, roles)`）——字段一多一少即失配
- principal 保持原生 `Jwt` + 按名字自取（选定）

## Decision Outcome

不投影固定结构。principal 保持原生 `Jwt`（claims 全量映射表），`SecurityUtil` 提供 `getClaim` / `getString` / `getStringList` 按名字读取（缺失返回 null/空）。唯一的字段缝是「角色 → 权限」（`@PreAuthorize` 需要），角色 claim 名经 `ywf.security.roles-claim` 配置。

## Consequences

- 消费方代码经 `SecurityUtil` 读 claim，无编译期字段约束（命名责任归业务）
- 角色 claim 是唯一需要配置对齐的字段（roles-claim）

## Confirmation

机械背书：
- 框架：`SecurityUtilTest.getJwt_returnsRawJwt` / `getString_anyField_readsByName` / `getString_missingClaim_returnsNull` / `getStringList_missingClaim_returnsEmpty` / `getString_numericClaim_normalizedToString`；`ResourceServerIntegrationTest.arbitraryClaims_readByName` / `minimalClaims_missingFieldsReadNull`（不定字段端到端）；`SecurityAutoConfigurationTest.rolesClaim_customizable`（角色缝配置位）
- 原记锚点：principal 为原生 `Jwt`、`SecurityUtil` 三读方法语义
