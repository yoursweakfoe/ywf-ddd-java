# ADR-0010 契约承载 HTTP 映射 + 文档注解（重契约）

**Status**: Accepted
**迁移来源**: docs/common/common-contract.md §6 · 旧 ADR-0003（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

contract 模块的契约接口是否承载 HTTP 映射注解（`@GetMapping`/`@PostMapping`）与文档注解（`@Operation`/`@Schema`），即契约是否绑定 HTTP 协议。

## Decision Drivers

- 契约完整性（类型 + 语义 + 路径）
- 路径归属的天然解（消费方从接口看到路径）
- 协议与契约表述的绑定关系

## Considered Options

- **重契约**：接口承载 HTTP 映射 + 文档注解，契约 = 完整 REST 定义（选定）
- **轻契约**：接口纯类型，HTTP 映射留 Controller（[ADR-0009](ADR-0009-contract-light-contract.md)，旧 common-contract §ADR-0002，本决策取代它）

## Decision Outcome

选重契约。契约本就该承载协议的完整定义——每个协议都需要自己的契约表述（HTTP 用映射注解，gRPC 用 protobuf），「协议无关的轻契约」是伪命题。

**Pro**：契约完整（类型+语义+路径一体）；路径归属天然解决（消费方从接口看到路径）；`@Operation` 有 `@GetMapping` 锚点；契约优先、集中一处。

**Con**：contract 依赖 spring-web（引入 HTTP 注解依赖）；动摇「零框架依赖」约束。

## Consequences

- 契约 jar 依赖 spring-web
- 若未来引入第二协议（如 gRPC 内部调用），以该协议自身的契约表述（protobuf 定义，内部包化）做独立重契约，与 HTTP 契约并存、互不影响——换协议是新增协议契约，而非迁移现有契约

## Confirmation

无机械规则直接针对「契约接口承载 HTTP 映射注解」→ 人工评审（迁移自 contract 旧 ADR-0003）。配套背书：
- ArchUnit C1 `CONTRACT_DOES_NOT_DEPEND_ON_SERVER` 守护契约边界另一面（contract 不依赖 server 四层及 Spring 运行时基础设施），双端扫描挂载
- 真实例（sample）：`RestEndpointIntegrationTest.lifecycle_*` 走通契约承载端点的完整 HTTP 链路
- 原记锚点：`common-contract` 引入 `swagger-annotations` + `spring-web`；契约接口承载 `@Tag`/`@RequestMapping`/`@Operation`/`@GetMapping`
