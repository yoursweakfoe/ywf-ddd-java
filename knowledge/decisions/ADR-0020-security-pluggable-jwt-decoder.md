# ADR-0020 验签可插拔：JwtDecoder 抽象 + 多方案分发

**Status**: Accepted
**迁移来源**: docs/common/common-security.md §6 · 旧 ADR-0007（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

不同来源 JWT 使用不同签名算法（HS256 / RS256 …），密钥方案未统一。

## Decision Drivers

- 密钥方案未统一 → 验签能力必须可插拔
- 框架保持薄：可选工具类，不强制装配

## Considered Options

- 单一固定验签方案（原记未展开——与「密钥方案未统一」现实冲突）
- `JwtDecoder` 接口抽象 + 按 `alg` 分发（选定）

## Decision Outcome

`JwtDecoder` 接口即抽象；`DelegatingJwtDecoder` 按 JOSE 头 `alg` 分发到各方案 decoder。可选工具类，谁需要谁 `new`。

## Consequences

- 新增签名方案 = 新增 decoder 实现，不改框架抽象
- 未知 `alg` 拒绝（fail-closed 语义延伸）

## Confirmation

机械背书：
- 框架：`DelegatingJwtDecoderTest.routesByAlg` / `unknownAlg_rejected`（分发与拒绝双向锁死）
- 原记锚点：`JwtDecoder` 抽象 + `DelegatingJwtDecoder` 按 alg 分发，不自动装配
