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

> `IntegrationEvent`（common-contract）在本包无对应 Handler 接口：入站经 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler；出站经 application 层 Capture **翻译 + 同事务捕获入集成 Outbox**，实际投 MQ 由框架集成排空器（`OutboxRelay` 集成实例）承担。领域事件的进程内反应由业务侧 `DomainEventListener`（`@EventListener`）承担，框架提供 `application/event/listener|capture/` 下的两个**空标记接口**定型该角色（见「领域事件」节）。

`PageResult<T>` 是框架级分页容器（record），定义在 **contract 层**（与 `PageableQuery` 同居 `dto/query`），隔离 MyBatis-Plus `Page<PO>`，提供 `map()` 支持逐层转换。服务端 application（读端口 / Handler / AppService）与 infrastructure（读实现装填）均使用它，消费方从 common-contract 直接拿到分页元数据（records / total / pageNum / pageSize）。

`ApplicationService` 是 application 层聚合协调入口的**标记接口**（`common-ddd/application/service/`），业务侧 `XxxAppService` 实现之，与 domain 层 `DomainService` 标记对偶（应用编排 vs 领域协调）。业务类名沿用缩写 `XxxAppService`（`App` = Application 的缩写，仅类名简洁），标记接口保持全名语义（与 `IntegrationEventCapture ↔ XxxIntegrationEventCapture` 缩写惯例同构）。

Adapter 层入口同样以**空标记**定型角色：

- `RestAdapter`（`common-ddd/adapter/rest/controller/`）—— REST 入口适配器标记。业务 `XxxControllerImpl` 在实现 contract 的 `XxxController` 契约接口之外再实现之（contract 接口承载 HTTP 面，标记声明「adapter 层 REST 入口」身份，供 ArchUnit 识别）。不命名 `Controller`：与 contract 契约接口及 Spring `@Controller` 过宽/易混淆。同类标记还有 `ScheduledAdapter`（`adapter/task/scheduler/`，定时任务入口）与 `IntegrationEventConsumer`（`adapter/event/consumer/`，MQ 入站），三者构成「协议伞 / 角色」两级式的对称包结构
- `IntegrationEventConsumer`（`common-ddd/adapter/event/consumer/`）—— 集成事件入站消费者标记，与 application 层出站捕获 `IntegrationEventCapture` 对偶。当前为框架预留（common-mq 未建设，无实现类），模板见 `docs/application/cookbook/mq-consumer.md`

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

- `saveDomain` / `updateDomain` — 持久化前自动 `validate()`，持久化成功后捕获已注册事件入箱
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
- `IntegrationEventCapture` — application 层集成事件**出站捕获** **空标记接口**（`application/event/capture/`）：定型「翻译领域事件 → 契约 IntegrationEvent → **同事务捕获入集成 Outbox**」的角色（不投递——出站投递由框架集成排空器经 `IntegrationEventSender` 完成），与 domain 层进程内发布的 `DomainEventPublisher` 划清边界

事件仅 AggregateRoot 可注册（opt-in 点是 `registerEvent()`），时序为：registerEvent → 持久化成功 → 先清后入箱（与业务写入同事务）→（异步）框架领域排空器 → 进程内派发。

> **边界**：领域事件仅进程内消费（受众为 `DomainEventListener` 标记的域内反应监听器），但捕获与投递的可靠性由本模块的全链路 Outbox 承担。集成事件（IntegrationEvent）的**契约**在 common-contract、**翻译 + 捕获**在 application 层 Capture，**MQ 投递**由本模块集成排空器经 `IntegrationEventSender` 接缝完成（MQ 实现依赖 common-mq，当前样例日志占位），**入站**由 adapter 层 Consumer 接收。

### 全链路 Outbox 可靠性规范：领域事件 + 集成事件强制经 Outbox 投递

