# 迁移计划：移除 Dubbo，升级 Spring Boot 4，切换 Spring gRPC

> 状态：**迁移完成**（Phase 0–3 全部落地，2026-08-12）；遗留人工项见 §14（Higress/Apifox 仓库外操作）
> 创建时间：2026-08
> 目标仓库：ywf-ddd-java（0.0.1-SNAPSHOT，无生产消费者，允许破坏性变更）

---

## 1. 背景与动机

### 1.1 为什么要动

- **Dubbo 过重**：项目内部通信接口量极少，但 `@DubboService` + Triple 双暴露模型会把**每一个**接口同时注册为 Nacos RPC 服务 + REST 端点，全量同步进注册中心，治理面/攻击面被无谓放大
- **升级节奏被拖住**：Boot 4 生态（Boot 4.0 GA 2025-11、Boot 4.1 GA 2026-06、Spring gRPC 1.1 GA 2026-06）已全面就绪，但 Dubbo / MyBatis-Plus / Seata 对 Boot 4 的适配节奏滞后，留在 Dubbo 阵营意味着整条技术栈被最慢的组件卡住
- **时机成本最低**：项目为 blueprint（0.0.1-SNAPSHOT），无真实生产消费者，cookbook/sample 尚未被外部引用

### 1.2 兼容性证据（已完成调研）

| 事项 | 结论 | 证据 |
|---|---|---|
| Boot 4 + SCA + Nacos | 可行 | mall4cloud master：Boot 4.0.3 + SC 2025.1.1 + SCA 2025.1.0.0，全程无 Dubbo |
| Seata 2.6.0 + Boot 4 | 可行（用户已核实 2.4.0+ 正式支持） | mall4cloud 在 Boot 4.0.3 下实证；与本项目 Seata 版本一致，无需换版 |
| MyBatis-Plus + Boot 4 | 官方已发布独立构件 `mybatis-plus-spring-boot4-starter`（用户确认）；实装磨合度待 spike 验证 | mall4cloud 用的是原生 MyBatis，不能背书；历史根因是 MP 内置 mybatis-spring 版本过低（issue #7009） |
| Spring gRPC | GA，且 **gRPC 支持已收编进 Boot 4.1 本体**：spring-grpc 1.1.0（2026-06-10 GA）的主要变更即"自动配置迁移至 Spring Boot 4.1.0"；Boot 4.1 统一管理 grpc-java 1.80.0、protobuf-maven-plugin，并提供独立 Netty 与 Servlet HTTP/2 两种服务端形态 | spring-grpc 1.1 release notes；Boot 4.1 官方 release notes |
| Spring Boot 4.0 → 4.1 增量破坏面 | 小：主要是移除 4.0 中已弃用 API（对 3.5 直迁场景不增加额外负担）；Derby 支持弃用（本项目用 H2 测试，无影响）；layertools jar 模式移除；`-DskipTests` 不再跳过 AOT（改用 `maven.test.skip`） | Boot 4.1 官方 release notes（已通读） |

### 1.3 目标架构

| 通道 | 现状（Dubbo Triple） | 目标 |
|---|---|---|
| 对外面 | 所有接口经 JAX-RS 注解自动 REST 暴露（50051 同端口） | Spring MVC Controller 显式声明，经 Higress 网关入口 |
| 内部通道 | 所有接口自动进 Nacos 注册中心 | 仅极少数 proto service，显式定义、显式调用 |
| 契约模型 | Java 接口 + JAX-RS/Swagger 注解（双暴露） | REST：Spring MVC；RPC：proto-first |
| 异常处理 | Dubbo SPI（ExceptionHandler + Filter 双通道） | REST：`@RestControllerAdvice`（RFC 9457）；gRPC：Interceptor → StatusRuntimeException |
| 身份模型 | Dubbo RestFilter + SPI Filter + PenetrateSelector（REST 解析与 RPC 传递混在同一条管线） | REST 边界**仅解析**（Higress 注入的 Header）；gRPC 内部**解析 + 传递**（Interceptor + Context/Metadata，sec_*） |

---

## 2. 已定设计决策

