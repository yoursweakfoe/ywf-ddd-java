# ADR-0022 熔断降级用 Resilience4j 而非 Sentinel

**Status**: Accepted
**迁移来源**: docs/common/common-cloud.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

客户端熔断降级需求；引入 SCA 后存在 Sentinel（SCA 体系）与 Resilience4j（SC 官方）两个选项。

## Decision Drivers

- 功能边界：Sentinel 的不可替代能力（系统自适应保护 / 热点参数限流 / 集群流控 / 流量整形 / Dashboard）面向大流量治理场景；本项目仅需东西向客户端熔断，入口限流已由 Higress 承担、监控已由 common-observability 覆盖
- 代码侵入：Sentinel 需资源定义 + blockHandler/fallback 双处理；Resilience4j 一套 fallback、可纯配置驱动（零注解也可）
- 维护状态：Sentinel v1.8.8（2024-06）后近两年无实质更新、2.0 仍 alpha、Boot 4 适配靠 SCA 补丁；Resilience4j 随 SC 2025.1.x 同步发布、活跃维护

## Considered Options

- **Sentinel（SCA）**：SCA 绑定 1.8.9，流量治理平台定位
- **Resilience4j（SC 官方）**：轻量容错库定位

## Decision Outcome

选 Resilience4j（经 SC CircuitBreaker 抽象）。即便引入 SCA，也不启用 Sentinel。

## Consequences

- 好：侵入性低、活跃维护、Boot 4 原生适配
- 坏：失去 Sentinel 的系统保护 / 热点流控 / Dashboard（本项目不需要）

## Confirmation

无机械规则针对依赖选型 → 人工评审（迁移自 cloud 旧 ADR-0002）；原记锚点：pom 引入 `spring-cloud-starter-circuitbreaker-resilience4j`，无 sentinel 构件。
