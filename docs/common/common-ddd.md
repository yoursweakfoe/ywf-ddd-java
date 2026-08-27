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

> `IntegrationEvent`（common-contract）在本包无对应 Handler 接口：入站经 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler；出站经 application 层 Publisher 投递 MQ。领域事件的进程内反应由业务侧 `DomainEventListener`（`@EventListener`）承担，框架提供 `application/event/listener|publisher/` 下的两个**空标记接口**定型该角色（见「领域事件」节）。

`PageResult<T>` 是框架级分页容器（record），定义在 **contract 层**（与 `PageableQuery` 同居 `dto/query`），隔离 MyBatis-Plus `Page<PO>`，提供 `map()` 支持逐层转换。服务端 application（读端口 / Handler / AppService）与 infrastructure（读实现装填）均使用它，消费方从 common-contract 直接拿到分页元数据（records / total / pageNum / pageSize）。

`ApplicationService` 是 application 层聚合协调入口的**标记接口**（`common-ddd/application/service/`），业务侧 `XxxAppService` 实现之，与 domain 层 `DomainService` 标记对偶（应用编排 vs 领域协调）。业务类名沿用缩写 `XxxAppService`（`App` = Application 的缩写，仅类名简洁），标记接口保持全名语义（与 `IntegrationEventPublisher ↔ XxxEventPublisher` 缩写惯例同构）。

Adapter 层入口同样以**空标记**定型角色：

- `RestAdapter`（`common-ddd/adapter/rest/controller/`）—— REST 入口适配器标记。业务 `XxxControllerImpl` 在实现 contract 的 `XxxController` 契约接口之外再实现之（contract 接口承载 HTTP 面，标记声明「adapter 层 REST 入口」身份，供 ArchUnit 识别）。不命名 `Controller`：与 contract 契约接口及 Spring `@Controller` 过宽/易混淆。同类标记还有 `ScheduledAdapter`（`adapter/task/scheduler/`，定时任务入口）与 `IntegrationEventConsumer`（`adapter/event/consumer/`，MQ 入站），三者构成「协议伞 / 角色」两级式的对称包结构
- `IntegrationEventConsumer`（`common-ddd/adapter/event/consumer/`）—— 集成事件入站消费者标记，与 application 层出站 `IntegrationEventPublisher` 对偶。当前为框架预留（common-mq 未建设，无实现类），模板见 `docs/application/cookbook/mq-consumer.md`

### 对象转换

| 接口 | 层 | 方向 |
|------|----|------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） |

三者均为普通 `@Component` 类、逐字段显式赋值（不使用代码生成器）。**最小契约原则**：`BasicAssembler` 仅声明 `toDomain`/`toDTO`、`BasicConverter` 仅声明 `toDomain`/`toPO`（+ List/Set 集合委托 default 方法），**不定义增量更新方法**——需要增量合并的实现类自行声明普通方法（如 `updatePO` 合并业务字段），富领域模型因此无需任何「不支持也要写 throw」样板。富领域模型的 `toDomain` 走 `reconstitute()` 静态工厂。

被转换的 `DTO` 由 `ApplicationDTO` **空标记接口**（`common-ddd/application/dto/`）定型：业务顶层 DTO 类（写侧 `XxxDTO` / 读侧 `XxxViewDTO`）实现之，与 contract 层对外 `CO` 标记对偶（DTO = 内部视图可含 version/审计，CO = 经 Presenter 清洗后对外暴露）。嵌套 DTO（如 `OrderDTO.OrderItemDTO`）随外层定型，不重复标记。

### 仓储支撑（MybatisPlusPersistence）

组合持有 `BaseMapper`（不继承 ServiceImpl，避免 `save(PO)`/`updateById(PO)` 等底层 PO 直操方法泄漏为公开 API），封装：

- `saveDomain` / `updateDomain` — 持久化前自动 `validate()`，持久化后发布领域事件
- `removeDomain` / `removeDomains` — 删除成功后发布聚合已注册事件；`removeDomains` 带**存在性过滤**：预查真实存在的 ID，仅为实际删除的聚合发事件，不存在的静默跳过
- `removeDomainById` / `removeDomainByIds` — 纯技术删除不发事件；**事件工厂重载**在删除成功后按 ID 构造并发布事件（适配"只查 ID 不加载 Domain"路径），`removeDomainByIds(ids, factory)` 同样带存在性过滤（部分存在 → 仅为真实删除者发事件不报错；全部不存在仍抛 `IllegalStateException`）
- `findDomainById` / `findDomainsByIds` / `findDomainOneByCondition` — 写侧加载聚合（load → 行为 → save 链路）
- 乐观锁冲突 → `IllegalStateException`（HTTP 409）
- **事务边界上收**：本类不声明 `@Transactional`，事务由应用层 Handler 控制（批量原子性由调用方包裹事务保证）