| # | 决策 | 内容 | 理由 |
|---|---|---|---|
| D1 | 契约模型 | proto-first 仅用于**真实的东西向接口**；对外 REST 走 Spring MVC；Java DTO（CO/Command/Query）保留为应用层内部契约 | 内部接口量极少，proto 面保持最小 |
| D2 | Spring Boot 版本 | **直接采用 4.1.x**（2026-06-10 GA，spike 取当时最新补丁）。理由：① gRPC 自动配置已收编进 Boot 本体（spring-grpc 1.1.0 的自动配置层迁入 Boot 4.1，官方 release notes 单行声明），grpc-java/protobuf-maven-plugin 版本由 Boot 统一管理——"Spring 原生"路线的终态就是 4.1，落在 4.0 等于停在中间态；② 若选 4.0 只能用 spring-grpc 1.0，随后升 4.1 还需照官方 wiki 迁移指南把 gRPC 自动配置再迁一次，churn 是白付的；③ 4.0→4.1 增量破坏面已逐项核查（见 1.2），对本项目（3.5 直迁、无 Derby、无 layertools、无 4.0 弃用 API 存量）几乎为零。**代价**：MP boot4 starter / Seata / springdoc 的实证均在 4.0 线（mall4cloud），4.1 无生产背书，全部押给 spike S6 验证；失败回退 4.0.7 | 避免中间态 churn；第三方兼容性风险用 spike + 回退线对冲 |
| D3 | Spring gRPC 版本 | spring-grpc **1.1.0**，但不再 import spring-grpc-bom：自动配置层由 Boot 4.1 原生提供，版本随 Boot dependency management；仅保留 spring-grpc 核心库与测试构件 | 1.1.0 起自动配置归属 Boot（官方声明），自管 bom 反而可能与 Boot 托管版本冲突 |
| D4 | 不引入 Spring Cloud Alibaba | 不引入 SCA 全家桶；Nacos 仅保留 `nacos-client`（3.2.3，独立版本管理），配置中心/服务发现功能本期下线 | 去 Dubbo 的初衷就是减负；SCA 会重新引入重量级抽象 |
| D5 | 服务发现 | 一期：gRPC 客户端静态地址直连（sample 现状即直连），网关入口继续由 Higress 承担（Higress 原生支持 Nacos 服务源 + gRPC 路由）；二期：基于 nacos-client 自写 gRPC `NameResolver`（grpc-java 标准扩展点） | 内部接口极少，注册中心非必需 |
| D6 | Seata | 保留 2.6.0 不变；XID 透传从 dubbo-filter 切换为 gRPC Interceptor 集成（spike 中验证 Seata 是否提供现成 grpc interceptor 构件，否则自写薄封装） | 已实证 Boot 4 兼容 |
| D7 | MyBatis-Plus | `mybatis-plus-spring-boot3-starter` → `mybatis-plus-spring-boot4-starter`（最新稳定版）；重点验证其内置 mybatis-spring 已升 4.x、DataSource 自动配置适配 Boot 4 模块化 | 用户已表态支持相应修改 |
| D8 | 多数据源 | `dynamic-datasource-spring-boot3-starter` → `dynamic-datasource-spring-boot4-starter`（**spike 已证实存在**，4.5.0，@DS 切换跑通） | spike 决策点，已闭环（见 4.1） |
| D9 | REST API 文档 | `dubbo-rest-openapi` → springdoc-openapi（Boot 4 兼容版，参考 mall4cloud：springdoc 3.0.2 + knife4j 4.5.0） | Swagger 注解从 contract 接口迁移至 Controller |
| D10 | 端口模型 | REST（MVC，server.port）与 gRPC（spring.grpc.server.port=50051）双端口分离 | 面/通道边界清晰 |
| D11 | REST 注解体系 | JAX-RS 全量退场：删除 `javax.ws.rs-api`，REST 面改用 **spring-web 原生注解**（`@RestController`/`@GetMapping` 等），swagger 注解挂 Controller | JAX-RS 是 Dubbo Triple REST 的方言，随 Dubbo 一并移除；引入 spring-web 原生注解后即不再需要第二套 REST 注解体系 |
| D12 | 通道架构与身份模型 | **REST 边界 + gRPC 内部**：公网流量经 Higress → REST；东西向仅 gRPC。**不引入 Feign/OpenFeign**；不做全链路 gRPC。身份模型分两段：REST 入站仅**解析**网关注入的一手身份（edge）；gRPC 入站解析传递身份（propagated）、出站传递，多跳由 gRPC Context 自动携带。SecurityContext 标记身份来源（edge/propagated），文档钉死"内部服务端口不得直暴公网" | ① Feign 仅在"内部走 HTTP"时才有存在必要，gRPC stub 是强类型编译期绑定，更简单；② "通道统一全 REST"不能简化安全模型（身份天然分解析/传递两段，只是把 interceptor 换成 HTTP client interceptor），反而丢失强类型契约；③ 全链路 gRPC 否决：浏览器不说 gRPC，公网 API 调试生态（curl/Apifox/OpenAPI）失效；④ 面/通道分离即安全边界收敛（对比 Dubbo 时代每接口同时公网 REST + 内网 RPC） |

---

## 3. Dubbo 触点清单（剥离范围）

