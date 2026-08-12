# common-ddd

DDD 战术框架 —— 领域建模基类、CQRS 应用层契约、MyBatis-Plus 仓储支撑、领域事件机制。

## 定位

为业务服务提供 DDD 战术层的通用构建块：聚合根/实体/值对象基类、CQRS Handler 接口、仓储支撑、领域事件发布。
面向所有采用 DDD 分层架构的业务服务，是框架的核心模块。
引入后获得领域建模基类 + MyBatis-Plus 插件自动配置 + 领域事件桥接。

## 设计原则

- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **领域事件先清后发**：持久化后发布，避免监听器异常导致重复发布
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件允许业务项目完全自定义覆盖

## 包结构

```
com.yoursweakfoe.common.ddd
├── application/
│   ├── assembler/        ← BasicAssembler（DTO ↔ Domain）
│   ├── presenter/        ← BasicPresenter（DTO → CO，单向呈现）
│   └── cqrs/
│       ├── command/      ← CommandHandler
│       ├── query/        ← QueryHandler
│       └── event/        ← EventHandler（外部事件）
├── domain/
│   ├── model/            ← Entity / AggregateRoot / ValueObject / Identifiable / PageResult
│   ├── event/            ← DomainEvent / DomainEventPublisher
│   ├── repository/       ← Repository 通用契约接口
│   ├── service/          ← DomainService 领域服务标记接口
│   ├── factory/          ← Factory 领域工厂标记接口
│   ├── policy/           ← Policy<C> 可插拔领域规则接口
│   └── portal/           ← Portal 外部资源访问标记接口
├── infrastructure/
│   ├── converter/        ← BasicConverter（Domain ↔ PO）
│   ├── event/            ← SpringDomainEventPublisher（Spring Event 桥接）
│   └── mybatis/
│       ├── config/       ← MybatisPlusPluginConfiguration（分页 + 乐观锁 + 防全表攻击）
│       ├── handler/      ← BasicAutoFillHandler（createdAt / updatedAt）
│       └── repository/   ← MybatisRepositorySupport（仓储支撑基类）
└── DddAutoConfiguration  ← Spring Boot 自动配置入口
```

## 核心功能

### 领域建模基类

| 类 | 职责 |
|----|------|
| `Entity<ID>` | 实体基类，提供 `entityEquals()` 基于 ID 判等；不持有 id/version 字段，子类自由声明 |
| `AggregateRoot<ID>` | 聚合根基类，管理领域事件（registerEvent / clearDomainEvents）+ `validate()` 不变量校验模板 |
| `ValueObject` | 值对象标记接口，推荐用 Java record 实现 |
| `Identifiable<ID>` | 标识接口，约束 `getId()` |
| `DomainService` | 领域服务标记接口，标识承载跨聚合领域逻辑的无状态服务 |
| `Factory<T>` | 领域工厂标记接口，标识创建复杂领域对象的工厂 |
| `Policy<C>` | 可插拔领域规则接口，提供 `isApplicable(C)` 适用性判断；业务方法由子接口定义 |
| `Portal` | 外部资源访问标记接口，Domain 层定义 XxxPortal，Infrastructure 层实现 XxxGateway |

### CQRS Handler 接口

纯接口，不含实现逻辑：

| 标记接口（common-contract） | Handler（本包） | 语义 | 返回值 |
|---|---|---|---|
| `Command` | `CommandHandler<C, R>` | “请做这件事” | **R**（调用方需要结果） |
| `Query` | `QueryHandler<Q, R>` | “请给我这个” | **R**（调用方需要数据） |
| `PageableQuery` | `QueryHandler<Q, PageResult<R>>` | “给我一页” | **PageResult&lt;R&gt;**（分页结果） |
| `Event` | `EventHandler<E>` | “这件事发生了” | **void**（通知，无需回复） |

Handler 接口支持基础设施层做统一 AOP 拦截（事务、幂等、审计等）。

`PageResult<T>` 是框架级分页容器（record），定义在 domain/model 层（application 和 infrastructure 均可依赖），隔离 MyBatis-Plus `Page<PO>`，提供 `map()` 方法支持逐层转换（Domain → DTO → CO）。

### 对象转换

| 接口 | 层 | 方向 | 实现方式 |
|------|----|------|----------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain | 纯手写显式映射 |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO | 纯手写显式映射 |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） | 纯手写显式映射 |

**实现约定（三者通用，不使用代码生成器）：**

