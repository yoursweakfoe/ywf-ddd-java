# 用法规范法卷：禁令全表（框架法 · 严格件）

> **身份**：本卷是所有「禁止」条款的一站式对照表——违反任何一条即架构违规；修卷走 `../../changes/`。前身 `.agents/rules/04`（兼收 02 的禁止面）升格入典（法案 `2026-10-rules-codification`）。ddd-review / PR 审查逐条对照本卷；已在他卷有正身者标互指。
> **机器对账**：C1/C3/C4 扫本卷；多数条款有 ArchUnit R## 或测试实证背书（括注处为取证）。

## §1 Domain 层禁止

- 禁止引入 Spring / MyBatis 框架**运行时**依赖（DI 容器、AOP、持久化 API）；唯一例外 `org.springframework.stereotype` 装配注解（纯元数据，R4 白名单：`DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE`）
- 禁止暴露 setter 或 public 字段（状态变迁只经行为方法；Lombok 律 → [CC-6](coding-conventions.md)）
- 禁止在 Domain 层实现 Repository（实现必须在 Infrastructure，AGENTS 九条 5）
- 禁止跨聚合直接修改对方内部状态（经 Repository 读取；协调律 → [cross-aggregate](cross-aggregate.md)）
- 禁止定义具名领域异常（统一 `BusinessException` + 错误码 → [EV-1](../modules/exception.md)；反例 `InsufficientStockException` 为在册反面教学例）
- 禁止使用 Lombok `@Data`（聚合根/实体/值对象手写 equals/toString，判等基于 ID）

## §2 Application 层禁止

- 禁止在 Handler 内写业务规则、含 if-else 业务判断（决策在聚合根 → WC-3）
- 禁止 Handler 直接使用 Mapper / PO（破坏依赖方向，ArchUnit R 系）
- 禁止 AppService 包含编排逻辑（只委托 Handler + Presenter → WC-4）
- 禁止 Handler 返回 CO（应返回 DTO，Presenter 收口 → WC-4/WC-5）
- 禁止 CO 暴露内部实现细节（version / deleted / 内部评分 → BP-9 互指）

## §3 Adapter 层禁止

- 禁止业务规则判断、禁止修改 Command/Query 内容（纯透传 → WC-4）
- 禁止越过 AppService 直接调用 Handler
- 禁止直接操作 Repository、禁止调用 Domain 层（R2）
- 禁止调用 Assembler / Presenter

## §4 Infrastructure 层禁止

- 禁止在 PO / Repository 中写业务逻辑（仅技术实现 + ACL 翻译 → GW-2）
- 禁止被 Domain 层引用（方向即违法）
- 禁止外部 SDK 类型泄漏到 Domain（必须 ACL 翻译 → GW-2）
- 禁止跨聚合共享 PO / Mapper（聚合自包含 → [blueprint §5](aggregate-blueprint.md)）
- 禁止在 Repository 拼接 SQL 字符串——一切 SQL 落手写 XML（见 §6）
- 最小充分原则：禁止引入当前不使用的组件（「以后可能用到」不是理由）；禁止死代码（注释块、TODO-restore、空实现）；禁止 `System.out` 替代 SLF4J

## §5 Contract 模块禁止

- 禁止任何实现类 / 业务逻辑 / 依赖 server 模块
- 禁止依赖 Spring / MyBatis **运行时基础设施**（DI / Bean / AutoConfiguration / 持久化）
- 允许（且应当）承载 HTTP 映射 + 文档 + 校验注解（重契约单一事实源，ADR-0010 判例）：`@RequestMapping` 系 / `@Tag` `@Operation` `@Schema` / `@NotNull` `@Valid`——均为注解级依赖，映射经 ControllerImpl 继承承载 → [CC-7](coding-conventions.md)

## §6 持久化与 SQL 铁律

- 禁止 MyBatis-Plus 进框架依赖树（持久化 = `DddMapper` 七语句 + 手写 XML；ADR-0007 判例）。dynamic-datasource 为经一手调研证实零耦合的多数据源 opt-in 方案，`@DS` 合法
- 禁止 PO 携带任何 ORM 注解（纯 `@Data` POJO；表名/主键/版本条件/逻辑删除全在 XML SQL 文本 → BP-X2）
- 禁止 Wrapper 式动态条件——查询一律具名 Mapper 方法 + 具名 XML 语句（`<sql>` 片段复用防漂移）
- 建表 DDL 默认含 `version INT NOT NULL DEFAULT 0` + `is_delete BOOLEAN NOT NULL DEFAULT FALSE`，PO 声明对应字段；显式豁免的聚合 XML 省略对应条件（逐聚合自决，无共享开关 → BP-12）
- `updateById`（有版本列）**必须**携 `SET version = version + 1 ... AND version = #{version} AND is_delete = false`，无运行时拦截器；0 行后果三分通道 → [optimistic-lock OL-1](optimistic-lock.md)（行为由 sample `OptimisticLockConcurrencyTest` 实证）
- 逻辑删除聚合的每条 select/update/delete **必须**显式 `AND is_delete = false`——漏一处即泄漏；豁免聚合写物理 `DELETE`

