# common-contract

CQRS 契约标记接口（Command / Query / PageableQuery / IntegrationEvent）—— 供 contract jar 与服务端共享的纯类型契约层。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

契约层公共构建块，含纯标记接口 + `jakarta.validation-api` 校验注解 + `swagger-annotations` 文档注解 + `spring-web` HTTP 映射注解，零运行时逻辑。业务服务的 `xxx-contract` 模块引入本包后，Command / Query 对象即可被基础设施层统一识别与拦截，契约接口即可声明完整 REST 契约。任何需要定义 CQRS 请求对象或 REST 契约的模块都应引入。

> 契约 = 完整 REST 定义（HTTP 映射 + 文档注解 + 类型一体），但零运行时：注解均为纯元数据，由服务端 Spring MVC 与消费方各自解释。

### 包结构

标记接口按 CQE 类型分各自子包，与业务 contract 包的 `dto/` 层级镜像对偶（业务 `dto/command/XxxCommand` implements 抽取 `dto/command/Command`，以此类推）：

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

> 包结构为扁平真值：集成事件契约直接位于 `dto/event/` 下——2026-09-05 包命名税迁移前此处曾多一层 `integration/` 嵌套子包，现已消除（迁移坐标对照见 `DddArchitectureRules` 类头）。

## 2. 核心能力

### 标记接口语义

| 接口 | 语义 | 命名约定 | 基础设施拦截点 |
|------|------|----------|---------------|
| `Command` | 「请做这件事」— 变更系统状态 | `XxxCommand` | 事务、审计日志、幂等校验 |
| `Query` | 「请给我这个」— 读取数据 | `XxxQuery` | 只读路由、缓存、权限校验 |
| `PageableQuery` | 「给我一页」— 分页读取 | `GetXxxPageQuery` | 同 Query + 分页参数约束 |
| `IntegrationEvent` | 「已经发生、需跨服务协作」— 跨边界事件契约 | `XxxIntegrationEvent` | —（传输通道为业务自持消息中间件，框架不拦截） |

### PageableQuery API

| 成员 | 类型 | 说明 |
|------|------|------|
| `DEFAULT_PAGE_SIZE` | `int = 20` | 每页大小建议值（框架不代注入缺省，使用与否属消费方） |
| `MAX_PAGE_SIZE` | `int = 1000` | 每页最大条数上限 |
| `pageNum()` | 抽象方法 | 当前页码（原始值）—— record 组件 `int pageNum` 天然实现 |
| `pageSize()` | 抽象方法 | 每页大小（原始值，批量导出逃生门）—— record 组件 `int pageSize` 天然实现 |
| `safePageNum()` | `default int` | 防御性页码：下限钳制为 1（读侧推荐消费入口） |
| `safePageSize()` | `default int` | 防御性每页大小：钳制到 `1..MAX_PAGE_SIZE`（读侧推荐消费入口） |

> **record 优先**：组件名 `pageNum`/`pageSize` 与抽象方法同签名，业务 record 实现本接口**零覆写**。读侧仓储统一消费 `safe*()` 取钳制值；未经校验的非法参数也不会产生非法分页。

## 3. 使用方式

> **严格规范在法卷**：本节正文已入法 → [../../../specs/current/modules/
contract
.md](../../../specs/current/modules/
contract
.md)（条款、代码形状、禁则以法卷为准）。本字典架只余宽松语感。

## 4. 依赖关系

```
common-contract（独立，无内部模块依赖）
├── jakarta.validation-api（纯 API，仅约束声明，无实现）
├── swagger-annotations（文档注解，纯注解 jar，零运行时零端点）
└── spring-web（HTTP 映射注解，@GetMapping/@RequestMapping 等）
```

> **声明与执行分离**：本包只声明校验约束（注解），不执行校验。`jakarta.validation-api` 是纯 API jar（无实现）。校验实现（Hibernate Validator）由服务端提供：通常经 `common-exception` → `spring-boot-starter-validation`（compile 传递）自动获得；若服务仅引入本包，则需自行引入 `spring-boot-starter-validation`。校验经契约接口上声明的 `@Valid` 在服务端 HTTP 绑定期触发（Controller 实现继承，见 §3.1）。

## 5. 设计原则

- **纯标记接口**：不含泛型、不含基类、不含任何实现逻辑
- **record 友好**：标记接口可被 record 实现，不强制继承关系
- **零运行时负担**：本模块不引入任何实现逻辑；依赖均为注解（jakarta.validation-api / swagger-annotations 为纯注解，spring-web 为 HTTP 映射注解来源）

## 6. 设计决策（已迁出）

> 本模块全部决策日志已迁至 [`knowledge/decisions/`](../../../decisions/README.md)（全局编号 ADR-NNNN；旧号映射见该文 §migration）。归属法：判例住卷宗，地图只留指针——本区不再维护决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：CQE 基类 / 抽象类 | 标记接口足够；基类强制继承关系，与 record 不兼容 |
| 边界：校验执行 | 契约层只声明约束（纯 API）；Hibernate Validator 实现由服务端提供（经 common-exception 传递或显式引入），经契约接口上声明的 `@Valid` 在 Controller 绑定期触发（Handler 层不做 Bean Validation） |
