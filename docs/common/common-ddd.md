# common-ddd

DDD 战术框架 —— 领域建模基类、CQRS 应用层契约、MyBatis-Plus 仓储支撑、领域事件机制。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为业务服务提供 DDD 战术层的通用构建块：聚合根/实体/值对象基类、CQRS Handler 接口、仓储支撑、领域事件发布。面向所有采用 DDD 分层架构的业务服务，是框架的核心模块。引入后获得领域建模基类 + MyBatis-Plus 插件自动配置 + 领域事件桥接。

> 只提供「基类 + 契约接口 + 自动装配」，不包含任何业务模型。ID 生成、序列化策略、跨服务通信均由业务侧自行决定。

## 2. 核心能力

### 领域建模基类

| 类 | 职责 |
|----|------|
| `Entity<ID>` | 实体基类，`entityEquals()` 基于 ID 判等；不持有 id/version 字段，子类自由声明 |
| `AggregateRoot<ID>` | 聚合根基类，管理领域事件（registerEvent / clearDomainEvents）+ `validate()` 不变量校验模板 |
| `ValueObject` | 值对象标记接口，推荐 Java record 实现 |
| `Identifiable<ID>` | 标识接口，约束 `getId()` |
| `DomainService` | 领域服务标记接口（跨聚合协调的无状态服务） |
| `Factory<T>` | 领域工厂标记接口 |
| `Policy<C>` | 可插拔领域规则接口，`isApplicable(C)` 适用性判断 |
| `Portal` | 外部资源访问标记接口（Domain 定义 XxxPortal，Infrastructure 实现 XxxGateway） |

### CQRS Handler 接口

| 标记接口（common-contract） | Handler（本包） | 语义 | 返回值 |
|---|---|---|---|
| `Command` | `CommandHandler<C, R>` | 请做这件事 | **R** |
| `Query` | `QueryHandler<Q, R>` | 请给我这个 | **R** |
| `PageableQuery` | `QueryHandler<Q, PageResult<R>>` | 给我一页 | **PageResult&lt;R&gt;** |

> `IntegrationEvent`（common-contract）在本包无对应 Handler 接口：入站经 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler；出站经 application 层 Publisher 投递 MQ。领域事件的进程内反应由业务侧 `DomainEventListener`（`@EventListener`）承担，框架不提供接口。

`PageResult<T>` 是框架级分页容器（record），定义在 `application` 层，隔离 MyBatis-Plus `Page<PO>`，提供 `map()` 支持逐层转换。**对偶原则**：业务在 application 层用它（读端口 / Handler / AppService 用 `PageResult<读 DTO>`），故框架支撑类也放在 `application` 包内，与业务分层对齐。

### 对象转换

| 接口 | 层 | 方向 |
|------|----|------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） |

三者均为普通 `@Component` 类、逐字段显式赋值（不使用代码生成器）。富领域模型的 `toDomain` 走 `reconstitute()` 静态工厂，不适用的 update 方法抛 `UnsupportedOperationException`。List/Set 集合方法由接口 default 实现提供。

### 仓储支撑（MybatisPersistence）

组合持有 `BaseMapper`（不继承 ServiceImpl，避免 `save(PO)`/`updateById(PO)` 等底层 PO 直操方法泄漏为公开 API），封装：

- `saveDomain` / `updateDomain` — 持久化前自动 `validate()`，持久化后发布领域事件
- `removeDomain` / `removeDomains` — 删除成功后发布聚合已注册事件
- `removeDomainById` / `removeDomainByIds` — 纯技术删除不发事件；**事件工厂重载**在删除成功后按 ID 构造并发布事件（适配"只查 ID 不加载 Domain"路径）
- `findDomainById` / `findDomainsByIds` / `findDomainOneByCondition` — 写侧加载聚合（load → 行为 → save 链路）
- 乐观锁冲突 → `IllegalStateException`（HTTP 409）
- **事务边界上收**：本类不声明 `@Transactional`，事务由应用层 Handler 控制（批量原子性由调用方包裹事务保证）

### 领域事件

- `DomainEvent` — 事件基类（eventId + occurredOn）
- `DomainEventPublisher` — 发布契约接口
- `InProcessDomainEventPublisher` — 桥接 Spring `ApplicationEventPublisher`，进程内同步发布（位于 `infrastructure/event/domain/`）

事件仅 AggregateRoot 可注册，仓储自动发布（opt-in 点是 `registerEvent()`），先落库后发事件 + 先清后发。

