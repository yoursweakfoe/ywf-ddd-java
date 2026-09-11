# Domain 层 — 核心业务逻辑

## 职责

domain 层承载核心业务逻辑，**零框架依赖**。它是整个系统最稳定、最有价值的部分。

## 设计原则

- **按聚合分包**：每个聚合根一个顶级子包，内部结构一致。
- 聚合根封装所有业务规则。外部不可绕过聚合根直接修改内部状态。
- 依赖方向：Domain 层不依赖任何其他层；Infrastructure 层依赖倒置，实现 Domain 接口。
- **数据源无关**：Domain 层不感知数据源归属，所有聚合同级平铺。

## 包结构

→ [aggregate-blueprint §5](../../specs/current/patterns/building-block/aggregate-blueprint.md)

> 完整代码示例 → [cookbook/new-aggregate.md](../how-to/new-aggregate.md)，即新聚合模板。

## 核心组件

### 聚合内部组件

聚合内部子包清单与逐包准入规则——model / repository / portal / service / factory / policy——canonical 见 [aggregate-blueprint §5](../../specs/current/patterns/building-block/aggregate-blueprint.md)，本文不复制表格。本文只强调两条贯穿全部组件的设计纪律：

1. 领域构件**零框架运行时依赖**：纯 Java + common-ddd 构建块。
2. 接口与实现分离：Repository / Portal 定义在本层，实现在 Infrastructure 层。

> **充血模型的渐进式实践**：理想状态是所有业务逻辑内聚于聚合根方法，即完全充血。实践中允许**渐进式充血**：初期可将部分逻辑放在领域服务中，随对领域理解加深再逐步内化到聚合根。因此聚合内领域服务是合法的——它不是贫血模型的借口，而是充血路上的过渡态。

### 跨聚合共享（shared）

先说判据：仅当操作涉及多个聚合时才使用领域服务，优先使用聚合根内方法。shared 放三类组件：

| 组件 | 职责 |
|------|------|
| 跨聚合领域服务 | 协调多个聚合的业务操作 |
| 通用策略 | 可插拔领域规则，如折扣、风控 |
| 共享值对象 | 跨聚合复用的值对象，如 Money、Address |

### common-ddd 构建块映射

| 构建块 | 接口位置 | 业务服务对应子包 |
|--------|---------|----------------|
| AggregateRoot | common-ddd/domain/model/ | `{aggregate}/model/` |
| Entity | common-ddd/domain/model/ | `{aggregate}/model/` |
| ValueObject | common-ddd/domain/model/ | `{aggregate}/model/` |
| Repository（写侧） | common-ddd/domain/repository/ | `{aggregate}/repository/` |
| Factory | common-ddd/domain/factory/ | `{aggregate}/factory/` |
| DomainService | common-ddd/domain/service/ | `{aggregate}/service/` 或 `shared/service/` |

## 协作关系

结论：domain 是被依赖的核心，不依赖任何外层。application 经其接口编排聚合行为——load → 行为 → save；infrastructure 反向依赖，实现其 Repository / Portal 接口，这就是依赖倒置。domain 自身零外部依赖：只有纯 Java + common-ddd。

→ 分层依赖方向法条——含结构图——canonical 在 [knowledge/specs/current/patterns/discipline/prohibitions.md](../../specs/current/patterns/discipline/prohibitions.md)「依赖方向」，ArchUnit 执法，本文不复制图。

## 专题

### 聚合根设计范式

- 继承 `AggregateRoot<ID>`，获得 `validate()` 不变量校验能力。save/update 持久化前由仓储自动调用。
- 基类**只给构建块语义、不占业务字段**：`Entity<ID>` / `AggregateRoot<ID>` 只泛型化，不内置 id/version。ID 的类型——UUID、Long 还是业务编码——连同生成策略都是业务决策。基类一旦持有字段，就强制统一了所有子类的身份形态，还背上子类未必需要的继承污染。子类多写几个字段的样板，是明确接受过的代价；换来的类型自由度是长期资产。
- 状态变迁通过行为方法暴露，不暴露 setter。
- 不变量校验使用显式 `if + throw new BusinessException(key)`，失败抛 BusinessException。
- 提供 `reconstitute()` 静态工厂，供 Converter 重建。

