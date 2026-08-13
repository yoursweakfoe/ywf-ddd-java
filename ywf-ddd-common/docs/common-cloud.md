# common-cloud

微服务治理基础 —— Nacos 客户端预留 + Seata 分布式事务 + Spring Cloud 官方（Feign / LoadBalancer / CircuitBreaker）。

## 定位

一站式微服务治理聚合包，引入即获得分布式事务、声明式调用、负载均衡、熔断降级能力。
面向使用分布式事务与东西向 HTTP 调用的微服务。
本模块为纯聚合 pom（无自有 Java 代码），能力通过依赖传递组装各官方组件。

> 统一异常体系（BusinessException + REST 全局处理）不在本包分发：按 opt-in 原则，
> 由业务服务显式引入 `common-exception`（或随 `common-ddd` 获得）。
> REST API 文档（springdoc）同样不在本包分发：由业务服务按需引入 `common-doc`。

## 设计原则

- **引入即生效**：Seata / Feign / CircuitBreaker 经 starter 自动装配
- **聚合不封装**：直接传递官方 starter，不做二次包装，业务服务可直接使用原生 API
- **不引入 Spring Cloud Alibaba**：SCA 版本更新滞后；Nacos / Seata 以独立构件引入，熔断/限流用 Spring Cloud 官方 CircuitBreaker（Resilience4J）替代 Sentinel
- **统一 HTTP**：对外 REST 经 Higress 网关，东西向服务间 RestClient / Feign 直连（一期静态地址）
- **版本精确声明**：Boot 未托管的第三方版本（nacos-client / seata）独立声明；SC 组件版本由 spring-cloud-dependencies BOM 统一管理（2025.1.x Oakwood，兼容 Boot 4.1）

## 核心功能

| 组件 | 职责 |
|------|------|
| Seata | AT/TCC 分布式事务（跨服务最终一致性） |
| nacos-client | 版本独立管理；一期不启用注册发现/配置中心，为二期服务注册发现与配置中心恢复预留 |
| Feign | 声明式 HTTP 客户端（东西向服务调用；`@FeignClient` 接口 + CQE/CO 契约） |
| LoadBalancer | 客户端负载均衡（实例来源：静态配置或二期 Nacos 注册发现） |
| CircuitBreaker (Resilience4J) | 客户端熔断/降级（替代停更且无 Boot 4 适配的 Sentinel；国际主流方案） |

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```

引入即生效。无需额外代码配置。

### 启用前提

| 组件 | 前提 | 配置位置 |
|------|------|------|
| Seata | Seata Server 地址 + 事务分组（TC 不可达时启动 fail-fast；测试环境 `seata.enabled: false`） | `seata.*` 配置项 |
| Nacos | 一期不启用（二期恢复） | — |
| Feign | 启动类标注 `@EnableFeignClients`；调用方声明 `@FeignClient` 接口 | 代码 |
| LoadBalancer | 服务端多实例：静态实例列表或二期 Nacos；单实例直连时无需 | `spring.cloud.discovery.*` 或二期 Nacos |
| CircuitBreaker | 无（默认 Resilience4J 自动装配；规则经 `resilience4j.*` 或 `spring.cloud.circuitbreaker.*` 配置） | 配置项 |

### 最小配置样例

```yaml
# application.yml
spring:
  application:
    name: order-service

seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
    grouplist:
      default: ${SEATA_SERVER:127.0.0.1:8091}

# Resilience4J 熔断默认规则（可选，不配则走框架默认）
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 20
```

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 聚合 pom，无自有装配代码 | 业务服务直接使用 Boot/Seata/SC 原生 API，无学习成本 |
| 引入 Spring Cloud 官方而非 SCA | SCA 版本滞后于 Boot 4 生态；仅取官方有良好维护的 Feign / LB / CircuitBreaker |
| 用 CircuitBreaker (Resilience4J) 替代 Sentinel | Sentinel 官方停更（最后版本 1.8.10）且无 Spring 7 适配；Resilience4J 为国际主流、随 SC 同步发布 |
| Nacos / Seata 独立构件引入 | 避免 SCA 全家桶抽象；nacos-client / seata-spring-boot-starter 均为独立维护 |
| 移除 gRPC，东西向统一 HTTP | 简化封装代码；东西向经 RestClient / Feign 直连提供方 REST 端点；gRPC 支持待架构稳定后再评估 |
| Seata 跨服务 XID 透传不内置 | HTTP 统一后透传为出站 RestClient interceptor + 入站 Filter（TX_XID header），配方见 sample-application cookbook distributed-transaction.md；模式稳定后再考虑内置 |
| Nacos Client 保留但不启用 | 去注册中心一期目标；保留客户端为二期服务注册发现与配置中心恢复铺路 |
| **未实现** 服务注册发现 | 一期东西向静态地址直连；二期基于 nacos-client 恢复（届时 LoadBalancer 实例来源就位） |
| **未实现** 限流（Sentinel） | 入口流量治理由 Higress 网关承担；应用层 CircuitBreaker 覆盖东西向客户端防护 |
| **未实现** 配置中心封装（Nacos Config） | 二期随注册发现一并恢复 |
| **未实现** 链路追踪 SDK | 由 common-observability 通过 OTel Agent 零侵入方式覆盖 |
| **未实现** 灰度发布/流量染色 | 由 Higress 网关层路由规则实现，不需要 SDK 级支持 |

## 依赖关系

```
common-cloud → nacos-client
             → seata-spring-boot-starter
             → spring-cloud-starter-openfeign
             → spring-cloud-loadbalancer
             → spring-cloud-starter-circuitbreaker-resilience4j
```

> 统一异常体系（common-exception）由业务服务按需显式引入（REST 全局异常处理），见定位一节。
> REST API 文档（common-doc）由需要对外暴露 REST 端点的服务按需引入。
> Spring Cloud 组件版本由 spring-cloud-dependencies BOM 管理；**BOM 不传递**，消费方如需版本对齐须自行 import（见 ywf-ddd-common 主 POM 与 sample-application 根 POM）。
