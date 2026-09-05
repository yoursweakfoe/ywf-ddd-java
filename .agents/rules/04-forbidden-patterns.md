# 04 — 禁止清单

合并各层设计文档中的"禁止"规则为统一清单。违反任何一条即为架构违规。

## Domain 层禁止

- 禁止引入 Spring / MyBatis 框架**运行时**依赖（DI 容器、AOP、持久化 API 等）；唯一例外：`org.springframework.stereotype` 装配注解（如领域服务上的 `@Service`）——注解是纯元数据，不影响分层纯净性（ArchUnit R4 白名单守护，common-test 共享规则 `DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE`）
- 禁止暴露 setter 或 public 字段（状态变迁只通过行为方法）
- 禁止在 Domain 层实现 Repository（实现必须在 Infrastructure）
- 禁止跨聚合直接修改对方内部状态（通过 Repository 读取）
- 禁止定义具名领域异常类（统一 BusinessException + 错误码）
- 禁止使用 Lombok `@Data`（聚合根/实体/值对象手写 equals/toString）

## Application 层禁止

- 禁止在 Handler 内写业务规则（决策在聚合根方法内）
- 禁止 Handler 直接使用 Mapper / PO（破坏依赖方向）
- 禁止 AppService 包含编排逻辑（委托 Handler）
- 禁止 AppService / Handler 包含 if-else 业务判断
- 禁止 Handler 返回 CO（应返回 DTO，由 Presenter 转 CO）
- 禁止 CO 暴露内部实现细节（version / deleted / 内部评分）

## Adapter 层禁止

- 禁止业务规则判断
- 禁止直接调用 Handler（必须经 AppService）
- 禁止直接操作 Repository
- 禁止调用 Domain 层
- 禁止修改 Command/Query 内容
- 禁止调用 Assembler / Presenter

## Infrastructure 层禁止

- 禁止在 PO / Repository 中写业务逻辑
- 禁止被 Domain 层引用（方向反了）
- 禁止将外部 SDK 类型泄漏到 Domain 层（必须 ACL 翻译）
- 禁止跨聚合共享 PO / Mapper
- 禁止在 Repository 中直接拼接 SQL 字符串（全部 SQL 写手写 XML）

## Contract 模块禁止

- 禁止任何实现类（纯接口 + 数据载体）
- 禁止业务逻辑
- 禁止依赖 server 模块
- 禁止依赖 Spring / MyBatis **运行时基础设施**（DI / Bean / AutoConfiguration / 持久化）
- 允许（且应当）承载 HTTP 映射 + 文档 + 校验注解（重契约，见 docs/common/common-contract.md ADR-0003）：
  - `@RequestMapping` / `@GetMapping` 等（spring-web）
  - `@Tag` / `@Operation` / `@Schema`（swagger-annotations）
  - `@NotNull` / `@Valid` 等（jakarta.validation-api）
  均为注解级依赖；HTTP 映射声明在契约接口、经 adapter 实现类继承（契约 = 完整 REST 定义）

## 通用禁止

- 禁止 Mediator 模式（Handler 1:1 对应 CQE，无需中间路由）
- 禁止在核心代码（src/ / sample-application/）中嵌入 AI 工具专属指令
- 禁止 `LocalDateTime` / `ZonedDateTime` 作为持久化时间类型（统一 `OffsetDateTime`：前者写入依赖会话时区、读 `timestamptz` 抛异常；后者 pgjdbc 双向抛异常。论证见 common-ddd.md ADR-0006）

> Specification：common-ddd 提供最小纯接口实现，属既定采纳项（见 docs/references.md「采纳」表），
> 可用于领域规则的 and/or/not 组合校验；查询过滤用业务 Mapper 具名方法 + 手写 XML 动态条件（`<if>`），
> 简单校验仍优先聚合根内 if-throw。

## 持久化与 SQL（含 PO 强制约束）