| 位置 | 内容 | 处置 |
|---|---|---|
| `ywf-ddd-common/pom.xml` | 6 个 dubbo-* properties + dependencyManagement 条目 | 删除 |
| `common-cloud/pom.xml` | dubbo starter/registry-nacos/configcenter-nacos/rest-openapi/observability + seata 排除项 | 模块重构（见 5.3） |
| `common-exception` | `GlobalRpcExceptionFilter`（Dubbo SPI Filter）、Triple REST `ExceptionHandler` SPI 实现、`META-INF/dubbo/internal/*` | 重写（见 5.2） |
| `common-security` | `dubbo/` 包 5 类（RestFilter、Provider/Consumer Filter、PenetrateSelector、ContextSupport）+ SPI 注册文件 + 7 个测试 | 重写（见 5.1） |
| `common-contract` | `javax.ws.rs-api` 依赖 | 删除 |
| `sample-service-contract` | OrderService/ProductService 接口上的 JAX-RS 注解 | 移除注解；接口转为 proto 或保留为内部 Java 契约（按 D1） |
| `sample-service-server` | `@DubboService` ×2、`@DubboReference` 集成测试、三套 profile 的 `dubbo:` yml | 重写（见第 6 节） |
| 文档 | cookbook / module-design / directory-structure / common docs 大量 Dubbo 描述 | 批量更新（见第 8 节） |

---

## 4. Phase 0：Spike 验证（GO/NO-GO 闸门）

**产物**：独立的 `spike-boot4-grpc` 临时目录（不入主干），最小可运行工程。

| # | 验证项 | 通过标准 |
|---|---|---|
| S1 | Boot 4.1.x + `mybatis-plus-spring-boot4-starter` 启动 | 应用正常启动；BaseMapper CRUD 跑通；无 DataSource 自动配置 ClassNotFound（MP starter 实证基于 4.0 线，4.1 无背书，此项为最高风险验证点） |
| S2 | `dynamic-datasource` Boot 4 可用性 | 有 boot4 构件且 @DS 切换跑通；否则触发 D8 备选 |
| S3 | Boot 4.1 原生 gRPC 闭环 | proto → protoc 生成（protobuf-maven-plugin，**Windows 构建验证**）→ Boot 原生 starter 暴露服务 → 客户端调用；确认 starter 构件名与 `spring.grpc.*` 属性命名（4.1 收编后有更名，以官方 reference 为准） |
| S4 | Seata 2.6.0 在 Boot 4 下启动（禁用/启用各一次） | 启动无异常；确认 gRPC XID 透传的现成构件是否存在 |
| S5 | springdoc-openapi 在 Boot 4 下生成文档 | /v3/api-docs 可访问 |
| S6 | Boot 4.1 第三方 starter 兼容矩阵 | S1/S2/S4/S5 均在 4.1.x 下执行（即本表整体就是 4.1 验证）；任一项失败且无 workaround → 触发 D2 回退线，全表在 4.0.7 下重跑一遍定基准 |

**闸门规则**：S1 + S3 必须通过（优先在 4.1.x 下，按 S6 规则必要时回退 4.0.7 重跑）；S2/S4 不通过则按备选方案降级执行并在本计划追加修订；Spike 预算半天。

### 4.1 Spike 结果（2026-08-12，GO）

环境：Windows 11 / Java 21.0.9 / Maven 3.9.11；spike 工程 `spike-boot4-grpc`（临时目录，未入主干）；Seata TC 为本地 `apache/seata-server:2.6.0.jdk21` 容器（8091）。

| # | 结果 | 证据 |
|---|---|---|
| S1 | ✅ PASS | `mybatis-plus-spring-boot4-starter:3.5.17` 在 Boot 4.1.0 下启动 + BaseMapper insert/select/update/delete 全通。注意：`@Version` 乐观锁更新依赖 `OptimisticLockerInnerInterceptor`（common-ddd 的 `MybatisPlusPluginConfiguration` 已提供，非 starter 缺陷） |
| S2 | ✅ PASS | **D8 解决**：`dynamic-datasource-spring-boot4-starter:4.5.0` 存在且 `@DS("slave")` 切换跑通（双 H2 库路由验证） |
| S3 | ✅ PASS | proto → `io.github.ascopes:protobuf-maven-plugin`（**Windows 构建验证通过**，protoc 退出码 0）→ Boot 原生 starter Netty server（随机端口）→ `GrpcChannelFactory` 客户端闭环；`@GlobalServerInterceptor` / `@GlobalClientInterceptor` Bean 注册机制验证通过（metadata → gRPC Context 链路断言成功） |
| S4 | ✅ PASS | Seata 2.6.0 在 Boot 4.1 下：`seata.enabled=false` 启动正常；enabled + 真实 TC：TM 注册成功（`register success, role:TMROLE`）。**D6 解决**：`org.apache.seata:seata-grpc:2.6.0` 提供现成构件 `ServerTransactionInterceptor` / `ClientTransactionInterceptor`，无需自写 |
| S5 | ✅ PASS | springdoc-openapi 3.1.0：`/v3/api-docs` 200 且包含端点文档 |
| S6 | ✅ | 以上全部在 4.1.0 下完成，未触发 4.0.7 回退线 |

**Phase 1 关键 API 结论（spike 实证）：**

