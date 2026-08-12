# common-cloud

微服务治理基础 —— springdoc OpenAPI + Nacos 客户端预留 + Seata 分布式事务。

## 定位

一站式微服务治理聚合包，引入即获得 REST API 文档、分布式事务、全局异常处理能力。
面向所有需要对外暴露 REST 端点、使用分布式事务的业务服务。
本模块为纯聚合 pom（无自有 Java 代码），能力通过依赖传递组装各官方组件。

## 设计原则

- **引入即生效**：异常处理通过 Spring Boot AutoConfiguration 注册，REST 文档走 Boot 自动配置
- **聚合不封装**：直接传递官方 starter，不做二次包装，业务服务可直接使用原生 API
- **统一 HTTP**：对外 REST 经 Higress 网关，东西向服务间 RestClient 直连（一期静态地址）；gRPC 已移除，架构稳定后再评估
- **版本精确声明**：Boot 未托管的第三方版本（nacos-client / seata / springdoc）在 ywf-ddd-common dependencyManagement 独立声明

## 核心功能

| 组件 | 职责 |
|------|------|
| springdoc-openapi | REST 面 OpenAPI 3.0 文档（`/v3/api-docs` + Swagger UI），配合 Apifox 导入同步 |
| Seata | AT/TCC 分布式事务（跨服务最终一致性） |
| nacos-client | 版本独立管理；一期不启用注册发现/配置中心，为二期服务注册发现与配置中心恢复预留 |
| common-exception | 聚合引入统一异常体系（BusinessException + REST 全局处理） |

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
```

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 聚合 pom，无自有装配代码 | 业务服务直接使用 Boot/Seata 原生 API，无学习成本 |
| 移除 gRPC，东西向统一 HTTP | 简化封装代码；东西向经 RestClient 直连提供方 REST 端点；gRPC 支持待架构稳定后再评估 |
| Seata 跨服务 XID 透传不内置 | HTTP 统一后透传为出站 RestClient interceptor + 入站 Filter（TX_XID header），配方见 sample-application cookbook distributed-transaction.md；模式稳定后再考虑内置 |
| Nacos Client 保留但不启用 | 去注册中心一期目标；保留客户端为二期服务注册发现与配置中心恢复铺路 |
| **未实现** 服务注册发现 | 一期东西向静态地址直连；二期基于 nacos-client 恢复 |
| **未实现** 服务熔断/降级 | 当前服务规模小；HTTP 超时/重试（RestClient）已够用 |
| **未实现** 配置中心封装（Nacos Config） | 二期随注册发现一并恢复 |
| **未实现** 链路追踪 SDK | 由 common-observability 通过 OTel Agent 零侵入方式覆盖 |
| **未实现** 灰度发布/流量染色 | 由 Higress 网关层路由规则实现，不需要 SDK 级支持 |

## 依赖关系

```
common-cloud → common-exception（BusinessException + REST 全局异常处理）
             → springdoc-openapi-starter-webmvc-ui
             → nacos-client
             → seata-spring-boot-starter
```