## §7 时间与线程

- 禁止 `LocalDateTime` / `ZonedDateTime` 作持久化时间类型（统一 `OffsetDateTime`，ADR-0006 判例：前者写入依赖会话时区、读 `timestamptz` 抛异常；后者 pgjdbc 双向抛异常）→ [CC-5](coding-conventions.md)
- 禁止生产代码使用 `synchronized` 块/方法（虚拟线程 pinning）；互斥用 `ReentrantLock`（AGENTS 九条 8；项目启用 JDK21 虚拟线程。ThreadLocal 正常；身份上下文由 Security 链管理，业务代码勿手工清理）

## §8 通用禁止

- 禁止 Mediator 模式（Handler 与 CQE 1:1，无中间路由——显式依赖教义）
- 禁止在核心代码（`src/` / sample 树）嵌入 AI 工具专属指令（未入围扩展点按需引入见 theory-map；Specification 为已采纳最小接口，非禁令对象）

## §9 Common 模块约束（构件身份二分法 · 单一事实源）

审 common 依赖先查登记表；两种判据按身份分叉：

- **工具库**：依赖 = 本包编译所需；判据：最小化，超出即裁剪。
- **定型装配**（opinionated starter，「我们做的是脚手架封装，不是库封装」）：依赖 = 使用方注定继承的**命运清单**。教义前提：所有采用方服务都是单 jar 全套四层，装配替使用方预先决策整条技术栈。

定型装配三条戒律：

1. **自我宣言在位**——命运清单与豁免边界写在该模块入口类/包 javadoc（读注释即得全图）。
2. **命运依赖必须被本包代码使用或封装**——pom 引入且代码零引用 = 裸传递（绑架使用方，违戒）；策略型能力封装为公开 API（判例：JUG → `AggregateIds.mint()`，与 `Identifiable` 配对共居 model）。
3. **消费方经装配公开 API 使用命运能力**——不裸 import 命运库；换策略时框架一处改动、全员齐步。

**exclusions 卫生集中制**（两种身份一体适用）：依赖排除只写在策略文件——`ywf-ddd-common/pom.xml` 与 `sample-application/pom.xml` 的 `<dependencyManagement>`；子 pom 声明处零 exclusions。判据来自 Maven 语义：无局部清单自动继承 managed exclusions，而任何局部 `<exclusions>` 块**整体替换**（非合并）managed 清单——声明处写一条排除即静默拆除全部集中卫生。判例：dynamic-datasource（Oracle UCP/ojdbc）与 seata starter（fastjson/druid/dubbo-filter-seata）条目。`<optional>/<scope>` 留在声明处（消费姿态，非卫生政策）。

| 模块 | 登记身份 | 备注 |
|------|---------|------|
| common-ddd | 定型装配 | MyBatis starter / JDBC 栈 = 命运；JUG 经 `AggregateIds` 封装（戒律②③判例）；宣言见 `MybatisDddAutoConfiguration` |
| common-observability | 纯装配（判例） | 零代码 pom 聚合——命运清单即其全部内容 |
| common-test | 定型装配 | ArchUnit + Boot Test 栈随引入即得 |
| common-exception | 定型装配（倾向） | validation starter = 命运：引入全局异常处理即承诺校验栈 |
| common-contract | 工具库 | 纯标记接口 |
| common-pg | 工具库 | TypeHandler 按需引入 |
| common-security / common-cloud | 工具库 | optional 依赖策略属工具库姿态（谁引入谁决策） |

- 禁止 common 模块包含任何业务逻辑（纯技术骨架）
- 禁止 common 模块 test scope 依赖泄漏给消费方（Maven test scope 不传递）
- 禁止新增 common 模块不附 `knowledge/docs/reference/api/common-{module}.md` 文档
- 禁止 common 模块硬编码业务包名（经泛型 / SPI / 配置注入）

## §10 Git 工作流（对 worker 的绝对法）

| # | 条款 | 背书 |
|---|---|---|
| PB-G1 | 禁止 Agent 执行任何 git **写**操作（commit / push / reset / rebase / merge / cherry-pick / tag / branch -d 等） | 人工提交惯例（C6 执法时以 `git diff` 只读取证） |
| PB-G2 | 禁止 Agent 修改 `.git/` 目录下任何文件 | — |
| PB-G3 | 仅允许 git 只读操作（status / log / diff / show / blame）用于上下文理解 | check-docs C6 自身遵守此法（只读 diff） |
| PB-G4 | 所有提交、分支管理、合并由人工完成 | 本 session 全部法案的落地方式即判例 |
| PB-G5 | `.gitignore` / `.gitattributes` 允许 Agent 修改（项目配置，非提交树） | — |

## §11 生效登记

全卷 ✅（各条 ArchUnit/测试/javadoc 背书签注于条款行；§9 登记表随模块增废经法案更新——本表即模块身份唯一登记处，javadoc 自宣言为二道mirror）。
