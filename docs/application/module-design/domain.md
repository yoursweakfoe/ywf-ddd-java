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

> 完整代码示例 → [cookbook/new-aggregate.md](../cookbook/new-aggregate.md)（新聚合模板）

## 核心组件

### 聚合内部组件

| 组件 | 职责 | 准入规则 |
|------|------|--------|
| 聚合根 / 实体 / 值对象 / 枚举 | 领域模型 | 零框架依赖，纯 Java + common-ddd 构建块 |
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

- 继承 `AggregateRoot<ID>`，获得 `validate()` 不变量校验能力（save/update 持久化前由仓储自动调用）
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
| 跨聚合通过 Repository 读取 | 跨聚合直接修改对方内部状态 |
| 通过显式 if-throw + 错误码报错 | 定义具名领域异常类 |
