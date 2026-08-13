# common-contract

CQRS 契约标记接口（Command / Query / PageableQuery / Event）—— 供 contract jar 与服务端共享的纯类型契约层。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

最轻量的公共契约层，仅含纯标记接口 + `jakarta.validation-api` 校验注解，零运行时逻辑。业务服务的 `xxx-contract` 模块引入本包后，Command / Query / Event 对象即可被基础设施层统一识别与拦截。任何需要定义 CQRS 请求对象的模块都应引入。

> 不含 REST/RPC 注解、不含序列化策略：契约 jar 保持纯类型，由服务端与消费方各自决定呈现形式。

## 2. 核心能力

### 标记接口语义

| 接口 | 语义 | 命名约定 | 基础设施拦截点 |
|------|------|----------|---------------|
| `Command` | 「请做这件事」— 变更系统状态 | `XxxCommand` | 事务、审计日志、幂等校验 |
| `Query` | 「请给我这个」— 读取数据 | `XxxQuery` | 只读路由、缓存、权限校验 |
| `PageableQuery` | 「给我一页」— 分页读取 | `GetXxxPageQuery` | 同 Query + 分页参数约束 |
| `Event` | 「这件事发生了」— 外部事实通知 | `XxxEvent` | 消息确认、重试、死信 |

### PageableQuery API

| 成员 | 类型 | 说明 |
|------|------|------|
| `DEFAULT_PAGE_SIZE` | `int = 20` | 默认每页大小 |
| `MAX_PAGE_SIZE` | `int = 1000` | 每页最大条数上限 |
| `getPageNum()` | `default int` → 1 | 当前页码（从 1 开始），`@Min(1)` |
| `getPageSize()` | `default int` → 20 | 每页大小，`@Min(1) @Max(1000)` |

> `@Min/@Max` 依赖调用点 `@Valid` 触发；框架层 `findDomainPage` 已内置防御性 clamp，未经校验的参数也不会产生非法分页。

### Event vs DomainEvent

| | Event（本模块） | DomainEvent（common-ddd） |
|---|---|---|
| 来源 | 外部进入（MQ / RPC / Webhook） | 领域内部产生（聚合根注册） |
| 发布方 | 外部系统 | 本进程 Spring Event |
| 处理入口 | `EventHandler<E>` | `@EventListener` / `@TransactionalEventListener` |

## 3. 使用方式

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

无运行时配置：本模块为纯接口 + 注解 jar，无 SPI、无 AutoConfiguration、无 Spring Bean。

## 4. 依赖关系

```
common-contract（独立，无内部模块依赖）
└── jakarta.validation-api（契约层参数约束声明，运行时由服务端 Handler 层执行校验）
```

## 5. 设计原则

- **纯标记接口**：不含泛型、不含基类、不含任何实现逻辑
- **record 友好**：标记接口可被 record 实现，不强制继承关系
- **零运行时负担**：所有依赖均为纯注解 jar，不引入任何实现代码

## 6. 设计决策

### ADR-0001 标记接口不含泛型

- 状态：accepted

**背景**：Query/Command 是否需要携带返回类型泛型。

**选项**：
- 带泛型 `Query<R>`：类型信息内聚，但 contract（对外）与 Handler（内部）产生类型耦合
- 纯标记 `Query`：返回类型由 Service 方法签名定义

**决策**：选纯标记。Query 定义在 contract（对外），Handler 在 application（内部），绑定泛型会导致内外类型耦合。

**后果**：返回类型不可从标记接口推断，需看 Service/Handler 方法签名。

**确认**：`Query.java` / `Command.java` 无泛型参数。

### ADR-0002 不含 REST/RPC 注解

- 状态：accepted

**背景**：契约对象是否需要携带 REST/RPC 呈现注解。

**选项**：
- 挂注解：契约 jar 自带路径/序列化定义
- 纯类型：呈现由服务端 Controller 显式声明

**决策**：选纯类型。REST 面由服务端 Controller（spring-web 注解）显式声明；东西向复用同一契约接口，契约 jar 保持纯类型。

**后果**：契约 jar 与具体通信协议解耦，可复用于 HTTP/RPC/事件多种通道。

**确认**：`common-contract` 无任何 spring-web / jax-rs / swagger 注解。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：CQE 基类 / 抽象类 | 标记接口足够；基类强制继承关系，与 record 不兼容 |
| 边界：序列化 / 校验注解 | 各服务序列化策略不同；校验由 spring-boot-starter-validation 在 Handler 层处理 |
