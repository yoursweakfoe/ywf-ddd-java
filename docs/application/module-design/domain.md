# Domain 层 — 核心业务逻辑

## 职责

承载核心业务逻辑，**零框架依赖**。是整个系统最稳定、最有价值的部分。

## 设计原则

- **按聚合分包**：每个聚合根一个顶级子包，内部结构一致
- 聚合根封装所有业务规则，外部不可绕过聚合根直接修改内部状态
- 依赖方向：Domain 层不依赖任何其他层，Infrastructure 层依赖倒置实现 Domain 接口
- **数据源无关**：Domain 层不感知数据源归属，所有聚合同级平铺

## 包结构

→ [directory-structure/server/domain.md](../directory-structure/server/domain.md)

> 完整代码示例 → [cookbook/event-flow.md](../cookbook/event-flow.md)（事件链路）| [cookbook/new-aggregate.md](../cookbook/new-aggregate.md)（新聚合模板）

## 核心组件

### 聚合内部组件

| 组件 | 职责 | 准入规则 |
|------|------|--------|
| 聚合根 / 实体 / 值对象 / 枚举 | 领域模型 | 零框架依赖，纯 Java + common-ddd 构建块 |
| 领域事件 | extends DomainEvent | 事件是模型的组成部分，仅进程内消费 |
| Repository 接口 | 持久化抽象 | 必须为接口，实现在 Infrastructure 层 |
| Portal 接口 | 外部资源访问（OSS/RPC/MQ/ES） | 必须为接口，实现在 infrastructure/gateway（含 ACL 翻译） |
| 领域服务 | 聚合内业务逻辑 | 仅当逻辑不自然归属于任何实体时使用 |
| Factory | 复杂创建逻辑 | 仅当构造器不足以表达创建语义时使用 |
| Policy | 可插拔领域规则 | 无状态、纯计算、无副作用 |

> **充血模型的渐进式实践**：理想状态是所有业务逻辑内聚于聚合根方法（完全充血）。
> 但实践中允许**渐进式充血**——初期可将部分逻辑放在领域服务中，
> 随着对领域理解加深再逐步内化到聚合根。因此聚合内领域服务是合法的，
> 不是贫血模型的借口，而是充血路上的过渡态。

### 跨聚合共享（shared）

| 组件 | 职责 |
|------|------|
| 跨聚合领域服务 | 协调多个聚合的业务操作 |
| 通用策略 | 可插拔领域规则（如折扣、风控） |
| 共享值对象 | 跨聚合复用的值对象（如 Money, Address） |

仅当操作涉及多个聚合时才使用领域服务，优先使用聚合根内方法。

### common-ddd 构建块映射

| 构建块 | 接口位置 | 业务服务对应子包 |
|--------|---------|----------------|
| AggregateRoot | common-ddd/domain/model/ | `{aggregate}/model/` |
| Entity | common-ddd/domain/model/ | `{aggregate}/model/` |
| ValueObject | common-ddd/domain/model/ | `{aggregate}/model/` |
| DomainEvent | common-ddd/domain/event/domain/ | `{aggregate}/event/domain/` |
| DomainEventPublisher | common-ddd/domain/event/publisher/ | `{aggregate}/event/publisher/`（业务自定义，可选；默认框架 InProcessDomainEventPublisher） |
| Repository | common-ddd/domain/repository/ | `{aggregate}/repository/` |
| Factory | common-ddd/domain/factory/ | `{aggregate}/factory/` |
| DomainService | common-ddd/domain/service/ | `{aggregate}/service/` 或 `shared/service/` |

## 协作关系

```
adapter ──→ application ──→ domain ←── infrastructure
                               │
                          零外部依赖
                     （纯 Java + common-ddd）
```

- **application** 编排 Domain 对象（调用聚合根行为、通过 Repository 接口存取）
- **infrastructure** 实现 Domain 定义的接口（Repository / Portal）
- Domain 层**不依赖**任何外层，是被依赖的核心

## 专题

### 聚合根设计范式

- 继承 `AggregateRoot<ID>`，获得 `registerEvent()` + `validate()` 能力
- 状态变迁通过行为方法暴露，不暴露 setter
- 不变量校验使用显式 `if + throw new BusinessException(key)`，失败抛 BusinessException
- 提供 `reconstitute()` 静态工厂供 Converter 重建

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#8-domain--聚合根行为 | [cookbook/new-aggregate.md](../cookbook/new-aggregate.md)##domain--聚合根

### Entity vs ValueObject

