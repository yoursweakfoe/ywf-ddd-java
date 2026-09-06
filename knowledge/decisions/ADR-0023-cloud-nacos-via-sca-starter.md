# ADR-0023 Nacos 经 SCA starter 引入，client 版本独立管理

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0003（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

需要 Nacos 服务注册发现 + 配置中心的 Spring Cloud 自动装配。裸 nacos-client 仅 SDK 无自动装配。

## Decision Drivers

- 自动装配开箱即用 vs 自写集成代码
- 版本节奏自主权（SCA BOM 版本落后于 Nacos 官方）

## Considered Options

- **裸 nacos-client**：编程式 SDK，需自写 DiscoveryClient / 配置注入
- **SCA nacos-discovery + nacos-config starter**：开箱即用（DiscoveryClient + spring.config.import）

## Decision Outcome

选 SCA starter（仅 discovery + config 两个构件），nacos-client 版本独立管理（3.2.3 覆盖 SCA BOM 的 3.1.1）。

## Consequences

- 好：LoadBalancer 自动从 Nacos 取实例；spring.config.import 声明式配置
- 坏：引入 SCA 构件（但仅 Nacos 集成，不带 Sentinel / 全家桶）

## Confirmation

无机械规则针对依赖选型 → 人工评审（迁移自 cloud 旧 ADR-0003）；原记锚点：pom 引入 nacos-discovery + nacos-config；父 DM 声明 nacos-client 3.2.3。
