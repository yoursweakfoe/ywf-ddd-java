# Contract — 公开契约

## 职责

定义服务的公开契约，是消费方（其他微服务）的**唯一依赖**。
涵盖 REST 端点契约接口（Controller，东西向复用）、CQRS 输入（Command/Query）、契约输出对象（CO）、集成事件（Integration Event）。

## 设计原则

- **纯类型定义**：仅包含接口、Command/Query、CO、Event，无任何实现
- **轻依赖**：仅依赖 `common-contract`（CQRS 标记接口）+ `swagger-annotations`（文档注解）+ `spring-web`（HTTP 映射注解，`provided` 不传递）
- **按聚合分包**：顶层以聚合名划分，内部结构一致
- **单通道复用**：对外 REST 与东西向复用同一契约接口；REST 路径在契约接口上声明（`@RequestMapping` + `@GetMapping`），东西向由消费方经 RestClient 调用提供方 REST 端点（HTTP 直连）
- **契约 = 类型 + 语义**：契约包含方法签名（类型）与能力/字段语义（文档注解）。语义是消费方理解接口的必要部分，应随契约分发，而非留在服务端实现里
- **契约承载 HTTP 映射 + 文档注解**：`@Tag` / `@RequestMapping` / `@Operation` / `@GetMapping`（含路径）声明在 Controller 契约接口，`@Schema` 声明在 CO / CQE 字段；ControllerImpl 仅标记 `@RestController` 并透传，不重复声明路径与语义

## 包结构