> **边界**：领域事件仅进程内消费（受众为 `@EventListener` 域内反应监听器）。集成事件（IntegrationEvent）的收发不在本模块：出站由 application 层 Publisher 投递 MQ（依赖 common-mq），入站由 adapter 层 Consumer 接收。

### MyBatis-Plus 自动配置

| 拦截器 | 说明 |
|--------|------|
| PaginationInnerInterceptor | 物理分页（AUTO 方言） |
| OptimisticLockerInnerInterceptor | 乐观锁（仅 @Version 实体生效） |
| BlockAttackInnerInterceptor | 防全表 UPDATE/DELETE（始终开启） |

`@ConditionalOnMissingBean` 允许业务项目完全自定义覆盖。

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-ddd</artifactId>
</dependency>
```

### 场景 1：聚合根 + 领域事件

```java
public class Order extends AggregateRoot<UUID> {
    private UUID id;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private Integer version;

    /** 业务构造器（创建新订单） */
    public Order(UUID id, List<OrderItem> items) {
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.totalAmount = calculateTotal();
    }

    /** 重建构造器（Converter 使用） */
    public static Order reconstitute(UUID id, OrderStatus status, List<OrderItem> items,
                                     BigDecimal totalAmount, Integer version) {
        Order order = new Order(id, items);
        order.status = status;
        order.totalAmount = totalAmount;
        order.version = version;
        return order;
    }

    @Override public UUID getId() { return id; }

    /** 支付：状态机校验 + 状态变迁 + 注册事件 */
    public void pay() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("order:err.invalidTransition");
        }
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(this.id));
    }

    /** 不变量校验（仓储 save/update 前自动调用） */
    @Override public void validate() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order:err.itemsEmpty");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("order:err.totalMustBePositive");
        }
    }
}
```

### 场景 2：RepositoryImpl（仓储实现）

```java
@Component
public class OrderRepositoryImpl
        extends MybatisPersistence<OrderMapper, OrderPO, Order>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               OrderConverter converter) {
        super(domainEventPublisherProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Order, OrderPO> getConverter() { return converter; }

    @Override public Optional<Order> findById(UUID id) { return findDomainById(id.toString()); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) { saveDomain(domain); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) { updateDomain(domain); }

    @Override public void deleteById(UUID id) { removeDomainById(id.toString()); }
}
```

### 场景 3：批量操作与事件工厂

`saveDomainBatch` / `updateDomainBatch` 语义为**单事务循环**（逐条 insert/update），非多行 VALUES SQL——每条聚合须独立 `validate()` + `registerEvent()` / 发布，多行 INSERT 无法触发逐聚合行为；批量原子性由调用方（Handler 标 `@Transactional`）保证。

```java
repository.saveDomainBatch(List.of(order1, order2, order3));           // 批量保存
repository.updateDomainBatch(List.of(order1, order2));                // 批量更新
List<Order> orders = repository.findDomainsByIds(List.of("id-1", "id-2"));

order.markCancelled();            // 聚合行为内部 registerEvent
repository.removeDomain(order);   // 删除成功 → 自动发布事件

// 按 ID 删除 + 事件工厂（只查 ID 不加载 Domain，事件只携带 ID）
repository.removeDomainById(orderId, id -> new OrderDeletedEvent(id));
```

### 场景 4：PageResult 分页链路（读侧，绕过 domain）

```java
// application 层：读端口返回读 DTO（PO → DTO 直接投影，不经过 domain）
public interface OrderQueryRepository {
    PageResult<OrderViewDTO> findPage(String status, String customerId, int pageNum, int pageSize);
}

// application 层：Handler 直接取读 DTO
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {
    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        return orderQueryRepository.findPage(
                query.status(), query.customerId(), query.pageNum(), query.pageSize());
    }
}
```

> 读侧完全绕过 domain 层（不 reconstitute 聚合根、不建领域读模型），基础设施层实现读端口
> （infrastructure → application，写侧依赖倒置的读侧镜像），直接从 PO 投影读 DTO。
> 读侧无业务判断，派生值在写侧计算并物化到 PO 列。详见 `docs/application/cookbook/read-path.md`。

### 场景 5：DomainEvent 监听

```java
@Component
public class OrderDomainEventListener {
    @EventListener  // 同事务，失败则主操作回滚
    public void onOrderPaid(OrderPaidEvent event) { /* 处理逻辑 */ }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 事务提交后
    public void onOrderPaidAfterCommit(OrderPaidEvent event) { /* 发通知、更新缓存等副作用 */ }
}
```

完整示例见 `docs/application/cookbook/write-path.md`。

## 4. 依赖关系

```
common-ddd → common-contract（Command / Query / IntegrationEvent 标记接口）
           → common-exception（BusinessException）
           → mybatis-plus-spring-boot4-starter
           → mybatis-plus-jsqlparser
           → dynamic-datasource-spring-boot4-starter（test scope，多数据源兼容性验证）
           → jackson-databind（test scope，测试 fixtures）
