# common-contract

CQRS 契约标记接口（Command / Query / Event）+ OpenAPI 文档注解 —— 供 contract jar 与服务端共享。

## 定位

最轻量的公共契约层，仅包含纯标记接口和注解依赖，零运行时逻辑。
业务服务的 `xxx-contract` 模块引入本包后，Command / Query / Event 对象即可被基础设施层统一识别和拦截。
任何需要定义 CQRS 请求对象的模块都应引入。

## 设计原则

- **纯标记接口**：不含泛型、不含基类、不含任何实现逻辑
- **record 友好**：标记接口可被 record 实现，不强制继承关系
- **零运行时负担**：所有依赖均为纯注解 jar，不引入任何实现代码

## 包结构

```
com.yoursweakfoe.common.contract
├── Command.java        ← 写操作意图标记（创建、修改、删除）
├── Query.java          ← 读操作请求标记（不改变系统状态）
├── PageableQuery.java  ← 分页查询标记（继承 Query，带 pageNum/pageSize + @Min/@Max 约束）
└── Event.java          ← 外部事件通知标记（MQ 消息、跨服务通知、Webhook）
```

## 核心功能

### 标记接口语义

| 接口 | 语义 | 命名约定 | 基础设施拦截点 |
|------|------|----------|---------------|
| `Command` | “请做这件事” — 变更系统状态 | `XxxCommand` | 事务、审计日志、幂等校验 |
| `Query` | “请给我这个” — 读取数据 | `XxxQuery` | 只读路由、缓存、权限校验 |
| `PageableQuery` | “给我一页” — 分页读取 | `GetXxxPageQuery` | 同 Query + 分页参数约束 |
| `Event` | “这件事发生了” — 外部事实通知 | `XxxEvent` | 消息确认、重试、死信 |

### PageableQuery 详细 API

| 成员 | 类型 | 说明 |
|------|------|------|
| `DEFAULT_PAGE_SIZE` | `int = 20` | 默认每页大小 |
| `MAX_PAGE_SIZE` | `int = 1000` | 每页最大条数上限 |
| `getPageNum()` | `default int` → 1 | 当前页码（从 1 开始），`@Min(1)` |
| `getPageSize()` | `default int` → 20 | 每页大小，`@Min(1) @Max(1000)` |

防御性说明：`@Min/@Max` 依赖调用点 `@Valid` 触发，未校验时不生效。框架层 `findDomainPage` 已内置防御性 clamp（pageNum≥1，1≤pageSize≤1000），保证即使未经校验的参数也不会产生非法分页行为。

### Event vs DomainEvent

| | Event（本模块） | DomainEvent（common-ddd） |
|---|---|---|
| 来源 | 外部进入（MQ / RPC / Webhook） | 领域内部产生（聚合根注册） |
| 发布方 | 外部系统 | 本进程 Spring Event |
| 处理入口 | `EventHandler<E>` | `@EventListener` / `@TransactionalEventListener` |

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-contract</artifactId>
</dependency>
```

引入即生效。业务 CQE 对象实现对应标记接口即可：

```java
public record PlaceOrderCommand(UUID productId, int quantity) implements Command {}
public record GetOrderQuery(UUID orderId) implements Query {}
```

## 配置项

无运行时配置。本模块为纯接口 + 注解 jar，无 SPI、无 AutoConfiguration、无 Spring Bean。

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 纯标记接口，不含泛型 | Query 定义在 contract（对外），Handler 在 application（内部）；绑定泛型会导致内外类型耦合 |
| 不含 REST/RPC 注解 | REST 面由服务端 Controller（spring-web 注解）显式声明；东西向复用同一契约接口（HTTP 直连），契约 jar 保持纯类型 |
| **未实现** CQE 基类 / 抽象类 | 标记接口足够；基类会强制继承关系，与 record 不兼容 |
| **未实现** 序列化 / 校验注解 | 各服务序列化策略不同；校验由 spring-boot-starter-validation 在 Handler 层处理 |

## 依赖关系

```
common-contract（独立，无内部模块依赖）
└── jakarta.validation-api（契约层参数约束声明，运行时由服务端 Handler 层执行校验）
```

> 纯标记接口 + 校验注解，零运行时代码，不会给 contract 包引入任何实现负担。