- MyBatis-Plus 不进框架依赖树（持久化 = DddMapper 七语句 + 手写 XML，见下几条）；多数据源用 dynamic-datasource（BOM 推荐坐标，消费方 opt-in 引入，`@DS` 注解合法），规则库对此无专法
- 禁止 PO 携带任何 ORM 注解——PO 为纯 `@Data` POJO，全部持久化语义（表名 / 主键 / 版本条件 / 逻辑删除）由手写 XML 的 SQL 文本承担
- 禁止 Wrapper 式动态条件——查询条件一律落成具名 Mapper 方法 + 具名 XML 语句（`<sql>` 片段复用防语句漂移）
- 建表 DDL **默认含两列**：`version INT NOT NULL DEFAULT 0`（乐观锁）与 `is_delete BOOLEAN NOT NULL DEFAULT FALSE`（逻辑删除），PO 声明对应 `version`（Integer）与 `isDelete`（Boolean）字段；聚合显式豁免时，XML 省略对应条件（见下两条）
- 手写 XML 的 `updateById` 语句（有版本列的聚合）**必须**携带乐观锁条件：`SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false`——**无任何运行时拦截器织入**，版本条件缺失即并发缺陷（防超卖依赖它，行为由 sample `OptimisticLockConcurrencyTest` 实证）；影响行数 0 由 `MybatisPersistence` 经存在性探测分类为 `OptimisticLockConflictException`（可重试）或 `IllegalStateException`（实体已消失）；无版本列的聚合在 XML 省略版本条件即可
- 逻辑删除聚合的每条 select / update / delete 语句**必须**显式携带 `AND is_delete = false` 过滤——漏写一处即数据泄漏；不需要逻辑删除的聚合在 XML 写物理 `DELETE`

## Infrastructure 层最小化原则

- 禁止引入当前不使用的组件（“以后可能用到”不是理由）
- 禁止保留死代码（注释掉的代码块、TODO-restore、空实现）
- 禁止手动记录日志替代框架机制（如用 System.out 替代 SLF4J）
- 禁止在 infrastructure 层定义业务规则（仅做技术实现 + ACL 翻译）

## 虚拟线程兼容

- 禁止在生产代码中使用 `synchronized` 块/方法（导致虚拟线程 pinning）
- 需要互斥时使用 `ReentrantLock`（虚拟线程友好，不会钉住载体线程）

## Common 模块约束

**构件身份二分法**（common 模块依赖审查的单一事实源；审依赖先查登记，两种判据按身份分叉）：

- **工具库**：依赖 = 本包编译所需。审查判据：最小化——超出编译需要的条目即裁剪。
- **定型装配**（opinionated starter，即「我们做的是脚手架封装，不是库封装」）：依赖 = 使用方注定继承的**命运清单**（「引入本包者注定的依赖」，而非「本包编译用了什么」）。前提教义：所有采用方服务都是单 jar 全套四层，装配替使用方预先决策整条技术栈。

定型装配三条戒律：

1. **自我宣言在位**——命运清单与豁免边界写在该模块入口类/包 javadoc（读注释即得全图）。
2. **命运依赖必须被本包代码使用或封装**——pom 引入且代码零引用 = 裸传递（绑架使用方，违戒）；策略型能力封装为公开 API（判例：JUG → `domain.model.AggregateIds.mint()`，与 `Identifiable` 配对共居 model）。
3. **消费方经装配公开 API 使用命运能力**——不裸 import 命运库；换策略时框架一处改动、全员齐步。

**exclusions 卫生集中制**（对两种身份一体适用）：依赖排除只写在策略文件——`ywf-ddd-common/pom.xml` 与 `sample-application/pom.xml` 的 `<dependencyManagement>`；子 pom 声明处保持零 exclusions。判据来自 Maven 语义：无局部清单的声明自动继承 managed exclusions，而**任何局部 `<exclusions>` 块整体替换（非合并）managed 清单**——在声明处写一条排除即静默拆除该构件的全部集中卫生。判例：`ywf-ddd-common/pom.xml` 的 dynamic-datasource（Oracle UCP/ojdbc 排除）与 seata starter（fastjson/druid/dubbo-filter-seata 排除）条目。`<optional>/<scope>` 留在声明处（它们是消费姿态，非卫生政策）。

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
- 禁止 common 模块的 test scope 依赖泄漏给消费方（Maven test scope 不传递）
- 禁止新增 common 模块时不附带 `docs/common/common-{module}.md` 文档
- 禁止在 common 模块中硬编码业务包名（通过泛型 / SPI / 配置注入）

## Git 工作流（绝对约束）

- 禁止 Agent 执行任何 git 写操作（commit / push / reset / rebase / merge / cherry-pick / tag / branch -d 等）
- 禁止 Agent 修改 .git/ 目录下的任何文件
- 仅允许 git 只读操作（status / log / diff / show / blame）用于上下文理解
- 所有提交、分支管理、合并操作由人工手动完成
- .gitignore / .gitattributes 允许 Agent 修改（属于项目配置，非提交树）