进程内直发（发布即弃）无法跨越「提交后进程崩溃 / 监听器失败」的丢失窗口。Transactional
Outbox 是业界标准解法。本框架的定档（audit F-04 收口，ADR-0007 → ADR-0008 → ADR-0009）：
**每一条领域事件、每一条集成事件，都强制经 Outbox 投递；框架交付契约（SPI）+ 排空策略
管线，持久化实现归使用方（SPI-only，框架零 SQL）**——业务侧写「域内反应」「翻译」与
「Outbox 实现」三段。

> **Breaking change（2026-08，ADR-0009）**：框架不再随发行携带缺省 JDBC 捕获实现与建表
> DDL（原 `common-ddd/src/main/resources/sql/` 下的两张 outbox 脚本已移除），二者外移为
> sample 中的**参考实现 / 参考 DDL**。框架不自动装配捕获 Bean——升级使用方必须自行实现
> 三个 SPI 并建表（见「使用方式」场景 0）。

> **Breaking change（2026-08，ADR-0010）**：框架不再清除事件行——`OutboxRowAccess.purge`
> 方法与 `relay.retention-days` / `relay.purge-cron` 配置已移除。已投递行软删留痕
> （`is_delete=TRUE`），搬运 / 归档由数据抽取层按自身节奏处理（见 ADR-0010）。

**两段管线**：

```
捕获（与业务写入同事务——可靠性锚点；SPI 契约归框架，实现归使用方）
  领域事件：聚合 registerEvent → 仓储 save/update → DomainEventCapture 先清后入箱
            → DomainEventOutboxStore.appendAll（SPI）→ 领域 outbox 表（参考表 ddd_domain_event_outbox）
  集成事件：DomainEventListener（排空事务内）调用 Capture 翻译
            → IntegrationEventOutboxStore.appendAll（应用层端口）→ 集成 outbox 表（参考表 ddd_integration_event_outbox）
            （行 id = 新铸 UUID = 未来 MQ messageId；source_event_id = 源领域事件 eventId）

投递（框架排空策略，at-least-once；行持久化经 OutboxRowAccess SPI 由使用方实现）
  OutboxRelay（每行一个 REQUIRES_NEW 事务）：
    claimOne 认领（实现须多实例并发安全，如 SKIP LOCKED 行锁跳过）
    → 派发 → markDone 标记完成 → 提交（三者原子）
  失败簿记：recordFailure 走独立 REQUIRES_NEW 事务（框架算好退避 / 死信，实现纯持久化）
  领域实例：经 codec 重建事件身份 → DomainEventPublisher 进程内派发（监听器加入本事务）
  集成实例：构造 OutboxEnvelope → IntegrationEventSender 投 MQ
  失败：attempts++、指数退避重投；超限转 DEAD（死信留表）
  已完成行软删留痕（is_delete=TRUE），框架不删除——搬运 / 归档归数据抽取层（ADR-0010）
```

**fail-fast（事件强制要求 Outbox）**：聚合注册了事件但容器中无 `DomainEventOutboxStore`
Bean 时，`DomainEventCapture` 抛 `IllegalStateException` 回滚业务写入——要么不用事件，
要么带上 Outbox。**不存在静默丢弃，也不存在直发降级路径。**

**两张标准表**（**参考约定，非框架强制**；参考 DDL 由 sample 持有：
`sample-application/sample-service/sample-service-server/src/main/resources/sql/`）：

| 表 | 信封列 | 其余 |
|---|---|---|
| `ddd_domain_event_outbox` | `id = eventId`（幂等键与行身份合一）/ `event_type`（类全限定名）/ `payload`（JSON，TEXT 存储）/ `occurred_on`（UTC） | 簿记列（`attempts`/`next_retry_at`/`status`/`last_error`，参考约定——框架经 `OutboxRowAccess` SPI 抽象，不感知具体列形态）+ 本仓标准结构列（`version`/`create_at`/`update_at`/`created_by`/`updated_by`/`is_delete`） |
| `ddd_integration_event_outbox` | 同上 + `source_event_id`（源领域事件 eventId 血缘；入站再发出为 NULL） | 同上 |

