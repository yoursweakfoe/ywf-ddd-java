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
| Factory (Evans) | 复杂创建逻辑抽离为独立工厂，仅当构造器不足以表达创建语义时使用 |
| Domain Service (Evans) | 跨聚合协调 / 逻辑不自然归属任何实体时使用；无状态 |
| Specification (Evans) | 纯接口（可选工具）：领域规则的 and/or/not 可组合表达（null 安全），供规则复杂到值得命名的校验场景使用；查询过滤用具名 Mapper 方法 + 手写 XML 动态条件（读侧绕过 domain），简单校验仍用聚合根内 if-throw，不强制走规约 |
| Bounded Context (Evans) | 每个微服务 = 一个限界上下文；contract 模块定义上下文对外边界 |
| Shared Kernel (Evans) | common-contract / common-ddd 为多个限界上下文共享的构建块 |
| Customer-Supplier (Evans) | contract jar 是消费方唯一依赖；CO 变更需协调消费方（Breaking Change） |
| Published Language (Evans) | contract 模块即跨上下文共享语言：CQE / CO 是发布方与消费方的共同词汇表，避免逐点翻译 |
| Context Map 策略集 | 已知策略显式定档：Shared Kernel（common-contract/ common-ddd）、Customer-Supplier（contract jar）。Conformist（顺从外部模型）/ Open Host Service / Anti-Corruption 的上下文级 Partner 关系**按需在业务上下文引入**（当前无此场景，不预设）；Separate Ways（无协作上下文）默认为未协作服务的常态 |
| 同事务跨聚合写入（写路径强一致） | 本项目自定，**有意偏离** Vernon《IDDD》「一事务一聚合实例」经验法则：用户同步等待的写用例（如下单 = 扣库存 + 建订单）采用同事务 fail-fast——任一聚合校验失败整体回滚，以 PO version 列 + UPDATE 语句版本条件（手写 XML）防超卖替代事件补偿；事后副作用（取消订单回补库存）同样同事务直调补偿（`CancelOrderHandler` → DomainService，补偿与状态原子提交，无中间态）。跨服务一致性由 Seata + HTTP 显式调用承担。不采纳「先建单再异步扣库存」：会引入『单建成而库存未扣』的中间态难题，用户体验与实现复杂度双输 |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| 具名领域异常 | Evans 原著、Vernon IDDD、多数 DDD 开源项目 | 统一 BusinessException + i18n 错误码；具名异常导致类爆炸且仍需转换为错误码 |
| 领域层异常目录 (exception/) | 多数 DDD 开源项目、COLA 示例 | 显式 if-throw + 错误码已足够，不设 exception/ 包 |
| 聚合根 ID 自动生成策略 | COLA、Axon Framework、Spring Data | ID 生成与业务强相关（UUID / 雪花 / 业务编码），由子类构造器自行决定 |
| 强类型 ID / Domain Primitives 基类 | jMolecules、COLA、部分 Hexagonal 实践 | 裸 ID（UUID / Long）刻意开放——ID 类型由子类决定（common-ddd §ADR-0001）；仅当跨聚合引用、Money 等需要领域语义时才就地封装，框架不提供基类，cookbook 提供复制粘贴示例 |
| 脏检查 / 变更追踪 (Unit of Work) | JPA/Hibernate、Axon Framework | 采用全量 UPDATE 策略（XML 逐列枚举），本框架场景下脏检查收益极低且增加复杂度 |
| 仓储泛型分页方法 | COLA、多数 MyBatis-Plus 脚手架 | 读侧已改为 application 层 `XxxQueryRepository` 直接 PO → 读 DTO 投影（绕过 domain），分页不在 Domain 层 Repository 接口暴露（属读侧 CQRS Query） |

### CQRS 与事件架构

**采纳：**

