# common-contract

CQRS 契约标记接口：Command / Query / PageableQuery / IntegrationEvent。它是纯类型契约层，供消费方的 contract jar 与服务端共享。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

契约层的公共构建块：纯标记接口 + `jakarta.validation-api` 校验注解 + `swagger-annotations` 文档注解 + `spring-web` HTTP 映射注解，不含任何运行时逻辑。业务服务的 `xxx-contract` 模块引入本包后，Command / Query 对象就能被基础设施层统一识别和拦截，契约接口就能声明完整的 REST 契约。需要定义 CQRS 请求对象或 REST 契约的模块都应引入本包。

> 一个契约接口就是一份完整的 REST 定义：HTTP 映射、文档注解、类型都在接口上。注解全是纯元数据，服务端 Spring MVC 和消费方各自解释，本包自身没有运行时行为。

### 包结构

标记接口按 CQE 类型各放一个子包。包层级与业务 contract 包的 `dto/` 镜像对偶：业务的 `dto/command/XxxCommand` 实现本包的 `dto/command/Command`，其余类推。

```
com.yoursweakfoe.common.contract
└── dto/
    ├── command/
    │   └── Command.java
    ├── query/
    │   ├── Query.java
    │   ├── PageableQuery.java
    │   └── PageResult.java
    ├── co/
    │   └── CO.java
    └── event/
        └── IntegrationEvent.java
```

> 包结构是扁平的，集成事件契约直接位于 `dto/event/` 下。包命名规则的论证 → [architecture-rules.md](../../explanation/architecture-rules.md)「保留段唯一语义」节。

## 2. 核心能力

### 标记接口语义

| 接口 | 语义 | 命名约定 | 基础设施拦截点 |
|------|------|----------|---------------|
| `Command` | 「请做这件事」，变更系统状态 | `XxxCommand` | 事务、审计日志、幂等校验 |
| `Query` | 「请给我这个」，读取数据 | `XxxQuery` | 只读路由、缓存、权限校验 |
| `PageableQuery` | 「给我一页」，分页读取 | `GetXxxPageQuery` | 同 Query，外加分页参数约束 |
| `IntegrationEvent` | 「已经发生、需跨服务协作」，跨边界事件契约 | `XxxIntegrationEvent` | —（传输通道是业务自持的消息中间件，框架不拦截） |

### PageableQuery API

| 成员 | 类型 | 说明 |
|------|------|------|
| `DEFAULT_PAGE_SIZE` | `int = 20` | 每页大小建议值。框架不注入缺省值，用不用由消费方决定 |
| `MAX_PAGE_SIZE` | `int = 1000` | 每页最大条数上限 |
| `pageNum()` | 抽象方法 | 当前页码，原始值；record 组件 `int pageNum` 天然实现 |
| `pageSize()` | 抽象方法 | 每页大小，原始值，是批量导出的逃生门；record 组件 `int pageSize` 天然实现 |
| `safePageNum()` | `default int` | 防御性页码，下限钳制为 1，读侧推荐入口 |
| `safePageSize()` | `default int` | 防御性每页大小，钳制到 `1..MAX_PAGE_SIZE`，读侧推荐入口 |

> **record 优先**：record 组件名 `pageNum`、`pageSize` 与抽象方法签名一致，业务 record 实现本接口零覆写。读侧仓储统一调 `safe*()` 拿钳制值，非法入参也造不出非法分页。

## 3. 使用方式

> **严格规范在法卷**：本节正文已升入法卷 → [../../../specs/current/modules/contract.md](../../../specs/current/modules/contract.md)。条款、代码形状、禁令以法卷为准，本字典条目只留面向人的宽松指引。

## 4. 依赖关系

```
common-contract（独立，无内部模块依赖）
├── jakarta.validation-api（纯 API，仅约束声明，无实现）
├── swagger-annotations（文档注解，纯注解 jar，零运行时零端点）
└── spring-web（HTTP 映射注解，@GetMapping/@RequestMapping 等）
```

> **声明与执行分离**：本包只声明校验约束（注解），不执行校验；`jakarta.validation-api` 是纯 API jar，无实现。校验实现（Hibernate Validator）由服务端提供：通常经 `common-exception` → `spring-boot-starter-validation` 的 compile 传递自动获得；若服务只引入本包，则需自行引入 `spring-boot-starter-validation`。校验在服务端 HTTP 绑定期触发，入口是契约接口上声明的 `@Valid`（Controller 实现继承，见 §3.1）。

## 5. 设计原则

- **纯标记接口**：不含泛型、不含基类、不含任何实现逻辑
- **record 友好**：标记接口可被 record 实现，不强制继承关系
- **零运行时负担**：本模块不引入任何实现逻辑。依赖全是注解：`jakarta.validation-api`、`swagger-annotations` 是纯注解，`spring-web` 提供 HTTP 映射注解

## 6. 设计决策（已迁出）

> 本模块的历史决策日志已随案卷清理归零，旧编号制已废。当时的裁决快照在封存案卷 §裁决记录，今天仍成立的论证在 `docs/explanation/`。地图只留指针，本区不维护决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：CQE 基类 / 抽象类 | 标记接口已够用。基类会强制继承关系，与 record 不兼容 |
| 边界：校验执行 | 契约层只声明约束，校验是纯 API。Hibernate Validator 实现由服务端提供：经 common-exception 传递，或显式引入。触发点在 Controller 绑定期，走契约接口上声明的 `@Valid`；Handler 层不做 Bean Validation |