### 领域事件

- `DomainEvent` — 事件基类（eventId + occurredOn，字段 `private final` 不可变）
- `DomainEventPublisher` — 发布契约接口
- `InProcessDomainEventPublisher` — 桥接 Spring `ApplicationEventPublisher`，进程内同步发布（位于 `infrastructure/event/domain/`）
- `DomainEventListener` — application 层域内反应监听器**空标记接口**（`application/event/listener/`）：定型「消费内部领域事件（Spring Event）」的角色，与 adapter 层处理外部集成事件的 Consumer 划清边界
- `IntegrationEventPublisher` — application 层集成事件出站 Publisher **空标记接口**（`application/event/publisher/`）：定型「翻译领域事件 → 契约 IntegrationEvent → 投递 MQ」的角色，与 domain 层进程内发布的 `DomainEventPublisher` 划清边界

事件仅 AggregateRoot 可注册，仓储自动发布（opt-in 点是 `registerEvent()`），先落库后发事件 + 先清后发。

> **边界**：领域事件仅进程内消费（受众为 `DomainEventListener` 标记的域内反应监听器）。集成事件（IntegrationEvent）的收发不在本模块：出站由 application 层 `IntegrationEventPublisher` 标记的 Publisher 投递 MQ（依赖 common-mq），入站由 adapter 层 Consumer 接收。

### 领域事件 Outbox：同事务捕获契约（框架领地）+ 实现与投递全部归业务

进程内直发（发布即弃）无法跨越「提交后进程崩溃 / 监听器失败」的丢失窗口。Transactional
Outbox 是业界标准解法，但它的两半**可靠性来源完全不同**（audit F-04 收口时据此划定领地）：

- **捕获（capture）**——事件与业务写入同事务落表。这是 outbox 的可靠性根基，
  框架只给出**契约**（`OutboxStore` SPI，`infrastructure/event/outbox/`）：
  「聚合状态已提交 ⇒ 事件必然已落库；业务回滚 ⇒ 事件随行回滚」；
- **捕获的实现与投递**——表结构、序列化、扫表、认领、派发、重试、死信**全部归业务侧**。
  框架不提供任何缺省实现、不内置投递器。

```
聚合持久化（业务事务内）
  → DomainEventFlusher 快照事件、清空暂存（先清后发）
  → OutboxStore.appendAll：业务实现将事件写入自己的消息表（与业务写入同事务——可靠性锚点）
  ★ 框架领地到此为止（契约 + 编解码工具）
业务事务提交后（业务领地）
  → 业务排空器 / 生态方案：认领 → 反序列化并重建事件身份 → 发布
    → 成功删行；失败不删行（标记重试），重试耗尽转死信留表
```

**为什么框架连缺省实现都不提供**：

1. 真实业务会按查询效率 / 单表洁净拆出**多张消息表**（按领域、甚至按业务环节），各表列结构
   与处理机制互不相通——通用缺省表是伪需求，留着只会让人误以为「框架那张表能直接用于生产」；
2. 投递侧生态已有成熟方案，按拓扑对号入座，自研通用版是最没有价值的一环：

   | 投递拓扑 | 成熟方案 |
   |---|---|
   | MQ 出站（本栈方向） | **RocketMQ 事务消息**（半消息 + 本地事务 + broker 回查，relay 职责被 broker 吞掉） |
   | Kafka 系 | Debezium / CDC 尾日志（经典 Transactional Outbox 的工业级形态） |
   | 进程内可靠监听 | Spring Modulith 事件发布注册表（event publication registry） |

3. 本仓 common-mq 未建设、拓扑未定——此刻造实现就是赌未知数（规则 04：「以后可能用到」不是理由）。

**统一信封标准（`DomainEventCodec` 定义的捕获侧约定）**：无论业务的表怎么拆、列怎么设计，
捕获与投递之间只有一条跨边界约定——信封四元组：