- gRPC starter 构件名：`org.springframework.boot:spring-boot-starter-grpc-server` / `spring-boot-starter-grpc-client`（版本随 Boot dependency management，不自管 bom——印证 D3）；核心库 `org.springframework.grpc:spring-grpc-core:1.1.0`、grpc-java 1.80.0、protobuf-maven-plugin 5.1.4 均由 Boot 4.1 托管
- protobuf 插件为 **`io.github.ascopes:protobuf-maven-plugin`**（非 xolstice）；继承 `spring-boot-starter-parent` 时仅需声明插件坐标，protoc 版本与 generate 执行开箱即用；proto 放 `src/main/proto`
- 全局 interceptor 注册：`ServerInterceptor` Bean + `@GlobalServerInterceptor`（`org.springframework.grpc.server`）；`ClientInterceptor` Bean + `@GlobalClientInterceptor`（`org.springframework.grpc.client`）；排序走 `@Order`
- 测试端口发现：`local.grpc.port` 占位符在测试注入期**不可解析**，改用监听 `GrpcServerStartedEvent#getPort()`；`@LocalGrpcPort` 注解在 1.1.0 核心库中不存在
- Boot 4 更名/移除（影响 Phase 2 测试写法）：`spring-boot-starter-web` → `spring-boot-starter-webmvc`；`TestRestClient` 已移除（替代：`org.springframework.boot.resttestclient.TestRestTemplate`，模块 `spring-boot-resttestclient`）；`@AutoConfigureMockMvc` 迁至 `org.springframework.boot.webmvc.test.autoconfigure`（模块 `spring-boot-webmvc-test`）；`@LocalServerPort` 仍在 `org.springframework.boot.test.web.server`
- Seata 配置语义：enabled 时必须提供 `seata.service.vgroup-mapping.<group>` 与 `seata.service.grouplist.<cluster>`（file 注册中心），且 TC 不可达时**启动 fail-fast**——sample 的 test profile 需保留 `seata.enabled=false`
- 附带发现：spring-grpc-core 提供 `@GrpcAdvice` / `GrpcExceptionHandler` 异常抽象，可作 common-exception 的备选实现路径（本计划仍按 5.2 的自定义 interceptor 方案执行，语义可控性更强）

---

## 5. Phase 1：核心框架重构（ywf-ddd-common）

工作分支：`feature/remove-dubbo-spring-grpc`

### 5.1 common-security 重写（按 D12 身份模型）

**身份模型**：身份可信源只在网关进入系统一次。REST 入站 = 一手身份解析（Higress 已验 JWT）；gRPC 链路 = 已验证身份的传递。两类入站语义在 SecurityContext 中以来源标记区分（`edge` / `propagated`）。

- 保留：`AuthConstants`、`SecurityUtil`、`SecurityContextSupport`（存取逻辑平移，SecurityContext 增加身份来源标记）
- REST 入站（仅解析）：`SecurityRestFilter`（Dubbo Netty RestFilter）→ `SecurityWebFilter`（`OncePerRequestFilter`），解析 Higress 透传 Header → SecurityContext（source=edge）
- gRPC 入站（解析传递身份）：新 `GrpcSecurityServerInterceptor`，从 Metadata 读 `sec_*` → 写入 gRPC `Context`（source=propagated）
- gRPC 出站（传递）：新 `GrpcSecurityClientInterceptor`，SecurityContext → Metadata；多跳透传由 Context 自然携带（**PenetrateAttachmentSelector 无对应物，直接消失**）
- 删除：`dubbo/` 包全部 5 类、`META-INF/dubbo/internal/*` 4 个 SPI 文件
- 注册方式：SPI → Spring AutoConfiguration（interceptor 通过 Boot 4.1 的全局 interceptor 注册机制装配，具体 API 以 spike S3 确认；Filter 走常规 Bean）
- 测试：7 个 Dubbo mock 测试重写为 Filter + interceptor 单测；新增 REST→gRPC 跨通道链路测试（edge 身份经出站 interceptor 进入 metadata）

### 5.2 common-exception 重写

- REST 通道：Triple REST `ExceptionHandler` SPI → 标准 `@RestControllerAdvice`，保留 RFC 9457 响应格式与 i18n messageKey 语义不变
- gRPC 通道：`GlobalRpcExceptionFilter` → `GrpcExceptionServerInterceptor`，映射规则保持语义一致：
  - `BusinessException` → `StatusRuntimeException`，messageKey 放 status description，params 放 Trailers（自定义 metadata key）
  - `IllegalStateException` → FAILED_PRECONDITION；`IllegalArgumentException` → INVALID_ARGUMENT；其他 RuntimeException → INTERNAL（原始信息仅落日志）
- Consumer 侧：新 `GrpcExceptionClientInterceptor`，将 StatusRuntimeException + Trailers 还原为 `BusinessException`（Consumer 语义与现状一致）
- 删除：`META-INF/dubbo/internal/org.apache.dubbo.rpc.Filter`、`org.apache.dubbo.remoting.http12.ExceptionHandler`
- `GlobalRestExceptionHandlerTest` 迁移为 `@WebMvcTest` 风格；Filter 测试改为 interceptor 测试