| 特征 | Entity | ValueObject |
|------|--------|-------------|
| 唯一标识 | 有 | 无 |
| 可变性 | 可变 | 不可变 |
| 判等方式 | ID 判等 | 属性值判等 |
| 推荐实现 | class | record |

### 事件边界

#### DomainEvent vs IntegrationEvent

| 维度 | DomainEvent（领域事件） | IntegrationEvent（集成事件，common-contract） |
|------|----------------------|---------------------------|
| 方向 | 由内向外（聚合根 → 进程内） | 双向（出站 + 入站） |
| 产生者 | 本服务聚合根 | 本服务 Publisher（出站）/ 其他微服务（入站） |
| 所在层 | domain 层 | contract 模块（跨服务契约） |
| 发布机制 | 仓储持久化后 → Spring Event（进程内） | Publisher 投递 MQ（出站）/ 外部消息到达 → Adapter Consumer（入站） |
| 典型例子 | `OrderPlacedEvent`（我下单了） | `OrderPlacedIntegrationEvent`（我通知外界） |

简记：**DomainEvent 是"进程内我告诉自己人"，IntegrationEvent 是"跨服务我告诉别人 / 别人告诉我"**。

#### 事件监听原理（Spring 机制）

聚合根 `registerEvent()` 暂存事件 → 仓储持久化成功后 `DomainEventFlusher` 冲刷（先清后发）→ 业务提供 `OutboxStore` 时经 **Outbox** 与业务同事务入箱（投递由业务排空器在**提交后**承担）；未提供时走直发降级路径（提交后经 `DomainEventPublisher` 进程内派发）→ 最终经 `DomainEventPublisher` 桥接 Spring `ApplicationEventPublisher` 按类型路由到 `@EventListener` 方法。

`DomainEvent` 不需要实现任何 Spring 接口（Spring 4.2+ 的 `publishEvent` 接受任意 Object），领域层保持零框架依赖。

→ 完整链路代码见 [cookbook/event-flow.md](../cookbook/event-flow.md)；Outbox 可靠性语义见 [common-ddd.md Outbox 节](../../common/common-ddd.md)

#### 事件发布通道（谁来发、什么时候发）

唯一的 opt-in 点是 `AggregateRoot.registerEvent()`，仓储只是可靠的投递机制（保证先持久化成功后冲刷 + 先清后发 + Outbox at-least-once），不是策略决定者：

| 场景 | 通道 |
|------|------|
| 聚合行为方法产生事件（create/update/实体删除） | `registerEvent()` → 仓储 save/update/removeDomain(s) 自动发 |
| 按 ID 删除（无 Domain 对象，性能优化路径） | 事件工厂重载 `removeDomainById(id, eventFactory)` |
| 非聚合根想发事件 | 建模信号 → 升级为聚合根；纯技术特例 → Handler 注入 `DomainEventPublisher` 手动发（自担时序契约） |
| 抑制自动发布（如 Saga 补偿） | save 前显式 `clearDomainEvents()` |

为什么不改成全手动发布、为什么 save/update 没有事件工厂重载 → 设计决策详见 [common-ddd.md 领域事件节](../../common/common-ddd.md)。

#### 事件类型与消费方式

| 事件类型 | 位置 | 消费方式 | 暴露范围 |
|---------|------|---------|--------|
| **领域事件** | `domain/{aggregate}/event/domain/` | Spring `@EventListener`（进程内） | 不对外 |
| **集成事件** | `contract/{aggregate}/dto/event/integration/` | MQ / RPC（跨服务） | 对外发布 |

微服务拆分时：领域事件仍留在服务内部；需要跨服务通知时，由 application 层将领域事件转换为集成事件发布到 MQ。

#### 事件监听器事务传播

领域事件经 Outbox 同事务捕获、由业务排空器在业务事务**提交之后**投递（详见
[common-ddd.md Outbox 节](../../common/common-ddd.md)）——监听器执行时**无活动事务**，
「提交后才执行」由捕获+排空机制保证，不再由监听器注解表达：

| 注解 | 线程 | 事务 | 监听器异常的影响 |
|------|------|------|----------------|
| `@EventListener`（域内反应默认） | 同线程（排空线程） | **无**（已提交；写入须自带事务） | 原始业务事务不受影响；排空器标记失败后重投（策略由排空器定） |
| `@EventListener + @Transactional(REQUIRES_NEW)` | 同线程 | **独立新事务** | 新事务回滚；排空器重投 |
| `@Async @EventListener` | 新线程 | **无** | 完全隔离（注意丢失 Outbox 重投兜底） |