| 约定 | 说明 |
|------|------|
| 普通 `@Component` 类，逐字段显式赋值 | 映射关系在代码中可见可读，无生成器魔法 |
| 富领域模型（聚合根无 setter） | `toDomain` 走 `reconstitute()` 静态工厂；不适用的 update 方法抛 `UnsupportedOperationException` |
| 字段增删同步 | 修改 Domain/DTO/PO/CO 字段时必须同步修改对应映射方法，完整性由往返测试守护 |
| 集合方法免手写 | List/Set 批量方法均由接口 default 实现提供（委托单体方法） |

> 为什么不用 MapStruct：AI 辅助开发下手写模板代码成本归零，而代码生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫接口）仍在，见 `docs/references.md` 未采纳清单。

### 仓储支撑（MybatisRepositorySupport）

继承 MyBatis-Plus `ServiceImpl`，封装：

- `saveDomain` / `updateDomain` — 持久化前自动调用 `validate()`，持久化后发布领域事件
- `removeDomain` / `removeDomains` — 删除成功后发布聚合根已注册的领域事件（与 save/update 契约一致）
- `removeDomainById` / `removeDomainByIds` — 纯技术删除不发事件；**事件工厂重载**（`removeDomainById(id, eventFactory)` / `removeDomainByIds(ids, eventFactory)`）在删除成功后按 ID 构造并发布事件，适配"只查 ID 不加载 Domain"的性能优化路径
- `findDomainById` / `findDomainsByCondition` — 查询并转换为领域对象
- 乐观锁冲突 → 抛出 `IllegalStateException`（HTTP 409）
- 全量 UPDATE 策略（保证 update_time 审计字段始终刷新）
- 领域事件“先清后发”（避免监听器异常导致重复发布）
- `getEntityClass()` 使用 Spring `GenericTypeResolver.resolveTypeArguments()` 解析泛型参数（避免硬编码泛型位置索引）
- `findDomainPage(wrapper, pageNum, pageSize)` 内置防御性下限（pageNum≥1，pageSize≥1），不截断上限——上限由契约层 @Max 或实现类自行决定
- **事务约定**：`saveDomain()`/`updateDomain()` 不声明 `@Transactional`（Spring AOP 自调用不生效），子类 Repository 的 `save()`/`update()` 覆写方法必须自行标注 `@Transactional(rollbackFor = Exception.class)`；批量方法（`saveDomainBatch`/`updateDomainBatch`/`removeDomainByIds`/`removeDomains`）由基类声明事务（外部调用走代理）

### 领域事件

- `DomainEvent` — 事件基类（eventId + occurredOn）
- `DomainEventPublisher` — 发布契约接口
- `SpringDomainEventPublisher` — 桥接 Spring `ApplicationEventPublisher`，进程内同步发布

**事件能力设计要点：**

- 仅 AggregateRoot 有事件能力（聚合根 = 一致性边界 = 事件唯一出口）
- 仓储自动发布（有则发，无则静默），opt-in 点是 `registerEvent()`
- 先落库后发事件 + 先清后发（与 Spring Data `@DomainEvents` 同模式）
- 事件工厂重载仅删除有（补“按 ID 删无 Domain 对象”缺口）
- 逃生门：Handler 注入 `DomainEventPublisher` 手动发 / `clearDomainEvents()` 抑制
- 未来演进：Outbox 模式只需替换 publisher 实现，仓储契约不变

详细设计决策见下方「设计决策与未实现功能」表 + `docs/references.md`。

### MyBatis-Plus 自动配置

| 拦截器 | 说明 |
|--------|------|
| PaginationInnerInterceptor | 物理分页（AUTO 方言） |
| OptimisticLockerInnerInterceptor | 乐观锁（仅 @Version 实体生效） |
| BlockAttackInnerInterceptor | 防全表 UPDATE/DELETE（始终开启） |

`@ConditionalOnMissingBean` 允许业务项目完全自定义覆盖。

## 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-ddd</artifactId>
</dependency>
```

引入即生效。MyBatis-Plus 插件 + 领域事件发布器通过 Spring Boot 自动配置注册。

### 场景 1：聚合根 + 领域事件

```java
public class Order extends AggregateRoot<UUID> {

    private UUID id;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;

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

    @Override
    public UUID getId() { return id; }

    /** 支付：状态机校验 + 状态变迁 + 注册事件 */
    public void pay() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("order:err.invalidTransition");
        }
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(this.id));
    }

    /** 不变量校验（仓储 save/update 前自动调用） */
    @Override
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order:err.itemsEmpty");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("order:err.totalMustBePositive");
        }
    }
}
```

### 场景 2：对象转换（Assembler / Converter / Presenter）

三者均为普通 `@Component` 类，逐字段显式映射（不使用代码生成器）：

```java
// Assembler（Domain → DTO）
@Component
public class OrderAssembler implements BasicAssembler<Order, OrderDTO> {
    @Override
    public OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId().toString());
        dto.setStatus(order.getStatus().name());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setVersion(order.getVersion());
        return dto;
    }
    // toDomain / updateDomain / updateDTO 抛 UnsupportedOperationException（富领域模型走 reconstitute）
}

