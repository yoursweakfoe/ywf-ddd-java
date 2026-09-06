# ADR-0018 零信任：服务自验 JWT（资源服务器）

**Status**: Accepted
**迁移来源**: docs/common/common-security.md §6 · 旧 ADR-0005（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

服务如何获得可信身份：信任网关透传的身份 Header，还是自行验签。企业推动零信任——不信任网络、每跳验证。

## Decision Drivers

- 零信任 / 防御纵深：不信任网络身份 Header（Header 可伪造）
- 网关职责收窄为 PEP（策略执行点），不做身份断言

## Considered Options

- 网关验签 + 服务信任 Header（旧模型，[ADR-0014](ADR-0014-security-gateway-trusted-header-identity.md)，本决策取代它）
- 服务下沉为 OAuth2 资源服务器自验 JWT（选定）

## Decision Outcome

服务下沉为 OAuth2 资源服务器，自行验签 JWT；网关降为 PEP（转发 JWT 而非身份断言）。fail-closed：坏 token → 401，绝不静默放行（对比旧框架的 `catch (Exception ignored)`）。

## Consequences

- 每个服务引入 JWT 验签能力（密钥方案可插拔，见 ADR-0020）
- 东西向调用需透传已验签 JWT 供下游自验（见 ADR-0026-cloud-east-west-jwt-propagation）
- 服务侧不提供签发能力（签发/刷新/登出归独立 IdP）

## Confirmation

机械背书：
- 框架：`ResourceServerIntegrationTest`（验签链路端到端）；`SecurityAutoConfigurationTest`
- ArchUnit R6 `DOMAIN_DOES_NOT_DEPEND_ON_SECURITY`（身份验证止步于边界/应用层，domain 不感知）
- 真实例（sample）：双端 `ApplicationArchitectureTest` / `DddArchitectureTest` 挂载 R6
- 原记锚点：服务作为 OAuth2 资源服务器自验 JWT 的自动装配
