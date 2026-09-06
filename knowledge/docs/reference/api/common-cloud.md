# common-cloud

微服务治理聚合包 —— Nacos + Seata + Spring Cloud 官方（Feign / LoadBalancer / CircuitBreaker）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

聚合 pom + 东西向 JWT 身份传播（内置 Feign RequestInterceptor）。**所有依赖均为 optional opt-in**——本模块自身代码（`JwtPropagationRequestInterceptor` + `FeignJwtPropagationAutoConfiguration` + 配置 record）仅依赖 Feign 接口与 common-security（`Jwt`）类型，自动装配入口经 `@ConditionalOnClass`（Feign 与 JWT 类均在位）门控，缺依赖时静默不激活。Feign、JWT 透传、Nacos、Seata、LoadBalancer、CircuitBreaker **消费方用到哪个就显式声明哪个**（见 §3.1「能力 → 需显式声明的依赖」清单）。面向使用分布式事务与东西向 HTTP 调用的微服务。

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

引入 common-cloud 后，所有能力均按需显式声明依赖后自动装配（本模块编译期依赖 Feign/starter 与 common-security，但不传递——消费方需要用到的能力自行声明）：

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>

<!-- 按需：声明式 HTTP -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- 按需：东西向身份传播所依赖的身份上下文 -->
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-security</artifactId>
</dependency>
<!-- 按需：分布式事务 -->
<dependency>
    <groupId>org.apache.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

### 3.1 能力 → 需显式声明的依赖

| 能力 | 需显式声明的依赖（坐标） | 启用前提 |
|------|------------------------|---------|
| Feign（声明式 HTTP） | `org.springframework.cloud:spring-cloud-starter-openfeign` | 启动类标注 `@EnableFeignClients` |
| 东西向 JWT 身份传播 | `spring-cloud-starter-openfeign` + `com.yoursweakfoe:common-security` | 自动装配，可经 `ywf.cloud.feign.jwt.*` 配置 |
| 服务注册发现 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery` | Nacos Server 地址 |
| 配置中心 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config` | Nacos Server + `spring.config.import=nacos:` |
| 分布式事务 | `org.apache.seata:seata-spring-boot-starter` | Seata Server + 事务分组（TC 不可达 fail-fast） |
| 客户端负载均衡 | `org.springframework.cloud:spring-cloud-starter-loadbalancer` | 服务端多实例 |
| 熔断 / 降级 | `org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j` | 规则经 `resilience4j.*` 配置 |

### 3.2 最小配置样例

```yaml
spring:
  application:
    name: service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
      config:
        server-addr: ${NACOS_SERVER:127.0.0.1:8848}
  config:
    import: nacos:service.yaml?group=DEFAULT_GROUP

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
common-cloud → spring-cloud-starter-openfeign                    （optional）
             → common-security                                   （optional，JWT 身份传播）
             → spring-cloud-starter-alibaba-nacos-discovery      （optional）
             → spring-cloud-starter-alibaba-nacos-config         （optional）
             → seata-spring-boot-starter                         （optional）
             → spring-cloud-starter-loadbalancer                 （optional）
             → spring-cloud-starter-circuitbreaker-resilience4j  （optional）
```

> 依赖树以 `mvn dependency:tree` 为准，本清单可能滞后。**全部 optional 依赖不传递**——消费方用到相应能力时须自行显式声明（门控机制见 §1）。
> Spring Cloud 组件版本由 spring-cloud-dependencies BOM 管理；SCA 组件版本由 spring-cloud-alibaba-dependencies BOM 管理；nacos-client / seata 版本独立声明覆盖 SCA BOM。**BOM 不传递**，消费方需自行 import 对齐版本。

## 5. 设计原则

- **全 optional opt-in**：所有依赖不传递，消费方用到哪个能力就显式声明哪个（`@ConditionalOnClass` 门控细节见 §1）
- **聚合不封装**：直接传递官方 starter，业务服务直接使用原生 API
- **统一 HTTP**：对外 REST 经 Higress 网关；东西向一期为 RestClient 直连（静态地址），Feign 经 common-cloud opt-in（JWT RequestInterceptor 自动透传）
- **零信任东西向**：服务间调用透传已验签 JWT，下游自验签，不靠内网可信

## 6. 设计决策（已迁出）

> 本模块全部决策日志已迁至 [`knowledge/decisions/`](../../../decisions/README.md)（全局编号 ADR-NNNN；旧号映射见该文 §migration）。归属法：判例住卷宗，地图只留指针——本区不再维护决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：入口限流 | 由 Higress 网关承担 |
| 边界：链路追踪 | 由 common-observability 经 OTel Agent 覆盖 |
| 边界：灰度发布 / 流量染色 | 由 Higress 网关层路由规则实现 |
| 技术债：netty-all 体积 | seata-all 传递引入全量 netty 模块（mqtt/redis/http3 等）。**不可排除**：seata TM 客户端（NettyClientBootstrap）直接引用 `io.netty.*`，排除后运行时 NoClassDefFoundError，TM 无法注册 TC |