// Converter（Domain ↔ PO）
@Component
public class OrderConverter implements BasicConverter<Order, OrderPO> {
    @Override
    public Order toDomain(OrderPO po) {
        return Order.reconstitute(UUID.fromString(po.getId()), ...);
    }
    @Override
    public OrderPO toPO(Order domain) { /* 逐字段映射 */ }
}

// Presenter（DTO → CO，单向，过滤内部字段）
@Component
public class OrderPresenter implements BasicPresenter<OrderDTO, OrderCO> {
    @Override
    public OrderCO present(OrderDTO dto) {
        OrderCO co = new OrderCO();
        co.setId(dto.getId());
        co.setStatus(dto.getStatus());
        // version / 审计字段不暴露
        return co;
    }
}
```

完整示例见 `docs/sample-application/cookbook/write-path.md`。List/Set 集合方法由接口 default 实现提供，无需手写。

### 场景 5：RepositoryImpl（仓储实现）

```java
@Component
public class OrderRepositoryImpl
        extends MybatisRepositorySupport<OrderMapper, OrderPO, Order>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               OrderConverter converter) {
        super(domainEventPublisherProvider);
        this.converter = converter;
    }

    @Override
    protected BasicConverter<Order, OrderPO> getConverter() { return converter; }

    @Override
    public Optional<Order> findById(UUID id) { return findDomainById(id.toString()); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) { saveDomain(domain); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) { updateDomain(domain); }

    @Override
    public boolean exists(UUID id) { return existsDomainById(id.toString()); }

    @Override
    public void deleteById(UUID id) { removeDomainById(id.toString()); }
}
```

### 场景 6：批量操作与高级查询

```java
// 批量保存（方法内保证事务，中途失败整体回滚）
repository.saveDomainBatch(List.of(order1, order2, order3));

// 批量更新
repository.updateDomainBatch(List.of(order1, order2));

// 根据 ID 集合批量加载（返回顺序不保证与传入 ID 顺序一致）
List<Order> orders = repository.findDomainsByIds(List.of("id-1", "id-2", "id-3"));

// 删除领域实体（传入实体对象，内部提取 ID；删除成功后发布聚合已注册事件）
order.markCancelled();          // 聚合行为方法内部 registerEvent(new OrderCancelledEvent(...))
repository.removeDomain(order); // 删除成功 → 自动发布 OrderCancelledEvent

// 批量删除（传入实体列表，方法内保证事务；删除成功后逐聚合发布已注册事件）
repository.removeDomains(List.of(order1, order2));

// 按 ID 删除 + 事件工厂（性能优化路径：只查 ID 不加载 Domain，事件只携带 ID）
repository.removeDomainById(orderId, id -> new OrderDeletedEvent(id));

// 批量按 ID 删除 + 事件工厂（事件与 ID 一一对应）
repository.removeDomainByIds(orderIds, id -> new OrderDeletedEvent(id));

// 条件查询单个实体（多条匹配时抛 IllegalStateException）
Optional<Order> order = repository.findDomainOneByCondition(
        new LambdaQueryWrapper<OrderPO>().eq(OrderPO::getId, "order-1"));

// 条件统计
long count = repository.countByCondition(
        new LambdaQueryWrapper<OrderPO>().eq(OrderPO::getStatus, "PAID"));
```

### 场景 7：PageResult 分页链路

```java
// QueryHandler 中使用分页
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderDTO>> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    @Override
    public PageResult<OrderDTO> handle(GetOrderPageQuery query) {
        // Repository 返回 PageResult<Domain>，.map() 逐层转换
        return orderRepository.findDomainPage(query).map(orderAssembler::toDTO);
    }
}

// RepositoryImpl 中实现分页
public PageResult<Order> findDomainPage(GetOrderPageQuery query) {
    LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<OrderPO>()
            .eq(query.status() != null, OrderPO::getStatus, query.status())
            .orderByDesc(OrderPO::getCreateAt);
    // MybatisRepositorySupport 内置方法：Page<PO> → PageResult<Domain>
    return findDomainPage(wrapper, query.pageNum(), query.pageSize());
}
```

### 场景 8：DomainEvent 监听

```java
// 定义事件（domain 层）
public class OrderPaidEvent extends DomainEvent {
    private final UUID orderId;
    public OrderPaidEvent(UUID orderId) {
        super();  // 自动生成 eventId + occurredOn
        this.orderId = orderId;
    }
    public UUID getOrderId() { return orderId; }
}