选择原则：
- 纯反应（日志 / 出站翻译）→ `@EventListener`
- 带数据库写入的补偿副作用 → `@EventListener` + `@Transactional(REQUIRES_NEW)`
- 完全异步不关心结果 → `@Async @EventListener`
- **禁用** `@TransactionalEventListener(AFTER_COMMIT)`：投递时无事务可挂靠，默认不执行

### 多数据源策略

Domain 层**不感知数据源**。无论聚合的持久化目标是 master 还是 second 数据源：

- 所有聚合在 `domain/` 下**同级平铺**，语义完全平等
- 数据源归属由 Infrastructure 层的 `@DS` 注解决定
- **不因数据源不同而嵌套聚合包**

```
domain/
├── order/       ← 可能持久化到 master
├── product/     ← 可能持久化到 master
└── report/      ← 可能持久化到 second（但 domain 层无感知）
```

### 领域策略（Domain Policy）

领域策略是 Strategy 设计模式在 DDD 领域层的应用，框架提供 `Policy<C>` 接口（common-ddd/domain/policy/）：

- **无状态单例**，封装一条可插拔的领域规则
- 继承 `Policy<C>` 获得 `isApplicable(C)` 契约，业务方法由子接口定义
- **不直接修改任何对象**（无副作用），由 Domain Service 拿到结果后操作实体
- 多条 Policy 可链式组合、排优先级，新增规则只需加新类（OCP）

与 Domain Service 的区别：Policy 纯计算/决策、无副作用；Service 编排操作、可修改实体。

#### 抽取 Policy 前后对比

→ 完整 before/after 代码见 [cookbook/policy-pattern.md](../cookbook/policy-pattern.md)

核心对比：

| | Before（硬编码） | After（Policy） |
|--|---|---|
| 新增规则 | 修改已有方法（违反 OCP） | 新增类即可 |
| 单条测试 | 必须构造全量条件 | 独立单元测试 |
| 启用/禁用 | 改代码 | 移除 Bean / @Conditional |

#### Policy 的三种形态

| 形态 | 顺序要求 | 主流程逻辑 | 典型场景 |
|------|---------|-----------|--------|
| 互斥型 | 严格（`@Order`） | 命中第一个即返回 | 折扣计算、风控拦截 |
| 叠加型 | 无关 | 遍历累加 | 运费减免、优惠叠加 |
| 精准路由型 | 无关 | Map.get(type) | 多租户策略、支付渠道路由 |

**互斥型**：`@Order(1)` / `@Order(2)` 控制优先级，命中第一个即返回。

**叠加型**：所有满足条件的 Policy 都生效，结果累加，顺序无关。

**精准路由型**：每个 Policy 声明业务标识（`getType()`），主流程 List 转 Map，O(1) 命中。

### 异常策略

> **决定**：Domain 层不定义具名领域异常（如 `InsufficientStockException`），
> 统一使用 `BusinessException` + i18n 错误码。
>
> **原因**：
> 1. 前端对接需要统一的错误码体系（`"product:err.insufficientStock"`），具名异常仍需转换为错误码，多一层间接
> 2. 具名异常会导致类爆炸（每个错误场景一个类），维护成本高
> 3. 显式 `if + throw new BusinessException(key)` 已足够表达领域层内的业务规则校验
> 4. i18n 错误码天然支持多语言前端展示，无需额外映射
>
> **用法**：
> ```java
> if (status != OrderStatus.PENDING) {
>     throw new BusinessException("order:err.invalidTransition");
> }
> if (stock < quantity) {
>     throw new BusinessException("product:err.insufficientStock");
> }
> ```
>
> 因此 Domain 层目录中**不设 `exception/` 包**。

### 为什么不按类型分包（entity/ + vo/ + service/）？

1. **聚合是不可分割的业务整体**：Order、OrderItem、Money 属于同一个一致性边界，拆到不同包破坏内聚性
2. **一个聚合通常只有 3～10 个类**，再拆子包是过度设计
3. **类型区分已通过继承关系表达**：`extends AggregateRoot` / `extends Entity` / `implements ValueObject`，无需目录重复表达
4. 这是 DDD 社区（Evans、Vernon、COLA、淘系）的共识

## 规则

| 允许 | 禁止 |
|------|------|
| 使用 common-ddd 构建块 | 引入 Spring / MyBatis / 任何框架注解 |
| 聚合根内封装业务规则 | 暴露 setter 或 public 字段 |
| Repository 定义为接口 | 在 Domain 层实现 Repository |
| 领域事件 extends DomainEvent | 在领域事件中引用 Infrastructure 类 |
| 跨聚合通过 Repository 读取 | 跨聚合直接修改对方内部状态 |
| 通过显式 if-throw + 错误码报错 | 定义具名领域异常类 |