### 5.3 common-cloud 重构

- 移除全部 `org.apache.dubbo:*` 依赖
- 新增：Boot 4.1 原生 gRPC starter（server + client，构件名以 spike S3 确认的官方命名为准）、springdoc-openapi（替代 dubbo-rest-openapi）
- 保留：`nacos-client`（3.2.3，为二期 NameResolver 与配置中心恢复预留）、`seata-spring-boot-starter`（2.6.0，按 S4 结果调整排除项；删除 dubbo-filter-seata 相关）
- 可观测性：dubbo-observability-starter → Micrometer（Boot 自带）+ Boot 4.1 内置 gRPC 指标
- 模块更名评估：`common-cloud` → `common-rpc` 或保持原名（倾向保持，减少破坏面；仅改 description）
- `logback-spring.xml`（common-observability）不受影响，核对 Boot 4 logback 版本兼容性即可

### 5.4 common-contract / common-ddd / common-pg

- `common-contract`：删除 `javax.ws.rs-api`；`Command`/`Query`/`Event`/`PageableQuery` 标记接口不变；swagger-annotations 保留与否随 D9 落地决定（迁往 REST 层则删）
- `common-ddd`：MP starter 构件切换（D7）；`DddArchitectureRules`（ArchUnit）核对是否引用 dubbo 包路径，有则同步调整
- `common-pg`：与 RPC 无关，仅随 Boot 4 基线回归测试
- 根 POM：parent `spring-boot-starter-parent` 3.5.16 → 4.1.x（spike 确认的最新补丁；D2 回退线 4.0.7）；删除全部 dubbo properties 与 dependencyManagement 条目；protobuf-maven-plugin 版本由 Boot 4.1 dependency management 托管，仅按需声明插件

---

## 6. Phase 2：sample-application 改造

### 6.1 契约层（sample-service-contract）

- `OrderService`/`ProductService` 接口：移除 JAX-RS 与 swagger 注解
- 按 D1：对外 REST 契约以 Controller 为准（不再需要接口层承载路径）；Java 接口可保留为服务内部 facade 契约或删除
- proto 契约：按"内部接口极少"的事实，仅新增**演示性**东西向 proto（如 `ProductInternalService`，供订单侧查询商品信息），置于 contract 模块 `src/main/proto`，protobuf-maven-plugin 生成
- CO/Command/Query DTO 保持 Java record + Bean Validation 不变（应用层契约不受影响）

### 6.2 服务层（sample-service-server）

- `adapter.web`（新增）：`OrderController`/`ProductController`，**spring-web 原生注解**（D11），路径语义完整平移现有 JAX-RS 定义（`POST /orders`、`PUT /orders/{orderId}/pay`、`GET /orders/{orderId}` 等），swagger 注解挂 Controller，调 `OrderAppService`/`ProductAppService`
- `adapter.grpc`（新增）：`ProductInternalGrpcService`（`@GrpcService`），实现 proto stub，透传应用服务
- 删除：`adapter/*/facade` 下 `@DubboService` 实现类（其透传职责由 Controller/gRPC service 承接）
- 配置文件：
  - `application.yml`：删除 `dubbo:` 块；新增 `spring.grpc.server.port: 50051`；`server.port` 保持
  - `application-dev/prod.yml`：删除 dubbo registry 配置；Nacos 相关仅保留注释占位（二期恢复）
  - `application-test.yml`：dubbo 直连配置替换为 gRPC 测试通道配置
- Dockerfile：EXPOSE 增加 REST 端口说明；优雅停机参数从 dubbo shutdown 切换为 Boot lifecycle
- 集成测试：`RpcEndpointIntegrationTest` 重写为 Boot 4.1 gRPC 测试支持（`@SpringBootTest` + InProcess 或真实端口 Channel）；REST 端点补充 `TestRestClient` 冒烟用例

### 6.3 验证清单

- `mvn clean install` 全绿（common + sample）
- REST：下单→支付→查询全链路 HTTP 冒烟（curl / Apifox 导入 springdoc 文档）
- gRPC：grpcurl 或集成测试调用 ProductInternalService
- 异常映射：BusinessException → REST RFC 9457 / gRPC Status 双通道断言
- 安全透传：Header → SecurityContext → gRPC Metadata 链路测试

---

## 7. Phase 3：基础设施与周边

- `ywf-infra`：Nacos 容器保留（二期发现/配置中心复用）；无需 Seata/Higress 变更
- Higress：对外 REST 路由规则从 50051（Triple REST）改指向 MVC 端口；东西向不经网关
- Apifox：OpenAPI 文档来源从 dubbo-rest-openapi 切换为 springdoc 端点

## 8. 文档更新清单