// 监听事件（application 层）
@Component
public class OrderEventHandler {

    @EventListener  // 同事务，失败则主操作回滚
    public void onOrderPaid(OrderPaidEvent event) {
        // 处理逻辑
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 事务提交后
    public void onOrderPaidAfterCommit(OrderPaidEvent event) {
        // 发通知、更新缓存等副作用
    }
}
```

## 配置项

### DddAutoConfiguration

通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。

```java
@AutoConfiguration(after = MybatisPlusAutoConfiguration.class)
@ConditionalOnClass(BaseMapper.class)
@Import({SpringDomainEventPublisher.class, BasicAutoFillHandler.class, MybatisPlusPluginConfiguration.class})
public class DddAutoConfiguration {}
```

| 注册 Bean | 职责 | 覆盖方式 |
|---------|------|------|
| `SpringDomainEventPublisher` | 领域事件发布（桥接 Spring ApplicationEventPublisher） | 自行声明 DomainEventPublisher Bean |
| `BasicAutoFillHandler` | createAt/updateAt 自动填充 | 自行声明 MetaObjectHandler Bean |
| `MybatisPlusPluginConfiguration` | 分页 + 乐观锁 + 防全表攻击拦截器 | 自行声明 MybatisPlusInterceptor Bean（@ConditionalOnMissingBean） |

激活条件：仅当 MyBatis-Plus（`BaseMapper`）存在于 classpath 时激活。装配顺序：在 `MybatisPlusAutoConfiguration` 之后（确保 SqlSessionFactory 已就绪）。

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| 基类不持有 id/version 字段 | 避免继承污染；子类按业务需要声明（UUID / Long / 业务编码） |
| 全量 UPDATE 而非脏检查 | MyBatis-Plus 场景下脏检查收益极低且增加复杂度；全量更新保证审计字段刷新 |
| 领域事件进程内 Spring Event | 满足单体/模块内解耦需求；跨服务通过 Seata + RPC 显式调用 |
| EventHandler 无返回值 | Event 是 fire-and-forget；需要返回值的是 Command |
| Query 为纯标记（无泛型） | 避免 contract 与 internal 类型耦合；返回类型由 Service 方法签名定义 |
| Converter/Assembler/Presenter 纯手写显式映射 | AI 辅助开发下手写模板成本归零；不引入 MapStruct 等代码生成器（注解处理链/生成代码不可见/@MapperScan 误扫）；聚合根重建走 reconstitute，完整性由往返测试守护 |
| **未实现** 聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| **已实现** 仓储分页方法 | `findDomainPage` 返回 `PageResult<Domain>`，隔离 MyBatis-Plus Page；分页属于读侧 CQRS Query |
| **未实现** 脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| **未实现** 领域事件异步/跨进程发布 | 当前为进程内 Spring Event；跨服务通过 Seata + RPC |
| **已移除** Specification 模式 | MyBatis-Plus `LambdaQueryWrapper` 已是可组合查询规约；充血模型下校验内聚于聚合根；CQRS 分离后无“同一规则既做校验又做查询”场景 |
| 领域事件仅 AggregateRoot 可注册 | 聚合根是一致性边界和事件唯一出口；独立实体需要发事件 = 它应该建模为 AggregateRoot |
| 仓储自动发布事件（有则发，无则静默） | opt-in 点是 registerEvent()，不是 Repository；手动发布必然有人遗忘；极端抑制用 clearDomainEvents() |
| 事件发布封装在仓储而非调用方手动 | 保护“先持久化成功后发事件 + 先清后发”契约；与 Spring Data @DomainEvents 同模式；逃生门：Handler 注入 DomainEventPublisher 手动发 / clearDomainEvents() 抑制；未来可靠化走 Outbox（换 publisher 实现，契约不变） |
| save/update 无事件工厂重载（仅删除有） | 删除重载补“按 ID 删无 Domain 对象”的缺口；save/update 始终有 Domain，registerEvent 是唯一通道，双通道有重复发布风险；非聚合根发事件是建模信号（应升级为聚合根）而非框架缺口 |

## 依赖关系

```
common-ddd → common-contract（Command / Query / Event 标记接口）
           → common-exception（BusinessException）
           → mybatis-plus-spring-boot3-starter
           → mybatis-plus-jsqlparser
           → dynamic-datasource-spring-boot3-starter
```