| 模式 | 本项目采纳要素 |
|------|--------|
| CQRS (Greg Young) | Command / Query 双通道分离；写侧走聚合根，读侧绕过聚合根；PageableQuery + PageResult 同居契约层，框架级分页 |
| 标记接口定型体系 | 空标记接口 + ArchUnit 锚点：RestAdapter / ScheduledAdapter / ApplicationDTO / QueryRepository 各定型一层角色（REST 入口 / 定时任务入口 / 应用层内部视图 / 读端口），供架构规则按类型锚点识别与约束（非包名猜测） |
| Saga / Process Manager | 无主长流程引入独立 Saga 服务，不在业务服务内塞入跨服务编排 |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Mediator / Dispatcher (MediatR) | MediatR (.NET)、Spring Modulith、COLA ExtensionExecutor | Handler 数量少时直接注入更简单透明；引入 Mediator 增加间接层但无实际收益 |
| Event Sourcing | Axon Framework、EventStoreDB、Greg Young | 当前业务无审计回放 / 时间旅行需求；CRUD + 乐观锁已满足 |
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
| Facade Pattern (GoF) | adapter/rest 纯透传 AppService，不含业务逻辑、不含转换 |
| Presenter / ViewModel (MVP 变体) | Handler 返回 DTO（内部视图），AppService 通过 Presenter 呈现为 CO（外部安全视图） |
| Contract-First / API-First | contract 模块定义完整 REST 契约（Controller 契约接口 + CQE + CO + HTTP 映射注解 + 文档注解）、零实现；消费方仅依赖 contract jar |

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

**采纳：**

| 模式 | 本项目采纳要素 |
|------|--------|
| Resilience4j（熔断） | common-cloud 以 **optional** 提供 `spring-cloud-starter-circuitbreaker-resilience4j`（消费方按需显式声明），规则经 `resilience4j.*` 配置（common-cloud §ADR-0002）；采用 circuitbreaker-resilience4j，**不引入 Sentinel** |

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Sentinel（熔断/降级） | Spring Cloud Alibaba | 功能与 Resilience4j 重叠；Sentinel 控制台 + 配置体系增加部署复杂度 |
| 服务网格 / Sidecar (Istio / Linkerd) | CNCF 生态 | 当前部署规模不需要 Mesh；Higress 网关已提供流量治理能力 |
| 灰度发布 / 流量染色 SDK | Spring Cloud Alibaba | 由 Higress 网关层路由规则实现，不需要 SDK 级支持 |
| 配置中心封装 (Nacos Config Starter) | Spring Cloud Alibaba | 各服务已直接使用 `spring.config.import=nacos:` 按需接入，无需框架封装；common-cloud 仅以 **optional** 提供 nacos-config starter（消费方按需显式声明），不做强传 |
| 链路追踪 SDK (SkyWalking / Zipkin) | Spring Cloud Sleuth、SkyWalking | 由 OTel Java Agent 零侵入方式覆盖，不在代码中引入 SDK |
| API 版本管理框架 | Spring Boot、API 网关插件 | REST 路径由 Spring MVC 显式声明，天然支持版本（`/v1/orders`），无需框架级抽象 |
| 幂等性框架 | 各类幂等 starter、分布式锁方案 | 幂等逻辑与业务强相关（唯一键、状态机、Token），通用抽象反而增加理解成本 |
| 多租户 | MyBatis-Plus TenantLineInnerInterceptor | 当前业务无多租户需求；可按需在业务项目中自行开启 |

### 安全与认证

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| JWT 签发 / Token 刷新套件（Keycloak、Spring Authorization Server） | Spring Security OAuth2、Keycloak | 服务下沉为 OAuth2 资源服务器**自验 JWT**（零信任，`DelegatingJwtDecoder` 按 `alg` 分发，common-security §ADR-0005 / §ADR-0007），Higress 网关层仍可做粗筛（PEP）；签发 / 刷新 / 登出归独立认证服务（IdP），服务侧不提供签发能力，本框架不内置登录套件 |
| URL 级鉴权 FilterChain | Spring Security 官方 | 鉴权决策收口在网关；服务层仅提供 permit-all 边界链（common-security），方法级用 `@PreAuthorize` |
| RBAC 权限模型（数据库存储） | Spring Security、Apache Shiro | 角色/权限管理属于业务域，各服务按需实现；框架只提供身份原语——principal 为原生 `Jwt`、claims 按名字自取（common-security §ADR-0006），不投影固定身份结构 |
| OAuth2 / SSO 登录流程 | Spring Authorization Server、Keycloak | 登录由独立认证服务 + 网关处理，业务微服务不参与登录流程 |
| 数据权限（行级过滤） | MyBatis-Plus DataPermissionInterceptor | 数据权限与业务模型强耦合，由业务层 SQL 条件自行实现 |

### 测试与工程

**未采纳：**

| 模式 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Testcontainers | Spring Boot 官方推荐 | 各服务数据库/中间件组合不同，由业务项目自行引入 |
| 契约测试 (Spring Cloud Contract / Pact) | Spring Cloud、Pact Foundation | 东西向消费方依赖同一 contract jar（纯 Java 强类型契约），编译期即可发现契约不兼容 |
| 测试数据工厂 (Fixture Builder) | Test Data Builder 模式、Instancio | 领域对象构造与业务强相关，通用工厂反而增加维护成本 |
| 性能/压力测试工具 | JMeter、k6、Gatling | 属于 CI/CD 流水线职责，不纳入代码仓库依赖 |

