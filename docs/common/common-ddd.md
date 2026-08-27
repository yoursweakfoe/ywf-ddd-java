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

> `IntegrationEvent`（common-contract）在本包无对应 Handler 接口：入站经 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler；出站经 application 层 Publisher **翻译 + 同事务捕获入集成 Outbox**，实际投 MQ 由框架集成排空器（`OutboxRelay` 集成实例）承担。领域事件的进程内反应由业务侧 `DomainEventListener`（`@EventListener`）承担，框架提供 `application/event/listener|publisher/` 下的两个**空标记接口**定型该角色（见「领域事件」节）。

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
- `InProcessDomainEventPublisher` — 桥接 Spring `ApplicationEventPublisher`，进程内同步发布（位于 `infrastructure/event/domain/`）；由框架领域排空器在**其自有事务内**调用
- `DomainEventListener` — application 层域内反应监听器**空标记接口**（`application/event/listener/`）：定型「消费内部领域事件（Spring Event）」的角色，与 adapter 层处理外部集成事件的 Consumer 划清边界
- `IntegrationEventPublisher` — application 层集成事件出站 Publisher **空标记接口**（`application/event/publisher/`）：定型「翻译领域事件 → 契约 IntegrationEvent → **同事务捕获入集成 Outbox**」的角色（不直发 MQ），与 domain 层进程内发布的 `DomainEventPublisher` 划清边界

事件仅 AggregateRoot 可注册，仓储持久化后自动冲刷（opt-in 点是 `registerEvent()`），先落库后冲刷 + 先清后捕，捕获经 Outbox 与业务写入同事务（见下节）。

> **边界**：领域事件仅进程内消费（受众为 `DomainEventListener` 标记的域内反应监听器），但捕获与投递的可靠性由本模块的全链路 Outbox 承担。集成事件（IntegrationEvent）的**契约**在 common-contract、**翻译 + 捕获**在 application 层 Publisher，**MQ 投递**由本模块集成排空器经 `IntegrationEventSender` 接缝完成（MQ 实现依赖 common-mq，当前样例日志占位），**入站**由 adapter 层 Consumer 接收。

### 全链路 Outbox 可靠性规范：领域事件 + 集成事件强制经 Outbox 投递

进程内直发（发布即弃）无法跨越「提交后进程崩溃 / 监听器失败」的丢失窗口。Transactional
Outbox 是业界标准解法。本框架的定档（audit F-04 收口，ADR-0007 → ADR-0008 反转定稿）：
**每一条领域事件、每一条集成事件，都强制经 Outbox 投递；捕获与排空的完整管线由框架交付**，
业务侧只写「域内反应」与「翻译」两段业务语义。

**两段管线**：

```
捕获（与业务写入同事务——可靠性锚点）
  领域事件：聚合 registerEvent → 仓储 save/update → DomainEventFlusher 先清后捕
            → DomainEventOutboxStore.appendAll → ddd_domain_event_outbox
  集成事件：DomainEventListener（排空事务内）调用 Publisher 翻译
            → IntegrationEventOutboxStore.appendAll → ddd_integration_event_outbox
            （行 id = 新铸 UUID = 未来 MQ messageId；source_event_id = 源领域事件 eventId）

投递（框架排空器，at-least-once）
  OutboxRelay（每行一个 REQUIRES_NEW 事务）：
    认领（ORDER BY occurred_on LIMIT 1 FOR UPDATE SKIP LOCKED）
    → 派发 → UPDATE is_delete=TRUE（标记完成）→ 提交
  领域实例：经 codec 重建事件身份 → DomainEventPublisher 进程内派发（监听器加入本事务）
  集成实例：构造 OutboxEnvelope → IntegrationEventSender 投 MQ
  失败：attempts++、指数退避重投；超限转 DEAD（死信留表）
  已软删行过保留期后每日物理清除
```

**fail-fast（事件强制要求 Outbox）**：聚合注册了事件但容器中无 `DomainEventOutboxStore`
Bean 时，`DomainEventFlusher` 抛 `IllegalStateException` 回滚业务写入——要么不用事件，
要么带上 Outbox。**不存在静默丢弃，也不存在直发降级路径。**

