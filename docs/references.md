## 架构理论参考

### 分层架构与包结构

**采纳：**

| 架构 / 理论 | 本项目采纳要素 |
|------|--------|
| Evans DDD 四层 | adapter / application / domain / infrastructure 分层基准 |
| Clean Architecture (Robert C. Martin) | 依赖规则——内层不知道外层存在；Domain 零框架依赖 |
| Onion Architecture (Jeffrey Palermo) | 同心圆分层，Domain 在核心，依赖方向始终向内 |
| Hexagonal / Ports & Adapters (Cockburn) | adapter 命名（in/out 方向）、Portal/Gateway 对偶、Domain 通过端口与外界交互 |
| COLA (张建飞) | 单 Module + Package 分层、adapter 命名、按聚合分包 |
| Screaming Architecture (Uncle Bob) | 包结构按聚合名尖叫业务语义（order/ product/），而非按技术类型（entity/ vo/ service/） |
| Package by Feature | 按聚合自包含（打开即全貌），而非按技术类型分包 |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Package by Type (entity/ + vo/ + service/) | 传统 Spring MVC 项目、部分 MyBatis 脚手架 | 破坏聚合内聚性；类型区分已由继承关系表达，无需目录重复 |
| 多 Module 分层 (domain-module + infra-module) | DDD-Lib、jMolecules、部分 COLA 变体 | 单 Module + Package 分层已足够；多 Module 增加构建复杂度，小团队无收益 |
| 六边形架构多端口命名 (XxxInputPort / XxxOutputPort) | Hexagonal 原著、Alistair Cockburn | 本项目用 Portal/Gateway 对偶替代，语义更直观；避免 Port 命名泛滥 |

### DDD 战术模式

**采纳：**

| 模式 | 本项目采纳要素 |
|------|--------|
| Aggregate (Evans) | 聚合根封装所有业务规则，外部不可绕过聚合根修改内部状态；按聚合分包 |
| Rich Domain Model (Fowler) | 充血模型——行为内聚于聚合根方法，不暴露 setter；渐进式充血（领域服务为过渡态） |
| Value Object (Evans) | 不可变、属性值判等、推荐 Java record 实现 |
| Repository (Evans / Fowler) | Domain 层定义接口，Infrastructure 层实现；写侧 reconstitute 聚合，读侧投影 DTO |
| Domain Event (Evans) | registerEvent → save → publish；事件是模型的组成部分；进程内 Spring Event |
| Factory (Evans) | 复杂创建逻辑抽离为独立工厂，仅当构造器不足以表达创建语义时使用 |
| Domain Service (Evans) | 跨聚合协调 / 逻辑不自然归属任何实体时使用；无状态 |
| Bounded Context (Evans) | 每个微服务 = 一个限界上下文；contract 模块定义上下文对外边界 |
| Shared Kernel (Evans) | common-contract / common-ddd 为多个限界上下文共享的构建块 |
| Customer-Supplier (Evans) | contract jar 是消费方唯一依赖；CO 变更需协调消费方（Breaking Change） |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Specification 模式 | Evans 原著、Spring Data JPA Specification、jMolecules | MyBatis-Plus `LambdaQueryWrapper` 已是类型安全可组合查询规约；充血模型下校验内聚于聚合根；CQRS 分离后无"同一规则既做校验又做查询"场景 |
| 具名领域异常 | Evans 原著、Vernon IDDD、多数 DDD 开源项目 | 统一 BusinessException + i18n 错误码；具名异常导致类爆炸且仍需转换为错误码 |
| 领域层异常目录 (exception/) | 多数 DDD 开源项目、COLA 示例 | 显式 if-throw + 错误码已足够，不设 exception/ 包 |
| 聚合根 ID 自动生成策略 | COLA、Axon Framework、Spring Data | ID 生成与业务强相关（UUID / 雪花 / 业务编码），由子类构造器自行决定 |
| 脏检查 / 变更追踪 (Unit of Work) | JPA/Hibernate、Axon Framework | 采用全量 UPDATE 策略，MyBatis-Plus 场景下脏检查收益极低且增加复杂度 |
| 仓储泛型分页方法 | COLA、多数 MyBatis-Plus 脚手架 | 已实现 `findDomainPage` + `PageResult<Domain>`，但不在 Domain 层 Repository 接口暴露（分页属于读侧 CQRS Query） |
| 领域事件异步/跨进程发布 | Axon Framework、EventStoreDB、Kafka + Outbox | 当前为进程内 Spring Event；跨服务通过 Seata + RPC 显式调用，不引入 MQ 耦合 |

### CQRS 与事件架构

**采纳：**

