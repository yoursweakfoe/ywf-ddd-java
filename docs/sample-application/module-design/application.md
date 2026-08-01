# Application 层 — 用例编排

## 职责

编排业务用例，**极薄，不含业务逻辑**。所有决策委托给 Domain 层。
AppService 委托 Handler 执行用例（返回 DTO），然后通过 Presenter 呈现为 CO 返回。

## 设计原则

- **AppService 为聚合协调入口**：一个聚合一个 AppService 类，包含该聚合全部用例方法
- **Handler 返回 DTO**：Handler 负责编排领域逻辑，通过 Assembler 转为 DTO 返回
- **AppService 做呈现**：接收 Handler 的 DTO，通过 Presenter 转为 CO 返回给调用方
- **写侧不绕过 Domain**：CommandHandler 的业务决策始终在领域模型内，Handler 只做编排
- **读侧可绕过聚合根**：QueryHandler 无需加载完整领域模型，通过 Repository 读优化方法直接投影 DTO
- **按聚合自包含**：每个聚合子包内含 AppService + handler + assembler + presenter + dto，打开即全貌

## 包结构

→ [directory-structure/server/application.md](../directory-structure/server/application.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（写路径）| [cookbook/read-path.md](../cookbook/read-path.md)（读路径）

## 核心组件

### AppService（聚合协调入口）

一个聚合一个类，标注 `@Service`。职责：委托 Handler 执行用例，然后通过 Presenter 将 DTO 呈现为 CO。

- 有返回值：`presenter.present(handler.handle(command))`
- 无返回值：直接 `handler.handle(command)`

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#4-application--appservice聚合入口

### Handler（用例执行单元）

每个用例对应一个 Handler，实现 `CommandHandler<C, R>` 或 `QueryHandler<Q, R>`（common-ddd 提供）。

| 特征 | CommandHandler（写侧） | QueryHandler（读侧） |
|---|---|---|
| 返回类型 | DTO（不是 CO） | DTO 或 PageResult&lt;DTO&gt;（不是 CO） |
| 事务 | `@Transactional` | 可省略（只读） |
| 依赖 | Repository、DomainService、Assembler | Repository（读优化方法 / 分页方法） |
| 路径 | load 聚合 → 调用行为 → save → Assembler.toDTO() | Repository.findDomainPage() → .map(assembler::toDTO)（不加载聚合） |
| 拆分标准 | 每个 Command 对应一个 Handler（1:1） | 每个 Query 对应一个 Handler（1:1） |

- **写侧**（经过 Domain）：load 聚合 → 调用行为 → save → Assembler.toDTO()
- **读侧**（绕过聚合根）：Repository.findDtoXxx() → 直接投影 DTO，不加载聚合

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#5-application--commandhandler用例执行 | [cookbook/read-path.md](../cookbook/read-path.md)#4-application--queryhandler

> Repository 接口定义在 Domain 层，实现在 Infrastructure 层（内部用 Mapper 投影）。
> Application 层始终只依赖 Domain 层接口，不触碰 Mapper / PO。

### Assembler + Presenter（两层转换）

| 组件 | 基接口（common-ddd） | 方向 | 调用者 | 职责 |
|------|------|------|------|------|
| `Assembler` | `BasicAssembler<Domain, DTO>` | Domain → DTO | Handler | 从领域模型组装内部完整视图 |
| `Presenter` | `BasicPresenter<DTO, CO>` | DTO → CO | AppService | 清洗内部细节，呈现契约安全视图 |

### EventHandler（领域事件编排）

位于 `handler/event/`，监听领域事件并编排后续业务（如：取消订单 → 回补库存）。
与 adapter 层 consumer 的区别：EventHandler 处理**内部**领域事件（Spring Event），adapter consumer 处理**外部**事件（MQ/Webhook）。

### Publisher（集成事件出站）

位于 `publisher/`，被 Handler（普通或 event）显式调用，将领域事件翻译为契约 Integration Event 并投递到 MQ。
AppService 不直接依赖 publisher。

## 协作关系

```
adapter/facade ──→ AppService ──→ CommandHandler ──→ Domain（聚合根行为 + Repository 存取）
                       │                  │
                       │                  └── Assembler（Domain → DTO）
                       │
                       ├──→ QueryHandler ──→ Repository.findDtoXxx()（绕过聚合根，直接投影 DTO）
                       │
                       └── Presenter（DTO → CO）──→ 返回调用方
```

- **adapter** 透传调用 AppService
- **domain** 被 CommandHandler 编排（聚合根行为）；QueryHandler 绕过聚合根但仍走 Repository 接口
- **infrastructure** 实现 Repository 接口（写侧 reconstitute 聚合；读侧 Mapper 投影 DTO）
- **contract** 提供 CO 类型定义，由 Presenter 产出

## 专题

### DTO 与 CO 强制分离

DTO 和 CO 是**两个不同职责的边界对象**，强制分离，不可合并：

| | DTO（内部视图） | CO（契约输出） |
|--|--|--|
| 归属 | application 层内部 | contract 模块（对外发布） |
| 职责 | 领域模型的完整内部投影 | 内部细节清洗后的外部安全视图 |
| 可包含 | 审计字段、内部评分、软删除标记、分页元数据 | 仅消费方需要的字段 |
| 变更影响 | 内部重构，无外部影响 | Breaking change，需协调消费方 |
| 生产者 | Handler（通过 Assembler） | AppService（通过 Presenter） |

```
Handler 内部：Domain → Assembler.toDTO() → DTO
AppService：DTO → Presenter.present() → CO（返回给调用方）
```

### 跨聚合编排与微服务拆分

**单体阶段（当前）**：跨聚合 Handler 放在**用例发起方**的 `handler/` 下：

```
application/order/handler/PlaceOrderHandler.java
  → productRepository.findById(...)          // 跨 Product 聚合查询
  → inventoryDomainService.deductStock(...)  // 跨聚合协调（领域服务）
  → order.place(...)                         // 本聚合
  → orderRepository.save(order)
```

归属判断原则：**谁发起用例、谁承担一致性责任，编排逻辑就归谁。**

**微服务拆分后**：Handler 零迁移——仍在 order-service 的 `application/order/handler/`，
只是原来直接调 `ProductRepository` 变为通过 `ProductService`（RPC）调 product-service。

**无主长流程（Saga）**：引入独立的 Saga/Process Manager 服务，不在某个业务服务的 application 层内塞入跨服务编排。

## 规则

| 允许 | 禁止 |
|------|------|
| CommandHandler 调用 Repository 存取聚合根 | 在 Handler 内写业务规则 |
| QueryHandler 调用 Repository 读优化方法投影 DTO | Handler 直接使用 Mapper / PO（破坏依赖方向） |
| Handler 调用 Assembler 转 DTO | AppService 包含编排逻辑 |
| AppService 调用 Presenter 转 CO | 包含 if-else 业务判断 |
| 发布/订阅领域事件 | Handler 返回 CO（应返回 DTO） |
| AppService 返回 CO | CO 暴露内部实现细节 |
| DTO 携带内部字段 | |
