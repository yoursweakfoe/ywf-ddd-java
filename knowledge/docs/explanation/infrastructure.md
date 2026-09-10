# Infrastructure 层 — 基础设施

## 职责

提供技术实现，通过**依赖倒置**实现 Domain 层定义的接口。
Domain 层定义"做什么"，Infrastructure 层决定"怎么做"。

## 设计原则

- **依赖倒置**：Infrastructure 依赖 Domain（实现其接口），Domain 不依赖 Infrastructure
- **可复用能力上提 common**：通用技术能力（MQ/Cache/OSS/通知）抽取为 common 模块，服务级 infra 不重复建设
- **服务级 infra 只放不可复用的、绑定本服务领域的实现**：persistence / gateway / config
- **按聚合自包含**：persistence 内每个聚合的 mybatis/converter/repository 在一起，打开即全貌
- **手写端到端**：真正执行的 SQL 与对象映射都是仓库里可见的代码，不依赖运行时生成 / 代理魔法（论证见 persistence 节「手写教义」小节）

## 包结构

→ [aggregate-blueprint §5](../../specs/current/patterns/aggregate-blueprint.md)

> 完整代码示例 → [how-to/write-path.md](../how-to/write-path.md)（PO / Converter / RepositoryImpl）| [how-to/new-aggregate.md](../how-to/new-aggregate.md)（完整模板）

## 核心组件

### persistence/ — 持久化实现

实现 Domain 层 Repository 接口。分包：**数据源 → 聚合 → 技术/语义归属**。聚合命名空间下，纯 MyBatis 技术文件（PO / Mapper）归拢到 `mybatis/` 子目录；带独立身份锚点的文件（Converter 对偶框架 BasicConverter、RepositoryImpl 对偶 domain Repository）留在聚合根下。

| 组件 | 命名规范 | 准入规则 | 归属 |
|------|---------|--------|------|
| PO | `XxxPO`，零 ORM 注解（纯 `@Data` POJO） | 纯数据载体，无业务逻辑（持久化语义见下方指针） | `mybatis/po/` |
| Mapper | `XxxMapper extends DddMapper<XxxPO>`，标注 `@Mapper` | 七条通用语句契约 + 业务具名查询，全部手写 XML | `mybatis/mapper/` |
| Mapper XML | `XxxMapper.xml`，namespace = Mapper 接口全限定名 | 每条真正执行的 SQL 的唯一事实源 | `resources/mapper/{agg}/` |
| Converter | `XxxConverter implements BasicConverter<D, P>` | 手动实现（富领域模型需 reconstitute） | 聚合根 `converter/` |
| Repository 实现 | `XxxRepositoryImpl implements XxxRepository` | 继承 `MybatisPersistence`，标注 `@Component` | 聚合根 `repository/`（写读两侧 Impl **同包平铺**，读实现 `XxxQueryRepositoryImpl` 以类名后缀区分） |

> **mybatis/ 边界**：仅收「撤换 ORM 时需彻底删除」的纯技术文件（PO / Mapper 及其 XML），Converter / RepositoryImpl 留聚合根下。完整论证（为何 PO+Mapper 整体属 MyBatis 家族、撤换后各自删还是改）→ canonical 见 [aggregate-blueprint §5](../../specs/current/patterns/aggregate-blueprint.md) 的 mybatis/ 边界注记，本文不复制。