**两张标准表**（PG 规范 DDL 随框架发行：`common-ddd/src/main/resources/sql/`）：

| 表 | 信封列 | 其余 |
|---|---|---|
| `ddd_domain_event_outbox` | `id = eventId`（幂等键与行身份合一）/ `event_type`（类全限定名）/ `payload`（JSON，TEXT 存储）/ `occurred_on`（UTC） | 簿记列（`attempts`/`next_retry_at`/`status`/`last_error`，形状由 `OutboxRelay` 钉死）+ 本仓标准结构列（`version`/`create_at`/`update_at`/`created_by`/`updated_by`/`is_delete`） |
| `ddd_integration_event_outbox` | 同上 + `source_event_id`（源领域事件 eventId 血缘；入站再发出为 NULL） | 同上 |

载荷用 `TEXT` 而非 `JSONB`：缺省实现以一条可移植 INSERT 写入（字符串绑定），框架从不
查询载荷内部字段，跨 H2（测试）/ PostgreSQL（生产）一致；需在库内查询载荷字段时经 SPI
替换实现（见 DDL 文末说明，含 payload > 2KB 的容量策略）。

**监听器契约（关键反转）**：派发发生在排空器事务内（**有活动事务**），因此——

- 监听器一律用普通 `@EventListener`；带数据库写入的副作用用普通 `@Transactional`
  （REQUIRED，**加入**排空事务）——「内部反应 + 集成入箱 + 标记完成」原子提交
- **禁用 `REQUIRES_NEW` 与 `@Async`**——二者都会撕碎上述原子性，重试时产生双份副作用
- 监听器不做任何非事务副作用（HTTP 调用 / 直发 MQ）——对外通知一律经集成 Outbox 捕获
- 监听器抛异常向上传播 → 排空事务回滚 → 行保持待投 → 退避重投；补偿型副作用
  （如取消订单回补库存）不再「失败只落日志」

**投递语义与身份契约**：

- **at-least-once**：崩溃恢复 / 并发认领下同一事件可能重复投递，消费端按身份幂等去重
  （领域 = `eventId`，经 codec 身份重建跨重投稳定；集成 = `messageId` = outbox 行 id）。
  框架不做 exactly-once，跨服务精确性依赖消费端幂等契约
- **顺序**：尽力 FIFO（按 `occurred_on` 认领）；退避重试可能乱序
- 业务回滚则入箱事件随行回滚（同事务），绝不会出现「状态未提交而事件已发出」
- 多实例部署：认领经 `FOR UPDATE SKIP LOCKED` 天然互斥（行级），无需分布式锁

**框架交付物**（详见类 Javadoc）：

- `DomainEventOutboxStore` —— 领域捕获 SPI（唯一方法 `appendAll(List<DomainEvent>)`，
  同事务义务写在契约里）；缺省实现 `JdbcDomainEventOutboxStore`（经 `JdbcTemplate`
  复用事务绑定连接写 `ddd_domain_event_outbox`）
- `IntegrationEventOutboxStore` —— 集成捕获端口，定义在**应用层**（domain 不得依赖
  contract、infrastructure 不得回调应用组件，与读侧 `QueryRepository` 端口同构）；
  缺省实现 `JdbcIntegrationEventOutboxStore`（`infrastructure/event/outbox/`）
- `OutboxRelay` —— 排空引擎（`infrastructure/event/outbox/scheduler/`），
  `OutboxAutoConfiguration` 装配领域 / 集成两个实例；`OutboxRelayScheduler`
  （`@Scheduled(fixedDelay)` 轮询 + 每日清除）统一驱动。排空器是框架管线，
  不实现 `ScheduledAdapter` 业务标记
- `IntegrationEventSender` —— MQ 投递接缝 SPI：集成排空器认领一行、构造
  `OutboxEnvelope`（messageId / eventType / payload / occurredOn）、调用实现投递，
  成功后才标记完成。common-mq 未建设，样例以 `LoggingIntegrationEventSender`
  （日志占位）接入；接入 RocketMQ / Kafka 时提供实现经 `@ConditionalOnMissingBean`
  顶替，把 `messageId` 置消息头供消费端去重