### 框架与工具链

**采纳：**

| 决策 | 本项目采纳要素 |
|------|--------|
| RFC 9457 Problem Details | REST 错误响应贴靠标准（type/title/status/detail/instance + `application/problem+json`），外部消费方可程序化处理 |
| JDK 21 虚拟线程 | `spring.threads.virtual.enabled: true`；Tomcat/Spring 异步/定时任务均用虚拟线程；禁止 synchronized（pinning） |
| 异常不透传内部堆栈 | REST 通道 GlobalRestExceptionHandler（@RestControllerAdvice）将 BusinessException 统一映射为 HTTP 422 + RFC 9457 Problem Details，Consumer 仅收到 messageKey + params |
| PG TypeHandler 自动注册 | PgTypeHandlerAutoConfiguration 启动时批量注册，无需配置 type-handlers-package；@MappedTypes 自动路由 |
| 模式匹配 switch | 状态转换守卫使用 JDK 21 穷尽性 switch，新增枚举值时编译器强制处理 |
| 时间类型约定 | 全框架统一 `OffsetDateTime`，唯一时间源 = 框架注入 `Clock`；`timestamptz` 写入方偏移被丢弃、读回恒 `+00:00`（论证见 common-ddd §ADR-0006） |
| sealed 类型（框架适用性决策） | **不施加于框架扩展点**：`AggregateRoot` / `Entity` / `ValueObject` / `Repository` / `Policy` / `Portal` / `DomainService` 是业务扩展点，sealed 会锁死业务继承；框架内唯一封闭层级 `PgArrayType` 已是 enum。sealed / pattern-matching switch 留给**业务侧**：状态转换守卫在聚合根内使用 JDK 21 穷尽性 switch（框架无 enum-switch 场景，不强制） |
| swagger-annotations | REST 面文档注解（`@Operation` / `@Tag` / `@Schema` 纯注解 jar，零运行时零端点），契约层声明语义，配合 Apifox IDE 插件识别 |

**未采纳：**

| 框架 / 工具 | 常见出处 | 不采纳原因 |
|------|---------|----------|
| Axon Framework | AxonIQ | 全套 Event Sourcing + CQRS 框架，过重；本项目只需轻量 CQRS 分离 |
| jMolecules | xMolecules 项目 | DDD 注解库（@AggregateRoot、@Repository），本项目用 common-ddd 构建块替代 |
| Spring Modulith | Spring 官方 | 模块化单体框架，本项目已是微服务架构，无需模块级事件/验证 |
| MapStruct（代码生成映射） | 多数 CRUD 脚手架 | 未采纳（曾用后移除，定案见 common-ddd §ADR-0004）：AI 辅助开发下手写模板代码成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在；Converter/Assembler/Presenter 统一纯手写显式映射，富领域模型走 reconstitute，完整性由往返测试守护 |
| MyBatis-Plus（ORM 增强框架） | 国内 MyBatis 生态主流增强库 | 未采纳（2026-09 移除，common-ddd §ADR-0007）：Wrapper 动态生成 SQL + 拦截器织入使「真正执行的 SQL 不在代码库里」，与全链路可见性目标冲突；乐观锁 / 逻辑删除 / 审计填充 / 分页全部由每聚合手写 XML 的 SQL 文本承担。注：baomidou 系中独立于 ORM 增强的 dynamic-datasource 经一手调研证实与 MyBatis-Plus 零耦合，仍作消费方多数据源 opt-in 方案（见 docs/application/module-design/infrastructure.md） |
| Lombok @Data 用于领域模型 | 多数业务项目 | 充血模型禁止暴露 setter；@Data 生成 equals/hashCode 与 Entity ID 判等冲突 |

### 书籍与文章

| 来源 | 关联要素 |
|------|--------|
| 《Domain-Driven Design》 Eric Evans | 聚合、实体、值对象、Repository、Factory、Domain Service、ACL、Bounded Context |
| 《Implementing Domain-Driven Design》 Vaughn Vernon | 聚合设计、事件驱动、按聚合分包、充血模型实践 |
| 《Clean Architecture》 Robert C. Martin | 依赖规则、分层边界、Screaming Architecture |
| 《Patterns of Enterprise Application Architecture》 Martin Fowler | Repository、DTO、Anemic/Rich Domain Model、Optimistic Concurrency |
| 《Design Patterns》 GoF | Strategy（Policy）、Facade（adapter） |
| 《Implementing CQRS and Event Sourcing》 Microsoft | CQRS 读写分离、Command/Query 分离、读模型投影 |
| COLA 4.x 开源架构 (张建飞) | 分层结构、adapter 命名、应用层编排模式 |
| 《微服务架构设计模式》 Chris Richardson | Saga、服务拆分策略 |

