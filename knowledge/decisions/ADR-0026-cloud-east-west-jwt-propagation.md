# ADR-0026 东西向身份传播：透传已验签 JWT（零信任）

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0006（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

零信任下东西向（服务间）调用不能靠内网可信，也不能靠网关注入身份 Header（[ADR-0018](ADR-0018-security-zero-trust-jwt-self-verify.md) 的延伸场景）。

## Decision Drivers

- 零信任：下游必须能自验，不信任网络
- 基础设施成本（避免服务网格/证书体系）
- 复用现有 Feign 栈

## Considered Options

- **mTLS / SPIFFE**：传输层双向认证，需服务网格或证书体系，重
- **JWT 透传**：把当前线程已验签的 JWT 原样带上 `Authorization: Bearer` 透传，下游作为资源服务器自验签

## Decision Outcome

选 JWT 透传。Feign `RequestInterceptor` 读取 SecurityContext 里的 `Jwt`，注入 `Authorization: Bearer`；下游用 common-security 自验签。无需服务网格，契合现有 Feign 栈。

## Consequences

- 好：零额外基础设施；用户身份跨服务连续可审计；下游自验不信任网络
- 坏：机器身份（定时任务 / MQ 无用户上下文）需另行走 client-credentials；长链透传同一 token，受众 / scope 限制留待演进

## Confirmation

机械背书：
- 框架：`JwtPropagationRequestInterceptorTest`（Bearer 注入行为）
- 原记锚点：`common-cloud` 内置 `JwtPropagationRequestInterceptor` + `FeignJwtPropagationAutoConfiguration`
