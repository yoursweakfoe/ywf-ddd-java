# Contract — 公开契约

## 职责

contract 模块定义服务的公开契约。它是消费方——其他微服务——的**唯一依赖**。内容只有三类：REST 端点契约接口 Controller、CQRS 输入 Command/Query、契约输出对象 CO。

## 设计原则

- **纯类型定义**：仅包含接口、Command/Query、CO，无任何实现。
- **CQRS 标记纯类型**：Command / Query 标记只给请求归类，不携带返回类型泛型。契约在外，处理器在内。把返回类型写进标记，等于把内部实现类型钉进对外契约。返回类型由处理侧的方法签名承载，内外各自演进。
- **轻依赖**：服务契约模块仅依赖 `common-contract`。后者定义 CQRS 标记接口，并传递提供 `swagger-annotations` 文档注解、`spring-web` HTTP 映射注解、`jakarta.validation-api` 校验注解。三者均为注解级依赖。
- **按聚合分包**：顶层以聚合名划分包，各聚合内部结构一致。
- **单通道复用**：对外 REST 与东西向复用同一契约接口。REST 路径声明在契约接口上，即 `@RequestMapping` + `@GetMapping`。东西向一期为 RestClient 直连，地址静态配置。Feign 客户端经 common-cloud opt-in，JWT RequestInterceptor 自动透传。
- **契约 = 类型 + 语义**：契约包含方法签名，也包含能力与字段的语义描述，即文档注解。语义是消费方理解接口的必要部分，应随契约分发，而不是留在服务端实现里。
- **契约承载 HTTP 映射 + 文档注解**：`@Tag` / `@RequestMapping` / `@Operation` / `@GetMapping` 连同路径，声明在 Controller 契约接口上；`@Schema` 声明在 CO / CQE 字段上。ControllerImpl 只标记 `@RestController` 并透传，不重复声明路径与语义。

## 包结构

→ [aggregate-blueprint §5](../../specs/current/patterns/aggregate-blueprint.md)

> 完整代码示例 → [cookbook/write-path.md](../how-to/write-path.md)，看 Command / CO 的定义。

## 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| Controller 契约接口 | `{aggregate}/adapter/rest/controller/` | 完整 REST 契约：方法签名、能力语义、HTTP 映射都在接口上，是单一事实源。声明 `@Tag` / `@RequestMapping` / `@Operation` / `@GetMapping`。服务端 ControllerImpl 实现它。 |
| Command | `{aggregate}/dto/command/` | 写操作命令。实现 `Command` 标记接口。 |
| Query / PageableQuery | `{aggregate}/dto/query/` | 读操作查询。实现 `Query` / `PageableQuery` 标记接口。分页查询带 pageNum/pageSize 字段，并以 @Min/@Max 约束。 |
| CO | `{aggregate}/dto/co/` | Contract Object。对内部 DTO 清洗后的外部安全视图。字段以 `@Schema` 声明语义。 |
| IntegrationEvent | `{aggregate}/dto/event/` | 跨服务事件契约，覆盖出站发布与入站消费。实现 `IntegrationEvent` 标记接口，该接口在 common-contract。 |
| 枚举 | `{aggregate}/enums/` | 契约共享的枚举类型。 |

## 文档注解归属

本节回答一句话：语义描述归契约，不归服务端实现。契约 = 类型 + 语义。消费方引入 contract 包后，应当获得「能力是什么、字段是什么」的完整理解，而不只是方法签名。

| 注解 | 声明位置 | 描述对象 |
|------|---------|---------|
| `@Tag` | Controller 契约接口类 | 能力分组，如「支付服务」，虚构教例 |
| `@RequestMapping` | Controller 契约接口类 | 基路径，如 `/payments`，虚构教例 |
| `@Operation` | Controller 契约接口方法 | 能力语义：summary / description |
| `@GetMapping` 等 | Controller 契约接口方法 | HTTP 映射：方法 + 路径，这是契约的一部分 |
| `@Schema` | CO / CQE 字段 | 字段语义 |
| `@Parameter` | Controller 契约接口方法参数 | 参数语义。现行仅配合 `@PathVariable` 使用；表单/体绑定参数的语义由载体字段上的 `@Schema` 承载，不重复标注。 |
| `@ModelAttribute` | Controller 契约接口方法参数 | 查询参数绑定载体，record 表单。wire 形态保持 `?field=...`；`@Valid` 在绑定层把非法输入拦为 400 + fieldErrors。 |

> 现状注记，2026-09 源码核实：契约模块**不使用 `@RequestParam`**，全模块 0 绑定位。查询参数一律经 `@ModelAttribute` 载体 record 绑定。真实例是 sample 契约的发货端点：Form 载体，wire 保持查询参数形态。见 [OrderController.java](../../../sample-application/sample-service/sample-service-contract/src/main/java/com/yoursweakfoe/sampleapplication/sampleservice/contract/order/adapter/rest/controller/OrderController.java) 类与 Ship 方法 javadoc。

**分工边界**：Controller 契约接口承载「能力语义 + HTTP 映射」，`@Operation` 与 `@GetMapping` 连同路径一体声明，契约就是完整的 REST 定义。CO / CQE 承载「字段语义」，即 `@Schema`。ControllerImpl 实现接口，只补充「协议标记」`@RestController` 并透传，不重复声明路径与语义。

> 注解来源 `swagger-annotations` 是纯注解 jar：零运行时、零端点。与 `jakarta.validation-api` 同类，不引入框架依赖。