表名 / 列形 / 载荷存储（参考实现用 `TEXT` 而非 `JSONB`，一条可移植 INSERT 写入）均为
sample 参考实现的选型，使用方可按自身数据库方言与容量策略自行变化——捕获侧只依赖
`appendAll` 契约、排空侧只依赖 `OutboxRowAccess` 契约，框架从不绑定具体表结构。

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
- 多实例部署：认领的并发互斥由行访问实现保证（SPI 硬约束「claimOne 必须并发安全」；
  参考实现经 `FOR UPDATE SKIP LOCKED` 行级互斥），无需分布式锁

**框架交付物**（详见类 Javadoc；**全部为契约与策略，无任何缺省实现 / SQL**）：

- `DomainEventOutboxStore` —— 领域捕获 SPI（唯一方法 `appendAll(List<DomainEvent>)`，
  同事务义务写在契约里）；**框架不提供缺省实现**，使用方自行实现（参考实现见 sample
  `infrastructure/event/outbox/`）
- `IntegrationEventOutboxStore` —— 集成捕获端口，定义在**应用层**（domain 不得依赖
  contract、infrastructure 不得回调应用组件，与读侧 `QueryRepository` 端口同构）；
  **框架不提供缺省实现**，使用方实现（参考实现见 sample）
- `OutboxRowAccess` —— 排空侧行访问 SPI（`infrastructure/event/outbox/scheduler/`）：
  `kind()` / `claimOne(OffsetDateTime)` / `markDone(String, OffsetDateTime)` /
  `recordFailure(String, int, OffsetDateTime, String, boolean, OffsetDateTime)`——
  全部方法必须加入调用方当前事务（同事务义务写在契约里），
  认领必须多实例并发安全；**使用方实现**（参考实现见 sample）
- `OutboxRow` —— 认领行载体 record（`id` / `eventType` / `payload` / `occurredOn` /
  `attempts`，不可变）：捕获与投递之间跨边界约定在排空侧的最小集
- `OutboxKind` —— 行类别枚举（`DOMAIN` / `INTEGRATION`）：决定排空派发回调走向；
  同类别的多个实现各自独立装配排空引擎（支持分表）
- `OutboxRelay` —— 排空引擎（**纯 Java 策略骨架，零 SQL / 零 JDBC / 零 DataSource**）：
  每行一个 REQUIRES_NEW 事务（claimOne → 派发 → markDone 原子提交），失败簿记走独立
  REQUIRES_NEW 事务（recordFailure）；指数退避 `2^n` 封顶 `max-backoff`，超限转死信
  （默认 `max-attempts` = 10）——**重试策略全在框架，实现只做纯持久化**；
  `OutboxRelayScheduler`（`@Scheduled(fixedDelay)` 轮询）统一驱动。排空器只捕获后排空、
  绝不删除事件行（ADR-0010），是框架管线，不实现 `ScheduledAdapter` 业务标记
- `IntegrationEventSender` —— MQ 投递接缝 SPI：集成排空器认领一行、构造
  `OutboxEnvelope`（messageId / eventType / payload / occurredOn）、调用实现投递，
  成功后才标记完成。common-mq 未建设，样例以 `LoggingIntegrationEventSender`
  （日志占位）接入；接入 RocketMQ / Kafka 时提供实现经 `@ConditionalOnMissingBean`
  顶替，把 `messageId` 置消息头供消费端去重
- `DomainEventCodec` —— 载荷格式自持（专用 `JsonMapper`，不随应用级序列化配置漂移），
  反序列化后以行身份重建 `eventId`/`occurredOn`（幂等键跨重投稳定）。消费方事件重放
  依赖构造器参数名绑定：编译开启 `-parameters`（spring-boot-starter-parent 默认）
  或提供 `protected` 无参构造器

