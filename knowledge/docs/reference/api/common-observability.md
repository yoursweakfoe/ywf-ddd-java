# common-observability

运维可观测性 —— 结构化日志 + Actuator 健康检查/指标 + Prometheus 抓取端点。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为所有微服务提供开箱即用的可观测性基础：结构化日志、健康检查、指标暴露。面向需要容器化部署并接入监控体系的服务。Actuator 与 Prometheus 端点引入即生效；结构化日志靠一行配置启用，即 `logging.structured.format.console=logstash`；链路追踪由部署时挂载 OTel Agent 获得。

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

库不 ship `logback-spring.xml`，库内放置日志配置属反模式：业务 app 无法用同名文件覆盖它，且 `<springProfile>` 仅在 Spring Boot 日志系统下生效。改用 Spring Boot 内置结构化日志：

| Profile | 配置 | 格式 |
|---------|------|------|
| prod | `logging.structured.format.console=logstash` | JSON（自动输出 MDC，含 `trace_id`/`span_id`） |
| dev/test | 默认彩色（或自定义 `logging.pattern.console`） | 人类可读 |

OTel Java Agent 自动向 SLF4J MDC 注入 `trace_id` / `span_id` / `trace_flags`。logstash 格式会把这些 MDC key 直接输出为 JSON 字段，对接 ELK / SLS 不需要额外映射。若需 ECS 格式，即 `trace.id`/`span.id` 这种带点的字段名，可设 `logging.structured.format.console=ecs` 并自定义 `StructuredLogFormatter` 做映射。

### 链路追踪（OTel 零侵入）

- 部署时挂载 Agent：`-javaagent:/app/otel-javaagent.jar`
- Agent 自动向 SLF4J MDC 注入 `trace_id` / `span_id`
- 对接 Jaeger / Tempo / Datadog 只需设 `OTEL_EXPORTER_OTLP_ENDPOINT`

## 3. 使用方式

> **严格规范在法卷**：本节正文已入法，见 [../../../specs/current/modules/observability.md](../../../specs/current/modules/observability.md)。条款、代码形状、禁则以法卷为准，本文只留宽松指引。

## 4. 依赖关系

```
common-observability（独立，无内部模块依赖）
├── spring-boot-starter-actuator
└── micrometer-registry-prometheus
```

## 5. 设计原则

- **库不 ship logback 配置**：结构化日志走 Spring Boot 内置能力，业务 app 配一行 `logging.structured.format.console=logstash` 即可
- **stdout 唯一输出**：容器化部署统一走 stdout + 日志采集，不落盘文件
- **Agent 零侵入追踪**：链路追踪通过 OTel Java Agent 部署时挂载，不引入 Maven 依赖

## 6. 设计决策（已迁出）

> 本模块的历史决策日志已随案卷到期清册删除，本区不再维护。旧编号制已废，无编号可查。当时的裁决看封存案卷 §裁决记录；今天仍成立的论证看 `docs/explanation/` 同题篇。本文只留指针，不留决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：自定义业务指标埋点 API | Micrometer `MeterRegistry` 已由 Actuator 自动注入，业务代码直接 `@Autowired` 使用 |
| 边界：告警规则（AlertManager） | 属于 Prometheus/Grafana 侧配置，非应用 SDK 职责 |
| 边界：慢 SQL 监控（P6Spy/Druid） | MyBatis `log-impl: Slf4jImpl` 输出全部执行 SQL 到日志（dev）；生产由 PG `pg_stat_statements` 覆盖 |
| 边界：APM Dashboard | 由 OTel + Grafana/Jaeger 基础设施承载 |