## ADR 总索引

`docs/common/` 8 篇文档各自独立编号（ADR-0001 曾被 8 个模块重复定义），本表为全仓 ADR 的**唯一对照总索引**。md 层引用一律使用限定名 `模块 §ADR-000N`；废弃编号**保留空置、不重排、不迁移**。共 32 个编号位（29 条正文 + 3 个空置位）。

| 模块 §编号 | 决策 | 状态 |
|-----------|------|------|
| common-contract §ADR-0001 | 标记接口不含泛型 | 生效 |
| common-contract §ADR-0002 | 契约不含 REST/RPC 注解（轻契约） | 已废弃——被 §ADR-0003 取代（正文保留，标注 superseded） |
| common-contract §ADR-0003 | 契约承载 HTTP 映射 + 文档注解（重契约） | 生效 |
| common-ddd §ADR-0001 | 基类不持有 id/version 字段 | 生效 |
| common-ddd §ADR-0002 | 全量 UPDATE 而非脏检查 | 生效 |
| common-ddd §ADR-0003 | 领域事件自动发布 | 已废弃移除（2026-09 事件留白决策），无正文、编号空置 |
| common-ddd §ADR-0004 | 对象转换纯手写，不用 MapStruct | 生效 |
| common-ddd §ADR-0005 | CQRS 契约：Query 纯标记 | 生效（2026-08 修订） |
| common-ddd §ADR-0006 | 时间统一 OffsetDateTime + 统一注入 Clock | 生效（2026-09 补录） |
| common-ddd §ADR-0007 | 持久化手写 XML SQL 全面接管，移除 MyBatis-Plus | 生效（2026-09） |
| common-exception §ADR-0001 | i18n 位点（字符串 key）而非数字错误码 | 生效 |
| common-exception §ADR-0002 | RFC 9457 响应格式 | 生效 |
| common-exception §ADR-0003 | IllegalStateException → 409 | 生效 |
| common-cloud §ADR-0001 | 东西向通信统一 HTTP（移除 gRPC） | 生效 |
| common-cloud §ADR-0002 | 熔断降级用 Resilience4j 而非 Sentinel | 生效 |
| common-cloud §ADR-0003 | Nacos 经 SCA starter 引入，client 版本独立管理 | 生效 |
| common-cloud §ADR-0004 | Seata 独立构件 + 版本独立管理 | 生效 |
| common-cloud §ADR-0005 | Seata XID 透传不内置 | 生效 |
| common-cloud §ADR-0006 | 东西向身份传播：透传已验签 JWT（零信任） | 生效 |
| common-pg §ADR-0001 | TypeHandler 自动注册而非手动配置 | 生效 |
| common-pg §ADR-0002 | JSONB 需显式指定 typeHandler | 生效 |
| common-security §ADR-0001 | 网关验签 + 服务信任 Header | 已废弃——被 §ADR-0005 取代（正文保留废弃注记；旧模型勿再引用） |
| common-security §ADR-0002 | — | 已废弃，无正文（编号空置） |
| common-security §ADR-0003 | 边界 permit-all SecurityFilterChain | 生效 |
| common-security §ADR-0004 | — | 已废弃，无正文（编号空置） |
| common-security §ADR-0005 | 零信任：服务自验 JWT（资源服务器） | 生效 |
| common-security §ADR-0006 | 身份不投影：原生 Jwt + 按名字自取 | 生效 |
| common-security §ADR-0007 | 验签可插拔：JwtDecoder 抽象 + 多方案分发 | 生效 |
| common-observability §ADR-0001 | 日志 stdout 输出，不落盘文件 | 生效 |
| common-observability §ADR-0002 | OTel Java Agent 而非 SDK | 生效 |
| common-test §ADR-0001 | ArchUnit 而非人工 Code Review | 生效 |
| common-test §ADR-0002 | 规则集为静态常量 | 生效 |

> 注：Java javadoc 中仍存在裸引 `ADR-000N`（无模块前缀），限定名化列为后续代码窗口处理项，不属文档层范围。