**装配**（`OutboxAutoConfiguration`，不再自动提供任何捕获实现）：提供 `DomainEventCodec`
（`@ConditionalOnMissingBean`）；按**全部已注册的 `OutboxRowAccess` Bean** 组装
`OutboxRelayScheduler`——门控 `@ConditionalOnBean(OutboxRowAccess + PlatformTransactionManager)`，
使用方注册了行访问（且存在事务管理器）即自动获得排空，否则整体静默跳过；每个行访问按
`OutboxKind` 各配一个排空引擎——**注册了 INTEGRATION 行访问而没有 `IntegrationEventSender`
Bean 时启动即失败**（有入箱无投递 = 捕获后永无排空）。

**配置**（`ywf.ddd.outbox.*`）：`enabled`（总开关，默认 `true`；`false` 时自动配置整体
退位——聚合一旦注册事件即 fail-fast）；`relay.fixed-delay`（轮询间隔，默认 1000ms）/
`relay.batch-size`（50）/ `relay.max-attempts`（10）/ `relay.max-backoff`（5m）。

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

### 场景 0：接入 Outbox（SPI-only，必读）

框架只定契约与策略，不绑死数据库 / SQL：捕获与排空的持久化由使用方按 API 标准自行实现，
以获得最大的形态自由（表结构 / 方言 / 容量策略自持）。接入清单：

1. 实现 `DomainEventOutboxStore`（领域捕获，同事务入箱）——**不提供该 Bean 时聚合一旦
   注册事件即 fail-fast 回滚业务写入**
2. （仅需要集成事件时）实现 `IntegrationEventOutboxStore`（集成捕获，应用层端口）
3. 实现 `OutboxRowAccess` 并注册为 Bean：领域排空必配一个（`kind() = DOMAIN`），
   集成排空按需（`kind() = INTEGRATION`）；全部方法加入调用方事务、认领并发安全
4. （注册了 INTEGRATION 行访问时）提供 `IntegrationEventSender` Bean——不提供则启动 fail-fast
5. 按参考 DDL 建 outbox 表：`sample-application/sample-service/sample-service-server/src/main/resources/sql/`；
   参考实现模板：sample `com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox`

完成后零配置自动装配：行访问 Bean + 事务管理器齐备即获得排空（`OutboxRelayScheduler`
按 `OutboxKind` 各建引擎）。

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

出站捕获同理：`public class OrderIntegrationEventCapture implements IntegrationEventCapture { ... }`（翻译领域事件 → 契约 IntegrationEvent → 经 `IntegrationEventOutboxStore` 同事务捕获入集成 Outbox，**不投递**——出站投递归框架集成排空器经 `IntegrationEventSender` 完成，见 `docs/application/cookbook/event-flow.md`）。

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

- **对偶原则（包结构镜像）**：框架支撑类的包层级与业务使用它的层级对齐——业务在 domain 层用（`AggregateRoot`、`Repository`、`DomainEvent`、`DomainService`）→ 放 `common-ddd/domain`；业务在 application 层用（`QueryHandler`、`BasicAssembler`、`ApplicationService`、`DomainEventListener`、`IntegrationEventCapture`、`ApplicationDTO`）→ 放 `common-ddd/application`；业务在 adapter 层用（`RestAdapter`、`IntegrationEventConsumer`）→ 放 `common-ddd/adapter`；业务在 infrastructure 层用（`MybatisPlusPersistence`、`BasicConverter`）→ 放 `common-ddd/infrastructure`。`PageResult`/`PageableQuery` 属契约层（分页信封是消费方可见的契约类型）→ 放 `common-contract/dto/query`。
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **全链路 Outbox 可靠性规范**：领域事件与集成事件强制经 Outbox 投递——持久化成功后先清后入箱（快照 + 清空暂存，下游异常不重复捕获），捕获与业务写入同事务（提交 ⇒ 落库，跨崩溃不丢），框架排空器（`OutboxRelay`）在自有事务内派发（内部反应 + 集成入箱 + 标记完成原子提交，at-least-once）；无 Outbox Bean 时 fail-fast 回滚业务写入，不存在直发降级
- **SPI-only Outbox**：框架只定捕获 / 行访问 SPI 与排空策略，不提供缺省实现、不携带 SQL——使用方按参考实现（sample）自行落地，获得最大形态自由（ADR-0009）
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件 / `DomainEventCodec` 均允许业务项目完全自定义覆盖

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