| 元素 | 含义 | 业界对照 |
|---|---|---|
| `id` = `eventId` | 幂等键与行身份合一（at-least-once 去重锚点） | Debezium/Apicurio outbox 的 `id`；Modulith 的 publication `id` |
| `eventType` | 事件类全限定名，反序列化锚点 | Modulith `eventType`；Debezium `type` |
| `payload` | 事件 JSON 载荷（字段级捕获，不依赖 getter 惯例） | 各家一致的 `payload`/`serializedEvent` 列 |
| `occurredOn` | 事件发生时间（UTC） | Modulith `publicationDate` |

参考表结构随框架发行：`common-ddd/src/main/resources/sql/ddd_outbox.example.sql`
（注意是 **example**——与本仓表标准对齐：信封列 + 簿记列 + `version`/`create_at`/`update_at`/
`created_by`/`updated_by`/`is_delete` 标准结构，载荷用原生 `JSONB`，`last_error` 为 `TEXT`，
认领索引为排除软删行的部分索引；文末附 payload > 2KB 的三种容量策略）。
簿记列（`attempts`/`next_retry_at`/`status`/`last_error`）**不构成标准**：业界 relay 型表
普遍携带此类列（认领租约 / 重试计数 / 状态枚举），但具体形状由排空器自定——Modulith 甚至
按「事件 × 监听器」粒度记录完成状态（`listenerId` + `completionAttempts`），正说明簿记语义
不该由框架钉死。

**投递语义与监听器契约**：

- **at-least-once**：崩溃恢复 / 并发认领下同一事件可能重复投递，消费端以 `eventId` 幂等去重（固有语义，框架不做 exactly-once）
- **监听器只在业务事务提交之后执行**（无活动事务）：一律用普通 `@EventListener`，不要用 `@TransactionalEventListener(AFTER_COMMIT)`（无事务可挂靠，默认不执行）；监听器内数据库写入须自带 `@Transactional(propagation = REQUIRES_NEW)`
- 监听器抛异常不回滚业务事务（已提交）；排空器对失败条目「不删行」即天然重投——补偿型副作用（如取消订单回补库存）不再「失败只落日志」
- 业务回滚则入箱事件随行回滚（同事务），绝不会出现「状态未提交而事件已发出」
- 多实例部署：排空入口的互斥（分布式锁 / 平台化调度单点执行）业务自担，重复投递由消费端幂等兜底

**框架交付物**（详见类 Javadoc）：

- `OutboxStore` —— 捕获 SPI，唯一方法 `appendAll(List<DomainEvent>)`，同事务义务写在契约里。业务提供该 Bean 即激活捕获路径（`DomainEventFlusher` 经 `ObjectProvider` 自动接入）；未提供则事件回退直发路径（提交后进程内派发，at-most-once）
- `DomainEventCodec` —— 载荷格式自持（专用 `JsonMapper`，不随应用级序列化配置漂移），反序列化后以行身份重建 `eventId`/`occurredOn`（幂等键跨重投稳定）；自动配置注册为 Bean，业务的捕获实现与排空器共用。消费方事件重放依赖构造器参数名绑定：编译开启 `-parameters`（spring-boot-starter-parent 默认）或提供 `protected` 无参构造器
- `sql/ddd_outbox.example.sql` —— 参考表结构（见上）

**业务侧待办（框架不做）**：实现 `OutboxStore`（含「捕获走事务感知连接 / 排空簿记走独立
连接」的纪律——提交后回调时机复用事务绑定连接会写入僵尸事务，簿记被回滚、条目反复重投）；
实现排空器（定时入口 + 认领/发布/标记）；决定重试 / 死信 / 多实例互斥策略，或直接选用生态方案。

