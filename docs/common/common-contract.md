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
    │   └── PageableQuery.java
    ├── co/
    │   └── CO.java
    └── event/
        └── integration/
            └── IntegrationEvent.java
```

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
| `DEFAULT_PAGE_SIZE` | `int = 20` | 默认每页大小 |
| `MAX_PAGE_SIZE` | `int = 1000` | 每页最大条数上限 |
| `pageNum()` | 抽象方法 | 当前页码（原始值）—— record 组件 `int pageNum` 天然实现 |
| `pageSize()` | 抽象方法 | 每页大小（原始值，批量导出逃生门）—— record 组件 `int pageSize` 天然实现 |
| `safePageNum()` | `default int` | 防御性页码：下限钳制为 1（读侧推荐消费入口） |
| `safePageSize()` | `default int` | 防御性每页大小：钳制到 `1..MAX_PAGE_SIZE`（读侧推荐消费入口） |

> **record 优先**：组件名 `pageNum`/`pageSize` 与抽象方法同签名，业务 record 实现本接口**零覆写**。读侧仓储统一消费 `safe*()` 取钳制值；未经校验的非法参数也不会产生非法分页。

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-contract</artifactId>
</dependency>
```

引入即生效。业务 CQE 对象实现对应标记接口，并在字段上声明校验约束：

```java
public record PlaceOrderCommand(
        @NotBlank String customerId,
        @NotEmpty List<@Valid OrderItemDTO> items
) implements Command {}

public record GetOrderPageQuery(
        String status,
        @Min(1) int pageNum,
        @Min(1) @Max(PageableQuery.MAX_PAGE_SIZE) int pageSize
) implements PageableQuery {}
// 组件名 pageNum/pageSize 与接口抽象方法天然匹配——零覆写样板。
// 校验注解必须声明在组件上（见 §3.1）。
```

### 3.1 参数校验规范

契约层声明约束、服务端执行校验，三层协作：

| 层 | 职责 | 做法 |
|---|---|---|
| contract（声明约束） | 在 CQE 字段上声明校验注解 | `@NotNull` / `@NotBlank` / `@NotEmpty` / `@Min` / `@Max` / `@Size` |
| adapter（触发校验） | `@Valid` 声明于**契约接口方法参数**，Controller 实现继承、HTTP 绑定期触发 | `@Valid @RequestBody XxxCommand`（写在契约接口上） |
| 全局异常处理 | 统一翻译校验失败 | `MethodArgumentNotValidException` → 400 + fieldErrors（common-exception 已提供） |

要点：

- **嵌套校验**：容器元素用类型参数注解 `List<@Valid Xxx>`；不要在 `List` 字段上加 `@Valid`（Hibernate Validator 已弃用该用法）
- **record 分页字段**：`@Min/@Max` 声明在 record **组件上**（接口方法注解不被组件继承）；运行期防线由 `safePageNum()/safePageSize()` 兜底，两层互为冗余
- **字符串 ID 用 `@NotBlank`，对象 ID 用 `@NotNull`，集合用 `@NotEmpty`**
- **业务规则校验不在此列**：库存够不够、状态对不对属 Domain 层 `validate()` + 显式 if-throw，不用 Bean Validation

无运行时配置：本模块为纯接口 + 注解 jar，无 SPI、无 AutoConfiguration、无 Spring Bean。

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

### ADR-0002 轻契约（不含 REST/RPC 注解）

- 状态：已废弃，由 ADR-0003（重契约）取代。

### ADR-0003 契约承载 HTTP 映射 + 文档注解（重契约）

- 状态：accepted

**背景**：contract 模块的契约接口是否承载 HTTP 映射注解（`@GetMapping`/`@PostMapping`）与文档注解（`@Operation`/`@Schema`），即契约是否绑定 HTTP 协议。

**选项**：
- 重契约：接口承载 HTTP 映射 + 文档注解，契约 = 完整 REST 定义
- 轻契约：接口纯类型，HTTP 映射留 Controller（原 ADR-0002）

**决策**：选重契约。契约本就该承载协议的完整定义——每个协议都需要自己的契约表述（HTTP 用映射注解，gRPC 用 protobuf），「协议无关的轻契约」是伪命题。

**Pro**：契约完整（类型+语义+路径一体）；路径归属天然解决（消费方从接口看到路径）；`@Operation` 有 `@GetMapping` 锚点；契约优先、集中一处。

**Con**：contract 依赖 spring-web（引入 HTTP 注解依赖）；动摇「零框架依赖」约束。

**后果**：契约 jar 依赖 spring-web。若未来引入第二协议（如 gRPC 内部调用），以该协议自身的契约表述（protobuf 定义，内部包化）做独立重契约，与 HTTP 契约并存、互不影响——换协议是新增协议契约，而非迁移现有契约。

**确认**：`common-contract` 引入 `swagger-annotations` + `spring-web`；契约接口承载 `@Tag`/`@RequestMapping`/`@Operation`/`@GetMapping`。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：CQE 基类 / 抽象类 | 标记接口足够；基类强制继承关系，与 record 不兼容 |
| 边界：校验执行 | 契约层只声明约束（纯 API）；Hibernate Validator 实现由服务端提供（经 common-exception 传递或显式引入），经契约接口上声明的 `@Valid` 在 Controller 绑定期触发（Handler 层不做 Bean Validation） |