**决策**：选仓储自动捕获。opt-in 点是 `registerEvent()`，先落库后捕获 + 先清后入箱（与 Spring Data `@DomainEvents` 同模式），捕获入 Outbox 后由框架排空器派发（见 ADR-0008）。事件仅 AggregateRoot 可注册（一致性边界 = 事件唯一出口）。逃生门：Handler 注入 `DomainEventPublisher` 手动发 / `clearDomainEvents()` 抑制。

**后果**：跨服务通信仍走 Seata + HTTP 显式调用；可靠化已由框架全链路 Outbox 承担（见 ADR-0007 → ADR-0008），`DomainEventPublisher` 契约保持不变。

**确认**：`InProcessDomainEventPublisher` + `MybatisPlusPersistence` 自动捕获入箱。

### ADR-0004 对象转换纯手写，不用 MapStruct

- 状态：accepted

**背景**：Converter/Assembler/Presenter 用代码生成器还是手写。

**决策**：选手写显式映射。AI 辅助开发下手写模板成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在。聚合根重建走 reconstitute，完整性由往返测试守护。

**确认**：`BasicConverter` / `BasicAssembler` / `BasicPresenter` 无生成器依赖。

### ADR-0005 CQRS 契约：Query 纯标记（EventHandler 已移除）

- 状态：accepted（2026-08 修订）

**背景**：Handler 接口的契约设计。

**决策**：Query 为纯标记（无泛型，避免 contract 与 internal 类型耦合，返回类型由 Service 方法签名定义）。原 `EventHandler<E>` 接口已移除：入站集成事件（IntegrationEvent）统一由 adapter 层 Consumer 反序列化 → 构建 Command → 透传 CommandHandler，不设独立事件 Handler；域内反应（对领域事件）由业务侧 `DomainEventListener`（`@EventListener`）承担，框架以 `DomainEventListener` / `IntegrationEventCapture` 两个空标记定型 application 层「监听 / 出站捕获」两角色（2026-08 增补；标记更名见 ADR-0011）。

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

**确认**：`OutboxStore`（SPI）/ `DomainEventCodec` / `DomainEventCapture`（ADR-0011 更名）编排 + `sql/ddd_outbox.example.sql` 参考 DDL。（以上为历史记录，下述 ADR-0008 已反转。）

### ADR-0008 全链路 Outbox：框架交付捕获 + 排空完整管线（反转 ADR-0007）

- 状态：superseded（2026-08 accepted，audit F-04 最终收口；同月被 ADR-0009 收敛——缺省实现与规范 DDL 外移 sample，框架收归 SPI-only）

**背景**：ADR-0007 将领地收缩为「捕获契约 + 编解码工具」，实现与投递归业务。但样例始终无人实现捕获与排空，事件实际走在无持久化锚点的路径上，F-04 担保悬空；同时集成事件出站若由 Publisher 直发 MQ，「领域事件已派发 → 集成事件投 MQ」之间存在 dual-write 窗口。

**决策**：反转领地划分——**每一条领域事件、每一条集成事件强制经 Outbox 投递，框架交付完整管线**：

