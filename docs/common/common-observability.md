# common-observability

运维可观测性 —— 结构化日志 + Actuator 健康检查/指标 + Prometheus 抓取端点。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为所有微服务提供开箱即用的可观测性基础：结构化日志、健康检查、指标暴露。面向需要容器化部署并接入监控体系的服务。引入即生效（Actuator / Prometheus），结构化日志经一行配置启用（`logging.structured.format.console=logstash`），链路追踪由部署时 OTel Agent 挂载。

> 告警规则、APM Dashboard、慢 SQL 监控不在本包：属于运维基础设施（Prometheus/Grafana/PG 侧），非应用 SDK 职责。

## 2. 核心能力

### 能力总览

| 能力 | 实现方式 |
|------|---------|
| 结构化日志 | Spring Boot 内置（`logging.structured.format.console=logstash`，自动含 MDC） |
| 健康检查 / 指标暴露 | Spring Boot Actuator |
| Prometheus 抓取 | Micrometer Prometheus Registry |
| 链路追踪 | OTel Java Agent 零侵入（部署时挂载，非 Maven 依赖） |

### 结构化日志（Spring Boot 内置）

库不 ship `logback-spring.xml`（库内放置属反模式：业务 app 无法用同名文件覆盖，且 `<springProfile>` 仅 Spring Boot 日志系统下生效）。改用 Spring Boot 内置结构化日志：

| Profile | 配置 | 格式 |
|---------|------|------|
| prod | `logging.structured.format.console=logstash` | JSON（自动输出 MDC，含 `trace_id`/`span_id`） |
| dev/test | 默认彩色（或自定义 `logging.pattern.console`） | 人类可读 |

OTel Java Agent 自动向 SLF4J MDC 注入 `trace_id` / `span_id` / `trace_flags`；logstash 格式自动把这些 MDC key 作为 JSON 字段输出，对接 ELK / SLS 零额外映射。若需 ECS 格式（`trace.id`/`span.id` 带点语义），可设 `logging.structured.format.console=ecs` 并自定义 `StructuredLogFormatter` 映射。

### 链路追踪（OTel 零侵入）

- 部署时挂载 Agent：`-javaagent:/app/otel-javaagent.jar`
- Agent 自动向 SLF4J MDC 注入 `trace_id` / `span_id`
- 对接 Jaeger / Tempo / Datadog 只需设 `OTEL_EXPORTER_OTLP_ENDPOINT`

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-observability</artifactId>
</dependency>
```

引入即生效。日志格式随 Profile 自动切换，Actuator 端点自动暴露。接入采集系统（SLS / Loki / ELK）只需配置 logging-driver 或 DaemonSet，应用侧零改动。

## 4. 依赖关系

```
common-observability（独立，无内部模块依赖）
├── spring-boot-starter-actuator
└── micrometer-registry-prometheus
```

## 5. 设计原则

- **库不 ship logback 配置**：结构化日志走 Spring Boot 内置能力，业务 app 配 `logging.structured.format.console=logstash` 一行即可
- **stdout 唯一输出**：容器化部署统一走 stdout + 日志采集，不落盘文件
- **Agent 零侵入追踪**：链路追踪通过 OTel Java Agent 部署时挂载，不引入 Maven 依赖

## 6. 设计决策

### ADR-0001 stdout 输出，不落盘文件

- 状态：accepted

**背景**：日志输出到文件还是 stdout。

**决策**：选 stdout。容器化部署统一走日志采集；文件落盘增加运维复杂度且不利于弹性扩缩。

**确认**：结构化日志默认输出 console（Spring Boot 内置，无文件 Appender）。

### ADR-0002 OTel Agent 而非 SDK

- 状态：accepted

**背景**：链路追踪用 Agent 挂载还是引入 SDK 依赖。

**决策**：选 Agent 挂载。零代码侵入；未挂 Agent 时无任何副作用（MDC 兜底显示空串）。

**确认**：本包无任何 OTel Maven 依赖。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：自定义业务指标埋点 API | Micrometer `MeterRegistry` 已由 Actuator 自动注入，业务代码直接 `@Autowired` 使用 |
| 边界：告警规则（AlertManager） | 属于 Prometheus/Grafana 侧配置，非应用 SDK 职责 |
| 边界：慢 SQL 监控（P6Spy/Druid） | MyBatis-Plus 自带 SQL 日志（dev）；生产由 PG `pg_stat_statements` 覆盖 |
| 边界：APM Dashboard | 由 OTel + Grafana/Jaeger 基础设施承载 |
