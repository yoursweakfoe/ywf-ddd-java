# ADR-0014 Header 透传身份（网关验签 + 服务信任 Header）

**Status**: Superseded by [ADR-0018](ADR-0018-security-zero-trust-jwt-self-verify.md)（旧 common-security §ADR-0005 零信任）
**迁移来源**: docs/common/common-security.md §6 · 旧 ADR-0001 废弃墓碑（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

旧决策：验签由网关完成，服务只信任网关透传的 Header（`X-User-Id` 等）。

## Decision Outcome

已废弃，由 ADR-0018（旧 common-security §ADR-0005，零信任：服务自验 JWT）取代。废弃原因：企业推动零信任，该模式违反「不信任网络、每跳验证」——Header 可伪造。按 references.md §ADR 总索引注记：**旧模型勿再引用**。

## Consequences

- 依附于此模型的配套机制一并作废：身份来源标记（EDGE）→ [ADR-0015](ADR-0015-security-edge-identity-source-void.md)、预认证 + 链内注册 → [ADR-0017](ADR-0017-security-header-preauth-filter-void.md)，两者编号空置登记

## Confirmation

人工评审（迁移自 security 旧 ADR-0001）——历史墓碑记录，现行有效决策及其背书见 ADR-0018。