→ 完整代码见 [cookbook/write-path.md §4 Domain — 聚合根 + 值对象](../how-to/write-path.md) | [cookbook/new-aggregate.md](../how-to/new-aggregate.md) 的 Domain 聚合根模板。

### Entity vs ValueObject

| 特征 | Entity | ValueObject |
|------|--------|-------------|
| 唯一标识 | 有 | 无 |
| 可变性 | 可变 | 不可变 |
| 判等方式 | ID 判等 | 属性值判等 |
| 推荐实现 | class | record |

### 时间策略（一型一源）

时间贯穿 domain、持久化、契约、序列化四层，时区错误天然跨层扩散成系统性风险。所以收敛为**一型一源**：类型全框架只认 `OffsetDateTime`，当前时间只认框架注入的 `Clock`。

- **为何是这一型**：这不是口味选择，是驱动映射矩阵逼出的唯一原生位。`timestamptz` 与 pgjdbc 双向原生的类型只有 `OffsetDateTime`，Hibernate、jOOQ 等业界 ORM 也收敛于此。三个弃选的理由至今成立、至今指导读法：`ZonedDateTime` 驱动双向不支持；`LocalDateTime` 无时区语义，写入随会话时区漂移，同一瞬时能写出不同值，读 `timestamptz` 还会直接抛异常；`Instant` 非原生，走 `Timestamp` 旧桥，跨库语义漂移。
- **读数预期先校准**：PG 把 `timestamptz` 归一化为绝对瞬时，以 UTC 存储。写入的偏移即弃，读回恒 `+00:00`，与会话、JVM 时区无关——偏移只在类型里表达语义，不进存储。另外库端是微秒精度，Java 纳秒位落库必丢，内存值与 DB 回显别做逐位比较。
- **为何统一时间源**：为可测试性。业务测试注入固定时钟即可冻住时间。框架缺省 UTC 时钟；业务自行声明时钟时，框架缺省自动退位。聚合根与领域服务取当前时间一律经注入时钟派生，不裸调无参 `now()`。
- **比较与加锁纪律**：判「同一瞬时」用 `isEqual`。`equals` 还要求偏移相等——写读恒 UTC 后该坑已被结构性消除，但比较语义仍应写对表意。`OffsetDateTime` 是 value-based 对象，禁对其实例加锁，这与虚拟线程禁 `synchronized` 同向。

> 禁用类型清单与时间律条文 → [coding-conventions.md](../../specs/current/patterns/discipline/coding-conventions.md)、[prohibitions.md](../../specs/current/patterns/discipline/prohibitions.md)。裁决论证的现行版即本页正文；当时如何裁定的快照住封存案卷 §裁决记录。容器 `TZ=UTC`、展示层取串等落地细则住其他层解读，本篇不越界。

### 多数据源策略

Domain 层**不感知数据源**。无论聚合的持久化目标是 master 还是 second 数据源：

- 所有聚合在 `domain/` 下**同级平铺**，语义完全平等
- 数据源归属由 Infrastructure 层的 `@DS` 注解决定
- **不因数据源不同而嵌套聚合包**

```
domain/
├── {aggregate-x}/   ← 可能持久化到 master
├── {aggregate-y}/   ← 可能持久化到 master
└── {aggregate-z}/   ← 可能持久化到 second（但 domain 层无感知）
```

### 领域策略（Domain Policy）

领域策略是 Strategy 设计模式在 DDD 领域层的应用，框架提供 `Policy<C>` 接口，位于 common-ddd/domain/policy/。特征四条：

- **无状态单例**，封装一条可插拔的领域规则
- 继承 `Policy<C>` 获得 `isApplicable(C)` 契约；业务方法由子接口定义
- **不直接修改任何对象**，无副作用；由 Domain Service 拿到结果后操作实体
- 多条 Policy 可链式组合、排优先级；新增规则只需加新类，符合 OCP

与 Domain Service 的区别：Policy 纯计算、纯决策，无副作用；Service 编排操作，可修改实体。

#### 抽取 Policy 前后对比

→ 完整 before/after 代码见 [cookbook/policy-pattern.md](../how-to/policy-pattern.md)

核心对比：

| | Before：硬编码 | After：Policy |
|--|---|---|
| 新增规则 | 修改已有方法，违反 OCP | 新增类即可 |
| 单条测试 | 必须构造全量条件 | 独立单元测试 |
| 启用/禁用 | 改代码 | 移除 Bean / @Conditional |