| 模式 | 本项目采纳要素 |
|------|--------|
| CQRS (Greg Young) | Command / Query / Event 三通道分离；写侧走聚合根，读侧绕过聚合根；PageableQuery + PageResult 框架级分页 |
| Integration Event / EDA | 领域事件（进程内）→ 集成事件（跨服务 MQ）；DomainEvent vs Event 方向对偶 |
| Saga / Process Manager | 无主长流程引入独立 Saga 服务，不在业务服务内塞入跨服务编排 |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Mediator / Dispatcher (MediatR) | MediatR (.NET)、Spring Modulith、COLA ExtensionExecutor | Handler 数量少时直接注入更简单透明；引入 Mediator 增加间接层但无实际收益 |
| Event Sourcing | Axon Framework、EventStoreDB、Greg Young | 当前业务无审计回放 / 时间旅行需求；CRUD + 乐观锁已满足 |
| Outbox 模式（可靠事件发布） | Chris Richardson、Debezium、Microsoft eShop | 领域事件为进程内 Spring Event；跨服务通过 Seata + RPC，无需消息中间件配套 |
| 读模型投影 / 物化视图 | Greg Young CQRS、EventStoreDB、Axon | CQRS 读侧直接通过 Repository 投影 DTO，数据量未达需要物化视图的规模 |
| Change Data Capture (CDC) | Debezium、Canal、Maxwell | 无事件溯源 / 实时同步需求，不引入额外中间件 |
| 事件存储 (Event Store) | EventStoreDB、Axon Server | 非 Event Sourcing 架构，无事件持久化重放需求 |
| Application Service 拆分 Command/Query 两个类 | 部分 CQRS 严格实践 | 一个聚合一个 AppService 已足够内聚；拆分增加类数量无实际收益 |

### 架构模式与设计模式

**采纳：**

| 模式 | 本项目采纳要素 |
|------|--------|
| Anti-Corruption Layer (Evans DDD) | Gateway 实现内部将外部 SDK 模型翻译为领域语言，防止外部概念污染 Domain |
| Strategy Pattern (GoF) | Domain Policy——isApplicable + 业务方法；三种形态（互斥 / 叠加 / 精准路由）；OCP |
| Facade Pattern (GoF) | adapter/web 与 adapter/grpc 纯透传 AppService，不含业务逻辑、不含转换 |
| Presenter / ViewModel (MVP 变体) | Handler 返回 DTO（内部视图），AppService 通过 Presenter 呈现为 CO（外部安全视图） |
| Contract-First / API-First | contract 模块纯类型定义、零实现；消费方仅依赖 contract jar |

### SOLID 与工程原则

| 原则 | 本项目体现 |
|------|--------|
| 依赖倒置 (DIP) | Domain 定义 Repository / Portal 接口，Infrastructure 实现；Application 只依赖 Domain 接口 |
| 单一职责 (SRP) | Handler 与 CQE 1:1；一个聚合一个 AppService；Policy 一条规则一个类 |
| 开闭原则 (OCP) | Policy 新增规则只需加新类，不改旧代码；Handler 新增用例不影响已有 Handler |
| 关注点分离 | DTO（内部）vs CO（外部）强制分离；Assembler vs Presenter 两层转换 |
| 最小知识 / 迪米特法则 | adapter 不认识 Handler / Domain；Handler 不认识 Mapper / PO；消费方只看到 contract |
| 显式依赖 / 无隐式路由 | 无 Bus/Mediator；依赖可见、Ctrl+Click 可达 |

### 微服务治理

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| 服务熔断/降级 (Sentinel / Resilience4j) | Spring Cloud Alibaba、Netflix OSS | 当前服务规模小，gRPC deadline/重试已够用；引入 Sentinel 增加部署复杂度 |
| 服务网格 / Sidecar (Istio / Linkerd) | CNCF 生态 | 当前部署规模不需要 Mesh；Higress 网关已提供流量治理能力 |
| 灰度发布 / 流量染色 SDK | Spring Cloud Alibaba | 由 Higress 网关层路由规则实现，不需要 SDK 级支持 |
| 配置中心封装 (Nacos Config Starter) | Spring Cloud Alibaba | 各服务已直接使用 `spring.config.import=nacos:` 按需接入，无需框架封装 |
| 链路追踪 SDK (SkyWalking / Zipkin) | Spring Cloud Sleuth、SkyWalking | 由 OTel Java Agent 零侵入方式覆盖，不在代码中引入 SDK |
| API 版本管理框架 | Spring Boot、API 网关插件 | REST 路径由 Spring MVC 显式声明，天然支持版本（`/v1/orders`），无需框架级抽象 |
| 幂等性框架 | 各类幂等 starter、分布式锁方案 | 幂等逻辑与业务强相关（唯一键、状态机、Token），通用抽象反而增加理解成本 |
| 多租户 | MyBatis-Plus TenantLineInnerInterceptor | 当前业务无多租户需求；可按需在业务项目中自行开启 |

