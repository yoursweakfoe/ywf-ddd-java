# common-cloud

微服务治理聚合包 —— Nacos + Seata + Spring Cloud 官方（Feign / LoadBalancer / CircuitBreaker）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

聚合 pom + 东西向 JWT 身份传播（内置 Feign RequestInterceptor），引入即获得服务注册发现、配置中心、分布式事务、声明式 HTTP 调用（含 JWT 透传）、客户端负载均衡、熔断降级能力。面向使用分布式事务与东西向 HTTP 调用的微服务。

> 统一异常体系（common-exception）不在本包分发，由业务服务按需显式引入。

## 2. 核心能力

| 组件 | 职责 |
|------|------|
| Nacos（SCA discovery + config starter） | 服务注册发现 / 配置中心（Spring Cloud 自动装配） |
| Seata | AT/TCC 分布式事务（跨服务最终一致性） |
| Feign | 声明式 HTTP 客户端（东西向服务调用） |
| JWT 身份传播 | Feign RequestInterceptor 把当前已验签 JWT 透传下游（零信任东西向） |
| LoadBalancer | 客户端负载均衡（实例来源：Nacos 注册发现） |
| CircuitBreaker (Resilience4J) | 客户端熔断 / 降级 |

## 3. 使用方式

引入依赖即生效：

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```

### 3.1 启用前提

| 组件 | 前提 |
|------|------|
| Nacos | Nacos Server 地址（注册发现 + 配置中心） |
| Seata | Seata Server 地址 + 事务分组（TC 不可达时启动 fail-fast；测试环境 `seata.enabled: false`） |
| Feign | 启动类标注 `@EnableFeignClients` |
| JWT 身份传播 | 引入 common-cloud 即生效（自动装配 `JwtPropagationRequestInterceptor`），无需额外配置 |
| LoadBalancer | 服务端多实例（单实例直连时无需） |
| CircuitBreaker | 无（默认装配；规则经 `resilience4j.*` 配置） |

### 3.2 最小配置样例

```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
      config:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
  config:
    import: nacos:order-service.yaml?group=DEFAULT_GROUP

seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
    grouplist:
      default: ${SEATA_SERVER:127.0.0.1:8091}

resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 20
```

## 4. 依赖关系

```
common-cloud → spring-cloud-starter-alibaba-nacos-discovery
             → spring-cloud-starter-alibaba-nacos-config
             → seata-spring-boot-starter
             → spring-cloud-starter-openfeign
             → spring-cloud-starter-loadbalancer
             → spring-cloud-starter-circuitbreaker-resilience4j
             → common-security（JWT 身份传播）