| 文件 | 变更 |
|---|---|
| `ywf-ddd-common/docs/common-cloud.md` | 重写：去 Dubbo，描述 spring-grpc + Nacos 预留 + Seata |
| `ywf-ddd-common/docs/common-exception.md` | 双通道改为 ControllerAdvice + gRPC Interceptor |
| `ywf-ddd-common/docs/common-security.md` | 管线描述从 Dubbo Netty/SPI 改为 Servlet Filter + Interceptor |
| `ywf-ddd-common/docs/common-contract.md` | 删除 javax.ws.rs 行；契约模型更新 |
| `ywf-ddd-common/docs/common-test.md` | 契约测试论证更新（proto 强类型） |
| `docs/sample-application/module-design/{adapter,application,contract,domain,infrastructure}.md` | adapter 拆 web/grpc；contract 模型更新 |
| `docs/sample-application/directory-structure/**` | 目录树与说明同步 |
| `docs/sample-application/cookbook/{gateway,cross-aggregate,distributed-transaction,error-handling,event-flow,write-path,read-path,new-aggregate,mq-consumer}.md` | 逐篇替换 Dubbo 描述（可 agent 半自动化，逐篇人工校对） |
| `README.md` / `AGENTS.md` / `sample test TESTING.md` | 技术栈声明更新 |

## 9. 验收标准

1. 全仓库 `grep -ri dubbo`（源码 + pom + yml）零命中，文档中仅在"迁移历史"语境出现
2. `mvn clean install` 全绿，测试数量不净减少（重写后覆盖等价）
3. REST 与 gRPC 双通道冒烟通过，异常/安全语义与迁移前一致
4. 依赖树体积：sample-server fat jar 较迁移前显著缩小（记录前后对比数据）

## 10. 风险与回退

| 风险 | 概率 | 缓解 |
|---|---|---|
| Boot 4.1 下第三方 starter（MP boot4 / Seata / springdoc）不兼容——三者的生产实证均在 4.0 线，4.1 无背书 | 中 | Spike S6 前置拦截；失败回退 4.0.7 + spring-grpc 1.0 基准线（代价：后续升 4.1 需照官方 wiki 迁移 gRPC 自动配置） |
| MP boot4 starter 自身缺陷（与 Boot 版本无关） | 中 | Spike S1 前置拦截；最坏回退原生 MyBatis（mall4cloud 已实证路径） |
| dynamic-datasource 无 boot4 构件 | 中 | D8 备选：单数据源过渡（sample 本身单库） |
| Seata gRPC 透传构件缺失 | 低 | 自写 Client/Server Interceptor 薄封装（XID 即一个 metadata key） |
| protobuf-maven-plugin Windows 构建问题 | 低 | Spike S3 显式验证；os-maven-plugin 已支持 windows-x86_64 |
| 文档遗漏 Dubbo 残留 | 中 | 验收标准 1 的 grep 门禁 |

**回退策略**：全程在 feature 分支作业，主干保持 Boot 3.5.16 + Dubbo 可用；任一 Phase 闸门失败即冻结，不回滚已完成部分。

## 11. 排期估算

| Phase | 内容 | 预算 |
|---|---|---|
| 0 | Spike（GO/NO-GO） | 0.5 天 |
| 1 | common 框架重构 | 2–3 天 |
| 2 | sample 应用改造 | 1–2 天 |
| 3 | 周边 + 文档批量更新 | 1–2 天 |

## 12. 会话记录与交接（2026-08-12）

### 12.1 本次会话总结

本次会话按用户指令**仅执行 Phase 0（Spike 验证）**，Phase 1 及以后未启动。

**最终结果：GO。** 在 Spring Boot 4.1.0 下完成 S1–S6 全部验证项，spike 工程 8 个测试全绿：

- S1 MyBatis-Plus boot4 starter（3.5.17）启动 + BaseMapper CRUD ✅
- S2 dynamic-datasource boot4 starter（4.5.0）@DS 切换 ✅（D8 闭环）
- S3 Boot 4.1 原生 gRPC 闭环 + `@GlobalServerInterceptor`/`@GlobalClientInterceptor` 注册机制 ✅（含 Windows protoc 构建验证）
- S4 Seata 2.6.0 禁用/启用态启动 + 确认 `seata-grpc` 现成透传构件 ✅（D6 闭环；启用态验证依赖本地 `apache/seata-server:2.6.0.jdk21` 容器）
- S5 springdoc-openapi 3.1.0 `/v3/api-docs` ✅
- S6 全部在 4.1.0 下通过，未触发 4.0.7 回退线 ✅

详细证据与 Phase 1 关键 API 结论见 §4.1；目标版本矩阵已更新为实证值（附录 A）。

**本次会话未修改 ywf-ddd-common / sample-application 的任何源码**，主干唯一变更即本计划文档（状态、§4.1、D8、附录 A）。

### 12.2 任务清单与完成情况