### 安全与认证

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| JWT 验签 / Token 刷新 | Spring Security OAuth2、Keycloak | 验签由 Higress 网关 jwt-auth 插件统一处理；微服务不持有密钥 |
| URL 级鉴权 FilterChain | Spring Security 官方 | 鉴权决策收口在网关；服务层仅提供 permit-all 边界链（common-security），方法级用 `@PreAuthorize` |
| RBAC 权限模型（数据库存储） | Spring Security、Apache Shiro | 角色/权限管理属于业务域，各服务按需实现；本框架只提供 Header→Context 桥接 |
| OAuth2 / SSO 登录流程 | Spring Authorization Server、Keycloak | 登录由独立认证服务 + 网关处理，业务微服务不参与登录流程 |
| 数据权限（行级过滤） | MyBatis-Plus DataPermissionInterceptor | 数据权限与业务模型强耦合，由业务层 SQL 条件自行实现 |

### 测试与工程

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Testcontainers | Spring Boot 官方推荐 | 各服务数据库/中间件组合不同，由业务项目自行引入 |
| 契约测试 (Spring Cloud Contract / Pact) | Spring Cloud、Pact Foundation | 东西向通过 proto 契约（强类型，编译期生成 stub）通信，编译期即可发现契约不兼容 |
| 测试数据工厂 (Fixture Builder) | Test Data Builder 模式、Instancio | 领域对象构造与业务强相关，通用工厂反而增加维护成本 |
| 性能/压力测试工具 | JMeter、k6、Gatling | 属于 CI/CD 流水线职责，不纳入代码仓库依赖 |

### 框架与工具链

**采纳：**

| 决策 | 本项目采纳要素 |
|------|--------|
| RFC 9457 Problem Details | REST 错误响应贴靠标准（type/title/status/detail/instance + `application/problem+json`），外部消费方可程序化处理 |
| JDK 21 虚拟线程 | `spring.threads.virtual.enabled: true`；Tomcat/Spring 异步/定时任务均用虚拟线程；禁止 synchronized（pinning） |
| RPC 异常不透传内部堆栈 | GrpcExceptionServerInterceptor 映射为 Status + Trailers，客户端拦截器还原 BusinessException，Consumer 仅收到 messageKey + params |
| PG TypeHandler 自动注册 | PgTypeHandlerAutoConfiguration 启动时批量注册，无需配置 type-handlers-package；@MappedTypes 自动路由 |
| 模式匹配 switch | 状态转换守卫使用 JDK 21 穷尽性 switch，新增枚举值时编译器强制处理 |
| springdoc-openapi | REST 面 OpenAPI 3.0 文档（`/v3/api-docs` + Swagger UI），配合 Apifox 导入同步 |

**未采纳：**

| 框架 / 工具 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Axon Framework | AxonIQ | 全套 Event Sourcing + CQRS 框架，过重；本项目只需轻量 CQRS 分离 |
| jMolecules | xMolecules 项目 | DDD 注解库（@AggregateRoot、@Repository），本项目用 common-ddd 构建块替代 |
| Spring Modulith | Spring 官方 | 模块化单体框架，本项目已是微服务架构，无需模块级事件/验证 |
| MapStruct（代码生成映射） | 多数 CRUD 脚手架 | 已彻底移除：AI 辅助开发下手写模板代码成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在；Converter/Assembler/Presenter 统一纯手写显式映射，富领域模型走 reconstitute，完整性由往返测试守护 |
| Lombok @Data 用于领域模型 | 多数业务项目 | 充血模型禁止暴露 setter；@Data 生成 equals/hashCode 与 Entity ID 判等冲突 |

### 书籍与文章

| 来源 | 关联要素 |
|------|--------|
| 《Domain-Driven Design》 Eric Evans | 聚合、实体、值对象、领域事件、Repository、Factory、Domain Service、ACL、Bounded Context |
| 《Implementing Domain-Driven Design》 Vaughn Vernon | 聚合设计、事件驱动、按聚合分包、充血模型实践 |
| 《Clean Architecture》 Robert C. Martin | 依赖规则、分层边界、Screaming Architecture |
| 《Patterns of Enterprise Application Architecture》 Martin Fowler | Repository、DTO、Anemic/Rich Domain Model、Optimistic Concurrency |
| 《Design Patterns》 GoF | Strategy（Policy）、Facade（adapter） |
| 《Implementing CQRS and Event Sourcing》 Microsoft | CQRS 读写分离、Command/Query 分离、读模型投影 |
| COLA 4.x 开源架构 (张建飞) | 分层结构、adapter 命名、应用层编排模式 |
| 《微服务架构设计模式》 Chris Richardson | Saga、Integration Event、服务拆分策略 |