**配置**：仅 `ywf.ddd.outbox.enabled`（默认 `true`；`false` 时 codec 装配退位）。

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
        extends MybatisPlusPersistence<OrderMapper, OrderPO, Order, UUID>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper,
                               ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               ObjectProvider<OutboxStore> outboxStoreProvider,
                               OrderConverter converter) {
        super(mapper, domainEventPublisherProvider, outboxStoreProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Order, OrderPO> getConverter() { return converter; }

    @Override public Optional<Order> findById(UUID id) { return findDomainById(id); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) { saveDomain(domain); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) { updateDomain(domain); }

    @Override public void deleteById(UUID id) { removeDomainById(id); }
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
public class OrderDomainEventListener implements DomainEventListener {  // 实现空标记：定型「域内反应」角色
    // 投递已在业务事务提交后发生（Outbox）：普通 @EventListener 即可，
    // 不要用 @TransactionalEventListener(AFTER_COMMIT)（无事务可挂靠，默认不执行）
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) { /* 处理逻辑 */ }

    // 带数据库写入的副作用：自带独立事务（派发时无活动事务）
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) { /* 补偿写入，如库存回补 */ }
}
```

出站 Publisher 同理：`public class OrderEventPublisher implements IntegrationEventPublisher { ... }`（翻译领域事件 → 契约 IntegrationEvent → 投递 MQ，见 `docs/application/cookbook/event-flow.md`）。

完整示例见 `docs/application/cookbook/write-path.md`。

## 4. 依赖关系

```
common-ddd → common-contract（Command / Query / IntegrationEvent 标记接口）
           → common-exception（BusinessException）
           → mybatis-plus-spring-boot4-starter
           → mybatis-plus-jsqlparser
           → jackson-databind（Outbox 载荷编解码，版本随 Spring Boot BOM）
           → dynamic-datasource-spring-boot4-starter（test scope，多数据源兼容性验证）
           → h2（test scope，Outbox / 持久化测试的内嵌库）
