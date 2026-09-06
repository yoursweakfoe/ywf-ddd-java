# ADR-0021 东西向通信统一 HTTP

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

东西向（服务间）调用协议选型收敛为最终态——东西向统一 HTTP（RestClient 直连一期；Feign 经 common-cloud opt-in）。

## Decision Drivers

- 内部接口量少 → 强类型协议收益有限
- REST 端点复用
- 契约与安全模型简化

## Considered Options

- **gRPC**：强类型编译期绑定，但内部接口量少、收益有限
- **HTTP（Feign / RestClient）**：契约简单、REST 端点复用

## Decision Outcome

选 HTTP。内部接口量少，REST 端点可复用，简化契约与安全模型。

## Consequences

- 好：统一协议可以显著减少适配代码（约1000行）
- 坏：失去 gRPC 强类型与二进制性能；Feign 接口即契约

## Confirmation

无机械规则针对依赖选型 → 人工评审（迁移自 cloud 旧 ADR-0001）；原记锚点：`common-cloud/pom.xml` 引入 openfeign 且无 grpc 构件（`mvn dependency:tree` 检查）。