1. **捕获**：领域侧 `DomainEventOutboxStore` SPI + 缺省 `JdbcDomainEventOutboxStore`（`ddd_domain_event_outbox`）；集成侧应用层端口 `IntegrationEventOutboxStore` + 缺省 `JdbcIntegrationEventOutboxStore`（`ddd_integration_event_outbox`，行 id = 未来 MQ messageId，`source_event_id` 记录源领域事件血缘）。捕获一律与业务写入同事务。
2. **投递**：`OutboxRelay` 排空引擎（每行一个 REQUIRES_NEW 事务：`FOR UPDATE SKIP LOCKED` 认领 → 派发 → 软删标记完成），领域实例进程内派发、集成实例经 `IntegrationEventSender` SPI 投 MQ；失败指数退避重投，超限转死信，软删行过保留期物理清除。`OutboxRelayScheduler` 为框架管线（不实现 `ScheduledAdapter`）。
3. **fail-fast**：聚合注册事件但无捕获 Bean 时抛错回滚业务写入——要么不用事件，要么带上 Outbox，不存在直发降级。
4. **监听器契约反转**：派发在排空事务内执行，监听器用普通 `@EventListener` + 普通 `@Transactional`（加入排空事务），「内部反应 + 集成入箱 + 标记完成」原子提交；`REQUIRES_NEW` / `@Async` 禁用（撕碎原子性，重试产生双份副作用）；监听器不做非事务副作用，对外通知一律经集成 Outbox 捕获。

**反转理由**：① 捕获与排空是「机制」而非「策略」——两张标准表的形状由框架钉死后，排空器反而是最可复用、最不该让每个业务重写的一环；② ADR-0007 担心的「多张结构各异的消息表」并未出现，真实需求是统一的标准表 + 可替换接缝（`@ConditionalOnMissingBean` / SPI）；③ 生态方案（RocketMQ 事务消息 / CDC）仍是未来可选项，但框架先给出一条开箱即用的基线，业务拓扑定型后再整体替换，成本低于从零自建。

**后果**：`DomainEventPublisher` 契约不变（改由排空器调用）；仓储构造器收为 `(Mapper, ObjectProvider<DomainEventOutboxStore>)`；ADR-0007 的「业务侧待办」全部由框架承接；集成事件 MQ 投递在 common-mq 建设前以样例 `LoggingIntegrationEventSender` 日志占位。

**确认**：`DomainEventOutboxStore` / `JdbcDomainEventOutboxStore` / `IntegrationEventOutboxStore` / `JdbcIntegrationEventOutboxStore` / `OutboxRelay` / `OutboxRelayScheduler` / `IntegrationEventSender` / `DomainEventCodec` / `DomainEventCapture`（fail-fast，ADR-0011 更名）+ `sql/ddd_domain_event_outbox.sql`、`sql/ddd_integration_event_outbox.sql` 规范 DDL。（以上为历史记录，下述 ADR-0009 已将缺省实现与规范 DDL 外移 sample。）

### ADR-0009 Outbox 收归 SPI-only：框架零 SQL，缺省实现外移 sample（收敛 ADR-0008）

- 状态：accepted（2026-08；同月保留期清除职责被 ADR-0010 移除——框架不删除事件行，其余维持）

**背景**：ADR-0008 交付全链路管线时，框架随附了缺省 JDBC 捕获实现与两张规范 DDL。把手虽好，却把框架绑死在具体数据库形态上：表结构 / 方言 / 容量策略（TEXT vs JSONB）从此成为框架义务，使用方想换形态只能整体顶替实现，框架自身也被迫携带 SQL 与数据访问代码。

**决策**：领地收敛为「契约 + 策略」，**框架不提供任何缺省实现、不携带任何 SQL**：
1. 捕获侧保持 SPI：`DomainEventOutboxStore` / `IntegrationEventOutboxStore` 实现归使用方；
2. 新增排空侧 `OutboxRowAccess` 行访问 SPI（`claimOne` / `markDone` / `recordFailure` / `purge`，全部方法加入调用方事务、认领必须并发安全）——重试 / 死信 / 退避 / 保留期等**策略全在框架排空引擎**（`OutboxRelay` 纯 Java 骨架，零 SQL / 零 JDBC / 零 DataSource），实现只做纯持久化；
3. 缺省 JDBC 实现 + 规范 DDL 外移为 sample 中的**参考实现 / 参考 DDL**（`sample-application/sample-service/sample-service-server`，包 `...sampleservice.infrastructure.event.outbox`）——表形状、认领加锁方式、完成标记形态（软删或状态位）均为参考约定，由实现自持。