- `DomainEventCodec` —— 载荷格式自持（专用 `JsonMapper`，不随应用级序列化配置漂移），
  反序列化后以行身份重建 `eventId`/`occurredOn`（幂等键跨重投稳定）。消费方事件重放
  依赖构造器参数名绑定：编译开启 `-parameters`（spring-boot-starter-parent 默认）
  或提供 `protected` 无参构造器
- `sql/ddd_domain_event_outbox.sql`、`sql/ddd_integration_event_outbox.sql` —— 两张表的
  PG 规范 DDL

**配置**（`ywf.ddd.outbox.*`）：`enabled`（总开关，默认 `true`；`false` 时自动配置整体
退位——聚合一旦注册事件即 fail-fast）；`relay.fixed-delay`（轮询间隔，默认 1000ms）/
`relay.batch-size`（50）/ `relay.max-attempts`（10）/ `relay.max-backoff`（5m）/
`relay.retention-days`（7）/ `relay.purge-cron`（`0 0 3 * * *`）。

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
                               ObjectProvider<DomainEventOutboxStore> outboxStoreProvider,
                               OrderConverter converter) {
        super(mapper, outboxStoreProvider);
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
    // 投递发生在框架排空器（OutboxRelay）的自有事务内：普通 @EventListener 即可
    @EventListener
    public void onOrderPaid(OrderPaidEvent event) { /* 处理逻辑 */ }

    // 带数据库写入的副作用：普通 @Transactional（REQUIRED，加入排空事务，原子提交）
    // 禁用 REQUIRES_NEW / @Async —— 会撕碎「副作用 + 集成入箱 + 标记完成」的原子性
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) { /* 补偿写入，如库存回补 */ }
}
```

出站 Publisher 同理：`public class OrderEventPublisher implements IntegrationEventPublisher { ... }`（翻译领域事件 → 契约 IntegrationEvent → 经 `IntegrationEventOutboxStore` 同事务捕获入集成 Outbox，**不直发 MQ**——投递归框架集成排空器，见 `docs/application/cookbook/event-flow.md`）。

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
- **全链路 Outbox 可靠性规范**：领域事件与集成事件强制经 Outbox 投递——持久化后冲刷、先清后捕（快照 + 清空暂存，下游异常不重复捕获），捕获与业务写入同事务（提交 ⇒ 落库，跨崩溃不丢），框架排空器（`OutboxRelay`）在自有事务内派发（内部反应 + 集成入箱 + 标记完成原子提交，at-least-once）；无 Outbox Bean 时 fail-fast 回滚业务写入，不存在直发降级
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件 / Outbox 捕获实现均允许业务项目完全自定义覆盖

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

**决策**：选仓储自动发布。opt-in 点是 `registerEvent()`，先落库后冲刷 + 先清后捕（与 Spring Data `@DomainEvents` 同模式），捕获入 Outbox 后由框架排空器派发（见 ADR-0008）。事件仅 AggregateRoot 可注册（一致性边界 = 事件唯一出口）。逃生门：Handler 注入 `DomainEventPublisher` 手动发 / `clearDomainEvents()` 抑制。

**后果**：跨服务通信仍走 Seata + HTTP 显式调用；可靠化已由框架全链路 Outbox 承担（见 ADR-0007 → ADR-0008），`DomainEventPublisher` 契约保持不变。

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

### ADR-0007 领域事件 Outbox：框架只担保同事务捕获，投递归业务（已被 ADR-0008 反转）

- 状态：superseded（2026-08 accepted，audit F-04 收口、领地收缩定稿；同月被 ADR-0008 反转——框架改为交付全链路管线）

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

**确认**：`OutboxStore`（SPI）/ `DomainEventCodec` / `DomainEventFlusher` 编排 + `sql/ddd_outbox.example.sql` 参考 DDL。（以上为历史记录，下述 ADR-0008 已反转。）

### ADR-0008 全链路 Outbox：框架交付捕获 + 排空完整管线（反转 ADR-0007）

- 状态：accepted（2026-08，audit F-04 最终收口）

**背景**：ADR-0007 将领地收缩为「捕获契约 + 编解码工具」，实现与投递归业务。但样例始终无人实现捕获与排空，事件实际走在无持久化锚点的路径上，F-04 担保悬空；同时集成事件出站若由 Publisher 直发 MQ，「领域事件已派发 → 集成事件投 MQ」之间存在 dual-write 窗口。

**决策**：反转领地划分——**每一条领域事件、每一条集成事件强制经 Outbox 投递，框架交付完整管线**：

1. **捕获**：领域侧 `DomainEventOutboxStore` SPI + 缺省 `JdbcDomainEventOutboxStore`（`ddd_domain_event_outbox`）；集成侧应用层端口 `IntegrationEventOutboxStore` + 缺省 `JdbcIntegrationEventOutboxStore`（`ddd_integration_event_outbox`，行 id = 未来 MQ messageId，`source_event_id` 记录源领域事件血缘）。捕获一律与业务写入同事务。
2. **投递**：`OutboxRelay` 排空引擎（每行一个 REQUIRES_NEW 事务：`FOR UPDATE SKIP LOCKED` 认领 → 派发 → 软删标记完成），领域实例进程内派发、集成实例经 `IntegrationEventSender` SPI 投 MQ；失败指数退避重投，超限转死信，软删行过保留期物理清除。`OutboxRelayScheduler` 为框架管线（不实现 `ScheduledAdapter`）。
3. **fail-fast**：聚合注册事件但无捕获 Bean 时抛错回滚业务写入——要么不用事件，要么带上 Outbox，不存在直发降级。
4. **监听器契约反转**：派发在排空事务内执行，监听器用普通 `@EventListener` + 普通 `@Transactional`（加入排空事务），「内部反应 + 集成入箱 + 标记完成」原子提交；`REQUIRES_NEW` / `@Async` 禁用（撕碎原子性，重试产生双份副作用）；监听器不做非事务副作用，对外通知一律经集成 Outbox 捕获。

**反转理由**：① 捕获与排空是「机制」而非「策略」——两张标准表的形状由框架钉死后，排空器反而是最可复用、最不该让每个业务重写的一环；② ADR-0007 担心的「多张结构各异的消息表」并未出现，真实需求是统一的标准表 + 可替换接缝（`@ConditionalOnMissingBean` / SPI）；③ 生态方案（RocketMQ 事务消息 / CDC）仍是未来可选项，但框架先给出一条开箱即用的基线，业务拓扑定型后再整体替换，成本低于从零自建。

**后果**：`DomainEventPublisher` 契约不变（改由排空器调用）；仓储构造器收为 `(Mapper, ObjectProvider<DomainEventOutboxStore>)`；ADR-0007 的「业务侧待办」全部由框架承接；集成事件 MQ 投递在 common-mq 建设前以样例 `LoggingIntegrationEventSender` 日志占位。

**确认**：`DomainEventOutboxStore` / `JdbcDomainEventOutboxStore` / `IntegrationEventOutboxStore` / `JdbcIntegrationEventOutboxStore` / `OutboxRelay` / `OutboxRelayScheduler` / `IntegrationEventSender` / `DomainEventCodec` / `DomainEventFlusher`（fail-fast）+ `sql/ddd_domain_event_outbox.sql`、`sql/ddd_integration_event_outbox.sql` 规范 DDL。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：领域事件异步 / 跨进程发布 | 进程内可靠投递由框架全链路 Outbox 承担（ADR-0008：捕获 + 排空完整管线）；跨服务经集成事件 + 集成 Outbox + `IntegrationEventSender` 接缝，MQ 实现待 common-mq 建设（当前样例日志占位），东西向同步调用仍走 Seata + HTTP |
| 边界：Specification 模式 | 采纳为纯接口（可选工具）：领域规则 and/or/not 组合表达，供复杂校验场景；查询过滤仍用 MyBatis-Plus `LambdaQueryWrapper`，简单校验仍用聚合根 if-throw |
