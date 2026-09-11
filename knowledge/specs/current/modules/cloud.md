# 模块用法法卷：common-cloud（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-cloud.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。

---

引入 common-cloud 后，能力按需启用：要用哪项能力，就显式声明哪项依赖，声明后自动装配。本模块编译期依赖 Feign starter 与 common-security，但不向消费方传递。消费方要用到的能力，自行声明对应依赖：

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
| 东西向 JWT 身份传播 | `spring-cloud-starter-openfeign` + `com.yoursweakfoe:common-security` | 自动装配；可经 `ywf.cloud.feign.jwt.*` 配置 |
| 服务注册发现 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery` | Nacos Server 地址 |
| 配置中心 | `com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-config` | Nacos Server + `spring.config.import=nacos:` |
| 分布式事务 | `org.apache.seata:seata-spring-boot-starter` | Seata Server + 事务分组；TC 不可达时 fail-fast |
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