`MybatisPersistence` 基类方法语义与 `DddMapper<PO>` 七条通用语句的 XML 契约（insert / updateById 乐观锁条件 / selectById / deleteById 逻辑删除 / existsById 等）→ canonical 详表见 [common-ddd §2 仓储支撑](../reference/api/common-ddd.md#2-核心能力)，此处不复述。分层职责只此一句：

- **写侧**：`MybatisPersistence` 承载聚合生命周期（load → 行为 → save），事务由 CommandHandler 声明
- **读侧**：独立 `XxxQueryRepositoryImpl` 用 Mapper 从 PO 直接投影读 DTO，不经过 domain（→ [read-path.md](../how-to/read-path.md)）

XML 每条语句的表名必须写死 schema 前缀（形如 `{schema}.{table}` 两段式），因为多数据源按聚合分包后，
同一数据源内不同聚合可能对应不同 schema，不能依赖连接默认 search_path。schema 本身凭什么这么划、名字凭什么这么起 → 专题节「为何 PG 原生形状与 schema 命名法」。

MyBatis 配置（`mybatis.*` 命名空间，mybatis-spring-boot-starter）：

```yaml
mybatis:
  type-aliases-package: com.yoursweakfoe.xxx.infrastructure.persistence.master
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

XML 集中在 `src/main/resources/mapper/{agg}/`，与 PO 同聚合目录镜像对应（`persistence/master/{agg}/mybatis/` ↔ `resources/mapper/{agg}/`）；namespace 与语句 ID 的绑定规则属持久化契约，见上方 §2 指针。

#### 手写教义 — SQL 真相住在仓库里

PO 零 ORM 注解、Mapper 手写七条语句、Converter 手写映射，不是三个孤立口味，而是同一决策的三根柱子，脊柱一句话：**从领域模型追到一条真正执行的 SQL，任何一跳都不许跳出仓库**。读者（和 AI）能 grep 到、能评审到、能直接引用——这是本框架「全链路上下文人机共同可理解」这一一等设计目标在技术侧的落点。

**为何逐出 ORM 增强栈（纯 MyBatis + 每聚合手写 XML）**。动态 SQL 拼装与拦截器织入（Wrapper、分页 / 逻辑删除 / 乐观锁插件）意味着真正执行的 SQL 不在代码库里：数据链路从 domain 追到 Repository，再追到增强框架的拼装处即断。于是 MyBatis-Plus 整体逐出，原增强能力的落点全部由 SQL 文本接管：乐观锁版本条件与全列覆写直接写进 `updateById`；逻辑删除过滤显式化为每条语句一个 `AND`，不需要逻辑删除的聚合直接写物理 DELETE——该语义降级为聚合级选择，基类不变；审计填充改为基类写库前显式调用填充器，替代隐式触发链；分页变成双语句共享条件片段 + 数据库原生 LIMIT/OFFSET，分页契约零改动；单查 / 计数以具名 Mapper 方法 + 具名语句按业务命名，基类不设通用条件查询通道。「防全表 UPDATE/DELETE」这类隐式防护同步从运行时拦截器降级为代码评审可见项：手写语句条条在场，无 WHERE 的全表操作是评审可查缺陷，不再是不透明的运行时黑盒。ORM 的可剥离性由此由架构实证而非「圈禁在 infrastructure 内不许越界」的纪律担保——纯技术文件撤换时整体删除即可（见上方 mybatis/ 边界注）。代价也是真实的：每聚合新增约 80–100 行手写 XML，漏写过滤 / 版本条件的风险由 XML 评审 checklist + 行为等价测试承接（防超卖并发测试是关键证人）；换来的是运行时插件栈归零、行为与 SQL 文本一一对应、依赖树纯净。

**为何全量 UPDATE 而非脏检查**。脏检查要求变更追踪设施（实体代理、字段级拦截），而本框架写模型下收益极低：写侧本就是 load → 聚合行为 → save 的完整重建，PO 由 Converter 完整装配，逐列全量覆写是自然形态——且顺带买到两个确定性：审计字段恒刷新；**null 就是业务要写的 null**，字段传 null 即真实清列，不存在「null = 不更新」的歧义。乐观锁版本条件与影响行数 0 的三分分类链，也都建立在这条全量语句之上。读侧投影不走这条路（PO 直接投影读 DTO），全量覆写的写码成本没有传染到查询侧。

**为何转换层手写（不用 MapStruct）**。与 SQL 论证同源：AI 辅助开发下手写模板成本归零，生成器的认知负担却一分不少——注解处理链、生成代码不可见、Lombok 桥接、`@MapperScan` 误扫，全是「运行时看不见、却会替你做事」的东西。手写换来映射可见、可 grep、可评审，聚合根 reconstitute 的完整性就有了明确守护人：**往返测试（round-trip test）是本决策的证人**。同一教义上行覆盖应用层——Assembler / Presenter 亦纯手写显式映射（→ [application.md](application.md)）。

三根柱子共用一个人观：看不见的代码不能评审，不能评审就不能信任——所以让真相留在文本里。

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
- 实现类一律按外部能力分子包（无条件口径，一个能力一个子包）：`infrastructure/gateway/{capability}/`，如 `gateway/payment/`、`gateway/storage/`——口径 canonical 见 [how-to/gateway.md](../how-to/gateway.md)「gateway 按外部能力分子包」要点行，本文不复制规则细节

> 上文 Payment / Storage / Alipay / AliOss 均为虚构教例，sample 未实现（示例应用刻意不演示 Portal/Gateway，完整走查见 how-to 篇）。

→ 完整代码见 [how-to/gateway.md](../how-to/gateway.md)

### config/ — 全局配置

Spring `@Configuration` 类，存放**跨技术域的全局配置**。

- 技术域专属配置跟随 common 模块（如 MQ 配置在 common-mq 内）
- 当前内容：无（Domain Service 已改为 @Service 组件扫描注册，无需手动 Bean 注册配置）

## 协作关系

本层实现 domain 定义的 Repository / Portal 接口（依赖倒置），application 经 domain 接口间接使用本层实现、本层不被 domain / application 直接引用；同时本层是服务内持有技术框架 / SDK（MyBatis、OSS Client 等，虚构教例）的唯一合法位——SDK 类型不外泄出本层。

→ 依赖方向法条（含结构图）canonical 在 [knowledge/specs/current/patterns/prohibitions.md](../../specs/current/patterns/prohibitions.md)「依赖方向」「依赖倒置」两节，ArchUnit 执法，本文不复制图。

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

框架认可的多数据源方案为 **dynamic-datasource**（baomidou 独立模块，非 ORM 增强栈的组成部分）。2026-09 一手调研证实其与 ORM 增强框架零耦合：`DynamicRoutingDataSource` 直接继承 Spring `AbstractRoutingDataSource`——纯 MyBatis / JdbcTemplate / JPA 均可共用；ORM 逐出时经一手 POM / 源码调研甄别去留，唯独保留它（完整叙事见上方 persistence 节「手写教义」，出处账目见篇脚）。框架测试套件在 `DynamicRoutingDataSource` 包裹下以真 PG 双源运行——同库异 schema 的两路 `currentSchema` 视图，一路有表、一路是必脱靶的「空库」教例，作为 `MybatisPersistence` 多数据源兼容性的真实库实证。

消费方接入方式（opt-in，示例应用不演示——多数据源非最小闭环）：

1. 服务 pom 引入 `dynamic-datasource-spring-boot4-starter`（版本由 ywf-ddd-common 的 dependencyManagement 统一管理）
2. `spring.datasource.dynamic` 配置多源 + primary；非默认数据源的仓储实现标注 `@DS("second")`
3. **跟进项**：SpEL 数据源表达式注入加固（PR #767）已合入 master 但**不在 4.5.0 发布内**——使用 SpEL DS 表达式的消费方，待 4.5.1+ 发布后升级

结构约束：

- 每个数据源一个顶级目录（`master/`、`second/`），**永远平级，不嵌套**
- 每个数据源内按聚合分包，聚合内部结构完全一致（mybatis/{po,mapper}/ + converter/ + repository/（写读两侧 Impl 同包，类名后缀区分），XML 归 `resources/mapper/{agg}/`）
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

### 为何 PG 原生形状与 schema 命名法

**表形状跟终库走，不跟测试工具走**。旧库表形状（VARCHAR 主键、缩写式审计列名、整型 version 等）是当年迁就测试库 H2 的能力天花板而成形的；裁决原则是**工具与测试环境的局限不得反向影响业务设计**。测试轨全面切换真 PG 后，迁就一次性退役，形状按 PostgreSQL 本家能力定：主键用原生 UUID（PG18 内建 uuidv7 工厂铸造，应用侧传值可覆盖）、时间用 timestamptz、审计列对齐 PG 规范的全词蛇形命名、半结构化扩展用 jsonb、版本与删除标记用原生 BIGINT / BOOLEAN——每一列都是类型本身，不是有损的字符串替身。库表形状唯一权威 = `db-migration` 变更集（仓库事实，随库演进），形状条款的法卷正身在聚合构建宪卷，本篇不复制 DDL。审计列改名当年有过**桥接案**（框架默认值保留旧缩写 `createAt`，消费方各自写配置映射到新列）——被终审否决：桥接等于让每个消费方为框架自己的畸形缩写长期缴纳配置税，默认值与真实形状逐字同构才配得上「消费方通常零配置」的承诺；改默认虽属破坏性变更，本仓无真实下游、痛感归零，一步到位。

**聚合边界 = schema 边界，领域词单数**。每个聚合的表住同名 schema，schema 名与 Java 聚合包名单数惯例逐字同构——代码边界与库边界互相印证，微服务拆分时整 schema 随聚合目录平移即可。领域词撞上 SQL 保留字时，在库层升格为行业 UB 术语，不允许引号包裹或前缀之类的方言逃逸——真实例：sample 的订单表库层名为 `sales_order`（Java 类名不动，仅库层更名）。理由：逃逸把冲突藏进每次书写里，UB 术语让改名在通用语言层一次完成，且语义更精确；名字一旦需要引号才存在，就等于库在教业务怎么改名。**单数之理**：领域是概念、概念没有复数——`products` 这类复数表名是「行集合视角」的数据库遗习，库里的表是概念的外延实例，与 Java 聚合包名单数惯例天然对齐。**类名不随迁之理**：升格只发生在库层，Java 类名保留 `Order`——「订单」在类、命令、方法语境里本就是自然口语，sales 限定属于库/上下文层信息，强灌进代码反而让每处调用都说黑话；两侧映射关系一句话记账于 [glossary 命名映射](../reference/glossary.md)，不做代码级改名。外部工具账表（Liquibase 账表、Seata undo_log）住各自独立 schema 或优先分库，与业务 schema 互不混列——工具状态不污染业务域，业务域整体迁移时也不拖着工具状态陪跑。

**类型映射：通用者自动注册，JSONB 必须显式**。同一根线上的两个小决策，一松一紧，都为了消费方零惊讶：

- UUID、数组等类型映射是全服务通用的资产，common-pg 经自动装配启动时批量注册——**引入依赖即获得完整类型映射**，没有逐服务的配置声明义务（自动装配教义）。
- JSONB 恰恰不能自动路由：它的 Java 侧载体往往就是 String，而 String 的类型槽位早被 MyBatis 默认处理器占据，路由器无从分辨「这个 String 参数是普通文本还是 jsonb 文档」——这是技术不可行，不是口味偏好。于是每处 JSONB 参数位 / 结果位在 XML 语句里显式声明 typeHandler。这条「必须写出来」的义务与「手写教义」严丝合缝：义务可见、可 grep，宁多写一点，不留隐式魔法。
- 使用场景与语句形状 → [common-pg 法卷](../../specs/current/modules/pg.md)（严格件），速查在 [reference/api/common-pg.md](../reference/api/common-pg.md)，本篇不复抄代码。



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

---
*本页属 `explanation/` 解读架（地图区）：与 `specs/current/` 法卷形状冲突时以法卷为准（宽严双份，法卷赢）。*

