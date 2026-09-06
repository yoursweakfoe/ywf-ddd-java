# Domain 层 — 核心业务逻辑

## 职责

承载核心业务逻辑，**零框架依赖**。是整个系统最稳定、最有价值的部分。

## 设计原则

- **按聚合分包**：每个聚合根一个顶级子包，内部结构一致
- 聚合根封装所有业务规则，外部不可绕过聚合根直接修改内部状态
- 依赖方向：Domain 层不依赖任何其他层，Infrastructure 层依赖倒置实现 Domain 接口
- **数据源无关**：Domain 层不感知数据源归属，所有聚合同级平铺

## 包结构

→ [directory-structure/server/domain.md](../reference/structure.md)

> 完整代码示例 → [cookbook/new-aggregate.md](../how-to/new-aggregate.md)（新聚合模板）

## 核心组件

### 聚合内部组件

聚合内部子包清单与逐包准入规则（model / repository / portal / service / factory / policy）→ canonical 见 [directory-structure/server/domain.md「目录职责」](../reference/structure.md)，本文不复制表格。本文只强调两条贯穿全部组件的设计纪律：领域构件**零框架运行时依赖**（纯 Java + common-ddd 构建块）、接口与实现分离（Repository / Portal 定义在本层、实现在 Infrastructure 层）。

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
| Repository（写侧） | common-ddd/domain/repository/ | `{aggregate}/repository/` |
| Factory | common-ddd/domain/factory/ | `{aggregate}/factory/` |
| DomainService | common-ddd/domain/service/ | `{aggregate}/service/` 或 `shared/service/` |

## 协作关系

domain 是被依赖的核心、不依赖任何外层：application 经其接口编排聚合行为（load → 行为 → save），infrastructure 反向依赖实现其 Repository / Portal 接口（依赖倒置），并做到零外部依赖（纯 Java + common-ddd）。

→ 分层依赖方向法条（含结构图）canonical 在 [.agents/rules/02-architecture.md](../../../.agents/rules/02-architecture.md)「依赖方向」，ArchUnit 执法，本文不复制图。

## 专题

### 聚合根设计范式

- 继承 `AggregateRoot<ID>`，获得 `validate()` 不变量校验能力（save/update 持久化前由仓储自动调用）
- 状态变迁通过行为方法暴露，不暴露 setter
- 不变量校验使用显式 `if + throw new BusinessException(key)`，失败抛 BusinessException
- 提供 `reconstitute()` 静态工厂供 Converter 重建

→ 完整代码见 [cookbook/write-path.md §4 Domain — 聚合根 + 值对象](../how-to/write-path.md) | [cookbook/new-aggregate.md](../how-to/new-aggregate.md)（Domain 聚合根模板）

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
├── {aggregate-x}/   ← 可能持久化到 master
├── {aggregate-y}/   ← 可能持久化到 master
└── {aggregate-z}/   ← 可能持久化到 second（但 domain 层无感知）
```

### 领域策略（Domain Policy）

领域策略是 Strategy 设计模式在 DDD 领域层的应用，框架提供 `Policy<C>` 接口（common-ddd/domain/policy/）：

- **无状态单例**，封装一条可插拔的领域规则
- 继承 `Policy<C>` 获得 `isApplicable(C)` 契约，业务方法由子接口定义
- **不直接修改任何对象**（无副作用），由 Domain Service 拿到结果后操作实体
- 多条 Policy 可链式组合、排优先级，新增规则只需加新类（OCP）

与 Domain Service 的区别：Policy 纯计算/决策、无副作用；Service 编排操作、可修改实体。

#### 抽取 Policy 前后对比

→ 完整 before/after 代码见 [cookbook/policy-pattern.md](../how-to/policy-pattern.md)

核心对比：

| | Before（硬编码） | After（Policy） |
|--|---|---|
| 新增规则 | 修改已有方法（违反 OCP） | 新增类即可 |
| 单条测试 | 必须构造全量条件 | 独立单元测试 |
| 启用/禁用 | 改代码 | 移除 Bean / @Conditional |

#### Policy 的三种形态

互斥型（命中第一个即返回）/ 叠加型（遍历累加）/ 精准路由型（Map.get 命中）——形态对照表与每种形态的完整代码 → [cookbook/policy-pattern.md「三种组合形态」](../how-to/policy-pattern.md)（canonical）。

### 异常策略

**决定**：Domain 层不定义具名领域异常（如 `InsufficientStockException`），统一使用 `BusinessException` + i18n 错误码（`"{aggregate}:err.{场景}"`）；Domain 层目录中**不设 `exception/` 包**。

**异常出 domain 后的三通道**（一句话概览，完整映射表不在此复述）：`BusinessException` → 缺省 422、`IllegalStateException`（含其子类 `OptimisticLockConflictException`，乐观锁冲突可重试）→ 409 + WARN、`SilentWriteLossException`（框架持久化层抛出的 INSERT/DELETE 0 影响行不可能状态，非领域异常、领域无感知）→ 500 + ERROR 告警。

> 映射表 docs 侧 canonical → [knowledge/docs/reference/api/common-exception.md](../reference/api/common-exception.md)（与源码 `GlobalRestExceptionHandler` javadoc 映射表对表）；未采纳原因账本 → [knowledge/docs/explanation/theory-map.md](./theory-map.md)「具名领域异常」行；聚合根内 if-throw 完整示例 → [knowledge/docs/reference/api/common-ddd.md](../reference/api/common-ddd.md) 场景 1。

### 为什么不按类型分包（entity/ + vo/ + service/）？

1. **聚合是不可分割的业务整体**：Order、OrderItem、Money 属于同一个一致性边界，拆到不同包破坏内聚性
2. **一个聚合通常只有 3～10 个类**，再拆子包是过度设计
3. **类型区分已通过继承关系表达**：`extends AggregateRoot` / `extends Entity` / `implements ValueObject`，无需目录重复表达
4. 这是 DDD 社区（Evans、Vernon、COLA、淘系）的共识

## 规则

| 允许 | 禁止 |
|------|------|
| 使用 common-ddd 构建块 | 引入框架**运行时**依赖（DI 容器 / AOP / 持久化 API 等）——唯一例外 `org.springframework.stereotype` 装配注解（如领域服务上的 `@Service`），ArchUnit A2 白名单守护 |
| 聚合根内封装业务规则 | 暴露 setter 或 public 字段 |
| Repository 定义为接口 | 在 Domain 层实现 Repository |
| 跨聚合通过 Repository 读取 | 跨聚合直接修改对方内部状态 |
| 通过显式 if-throw + 错误码报错 | 定义具名领域异常类 |
