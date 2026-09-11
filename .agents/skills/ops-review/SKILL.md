---
name: ops-review
description: 运维/SRE 视角审查代码变更（容器化、可观测性、优雅停机、资源限制、环境变量）。当需要生产就绪审查、部署前检查、或评审 Docker/K8s/配置变更时使用。
---

# 运维审查

## Role

以运维/SRE 视角检视代码变更，确保容器化、可观测性、优雅停机、资源限制、环境变量管理符合生产就绪标准。
审查结果分级见「输出格式」（FAIL / WARN / PASS）。

## 前置阅读

法条在卷，本清单只提问不复述（D6）：

- 可观测法卷 `knowledge/specs/current/modules/observability.md`（宽松件 `knowledge/docs/reference/api/common-observability.md` §2：MDC 键、OTel 挂法细节）
- 云原生能力表 `knowledge/specs/current/modules/cloud.md` §3.1（能力 → 显式依赖 → 启用前提：Nacos / Seata / 熔断）
- 条款编号对照：`knowledge/specs/current/patterns/discipline/prohibitions.md` §4 §7（System.out / synchronized）、`knowledge/specs/current/patterns/chain/batch-write.md` BW-5（批量体量）、`knowledge/specs/current/patterns/boundary/external-gateway.md` GW-3（超时注入）、`knowledge/specs/current/patterns/chain/scheduler.md` SC-4（调度幂等）

## 审查维度

### 1. 容器化

- Dockerfile 是否有 HEALTHCHECK 指令？探针地址在激活 profile 下是否可达（Actuator 改独立管理端口后业务端口探测会 404）？
- JVM 参数是否容器感知（-XX:MaxRAMPercentage，非硬编码 -Xmx）？
- 基础镜像是否固定版本（非 latest）？
- COPY 指令是否利用 Docker 层缓存（依赖先于源码）？
- 是否暴露了正确的端口（EXPOSE 与 application.yml server.port 一致）？

### 2. 健康检查与优雅停机

- Actuator /actuator/health 是否可达（server.port + context-path + 管理端口三处合查；真实例 `sample-application/sample-service/sample-service-server/src/main/resources/application.yml`）？
- server.shutdown=graceful 是否配置？
- spring.lifecycle.timeout-per-shutdown-phase 是否 ≤ Docker stop_grace_period（真实例：30s ≤ 40s，`sample-application/docker-compose.yml`）？
- prod 是否收紧 Actuator（独立管理端口 + 仅 loopback + show-details=never，真实例 `sample-application/sample-service/sample-service-server/src/main/resources/application-prod.yml`）？

### 3. 可观测性

- server 模块是否引入 common-observability 依赖（引入即生效 → observability 法卷，真实例 `sample-application/sample-service/sample-service-server/pom.xml`）？
- 日志是否全走 SLF4J → Logback（System.out 替代 = 禁令卷 §4 违反）？
- trace_id / span_id 是否注入 MDC 且日志渲染位可读（OTel Agent 部署时挂载，机制 → common-observability §2）？
- 关键业务操作是否有 INFO 级日志（可审计）？
- 异常是否有足够上下文（ID / 参数 / 状态）？

### 4. 环境变量与配置

- 敏感凭证是否经环境变量注入（主配置占位符带默认值 = 漏配静默回落，真实例纪律：不带默认、缺失 fail-fast → `sample-application/sample-service/sample-service-server/src/main/resources/application.yml` 注释）？
- prod 配置是否收紧（不允许回退到 dev 弱默认）？
- docker-compose / K8s 中环境变量名是否与 application.yml ${...} 一致（真实例：DB_MASTER_* / SPRING_PROFILES_ACTIVE / OTEL_*）？
- 新增环境变量是否有归宿：IDE 本地（ide-dev.env，profile/OTel 模式）、dev 默认值（application-dev.yml）、容器注入（docker-compose）三处对齐？

### 5. 资源与稳定性

- 数据库连接池大小是否合理（HikariCP maximum-pool-size 对齐 DB 容量，虚拟线程解除入口上限 ≠ 放大池位）？
- 虚拟线程下是否存在 synchronized pinning（禁令卷 §7，互斥用 ReentrantLock；检测开关 -Djdk.tracePinnedThreads 真实例 Dockerfile 已带）？
- 批量写体量是否有管控（BW-5：调用方自行分片，建议值以法卷为准——框架刻意不设行数护栏）？
- 外部调用（Portal/Gateway）是否配超时（GW-3：策略参数经配置注入；熔断行 → cloud 卷 §3.1）？

### 6. 部署一致性

- docker-compose 服务定义是否与 Dockerfile 一致（build 上下文 / 端口 / 环境变量，真实例 `sample-application/docker-compose.yml`）？
- 多实例部署时定时任务是否幂等（SC-4：分布式锁防并发重入 + 业务状态守卫防重复效应，缺一不可）？
- 注册中心是否配置（cloud 卷 §3.1「服务注册发现」行：nacos-discovery 显式依赖 + Server 地址；真实例 compose 中 nacos 块为待启用注释）？

## 输出格式

```
PASS: N items
WARN: (上线前修复，逐条挂依据条款编号)
FAIL: (阻塞部署，逐条挂依据条款编号与违规位 file:line)
```