→ [directory-structure/contract/contract.md](../directory-structure/contract/contract.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（Command / CO 定义）| [cookbook/event-flow.md](../cookbook/event-flow.md)（IntegrationEvent）

## 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| Controller 契约接口 | `{aggregate}/adapter/rest/` | 完整 REST 契约（方法签名 + 能力语义 + HTTP 映射的单一事实源），`@Tag` / `@RequestMapping` / `@Operation` / `@GetMapping` 声明；服务端 ControllerImpl 实现 |
| Command | `{aggregate}/dto/command/` | 写操作命令，实现 `Command` 标记接口 |
| Query / PageableQuery | `{aggregate}/dto/query/` | 读操作查询，实现 `Query` / `PageableQuery` 标记接口（分页带 pageNum/pageSize + @Min/@Max） |
| CO | `{aggregate}/dto/co/` | Contract Object，对内部 DTO 清洗后的外部安全视图，`@Schema` 声明字段语义 |
| Integration Event | `{aggregate}/dto/event/integration/` | 跨服务集成事件（MQ 载荷） |
| 枚举 | `{aggregate}/enums/` | 契约共享枚举 |

## 文档注解归属

契约 = 类型 + 语义。消费方引入 contract 包，应获得「能力是什么、字段是什么」的完整理解，而不仅是方法签名。因此语义描述（文档注解）属于契约层，不属于服务端实现：

| 注解 | 声明位置 | 描述对象 |
|------|---------|---------|
| `@Tag` | Controller 契约接口类 | 能力分组（如「订单服务」） |
| `@RequestMapping` | Controller 契约接口类 | 基路径（如 `/orders`） |
| `@Operation` | Controller 契约接口方法 | 能力语义（summary / description） |
| `@GetMapping` 等 | Controller 契约接口方法 | HTTP 映射（方法 + 路径，契约的一部分） |
| `@Schema` | CO / CQE 字段 | 字段语义 |
| `@Parameter` | Controller 契约接口方法参数 | 参数语义（跟随 `@PathVariable` / `@RequestParam`） |

**分工边界**：Controller 契约接口承载「能力语义 + HTTP 映射」（`@Operation` + `@GetMapping` + 路径一体，契约 = 完整 REST 定义），CO / CQE 承载「字段语义」（`@Schema`）；ControllerImpl 实现接口，仅补充「协议标记」（`@RestController`）并透传，不重复声明路径与语义。

> 注解来源 `swagger-annotations`（纯注解 jar，零运行时、零端点），与 `jakarta.validation-api` 同类，不引入框架依赖。

## 对内对外契约边界

contract jar 本质是**东西向**产物（内部 Java 服务间类型契约）；南北向（外部客户端）消费「HTTP 端点 + 文档」，不引入 Java jar。两者虽同为 HTTP REST，消费者形态与演进节奏不同：

| | 消费者 | 消费什么 | 变更成本 |
|---|---|---|---|
| 南北向 | 外部客户端 | 端点 + 文档 | 高（外部不可控，需兼容承诺） |
| 东西向 | 内部微服务 | contract jar（CQE/CO 类型） | 低（内部可协调） |

**一期不分包**：南北向端点 = 东西向端点（共用 Controller），不存在「对内想改、对外不能改」的张力，拆分只会空转。

**触发分包的信号**：出现「对内想改、但对外不能改」的张力（对外需稳定兼容、对内需快速演进）时才拆：

- adapter 分 `rest/`（南北向，经网关）与 `internal/`（东西向，内网直连）
- contract 分 `external/` 与 `internal/`

**代码分包 ≠ 访问隔离**：包划分是编译期语义边界，运行期网络访问看不到它。访问隔离是正交维度（见下）。

## 契约访问边界（运行时保障）

「阻止外部访问对内契约」是访问控制问题，分包解决不了（编译期隔离，网络层无感知）。一期单包部署下靠三层保障：

| 层 | 机制 | 作用 |
|---|---|---|
| 拓扑 | 服务在网关后（无公网直连） | 外部唯一入口是网关（地基） |
| 网关 | 路径命名空间 + 路由白名单 | `/api/*` 路由，`/internal/*` 不路由 |
| 应用 | 东西向端点校验内部凭证 | 网关错配时兜底 |

**路径命名空间约定**：东西向统一 `/internal/*` 前缀，南北向 `/api/*`。网关据此过滤；这也是「东西向传输信息（路径）归属」的约定，消费方、服务方、网关三方共享。

**演进形态**（需要更强隔离时）：

- **一包两部署**：同一 jar，`@Profile("external")` / `@Profile("internal")` 区分 Controller，起两个进程两个端口，网络策略分别管控。jar 不拆，部署拆进程。
- **拆模块独立部署**：东西向能力真正分化、需独立扩缩容/安全域时，拆 Maven 模块 + 独立部署单元。

> 单进程多端口（多 Tomcat Connector）是伪隔离：同一 JVM、同一安全上下文，端口是入口不是边界，不推荐。

## 协作关系

```
contract（本模块）                             server
─────────────                             ──────
adapter/rest/{Aggregate}Controller.java           ←──  adapter/rest/（ControllerImpl 实现接口，纯透传 AppService）
{aggregate}/dto/dto/command/XxxCommand / dto/query/XxxQuery     ←──  application/handler/command|query/（接收 CQE 执行用例）
{aggregate}/dto/dto/co/XxxCO                      ←──  application/presenter/（DTO → CO 输出）
{aggregate}/dto/dto/event/integration/XxxIntegrationEvent ←──  application/event/capture/（翻译 + 同事务捕获入集成 Outbox）
{aggregate}/dto/dto/event/integration/XxxIntegrationEvent ──→  adapter/event/consumer/（接收 MQ 并透传 AppService）
```

### 消费方使用

```xml
<dependency>
    <groupId>com.yoursweakfoe.application</groupId>
    <artifactId>sample-service-contract</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

```java
// 东西向：消费方经 RestClient 调用提供方 REST 端点（一期静态地址直连），
// 请求/响应类型复用 contract 中的 CQE/CO（强类型，编译期契约）
ProductCO product = productRestClient.get()
        .uri("/products/{id}", productId)
        .retrieve()
        .body(ProductCO.class);
```

## 规则

| 允许 | 禁止 |
|------|------|
| 接口定义 | 任何实现类 |
| 纯数据载体（Command/Query/CO/Event） | 业务逻辑 |
| 实现 common-contract 标记接口 | 依赖 Spring 运行时（IoC 容器）/ MyBatis |
| `@Tag` / `@Operation` / `@GetMapping` / `@RequestMapping`（接口）、`@Schema`（CO/CQE） | 引入运行时框架 |
| java.io.Serializable | 依赖 server 模块 |
