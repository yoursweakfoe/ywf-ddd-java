# common-cloud

微服务治理基础 —— Boot 原生 gRPC 东西向通道 + springdoc OpenAPI + Nacos 客户端预留 + Seata 分布式事务。

## 定位

一站式微服务通信与治理聚合包，引入即获得 gRPC 通信、REST API 文档、分布式事务、全局异常处理能力。
面向所有需要对外暴露 REST 端点、提供/消费 gRPC 服务的业务服务。
本模块为聚合 pom + 一个自动装配类（`SeataGrpcAutoConfiguration`），其余能力通过依赖传递组装各官方组件。

## 设计原则

- **引入即生效**：异常处理与 Seata gRPC 透传通过 Spring Boot AutoConfiguration 注册，gRPC/REST 文档走 Boot 自动配置
- **聚合不封装**：直接传递官方 starter，不做二次包装，业务服务可直接使用原生 API
- **版本随 Boot 托管**：gRPC 全家桶（grpc-java / spring-grpc-core / protobuf 插件）由 Boot 4.1 dependency management 统一管理，不自管 bom
- **面/通道分离**：对外 REST（Spring MVC，经网关）与东西向 gRPC（显式 proto 契约）双通道，端口分离

## 核心功能

| 组件 | 职责 |
|------|------|
| spring-boot-starter-grpc-server | gRPC 服务端（Netty 传输，`spring.grpc.server.*` 属性；health/reflection 服务自动注册） |
| spring-boot-starter-grpc-client | gRPC 客户端（`GrpcChannelFactory` + 命名通道 `spring.grpc.client.channels.*`） |
| springdoc-openapi | REST 面 OpenAPI 3.0 文档（`/v3/api-docs` + Swagger UI），配合 Apifox 导入同步 |
| Seata | AT/TCC 分布式事务（跨服务最终一致性） |
| seata-grpc + SeataGrpcAutoConfiguration | Seata XID 经 gRPC Metadata 透传（官方 `ServerTransactionInterceptor` / `ClientTransactionInterceptor` 注册为全局 interceptor，`seata.enabled=false` 时不装配） |
| nacos-client | 版本独立管理；一期不启用注册发现/配置中心，为二期 gRPC NameResolver 预留 |
| common-exception | 聚合引入统一异常体系（BusinessException + REST/gRPC 双通道处理） |

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
| gRPC 服务端 | 自动（端口 `spring.grpc.server.port`，约定 50051） | `application.yml` |
| gRPC 客户端 | 命名通道地址（一期静态直连） | `spring.grpc.client.channels.<name>.address` |
| Seata | Seata Server 地址 + 事务分组（TC 不可达时启动 fail-fast；测试环境 `seata.enabled: false`） | `seata.*` 配置项 |
| Nacos | 一期不启用（二期恢复） | — |

### 最小配置样例

```yaml
# application.yml
spring:
  application:
    name: order-service
  grpc:
    server:
      port: 50051
    client:
      channels:
        product-internal:
          address: ${PRODUCT_INTERNAL_ADDRESS:localhost:50051}

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
| 聚合 pom + 最小自有装配 | 业务服务直接使用 Boot/gRPC/Seata 原生 API，无学习成本；自有代码仅补 Seata gRPC 透传注册 |
| gRPC 版本随 Boot 托管 | spring-grpc 1.1 起自动配置归属 Boot 4.1，自管 bom 会与托管版本冲突 |
| Nacos Client 保留但不启用 | 去注册中心一期目标；保留客户端为二期 gRPC NameResolver 与配置中心恢复铺路 |
| **未实现** 服务注册发现 | 一期东西向静态地址直连；二期基于 nacos-client 自写 gRPC NameResolver |
| **未实现** 服务熔断/降级 | 当前服务规模小；gRPC deadline/重试已够用 |
| **未实现** 配置中心封装（Nacos Config） | 二期随注册发现一并恢复 |
| **未实现** 链路追踪 SDK | 由 common-observability 通过 OTel Agent 零侵入方式覆盖 |
| **未实现** 灰度发布/流量染色 | 由 Higress 网关层路由规则实现，不需要 SDK 级支持 |

## 依赖关系

```
common-cloud → common-exception（BusinessException + REST/gRPC 双通道异常处理）
             → spring-boot-starter-grpc-server
             → spring-boot-starter-grpc-client
             → springdoc-openapi-starter-webmvc-ui
             → nacos-client
             → seata-spring-boot-starter
             → seata-grpc
             → spring-boot-autoconfigure + spring-grpc-core（SeataGrpcAutoConfiguration 编译依赖）
```
