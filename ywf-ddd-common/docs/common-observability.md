# common-observability

运维可观测性 —— 结构化日志 + Actuator 健康检查/指标 + Prometheus 抓取端点。

## 定位

为所有微服务提供开箱即用的可观测性基础：结构化日志、健康检查、指标暴露。
面向需要容器化部署并接入监控体系的服务。
引入即生效，日志格式通过 Profile 自动切换，链路追踪由部署时 OTel Agent 挂载。

## 设计原则

- **零配置日志**：`logback-spring.xml` 打包在 jar 内，业务模块无需再定义
- **stdout 唯一输出**：容器化部署统一走 stdout + 日志采集，不落盘文件
- **Agent 零侵入追踪**：链路追踪通过 OTel Java Agent 部署时挂载，不引入 Maven 依赖

## 核心功能

### 能力总览

| 能力 | 实现方式 |
|------|---------|
| 结构化日志 | `logback-spring.xml`（jar 内自动继承） |
| 健康检查 / 指标暴露 | Spring Boot Actuator |
| Prometheus 抓取 | Micrometer Prometheus Registry |
| 链路追踪 | OTel Java Agent 零侵入（非 Maven 依赖，部署时挂载） |

### 日志分档策略

通过 `spring.profiles.active` 自动切换：

| Profile | 格式 | 业务日志级别 | 适用场景 |
|---------|------|:-----------:|--------|
| dev | 控制台彩色 | DEBUG | 本地开发 |
| test | 控制台彩色 | INFO | 测试环境容器 |
| prod | 控制台 JSON | INFO | 生产，对接日志采集 |
| 其他 | 控制台彩色 | INFO | 兖底 |

接入采集系统（SLS / Loki / ELK）只需配置 logging-driver 或 DaemonSet，应用侧零改动。

### logback-spring.xml 逐项解析

| 配置项 | 内容 |
|--------|------|
| MDC 字段 | `trace_id` / `span_id`（OTel Agent 自动注入，未挂 Agent 时显示空串） |
| JSON 字段（prod） | `ts`, `lvl`, `app`, `trace_id`, `span_id`, `thread`, `logger`, `msg`, `ex` |
| Pattern（dev/test） | 彩色人类可读：时间 + 级别 + [app,trace_id,span_id] + [thread] + logger + msg |
| Logger 覆盖 | `com.yoursweakfoe`=DEBUG(dev)/INFO；`io.grpc`=WARN；`org.apache.seata`=WARN；`org.mybatis`=WARN/ERROR(prod) |
| Root level | INFO（所有 profile） |
| Appender | 唯一 CONSOLE（ConsoleAppender），stdout 输出，无文件落盘 |
| Profile 匹配 | `<springProfile name="dev">` / `"test"` / `"prod"` / `"!dev & !test & !prod"`（兖底） |
| 其他 | `scan=true` 60s 热加载；LevelChangePropagator JUL→SLF4J 联动；include Spring Boot defaults.xml |

### 链路追踪（OTel 零侵入）

- 部署时挂载 Agent：`-javaagent:/app/otel-javaagent.jar`
- Agent 自动向 SLF4J MDC 注入 `trace_id` / `span_id`
- 未挂 Agent 时 MDC 为空，日志 pattern 兜底显示空串
- 对接 Jaeger / Tempo / Datadog 只需设 `OTEL_EXPORTER_OTLP_ENDPOINT`

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-observability</artifactId>
</dependency>
```

引入即生效。日志格式随 Profile 自动切换，Actuator 端点自动暴露。

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| stdout 输出，不落盘文件 | 容器化部署统一走日志采集；文件落盘增加运维复杂度且不利于弹性扩缩 |
| OTel Agent 而非 SDK | 零代码侵入；未挂 Agent 时无任何副作用 |
| **未实现** 自定义业务指标埋点 API | Micrometer `MeterRegistry` 已由 Actuator 自动注入，业务代码直接 `@Autowired` 使用即可 |
| **未实现** 告警规则（AlertManager） | 告警属于运维基础设施（Prometheus/Grafana 侧配置），不属于应用 SDK 职责 |
| **未实现** 慢 SQL 监控（P6Spy/Druid） | MyBatis-Plus 自带 SQL 日志（dev）；生产慢查询由 PG `pg_stat_statements` 覆盖 |
| **未实现** APM Dashboard | 由 OTel + Grafana/Jaeger 基础设施承载，不在 SDK 中内置 |

## 依赖关系

```
common-observability（独立，无内部模块依赖）
├── spring-boot-starter-actuator
└── micrometer-registry-prometheus
```
