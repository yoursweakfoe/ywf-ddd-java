# ADR-0016 边界 permit-all SecurityFilterChain

**Status**: Accepted
**迁移来源**: docs/common/common-security.md §6 · 旧 ADR-0003（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

服务层的 Spring Security 链承担什么职责（路由级鉴权与细粒度鉴权如何分工）。

## Decision Drivers

- 鉴权分层：路由级在网关（PEP），细粒度在服务
- 框架链必须可被业务覆盖（不焊死安全策略）

## Considered Options

- 原记未列备选

## Decision Outcome

路由级鉴权在网关；服务层提供 permit-all + 无状态链，可被覆盖；细粒度鉴权用 `@PreAuthorize`。

## Consequences

- 服务链默认放行边界（路由级红线归网关），身份建立仍由资源服务器完成（见 ADR-0018）
- 细粒度鉴权责任下沉方法级注解

## Confirmation

机械背书：
- 框架：`ResourceServerIntegrationTest.preAuthorize_enforcesRole`（方法级鉴权生效）；`SecurityOptOutTest.defaultEnabled_securityFilterChainPresent` / `disabled_securityFilterChainAbsent`（链装配与可覆盖性）
- ArchUnit R6 `DOMAIN_DOES_NOT_DEPEND_ON_SECURITY`（domain 不感知认证上下文——本决策「鉴权在边界/应用层」的镜像守护）
- 原记锚点：permit-all + 无状态 SecurityFilterChain 装配