**理由**：框架层面不绑死数据库 / SQL——框架只定契约与策略，使用方按 API 标准自行实现以获得最大灵活性；框架不提供缺省实现（把手收回去）。

**后果**：Breaking change——升级使用方必须自行实现捕获 + 行访问 SPI 并建表（参考 sample 模板）；排空装配门控改为 `@ConditionalOnBean(OutboxRowAccess + PlatformTransactionManager)`，注册 INTEGRATION 行访问而无 `IntegrationEventSender` Bean 时启动 fail-fast。可靠性语义不变：同事务捕获、fail-fast、at-least-once + 幂等去重、监听器契约、事件流拓扑均维持 ADR-0008 定稿。

**确认**：`DomainEventOutboxStore` / `IntegrationEventOutboxStore` / `OutboxRowAccess` / `OutboxRow` / `OutboxKind` / `OutboxRelay`（纯策略）/ `OutboxRelayScheduler` / `IntegrationEventSender` / `DomainEventCodec` / `DomainEventCapture`（fail-fast，ADR-0011 更名）；参考实现与参考 DDL 见 sample-application。（以上为历史记录，下述 ADR-0010 已移除保留期清除职责。）

### ADR-0010 Outbox 不删除事件行：框架只捕获与排空，搬运归数据抽取层（收敛 ADR-0009）

- 状态：accepted（2026-08）

**背景**：ADR-0009 定稿的排空管线随附保留期清除——已完成行过 `retention-days` 由 `purge-cron` 物理删除。但已投递行存在审计留痕需求：物理删除抹掉投递历史，事后追溯无从谈起；且「删数据」是数据生命周期管理，不是消息投递机制的义务，属于应被收回的框架把手。

**决策**：框架把手再收回一层——**框架只做捕获与排空，绝不删除事件行**：

1. 移除清除职责：`OutboxRowAccess.purge()` 方法、`OutboxRelay.purge()` 与 `retentionDays` 参数、`OutboxRelayScheduler.purgeOutboxes()` 定时任务、`OutboxProperties.Relay` 的 `retentionDays` / `purgeCron` 配置（`ywf.ddd.outbox.relay.purge-cron` / `retention-days` 不复存在）；
2. 已投递行软删留痕（`is_delete=TRUE`），过往条目的搬运 / 归档由**数据抽取层**按自身节奏处理（按完成标记批量搬运历史条目；建表样例见 `docs/sql/event.example.sql`）；死信行（`status=1`）需人工介入，不属于框架自动化范围；
3. 排空正确性不受留存行影响：认领只取 `is_delete=FALSE AND status=0` 的行，部分索引保证认领扫描体积稳定。

**理由**：框架把手收回去——框架只做捕获与排空，绝不删除事件行。存在审计需求：已投递行必须留痕（软删 `is_delete=TRUE`），过往条目的搬运 / 归档由数据抽取层按自身节奏处理。

**后果**：`OutboxRowAccess` 收敛为四个方法（`kind` / `claimOne` / `markDone` / `recordFailure`）；排空调度只剩 `@Scheduled(fixedDelay)` 轮询（drain-only）；Breaking change——升级使用方须删去 `relay.retention-days` / `relay.purge-cron` 配置。可靠性语义不变：同事务捕获、认领 → 派发 → 标记完成、退避重试、死信、at-least-once + 幂等去重、监听器契约均维持 ADR-0008 / ADR-0009 定稿。

**确认**：`OutboxRowAccess`（无 purge）/ `OutboxRelay`（无 purge / retentionDays）/ `OutboxRelayScheduler`（drain-only）/ `OutboxProperties.Relay`（无 retentionDays / purgeCron）；抽取层建表样例 `docs/sql/event.example.sql`（数据抽取层持有）。

