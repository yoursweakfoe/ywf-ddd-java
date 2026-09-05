# Infrastructure 层 — 基础设施

## 职责

提供技术实现，通过**依赖倒置**实现 Domain 层定义的接口。
Domain 层定义"做什么"，Infrastructure 层决定"怎么做"。

## 设计原则

- **依赖倒置**：Infrastructure 依赖 Domain（实现其接口），Domain 不依赖 Infrastructure
- **可复用能力上提 common**：通用技术能力（MQ/Cache/OSS/通知）抽取为 common 模块，服务级 infra 不重复建设
- **服务级 infra 只放不可复用的、绑定本服务领域的实现**：persistence / gateway / config
- **按聚合自包含**：persistence 内每个聚合的 mybatis/converter/repository 在一起，打开即全貌

## 包结构

→ [directory-structure/server/infrastructure.md](../directory-structure/server/infrastructure.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（PO / Converter / RepositoryImpl）| [cookbook/new-aggregate.md](../cookbook/new-aggregate.md)（完整模板）

## 核心组件

### persistence/ — 持久化实现

实现 Domain 层 Repository 接口。分包：**数据源 → 聚合 → 技术/语义归属**。聚合命名空间下，纯 MyBatis 技术文件（PO / Mapper）归拢到 `mybatis/` 子目录；带独立身份锚点的文件（Converter 对偶框架 BasicConverter、RepositoryImpl 对偶 domain Repository）留在聚合根下。

| 组件 | 命名规范 | 准入规则 | 归属 |
|------|---------|--------|------|
| PO | `XxxPO`，零 ORM 注解（纯 `@Data` POJO） | 纯数据载体，无业务逻辑；表名 / 版本条件 / 逻辑删除过滤由 XML SQL 文本承担 | `mybatis/po/` |
| Mapper | `XxxMapper extends DddMapper<XxxPO>`，标注 `@Mapper` | 七条通用语句 + 全部业务查询一律手写 XML，无动态生成 | `mybatis/mapper/` |
| Mapper XML | `XxxMapper.xml`，namespace = Mapper 接口全限定名 | 每条真正执行的 SQL 的唯一事实源 | `resources/mapper/{agg}/` |
| Converter | `XxxConverter implements BasicConverter<D, P>` | 手动实现（富领域模型需 reconstitute） | 聚合根 `converter/` |
| Repository 实现 | `XxxRepositoryImpl implements XxxRepository` | 继承 `MybatisPersistence`，标注 `@Component` | 聚合根 `repository/` |

> **mybatis/ 边界**：仅收「撤换 ORM（如换 Hibernate）时需彻底删除」的纯技术文件。PO 与其手写 XML 语句共同构成 MyBatis 家族的映射层（接口方法 ↔ 语句 ID 一一对应），撤换后整体重建为 Entity + 注解映射；Mapper 本身即 MyBatis 独有概念（`@Mapper` + XML namespace），Hibernate 世界不存在同名对应物。Converter / RepositoryImpl 撤换后仅部分修改（改参数类型 / 重写实现体），故不进此目录。

`MybatisPersistence` 仅承载写侧聚合生命周期（load → 行为 → save）；读侧由独立的
`XxxQueryRepositoryImpl`（infra 读实现）直接用 Mapper 从 PO 投影读 DTO（PO → DTO 直接投影，
`PageResult<读 DTO>` 隔离底层分页形态），不经过 domain。

XML 每条语句的表名必须写死 schema 前缀（如 `orders.orders`），因为多数据源按聚合分包后，
同一数据源内不同聚合可能对应不同 schema，不能依赖连接默认 search_path。

MyBatis 配置（`mybatis.*` 命名空间，mybatis-spring-boot-starter）：

```yaml
mybatis:
  type-aliases-package: com.yoursweakfoe.xxx.infrastructure.persistence.master
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

XML 集中在 `src/main/resources/mapper/{agg}/`，namespace 绑定 Mapper 接口全限定名——
接口方法与语句 ID 一一对应，与 PO 同聚合目录语义一致（`persistence/master/order/mybatis/` ↔ `resources/mapper/order/`）。

### gateway/ — 外部系统网关实现

实现 Domain 层 Portal 接口。每个实现类完成两件事：

1. **技术调用**：对接具体外部资源（HTTP SDK、OSS Client、MQ Producer、ES Client 等）
2. **模型翻译（ACL）**：将外部系统的响应模型翻译为领域语言，防止外部概念污染领域

| | Repository → persistence | Portal → gateway |
|---|---|---|
| Domain 接口 | `domain/{agg}/repository/` | `domain/{agg}/portal/` |
| Infra 实现 | `infrastructure/persistence/` | `infrastructure/gateway/` |
| 操作对象 | 聚合的持久化（DB） | 外部资源（OSS/RPC/MQ/ES/第三方 API） |
| 语义 | "存取我的世界" | "打开传送门，获取外部能力" |

命名规范：
- Domain 接口以 `Portal` 结尾：`PaymentPortal`、`StoragePortal`
- Infra 实现以 `Gateway` 结尾：`AlipayPaymentGateway`、`AliOssStorageGateway`
- 实现类多于 3 个时，按外部能力分子包：`gateway/payment/`、`gateway/storage/`

→ 完整代码见 [cookbook/gateway.md](../cookbook/gateway.md)

### config/ — 全局配置

Spring `@Configuration` 类，存放**跨技术域的全局配置**。

- 技术域专属配置跟随 common 模块（如 MQ 配置在 common-mq 内）
- 当前内容：无（Domain Service 已改为 @Service 组件扫描注册，无需手动 Bean 注册配置）

## 协作关系

```
infrastructure → domain（实现 Repository / Portal 接口）
infrastructure → 外部框架/SDK（MyBatis、Alipay SDK、OSS Client 等）
```

- **domain** 定义接口（Repository / Portal），infra 提供实现
- **application** 通过 domain 接口间接使用 infra 实现（依赖倒置）
- infra 不被 domain / application 直接引用

## 专题

### 通用技术能力的归属决策

> 可复用的技术能力抽取为 common 模块，不在每个服务的 infra 层重复建设。
>
> | 技术能力 | 归属 | 理由 |
> |---------|------|------|
> | 仓储基类 | common-ddd（已有） | 所有服务都用，无业务逻辑 |
> | Redis 缓存工具 | common-cache（待建） | 连接配置、Cache-Aside 模板是通用的 |
> | OSS 文件存储 | common-storage（待建） | 上传/下载/签名 URL 是通用的 |
> | SMS/邮件通知 | common-notification（待建） | 发送能力是通用的 |
>
> 因此服务级 infra **不设** `messaging/`、`cache/`、`storage/`、`notification/` 目录。

### 多数据源规则

框架认可的多数据源方案为 **dynamic-datasource**（baomidou 独立模块，非 ORM 增强栈的组成部分）。2026-09 一手调研证实其与 ORM 增强框架零耦合：`DynamicRoutingDataSource` 直接继承 Spring `AbstractRoutingDataSource`——纯 MyBatis / JdbcTemplate / JPA 均可共用（独立性论证见 docs/common/common-ddd.md ADR-0007 决策 4）。框架测试套件在 `DynamicRoutingDataSource` 包裹下运行（双 H2 源路由），作为 `MybatisPersistence` 多数据源兼容性的真实库实证。

消费方接入方式（opt-in，示例应用不演示——多数据源非最小闭环）：

1. 服务 pom 引入 `dynamic-datasource-spring-boot4-starter`（版本由 ywf-ddd-common 的 dependencyManagement 统一管理）
2. `spring.datasource.dynamic` 配置多源 + primary；非默认数据源的仓储实现标注 `@DS("second")`
3. **跟进项**：SpEL 数据源表达式注入加固（PR #767）已合入 master 但**不在 4.5.0 发布内**——使用 SpEL DS 表达式的消费方，待 4.5.1+ 发布后升级

结构约束：

- 每个数据源一个顶级目录（`master/`、`second/`），**永远平级，不嵌套**
- 每个数据源内按聚合分包，聚合内部结构完全一致（mybatis/po/ + mybatis/mapper/ + converter/ + repository/，XML 归 `resources/mapper/{agg}/`）
- `@MapperScan` 按数据源分别扫描
- 默认数据源（master）的 RepositoryImpl 可省略 `@DS`
- Domain 层完全不感知数据源归属

### 为何数据源内按聚合分包

| 考量 | 说明 |
|------|------|
| 聚合自包含 | 一个聚合的 mybatis/converter/repository 在一起，打开即全貌 |
| 与 Domain 层对齐 | domain/{aggregate}/ ↔ persistence/{datasource}/{aggregate}/，映射清晰 |
| 拆分友好 | 微服务拆分时整个聚合目录迁走即可 |
| 结构一致 | 每个聚合内部结构相同，新人看一个即懂全部 |

### gateway 的边界

**属于 gateway**：
- 领域逻辑依赖的外部能力实现（如支付、汇率查询、文件存储）
- 每个实现类包含：调用 + 翻译 + 容错（超时/降级/重试）

**不属于 gateway**：
- 应用层编排型 RPC（如"下单后通知物流服务"）→ 应用层直接调用
- 查询组装型调用（如"查用户信息拼 DTO"）→ 应用层 Handler 内完成
- Repository 实现 → `persistence/`

## 规则

| 允许 | 禁止 |
|------|------|
| 实现 Domain 层定义的接口 | 在 PO / Repository 中写业务逻辑 |
| 使用框架注解（@Mapper、@DS、@Component） | 被 Domain 层引用 |
| ACL 翻译外部模型为领域模型 | 将外部 SDK 类型泄漏到 Domain 层 |
| 按聚合组织持久化代码 | 跨聚合共享 PO / Mapper |
| 复杂 SQL 写 MyBatis XML | 在 Repository 中直接拼接 SQL 字符串 |