| # | 任务 | 状态 |
|---|---|---|
| 1 | Phase 0：创建 spike-boot4-grpc 临时工程（pom + proto + 应用 + 5 项验证测试） | ✅ 完成 |
| 2 | Phase 0：运行 spike 验证 S1–S6，记录 GO/NO-GO | ✅ 完成（GO） |
| 3 | Phase 1：ywf-ddd-common 根 POM 升级 Boot 4.1.0，删除 dubbo 版本管理，新增 grpc/mp-boot4/dd-boot4/springdoc 版本管理 | ✅ 完成 |
| 4 | Phase 1：common-security 重写（IdentitySource/IdentityDetails + SecurityWebFilter + gRPC 身份 interceptors + permit-all SecurityFilterChain + AutoConfiguration） | ✅ 完成（51 测试） |
| 5 | Phase 1：common-exception 重写（@RestControllerAdvice RFC 9457 + gRPC 异常 interceptors + trailers 还原 BusinessException） | ✅ 完成（23 测试） |
| 6 | Phase 1：common-cloud 重构（Boot gRPC starter + springdoc + SeataGrpcAutoConfiguration + nacos-client/seata 保留） | ✅ 完成（3 测试） |
| 7 | Phase 1：common-contract 删 javax.ws.rs-api/swagger-annotations；common-ddd 切 MP boot4 starter + 删 DubboMockHelper；logback 去 dubbo logger | ✅ 完成 |
| 8 | Phase 1：ywf-ddd-common mvn clean install 全绿 + 模块内 grep dubbo 零命中 | ✅ 完成（9 模块 SUCCESS） |
| 9 | Phase 2：sample-service-contract proto 契约（ProductInternalService）+ protobuf-maven-plugin 管线 + 接口去 JAX-RS/swagger | ✅ 完成 |
| 10 | Phase 2：adapter.web OrderController/ProductController 平移（spring-web 原生注解，实现契约接口） | ✅ 完成 |
| 11 | Phase 2：adapter.grpc ProductInternalGrpcService + application*.yml 去 dubbo + Dockerfile EXPOSE 双端口 + 补 spring-boot-starter-webmvc | ✅ 完成 |
| 12 | Phase 2：集成测试重写（REST 14 例 + gRPC 4 例 + 并发乐观锁）+ 删除 @DubboService facade + ArchUnit 规则更新 | ✅ 完成（91 测试全绿） |
| 13 | 验收门禁：全仓 grep + jar 体积对比 + 测试数量核对 | ✅ 完成（数据见 §13） |
| 14 | Phase 3：Higress 路由切换 + ywf-infra/Apifox 周边核对（仓库外部分产出人工操作清单） | ✅ 完成（清单见 §14） |
| 15 | Phase 3：文档批量更新（common docs + module-design + directory-structure + cookbook + README/AGENTS + skills/rules） | ✅ 完成（全仓 grep dubbo 零命中，含 docs） |

## 13. 验收记录（2026-08-12）

### 13.1 验收标准逐项核对

| 标准 | 结果 | 数据 |
|---|---|---|
| 1. 全仓库 grep dubbo 零命中（源码 + pom + yml） | ✅ | 全仓 `*.java/*.xml/*.yml/*.yaml/*.properties/*.imports/*.env/Dockerfile` 扫描（排除 docs 与 target）零命中 |
| 2. mvn clean install 全绿，测试数量不净减少 | ✅ | ywf-ddd-common 9 模块 + sample-application 4 模块全部 BUILD SUCCESS；测试数 common 202→222（+20），sample 91→91（持平），合计 293→313 |
| 3. REST 与 gRPC 双通道冒烟，异常/安全语义一致 | ✅ | RestEndpointIntegrationTest 14 例（含 RFC 9457 422/409 断言、订单全生命周期）；GrpcEndpointIntegrationTest 4 例（业务调用 + FAILED_PRECONDITION 映射 + trailers 还原 BusinessException + health/reflection） |
| 4. fat jar 体积前后对比 | ⚠️ 未缩小（+6.5%） | 112.37 MB → 119.67 MB，构成分析见 13.2 |

### 13.2 jar 体积构成分析（预期"显著缩小"未成立的根因）

迁移本身移除约 15 MB（dubbo-* 全家约 10 MB + dubbo-rest-openapi 文档栈 redoc/swagger-ui 约 4.5 MB + fastjson2 约 2 MB），但新增与暴露约 22 MB：

| 新增项 | 体积 | 说明 |
|---|---|---|
| netty 4.2 QUIC 原生包（5 平台）+ http3/quic 类 | ≈ 11.5 MB | nacos-client 3.2.3 真实依赖 netty 4.2；Dubbo 时代被 Dubbo 的 netty 4.1 压制未暴露 |
| gRPC 栈（grpc-* + protobuf + common-protos + gson） | ≈ 7.8 MB | 东西向通道对价 |
| Tomcat + spring-web/webmvc | ≈ 7.2 MB | REST 面从 Dubbo Netty 迁至 Servlet 栈的对价（旧栈无内嵌 Web 容器） |
| springdoc + swagger-core/ui | ≈ 2.1 MB | 替代 dubbo-rest-openapi |