### 为什么契约绑定协议（重契约）

本框架选择了重契约：HTTP 映射写进契约接口。这里曾有过另一种形状，叫**轻契约**：契约接口只含纯类型，HTTP 映射全部留在服务端 Controller，契约层自称「零框架依赖」。这一立场后来被反转。

裁决理由一句话：**「协议无关的契约」是伪命题**。每个协议都需要自己的契约表述。HTTP 用映射注解，gRPC 用 protobuf 定义。剥离协议信息后，交付的只是半份契约：消费方看得见方法签名，看不见路径。路径沦为口头约定，散落在消费方、服务方、网关三方之间。

映射上契约之后，同时解决三件事：

- **契约完整性**：类型、语义、路径一体分发。消费方拿到接口即拿到全貌，可直接构造强类型调用。
- **路径归属天然解决**：路径声明在契约接口上，三方对账有了事实源。下文「契约访问边界」的路径命名空间约定，依托的正是这份归属。
- **文档注解有锚**：能力描述与 HTTP 映射挂在同一方法上，不再两处漂移。

代价是显式接受的：契约模块引入 `spring-web`。所以「零框架依赖」在此精确化为**「零运行时依赖」**：契约的框架相关依赖一律停在注解层，不拉起容器。消费方即使不在 Web 运行时里，也不受牵连。若将来引入第二协议，如内部 gRPC 调用，解法不是迁移现有 HTTP 契约，而是按该协议自身的契约表述另立一份独立重契约，与 HTTP 契约并存、互不影响。换协议是新增契约，不是改写契约。

## 对内对外契约边界

contract jar 本质是**东西向**产物：内部 Java 服务间的类型契约。南北向——外部客户端——消费「HTTP 端点 + 文档」，不引入 Java jar。两者虽同为 HTTP REST，但消费者形态与演进节奏不同：

| | 消费者 | 消费什么 | 变更成本 |
|---|---|---|---|
| 南北向 | 外部客户端 | 端点 + 文档 | 高。外部不可控，需兼容承诺。 |
| 东西向 | 内部微服务 | contract jar，即 CQE/CO 类型 | 低。内部可协调。 |

**一期不分包**：南北向端点与东西向端点是同一组 Controller，不存在「对内想改、对外不能改」的张力，拆分只会空转。

**触发分包的信号**：出现「对内想改、但对外不能改」的张力时才拆——对外需稳定兼容，对内需快速演进。拆法：

- adapter 分 `rest/` 与 `internal/`：前者南北向经网关，后者东西向内网直连
- contract 分 `external/` 与 `internal/`

**代码分包 ≠ 访问隔离**：包划分是编译期语义边界，运行期网络访问看不到它。访问隔离是正交维度，见下节。

## 契约访问边界（运行时保障）

结论：阻止外部访问对内契约是访问控制问题，分包解决不了——分包只在编译期生效，网络层无感知。一期单包部署下靠三层保障：

| 层 | 机制 | 作用 |
|---|---|---|
| 拓扑 | 服务在网关后，无公网直连 | 外部唯一入口是网关，这是地基 |
| 网关 | 路径命名空间 + 路由白名单 | `/api/*` 路由，`/internal/*` 不路由 |
| 应用 | 东西向端点校验内部凭证 | 网关错配时兜底 |

**路径命名空间约定**：东西向统一 `/internal/*` 前缀，南北向 `/api/*`。网关据此过滤。路径也是东西向传输信息的归属位点，由消费方、服务方、网关三方共享这一份约定。

**演进形态**，需要更强隔离时启用：

- **一包两部署**：同一 jar，以 `@Profile("external")` / `@Profile("internal")` 区分 Controller，起两个进程、两个端口，网络策略分别管控。jar 不拆，部署拆进程。
- **拆模块独立部署**：东西向能力真正分化、需要独立扩缩容或独立安全域时，拆 Maven 模块并配独立部署单元。

> 单进程多端口——即多 Tomcat Connector——是伪隔离：同一 JVM、同一安全上下文。端口是入口不是边界，不推荐。

## 协作关系

```
contract（本模块）                             server
─────────────                             ──────
adapter/rest/controller/{Aggregate}Controller.java ←──  adapter/rest/controller/{Aggregate}ControllerImpl（实现接口，纯透传 AppService）
{aggregate}/dto/command/XxxCommand / dto/query/XxxQuery     ←──  application/handler/command|query/（接收 CQE 执行用例）
{aggregate}/dto/co/XxxCO                      ←──  application/presenter/（DTO → CO 输出）
```

- **contract** 定义接口 + CQE + CO，adapter 负责实现接口
- **application** 接收 adapter 的透传调用，返回 CO

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
PaymentCO payment = paymentRestClient.get()
        .uri("/payments/{id}", paymentId)
        .retrieve()
        .body(PaymentCO.class);
```

> 示例中 `PaymentCO` / `paymentRestClient` 为虚构教例，sample 未实现。sample 真实例见 contract 模块 product 聚合的 CO 与消费方 RestClient。

## 规则

| 允许 | 禁止 |
|------|------|
| 接口定义 | 任何实现类 |
| 纯数据载体 Command/Query/CO | 业务逻辑 |
| 实现 common-contract 标记接口 | 依赖 Spring 运行时，即 IoC 容器；依赖 MyBatis |
| `@Tag` / `@Operation` / `@GetMapping` / `@RequestMapping` 声明在接口；`@Schema` 声明在 CO/CQE | 引入运行时框架 |
| java.io.Serializable | 依赖 server 模块 |