#### Policy 的三种形态

三种形态：互斥型命中第一个即返回；叠加型遍历累加；精准路由型以 Map.get 命中。形态对照表与每种形态的完整代码 → [cookbook/policy-pattern.md「三种组合形态」](../how-to/policy-pattern.md)，canonical 在那份设计卡。

### 异常策略

**决定**：Domain 层不定义具名领域异常，如 `InsufficientStockException`。统一使用 `BusinessException` + i18n 错误码，key 形状为 `"{aggregate}:err.{场景}"`。Domain 层目录中**不设 `exception/` 包**。

**为什么是字符串位点、不是数字码**：数字码紧凑，但代价是服务端要养一张码到文案的映射表，翻译责任钉死在服务端。字符串 key 让服务端零文案资源：前端按 key 渲染本地化文案，新增一门语言不动服务端一行码。key 的形状与命名细则是法卷条款，见 [exception 法卷](../../specs/current/modules/exception.md)；全仓清单登记在设计卡，本文不复述。

**异常出 domain 后的三通道**（概览，完整映射表见本节末尾指针）：

- `BusinessException` → 缺省 422
- `IllegalStateException` → 409 + WARN。其子类 `OptimisticLockConflictException` 也走这条：乐观锁冲突可重试。
- `SilentWriteLossException` → 500 + ERROR 告警。这是框架持久化层抛出的 INSERT/DELETE 0 影响行不可能状态，非领域异常，领域无感知。

**为什么 409 与 422 分道**：409 是 HTTP 标准的「状态冲突」语义位，专门留给乐观锁与存在性冲突。这类冲突是暂时的，调用方重试有意义。业务规则违反——状态机非法转换也算——重试永远同样失败，所以走 422 缺省通道。两条通道由异常类型天然区分，调用方看状态码即可决定重试还是改输入。domain 侧的纪律由此推出：聚合根内守卫失败一律抛 `BusinessException`，不伸手占用 409。409 是持久化冲突的语义，不是业务语义。

**为什么出参贴 RFC 9457**：错误响应首先是给机器消费的，外部调用方要能程序化处理，HTTP 语义标准化优先于自定义格式。`application/problem+json` 的标准成员承载状态语义；`params`、`fieldErrors` 走合规扩展位；`type` 现为 about:blank，是给错误类型文档化预留的升级位。技术类异常的 detail 一律稳定泛化文案，原始消息只进服务端日志——错误体是对外的渗漏面，内部信息不外带。

> 映射表 docs 侧 canonical → [knowledge/docs/reference/api/common-exception.md](../reference/api/common-exception.md)，与源码 `GlobalRestExceptionHandler` javadoc 映射表对表；未采纳原因账本 → [knowledge/docs/explanation/theory-map.md](./theory-map.md)「具名领域异常」行；聚合根内 if-throw 完整示例 → [knowledge/docs/reference/api/common-ddd.md](../reference/api/common-ddd.md) 场景 1。

### 为什么不按类型分包（entity/ + vo/ + service/）？

四条理由：

1. **聚合是不可分割的业务整体**：一笔交易与其行项、金额值对象属于同一个一致性边界，拆到不同包破坏内聚性。
2. **一个聚合通常只有 3～10 个类**，再拆子包是过度设计。
3. **类型区分已通过继承关系表达**：`extends AggregateRoot` / `extends Entity` / `implements ValueObject`，无需目录重复表达。
4. 这是 DDD 社区的共识，Evans、Vernon、COLA、淘系皆然。

## 规则

| 允许 | 禁止 |
|------|------|
| 使用 common-ddd 构建块 | 引入框架**运行时**依赖：DI 容器、AOP、持久化 API 等。唯一例外是 `org.springframework.stereotype` 装配注解，如领域服务上的 `@Service`；该例外由 ArchUnit R4 stereotype 白名单守护。 |
| 聚合根内封装业务规则 | 暴露 setter 或 public 字段 |
| Repository 定义为接口 | 在 Domain 层实现 Repository |
| 跨聚合通过 Repository 读取 | 跨聚合直接修改对方内部状态 |
| 通过显式 if-throw + 错误码报错 | 定义具名领域异常类 |