```

## 5. 设计原则

- **对偶原则（包结构镜像）**：框架支撑类的包层级与业务使用它的层级对齐——业务在 domain 层用（`AggregateRoot`、`Repository`、`DomainEvent`）→ 放 `common-ddd/domain`；业务在 application 层用（`PageResult`、`QueryHandler`、`BasicAssembler`）→ 放 `common-ddd/application`；业务在 infrastructure 层用（`MybatisPersistence`、`BasicConverter`）→ 放 `common-ddd/infrastructure`
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **领域事件先清后发**：持久化后发布，避免监听器异常导致重复发布
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件允许业务项目完全自定义覆盖

## 6. 设计决策

### ADR-0001 基类不持有 id/version 字段

- 状态：accepted

**背景**：聚合根基类是否内置 id/version 字段。

**选项**：
- 内置字段：子类少写样板，但 ID 类型（UUID/Long/业务编码）被强制统一
- 泛型化 + 不持有：子类自由声明

**决策**：选泛型化 + 不持有。ID 生成与业务强相关，由子类构造器自行决定。

**确认**：`Entity<ID>` / `AggregateRoot<ID>` 无 id/version 字段。

### ADR-0002 全量 UPDATE 而非脏检查

- 状态：accepted

**背景**：持久化采用脏检查还是全量更新。

**决策**：选全量 UPDATE。MyBatis-Plus 场景下脏检查收益极低且增加复杂度；全量更新保证审计字段刷新。

**确认**：`updateDomain` 走 MyBatis-Plus 全量 update。

### ADR-0003 领域事件：进程内 Spring Event + 仅聚合根注册 + 仓储自动发布

- 状态：accepted

**背景**：领域事件的发布模型。

**选项**：
- 手动发布：易遗忘
- 仓储自动发布（有则发，无则静默）

**决策**：选仓储自动发布。opt-in 点是 `registerEvent()`，先落库后发事件 + 先清后发（与 Spring Data `@DomainEvents` 同模式）。事件仅 AggregateRoot 可注册（一致性边界 = 事件唯一出口）。逃生门：Handler 注入 `DomainEventPublisher` 手动发 / `clearDomainEvents()` 抑制。

**后果**：跨服务通信仍走 Seata + HTTP 显式调用；未来可靠化走 Outbox 只需替换 publisher 实现，契约不变。

**确认**：`InProcessDomainEventPublisher` + `MybatisPersistence` 自动发布。

### ADR-0004 对象转换纯手写，不用 MapStruct

- 状态：accepted

**背景**：Converter/Assembler/Presenter 用代码生成器还是手写。

**决策**：选手写显式映射。AI 辅助开发下手写模板成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在。聚合根重建走 reconstitute，完整性由往返测试守护。

**确认**：`BasicConverter` / `BasicAssembler` / `BasicPresenter` 无生成器依赖。

### ADR-0005 CQRS 契约：Query 纯标记（EventHandler 已移除）

- 状态：accepted（2026-08 修订）

**背景**：Handler 接口的契约设计。

**决策**：Query 为纯标记（无泛型，避免 contract 与 internal 类型耦合，返回类型由 Service 方法签名定义）。原 `EventHandler<E>` 接口已移除：入站集成事件（IntegrationEvent）统一由 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler，不设独立事件 Handler；域内反应（对领域事件）由业务侧 `DomainEventListener`（`@EventListener`）承担。

**确认**：`QueryHandler` / `CommandHandler` 接口签名。

### ADR-0006 删除操作的事件工厂重载（save/update 不提供）

- 状态：accepted

**背景**：删除路径何时需要事件工厂。

**决策**：仅删除提供事件工厂重载，补「按 ID 删无 Domain 对象」的缺口。save/update 始终有 Domain，`registerEvent` 是唯一通道，双通道有重复发布风险；非聚合根发事件是建模信号（应升级为聚合根）而非框架缺口。

**确认**：`removeDomainById(id, eventFactory)` / `removeDomainByIds(ids, eventFactory)` 重载存在，save/update 无对应重载。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：领域事件异步 / 跨进程发布 | 当前进程内 Spring Event；跨服务经 Seata + HTTP 显式调用 |
| 边界：Specification 模式（已移除） | MyBatis-Plus `LambdaQueryWrapper` 已是可组合查询规约 |