### ADR-0011 事件组件正名：Flusher / Publisher 更名 Capture（读名字即知职责）

> **补遗（对称对齐）**：本 ADR 初版将领域侧捕获组件记为 `DomainEventOutboxCapture`，后经对称对齐简化为 `DomainEventCapture`（与 `IntegrationEventCapture` 对仗——两侧同叫 `XxxCapture`，outbox 含义统一由各自 `*OutboxStore` 表达）。最终名以本文其余处为准。

- 状态：accepted（2026-08）

**背景**：多轮重构（进程内直发 → 过渡 Outbox → 全链路 Outbox（ADR-0008）→ SPI 化（ADR-0009）→ 移除清除（ADR-0010））后，前 outbox 时代的命名残留导致职责与名字错位——「Publisher 不投递、Flusher 不发布」，阅读主链路时产生认知错位。

**决策**：正名遵循「读名字即知职责」，改名映射：

| 旧名 | 新名 | 说明 |
|---|---|---|
| `DomainEventFlusher`（`infrastructure/event/domain/`） | `DomainEventCapture` | 方法 `publishAndClear` → `captureAndClear`、`publishAll` → `captureAll`。职责正名：不做发布，只做「快照 → 清空 → 同事务捕获入 Outbox」 |
| `IntegrationEventPublisher`（application 层空标记，`application/event/publisher/`） | `IntegrationEventCapture`（`application/event/capture/`） | 职责正名：翻译 + 同事务入箱，**不投递**——出站投递由框架集成排空器经 `IntegrationEventSender` 完成。模式术语「出站 Publisher」统一改为「出站捕获」 |
| sample `OrderEventPublisher`（`application/order/event/publisher/`） | `OrderIntegrationEventCapture`（`application/order/event/capture/`） | 业务侧跟随标记接口与包名更名 |

**类名保留名单**（名实相符，不更名）：`DomainEventPublisher` / `InProcessDomainEventPublisher`——排空时刻的进程内发布语义准确；`OutboxRelay` / `OutboxRelayScheduler`——「排空」即投递侧语义；`IntegrationEventSender`——投递接缝，语义准确。

随本次一并清理：删除 `DomainEventPublisher.publishAll(List)` 默认方法（死 API，旧直发时代遗留）及其 Javadoc 的「替换为 MQ 发布」扩展指南（全链路 Outbox 规范下会重新打开 dual-write 窗口）；`AggregateRoot` 事件机制时序描述修正为「registerEvent → 持久化成功 → 先清后入箱（同事务）→（异步）领域排空器 → 进程内派发」。

**理由**：出站链路的动词只属于排空器与 Sender——捕获侧一律「捕获 / 入箱」，投递侧才谈「投递 / 发布」。名字与职责对齐后，主链路阅读无需在名字与实际职责之间做额外翻译。

**后果**：纯改名 + 死 API 删除，管线拓扑与可靠性语义不变（ADR-0008 / ADR-0009 / ADR-0010 定稿维持）；Breaking change——升级使用方按上表替换类名 / 方法名 / 包名。

**确认**：`DomainEventCapture`（`captureAndClear` / `captureAll`）/ `IntegrationEventCapture`（`application/event/capture/`）/ `DomainEventPublisher`（无 `publishAll`）。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：领域事件异步 / 跨进程发布 | 进程内可靠投递由框架全链路 Outbox 承担（ADR-0009：契约 + 排空策略归框架，持久化实现归使用方，框架零 SQL）；跨服务经集成事件 + 集成 Outbox + `IntegrationEventSender` 接缝，MQ 实现待 common-mq 建设（当前样例日志占位），东西向同步调用仍走 Seata + HTTP |
| 边界：Specification 模式 | 采纳为纯接口（可选工具）：领域规则 and/or/not 组合表达，供复杂校验场景；查询过滤仍用 MyBatis-Plus `LambdaQueryWrapper`，简单校验仍用聚合根 if-throw |