**结论**：体积增长全部来自架构切换的固有对价与 nacos 依赖树的真实暴露，无异常膨胀；不裁剪（QUIC 原生包裁剪有二期 Nacos 复用风险，health/reflection 保留供 grpcurl 调试）。计划 §9 标准 4 的"显著缩小"预期被实证推翻，按本记录修订。

### 13.3 Phase 1 实施中的计划外发现（已处置）

| 发现 | 处置 |
|---|---|
| Boot 4.1 下"spring-security-config 在 + spring-security-web 缺失 + servlet web"组合导致 `ServletWebSecurityAutoConfiguration` 内省 `WebSecurityConfiguration` 抛 NoClassDefFoundError | common-security 取消 spring-security-web 排除，改为提供 permit-all + 无状态 SecurityFilterChain（`@ConditionalOnMissingBean` 可覆盖，`beforeName` 先于 Boot 默认安全链），符合 D12 边界语义 |
| `GrpcServerStartedEvent` 监听在 sample 测试上下文中未触发（spike 中可用，根因未深究） | 集成测试改用注入 `GrpcServerLifecycle#getPort()`（官方公开 API，更直接） |
| REST 面需要内嵌 Web 容器（Dubbo 时代无） | sample-server 显式引入 `spring-boot-starter-webmvc` |
| Jackson 2/3 在 Boot 4 共存（jackson-databind 2.21.4 + 3.1.4 同入 fat jar） | 记录在案；框架代码统一走 Jackson 3（common-exception params 编解码），Jackson 2 由第三方传递引入，不强裁 |
| PowerShell 5.1 `Set-Content -Encoding UTF8` 写入 BOM 导致 javac 报"非法字符 \ufeff" | 批量去 BOM 修复；后续脚本统一用 `UTF8Encoding($false)` |

## 14. 仓库外人工操作清单（Phase 3 周边，2026-08-12）

以下三项不在本仓库内，代码迁移已完成，需人工在对应系统执行：

| 系统 | 操作 | 说明 |
|------|------|------|
| Higress | 对外 REST 路由从 50051（Triple REST）改指向 MVC 端口 8080（含 context-path `/api`） | 不改 = 对外服务直接断；路由路径语义不变（`/api/orders` 等） |
| Higress | 确认东西向流量不经网关（gRPC 50051 仅集群内部） | 安全边界收敛要求（D12） |
| ywf-infra | Nacos 容器保留不动（二期注册发现/配置中心复用）；无需 Seata 变更（TC 配置不变） | 已核对 docker-compose：Nacos 为注释占位，无 dubbo 相关内容 |
| Apifox | OpenAPI 文档来源从 dubbo-rest-openapi 端点切换为 springdoc：`http://<host>:8080/api/v3/api-docs` | 导入后 REST 接口文档与迁移前等价 |

## 附录 A：目标版本矩阵

| 构件 | 现版本 | 目标版本 |
|---|---|---|
| spring-boot-starter-parent | 3.5.16 | **4.1.0**（spike 确认当前最新即 4.1.0；回退线 4.0.7 未触发） |
| spring-grpc | — | 1.1.0（自动配置由 Boot 4.1 原生托管，不自管 bom；starter 构件名 `spring-boot-starter-grpc-server` / `-client`） |
| protobuf-maven-plugin | — | `io.github.ascopes:protobuf-maven-plugin` 5.1.4（Boot 4.1 托管版本与执行配置） |
| mybatis-plus | mybatis-plus-spring-boot3-starter 3.5.17 | mybatis-plus-spring-boot4-starter **3.5.17**（spike 确认） |
| dynamic-datasource | dynamic-datasource-spring-boot3-starter 4.5.0 | dynamic-datasource-spring-boot4-starter **4.5.0**（spike 确认） |
| seata-spring-boot-starter | 2.6.0 | 2.6.0（不变） |
| seata-grpc | — | 2.6.0（新增：XID gRPC 透传 interceptor，spike 确认现成构件） |
| nacos-client | 3.2.3 | 3.2.3（不变） |
| dubbo-*（6 个构件） | 3.3.6 | **移除** |
| javax.ws.rs-api | 有 | **移除**（REST 面由 spring-web 原生注解承接，D11） |
| springdoc-openapi | — | springdoc-openapi-starter-webmvc-ui **3.1.0**（spike S5 确认） |

## 附录 B：二期展望（不在本计划范围）

- Nacos gRPC NameResolver（东西向注册发现）+ Nacos 配置中心恢复
- proto 契约的 buf lint / breaking change CI
- gRPC-Web（若前端直连 gRPC 有需求，Higress 支持转换）
- Servlet HTTP/2 模式评估：Boot 4.1 支持 gRPC 经 Servlet 容器暴露，理论上可把 REST/gRPC 收拢到单端口（与 D10 双端口模型互斥，需重新评估安全边界）