```

> 依赖树以 `mvn dependency:tree` 为准，本清单可能滞后。
> Spring Cloud 组件版本由 spring-cloud-dependencies BOM 管理；SCA 组件版本由 spring-cloud-alibaba-dependencies BOM 管理；nacos-client / seata 版本独立声明覆盖 SCA BOM。**BOM 不传递**，消费方需自行 import 对齐版本。

## 5. 设计原则

- **引入即生效**：Seata / Feign / Nacos / CircuitBreaker 经 starter 自动装配
- **聚合不封装**：直接传递官方 starter，业务服务直接使用原生 API
- **统一 HTTP**：对外 REST 经 Higress 网关，东西向 Feign 直连
- **零信任东西向**：服务间调用透传已验签 JWT，下游自验签，不靠内网可信

## 6. 设计决策

### ADR-0001 东西向通信统一 HTTP（移除 gRPC）

- 状态：accepted

**背景**：迁移计划原定移除 Dubbo 后东西向走 Spring gRPC，后全仓移除 gRPC、统一 HTTP。

**选项**：
- gRPC：强类型编译期绑定，但内部接口量少、收益有限
- HTTP（Feign / RestClient）：契约简单、REST 端点复用

**决策**：选 HTTP。内部接口量少，REST 端点可复用，简化契约与安全模型。

**后果**： 
- 好：统一协议可以显著减少适配代码（约1000行）。
- 坏：失去 gRPC 强类型与二进制性能；Feign 接口即契约。

**确认**：`common-cloud/pom.xml` 引入 openfeign 且无 grpc 构件。

### ADR-0002 熔断降级用 Resilience4J 而非 Sentinel

- 状态：accepted

**背景**：客户端熔断降级需求；引入 SCA 后存在 Sentinel（SCA 体系）与 Resilience4J（SC 官方）两个选项。

**决策驱动因素**：
- 功能边界：Sentinel 的不可替代能力（系统自适应保护 / 热点参数限流 / 集群流控 / 流量整形 / Dashboard）面向大流量治理场景；本项目仅需东西向客户端熔断，入口限流已由 Higress 承担、监控已由 common-observability 覆盖
- 代码侵入：Sentinel 需资源定义 + blockHandler/fallback 双处理；Resilience4J 一套 fallback、可纯配置驱动（零注解也可）
- 维护状态：Sentinel v1.8.8（2024-06）后近两年无实质更新、2.0 仍 alpha、Boot 4 适配靠 SCA 补丁；Resilience4J 随 SC 2025.1.x 同步发布、活跃维护

**选项**：
- Sentinel（SCA）：SCA 绑定 1.8.9，流量治理平台定位
- Resilience4J（SC 官方）：轻量容错库定位

**决策**：选 Resilience4J（经 SC CircuitBreaker 抽象）。即便引入 SCA，也不启用 Sentinel。

**后果**：
- 好：侵入性低、活跃维护、Boot 4 原生适配
- 坏：失去 Sentinel 的系统保护 / 热点流控 / Dashboard（本项目不需要）

**确认**：pom 引入 `spring-cloud-starter-circuitbreaker-resilience4j`，无 sentinel 构件。

### ADR-0003 Nacos 经 SCA starter 引入，client 版本独立管理

- 状态：accepted

**背景**：需要 Nacos 服务注册发现 + 配置中心的 Spring Cloud 自动装配。裸 nacos-client 仅 SDK 无自动装配。

**选项**：
- 裸 nacos-client：编程式 SDK，需自写 DiscoveryClient / 配置注入
- SCA nacos-discovery + nacos-config starter：开箱即用（DiscoveryClient + spring.config.import）

**决策**：选 SCA starter（仅 discovery + config 两个构件），nacos-client 版本独立管理（3.2.3 覆盖 SCA BOM 的 3.1.1）。

**后果**：
- 好：LoadBalancer 自动从 Nacos 取实例；spring.config.import 声明式配置
- 坏：引入 SCA 构件（但仅 Nacos 集成，不带 Sentinel / 全家桶）

**确认**：pom 引入 nacos-discovery + nacos-config；父 DM 声明 nacos-client 3.2.3。

### ADR-0004 Seata 独立构件 + 版本独立管理

- 状态：accepted

**背景**：分布式事务需求。SCA 绑定 seata 2.5.0，落后于官方 2.6.0。

**选项**：
- 经 SCA 引入：版本绑 SCA 发布节奏
- 独立引入 seata-spring-boot-starter：版本独立管理

**决策**：选独立引入，版本独立管理 2.6.0（覆盖 SCA BOM 的 2.5.0）。

**后果**：Seata 升级不受 SCA 发布节奏约束。

**确认**：pom 引入 `org.apache.seata:seata-spring-boot-starter`；父 DM 声明 2.6.0。

### ADR-0005 Seata XID 透传不内置

- 状态：accepted

**背景**：分布式事务需 XID 跨服务透传。

**选项**：
- 内置到 common-cloud
- 不内置，配方沉淀到 cookbook

**决策**：选不内置。HTTP 统一后透传为出站 interceptor + 入站 Filter（TX_XID header），模式未稳定，先以配方形式沉淀。

**后果**：业务方需按配方自行接入；框架保持薄。

**确认**：common-cloud 无 XID 透传代码；配方见 sample-application cookbook distributed-transaction.md。

### ADR-0006 东西向身份传播：透传已验签 JWT（零信任）

- 状态：accepted

**背景**：零信任下东西向（服务间）调用不能靠内网可信，也不能靠网关注入身份 Header。

**选项**：
- mTLS / SPIFFE：传输层双向认证，需服务网格或证书体系，重
- JWT 透传：把当前线程已验签的 JWT 原样带上 `Authorization: Bearer` 透传，下游作为资源服务器自验签

**决策**：选 JWT 透传。Feign `RequestInterceptor` 读取 SecurityContext 里的 `Jwt`，注入 `Authorization: Bearer`；下游用 common-security 自验签。无需服务网格，契合现有 Feign 栈。

**后果**：
- 好：零额外基础设施；用户身份跨服务连续可审计；下游自验不信任网络
- 坏：机器身份（定时任务 / MQ 无用户上下文）需另行走 client-credentials；长链透传同一 token，受众 / scope 限制留待演进

**确认**：`common-cloud` 内置 `JwtPropagationRequestInterceptor` + `FeignJwtPropagationAutoConfiguration`。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：入口限流 | 由 Higress 网关承担 |
| 边界：链路追踪 | 由 common-observability 经 OTel Agent 覆盖 |
| 边界：灰度发布 / 流量染色 | 由 Higress 网关层路由规则实现 |
| 技术债：netty-all 体积 | seata-all 传递引入全量 netty 模块（mqtt/redis/http3 等）。**不可排除**：seata TM 客户端（NettyClientBootstrap）直接引用 `io.netty.*`，排除后运行时 NoClassDefFoundError，TM 无法注册 TC |