```

## 5. 设计原则

- **对偶原则（包结构镜像）**：框架支撑类的包层级与业务使用它的层级对齐——业务在 domain 层用（`AggregateRoot`、`Repository`、`DomainEvent`、`DomainService`）→ 放 `common-ddd/domain`；业务在 application 层用（`QueryHandler`、`BasicAssembler`、`ApplicationService`、`DomainEventListener`、`IntegrationEventPublisher`、`ApplicationDTO`）→ 放 `common-ddd/application`；业务在 adapter 层用（`RestAdapter`、`IntegrationEventConsumer`）→ 放 `common-ddd/adapter`；业务在 infrastructure 层用（`MybatisPlusPersistence`、`BasicConverter`）→ 放 `common-ddd/infrastructure`。`PageResult`/`PageableQuery` 属契约层（分页信封是消费方可见的契约类型）→ 放 `common-contract/dto/query`。
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **领域事件先清后发 + Outbox 捕获契约**：持久化后冲刷、先清后发，避免监听器异常导致重复发布；业务提供 `OutboxStore` 实现时事件与业务同事务入箱（跨崩溃不丢），投递归业务排空器（at-least-once，监听器只在提交后执行）；未提供时回退直发路径
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件 / Outbox 存储均允许业务项目完全自定义覆盖

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

**后果**：跨服务通信仍走 Seata + HTTP 显式调用；可靠化已由框架层 Outbox 承担（见 ADR-0007），`DomainEventPublisher` 契约保持不变。

**确认**：`InProcessDomainEventPublisher` + `MybatisPlusPersistence` 自动发布。

### ADR-0004 对象转换纯手写，不用 MapStruct

- 状态：accepted

**背景**：Converter/Assembler/Presenter 用代码生成器还是手写。

**决策**：选手写显式映射。AI 辅助开发下手写模板成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在。聚合根重建走 reconstitute，完整性由往返测试守护。

**确认**：`BasicConverter` / `BasicAssembler` / `BasicPresenter` 无生成器依赖。

### ADR-0005 CQRS 契约：Query 纯标记（EventHandler 已移除）

- 状态：accepted（2026-08 修订）

**背景**：Handler 接口的契约设计。

**决策**：Query 为纯标记（无泛型，避免 contract 与 internal 类型耦合，返回类型由 Service 方法签名定义）。原 `EventHandler<E>` 接口已移除：入站集成事件（IntegrationEvent）统一由 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler，不设独立事件 Handler；域内反应（对领域事件）由业务侧 `DomainEventListener`（`@EventListener`）承担，框架以 `DomainEventListener` / `IntegrationEventPublisher` 两个空标记定型 application 层「监听 / 出站」两角色（2026-08 增补）。

**确认**：`QueryHandler` / `CommandHandler` 接口签名。

### ADR-0006 删除操作的事件工厂重载（save/update 不提供）

- 状态：accepted

**背景**：删除路径何时需要事件工厂。

**决策**：仅删除提供事件工厂重载，补「按 ID 删无 Domain 对象」的缺口。save/update 始终有 Domain，`registerEvent` 是唯一通道，双通道有重复发布风险；非聚合根发事件是建模信号（应升级为聚合根）而非框架缺口。

**确认**：`removeDomainById(id, eventFactory)` / `removeDomainByIds(ids, eventFactory)` 重载存在，save/update 无对应重载。

### ADR-0007 领域事件 Outbox：框架只担保同事务捕获，投递归业务

- 状态：accepted（2026-08，audit F-04 收口；领地收缩定稿）

**背景**：进程内直发在「提交后崩溃 / 监听器失败」下永久丢事件（F-04）。Transactional Outbox 是标准解法，但需划定框架做到哪一步。

**选项**：
- 进程内直发 + `afterCommit`：可见性正确但无持久化锚点，崩溃即失
- **框架同事务捕获 + 业务侧投递**（采纳）：框架担保「提交 ⇒ 落库」；排空/重试/死信由业务排空器或生态方案承担
- 框架内置完整 relay（清扫线程 + 通用退避死信）（拒绝，领地收缩时推翻）：投递语义由投递拓扑决定，真实业务会拆多张结构各异的消息表、处理机制互不相通，通用排空器是对不存在的具体拓扑的假设；且生态已有成熟方案（RocketMQ 事务消息 / Debezium CDC / Modulith EPR）
- 内存 Outbox 兜底（拒绝）：重启即失却以「Outbox」为名，名实不符；无数据源时整体退位回直发路径

**决策**：框架领地 = 捕获契约 + 编解码工具，**不提供任何缺省实现**。`OutboxStore` SPI 唯一方法 `appendAll(List<DomainEvent>)`（同事务义务写在契约里）；`DomainEventCodec` 定义信封标准并注册为 Bean。三个关键正确性决策：
1. **契约只钉同事务义务**——「聚合状态已提交 ⇒ 事件必然已落库；业务回滚 ⇒ 事件随行回滚」；实现细节（表结构 / 连接纪律 / 序列化）归业务。连接纪律作为已知坑写入契约 Javadoc：捕获走事务感知连接，排空簿记走独立连接——提交后回调等时机复用事务绑定连接会写入僵尸事务，簿记被回滚、条目反复重投（H2 关闭即提交的宽松语义掩盖该缺陷，PG + HikariCP 必然显形）。
2. **统一信封标准**——捕获与投递之间唯一跨边界约定：`eventId`（幂等键=行身份）+ `eventType`（反序列化锚点）+ `payload`（字段级 JSON）+ `occurredOn`。对照 Debezium outbox / Modulith EPR 收敛；簿记列（attempts/next_retry_at/status/last_error）不构成标准（Modulith 按「事件×监听器」粒度记录即反例）。参考表结构随框架发行为 `sql/ddd_outbox.example.sql`（example，非组件）。
3. **身份重建**——反序列化经构造器生成临时 eventId，codec 以行身份覆盖，保证幂等键跨重投稳定；`DomainEvent` 字段保持 `private final`（不可变硬约束），重建经反射完成。

**后果**：监听器契约变更——只在提交后执行、无活动事务，一律 `@EventListener` + 写入自带 `REQUIRES_NEW`；`@TransactionalEventListener(AFTER_COMMIT)` 不再适用。业务未提供 `OutboxStore` Bean 时事件回退直发路径（提交后进程内派发，at-most-once）。实现、排空、重试/死信/多实例互斥全部由业务按真实考验落地或直接选用生态方案（RocketMQ 事务消息 / Debezium CDC / Modulith EPR）。F-04 担保收窄为「捕获契约 + 业务实现/排空的最小 at-least-once 责任」。

**确认**：`OutboxStore`（SPI）/ `DomainEventCodec` / `DomainEventFlusher` 编排 + `sql/ddd_outbox.example.sql` 参考 DDL。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：领域事件异步 / 跨进程发布 | 进程内可靠化由业务按 Outbox 捕获契约实现（ADR-0007，框架只给 SPI + codec）；跨服务经集成事件 + MQ（二期），当前仍走 Seata + HTTP 显式调用 |
| 边界：Specification 模式 | 采纳为纯接口（可选工具）：领域规则 and/or/not 组合表达，供复杂校验场景；查询过滤仍用 MyBatis-Plus `LambdaQueryWrapper`，简单校验仍用聚合根 if-throw |
