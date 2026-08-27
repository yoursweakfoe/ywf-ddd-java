# 事件全链路

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（事件边界章节）
> 事件事务语义详解 → `docs/common/common-ddd.md`（领域事件 Outbox 章节）

## 业务场景

本文以 **"订单取消 → 库存回补"**（域内反应）与 **"订单下单 → 出站通知"**（集成事件）为案例，
完整展示领域事件的定义、注册、Outbox 捕获、框架排空投递、监听、集成事件翻译入箱与 MQ 出站。

**为什么用事件而不是直接调用？**

| 如果直接调用 | 用领域事件 |
|---|---|
| `cancel()` 内硬编码 `replenishStock()` | `cancel()` 只注册事件，监听方自行决定做什么 |
| 新增副作用要改 `cancel()` | 新增 `@EventListener` 即可（OCP） |
| 回补失败导致取消回滚 | 事件经 Outbox 落库，框架排空器在独立事务内投递，失败退避重投 |

## 全链路 Outbox 可靠性规范

**每一条领域事件、每一条集成事件，都强制经 Transactional Outbox 投递**——框架
（common-ddd）提供完整管线：捕获（同事务入箱）+ 排空（认领 → 派发 → 标记完成）+
重试 / 死信 / 清除。**不存在直发路径，也不存在静默丢弃**：聚合注册了事件但容器中无
Outbox 捕获 Bean 时，冲刷直接抛错回滚业务写入——要么不用事件，要么带上 Outbox。

## 事件流全景

```
聚合根.cancel()
  → registerEvent(OrderCancelledEvent)            ① 暂存到聚合根内部 List

Repository.update(order)（业务事务内）
  → updateDomain(order)                            ② 先落库（UPDATE，乐观锁）
  → DomainEventFlusher 先清后捕                    ③ 快照事件、清空暂存
  → DomainEventOutboxStore.appendAll               ④ 同事务写 ddd_domain_event_outbox
                                                   （可靠性锚点：提交 ⇒ 落库；回滚 ⇒ 随行）
业务事务提交 ─────────────────────────────────────────────────────────────

框架排空器 OutboxRelay（领域实例，@Scheduled 轮询）
  → 每行一个 REQUIRES_NEW 事务：
    认领（ORDER BY occurred_on … FOR UPDATE SKIP LOCKED）
    → codec 重建事件身份（eventId 跨重投稳定）
    → DomainEventPublisher.publish                 ⑤ 排空事务内进程内派发
      → DomainEventListener.onOrderCancelled()     ⑥ @EventListener 加入排空事务
        → inventoryDomainService.replenishStock()  ⑦ 普通 @Transactional 加入同一事务
      →（出站）Publisher 翻译 → IntegrationEventOutboxStore.appendAll
                                                   ⑧ 同事务写 ddd_integration_event_outbox
    → UPDATE is_delete=TRUE（标记完成）→ 提交      ⑨ 内部反应 + 集成入箱 + 标记完成原子

框架排空器 OutboxRelay（集成实例）
  → 认领 → IntegrationEventSender.send(envelope)   ⑩ 投递 MQ（messageId = 行 id；
                                                     样例为日志占位，待 common-mq）
  → 标记完成 → 提交
  → 失败：attempts++、指数退避重投；超限转 DEAD（死信留表）
  → 已软删行过保留期后每日物理清除
```

> 可靠性语义（audit F-04 收口定稿，详见 `docs/common/common-ddd.md` Outbox 节）：
> 捕获与排空全部由框架交付（缺省 JDBC 实现 + `OutboxRelay`），投递语义
> **at-least-once**——崩溃恢复 / 并发认领下同一事件可能重复投递，消费端按身份幂等去重
> （领域事件 = `eventId`，集成事件 = `messageId`）。

## 事件流图

```mermaid
graph TB
    IE_IN[Integration Event<br/>外部入站 MQ] --> EC[IntegrationEventConsumer<br/>adapter ⛔ 未实现]
    CQ[Command / Query<br/>外部请求] --> FA[Facade]

    EC --> AS[AppService]
    FA --> AS

    AS --> RH[Handler<br/>@Transactional]

    RH --> AGG[聚合行为<br/>registerEvent]
    AGG --> REPO[Repository save/update<br/>先落库后冲刷]
    REPO --> DOX[(ddd_domain_event_outbox<br/>同事务捕获)]

    DOX --> RELAY1[OutboxRelay 领域实例<br/>认领 → 派发 → 标记完成]
    RELAY1 -->|排空事务内 @EventListener| EL[DomainEventListener<br/>域内反应]

    EL -->|仅域内处理| DONE[完成]
    EL -->|需要通知外部| PUB[IntegrationEventPublisher<br/>翻译 + 同事务捕获]
    PUB --> IOX[(ddd_integration_event_outbox<br/>同事务捕获)]

    IOX --> RELAY2[OutboxRelay 集成实例]
    RELAY2 -->|IntegrationEventSender| MQ[MQ 出站<br/>messageId = 行 id]
```

## 实现状态

> 各环节在当前示例应用 / 框架中的落地情况；「未实现」的环节待 common-mq 模块建设后按本文模板补全。

| 环节 | 状态 | 落地位置 |
|------|------|---------|
| 领域事件定义 + 聚合根注册 | ✅ 已实现 | `domain/{agg}/event/domain/` + `registerEvent()` |
| 仓储持久化后冲刷（先清后捕） | ✅ 已实现 | `MybatisPlusPersistence` → `DomainEventFlusher` |
| 领域事件 Outbox 捕获 | ✅ 已实现 | 框架缺省 `JdbcDomainEventOutboxStore` → `ddd_domain_event_outbox`（SPI `DomainEventOutboxStore`；PG DDL `sql/ddd_domain_event_outbox.sql`） |
| 领域事件排空投递 | ✅ 已实现 | 框架 `OutboxRelay`（领域实例）+ `OutboxRelayScheduler`（`infrastructure/event/outbox/scheduler/`），排空事务内经 `DomainEventPublisher` 进程内派发 |
| 域内反应（DomainEventListener） | ✅ 已实现 | `application/{agg}/event/listener/`（`@EventListener` 加入排空事务） |
| 集成事件契约 | ✅ 已实现 | `contract/{agg}/dto/event/integration/` |
| 集成事件翻译 + 捕获 | ✅ 已实现 | `application/{agg}/event/publisher/` → 应用层端口 `IntegrationEventOutboxStore`（缺省 `JdbcIntegrationEventOutboxStore` → `ddd_integration_event_outbox`） |
| 集成事件排空 → MQ | ⚠️ 日志占位 | 框架 `OutboxRelay`（集成实例）已装配；样例以 `LoggingIntegrationEventSender`（`infrastructure/mq/`，日志占位）实现 `IntegrationEventSender` SPI，待 common-mq 提供真实 MQ 实现顶替 |
| Consumer 入站 | ⛔ 未实现 | `adapter/event/consumer/`，设计见 [mq-consumer.md](mq-consumer.md) |

## 1. Domain — 定义 + 注册事件

```java
// domain/order/event/domain/OrderCancelledEvent.java
public class OrderCancelledEvent extends DomainEvent {

    private final UUID orderId;
    private final String reason;

    public OrderCancelledEvent(UUID orderId, String reason) {
        super();  // 自动生成 eventId + occurredOn
        this.orderId = orderId;
        this.reason = reason;
    }

    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
}

// domain/order/model/Order.java（节选）
public void cancel(String reason) {
    requireStatus("order:err.status.cancellable", OrderStatus.PENDING, OrderStatus.PAID);
    this.status = OrderStatus.CANCELLED;
    this.cancelReason = reason;
    registerEvent(new OrderCancelledEvent(id, reason));  // 暂存，持久化后捕获入箱
}
```

## 2. Infrastructure — 仓储触发冲刷 + Outbox 同事务捕获

```java
// MybatisPlusPersistence 内部逻辑（common-ddd 框架代码）
public void updateDomain(Domain domain) {
    validateIfAggregate(domain);                // ① 不变量校验
    PO po = getConverter().toPO(domain);
    int rows = baseMapper.updateById(po);       // ② UPDATE（乐观锁）
    if (rows == 0) throw ...;
    eventFlusher.publishAndClear(domain);       // ③ 先清后捕 → 同事务写 outbox 表
}
```

关键契约：

- **先落库，后冲刷**；**先清后捕**（快照 + 清空暂存，下游抛异常也不会重复捕获）
- **捕获与业务写入同事务**——「聚合状态已提交 ⇒ 事件必然已落库；业务回滚 ⇒ 事件随行回滚」
- **fail-fast**：聚合注册了事件但容器中无 `DomainEventOutboxStore` Bean 时，
  `DomainEventFlusher` 抛 `IllegalStateException` 回滚业务写入——要么不用事件，
  要么带上 Outbox，不存在静默丢弃，也不存在直发降级

缺省捕获实现 `JdbcDomainEventOutboxStore` 经 `JdbcTemplate` 复用事务绑定连接写入
`ddd_domain_event_outbox`（信封列：`id = eventId` / `event_type` / `payload` /
`occurred_on`；簿记列与标准结构列见 DDL）。`OutboxAutoConfiguration` 在存在
`DataSource` 时自动装配，`@ConditionalOnMissingBean` 允许业务整体替换。

## 3. 框架排空投递 + DomainEventListener 监听

业务事务提交后，框架排空器 `OutboxRelay`（领域实例）轮询认领在箱行，**每行一个
REQUIRES_NEW 事务**：

```
认领：SELECT id, event_type, payload, occurred_on, attempts FROM ddd_domain_event_outbox
      WHERE is_delete = FALSE AND status = 0 AND (next_retry_at IS NULL OR next_retry_at <= ?)
      ORDER BY occurred_on LIMIT 1 FOR UPDATE SKIP LOCKED
派发：codec 反序列化并以行身份重建 eventId/occurredOn → DomainEventPublisher.publish
      → Spring 按类型路由到 @EventListener 方法（监听器加入本事务）
标记：UPDATE … SET is_delete = TRUE（软删留痕，过保留期后物理清除）
提交：内部反应 + 集成入箱 + 标记完成三者原子
```

```java
// application/order/event/listener/OrderDomainEventListener.java
@Component
public class OrderDomainEventListener implements DomainEventListener {

    /**
     * 订单取消 → 库存回补（补偿型副作用）。
     * 投递发生在排空器自有事务内：普通 @EventListener 即可；
     * 数据库写入用普通 @Transactional（REQUIRED，加入排空事务）——
     * 回补写入与「标记完成」原子提交；回补失败 → 排空事务回滚 →
     * 行保持待投 → 退避重投，不再静默吞掉。
     * 禁用 REQUIRES_NEW / @Async：二者撕碎原子性，重试时产生双份副作用。
     */
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onOrderCancelled(OrderCancelledEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        inventoryDomainService.replenishStock(order.getItems());
    }

    /** 简单日志监听（无库写，无需 @Transactional） */
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order placed: orderId={}", event.getOrderId());
    }

    /** 出站通知：委托 Publisher 翻译 + 集成 Outbox 捕获（仍在排空事务内） */
    @EventListener
    public void onOrderPlacedOutbound(OrderPlacedEvent event) {
        orderEventPublisher.publishOrderPlaced(event);
    }
}
```

**监听器契约（全链路 Outbox 规范）**：

- 派发发生在排空器事务内（**有活动事务**）——一律用普通 `@EventListener`
- 带数据库写入的副作用用普通 `@Transactional`（REQUIRED，**加入**排空事务）：
  「内部反应 + 集成入箱 + 标记完成」原子提交
- **禁用 `REQUIRES_NEW` 与 `@Async`**——二者都会撕碎上述原子性，重试时产生双份副作用
- 监听器不做任何非事务副作用（HTTP 调用 / 直发 MQ）——对外通知一律经集成 Outbox 捕获
- 监听器抛异常向上传播 → 排空事务回滚 → 行保持待投 → 退避重投；
  投递语义 at-least-once，消费端以 `eventId` 幂等去重

## 3.5 框架排空器（OutboxRelay）与运行参数

排空引擎 `OutboxRelay`（`infrastructure/event/outbox/scheduler/`）由
`OutboxAutoConfiguration` 装配为两个实例，`OutboxRelayScheduler` 统一驱动：

| 实例 | 排空表 | 派发动作 | 装配条件 |
|------|--------|---------|---------|
| 领域（`domainEventOutboxRelay`） | `ddd_domain_event_outbox` | codec 重建身份 → `DomainEventPublisher` 进程内派发 | DataSource + 事务管理器 |
| 集成（`integrationEventOutboxRelay`） | `ddd_integration_event_outbox` | 构造 `OutboxEnvelope` → `IntegrationEventSender` 投 MQ | 上述 + 存在 `IntegrationEventSender` Bean |

- **每行一个事务**（REQUIRES_NEW）：认领 → 派发 → 标记完成 → 提交；行与行互不牵连
- **失败簿记**：派发抛异常 → `attempts++`、记 `last_error`、按指数退避
  （`min(2^attempts 秒, max-backoff)`）设置 `next_retry_at`；达到 `max-attempts`
  转死信（`status=1`，留表待人工处置）
- **顺序**：尽力 FIFO（按 `occurred_on` 认领）；退避重试可能乱序，消费端不应依赖严格顺序
- **身份 / 幂等**：领域 `eventId` 经 codec 身份重建跨重投稳定；集成 `messageId` = 行 id
  （捕获时铸造的新 UUID）。消费端按它们去重
- **清除**：投递完成 = `is_delete=TRUE` 软删留痕，过 `retention-days` 后由每日
  `purge-cron` 物理清除
- **调度入口**：`OutboxRelayScheduler` 是框架管线（`@Scheduled(fixedDelay)` 轮询 +
  每日清除），**不实现** `ScheduledAdapter` 标记——排空器是基础设施自驱，不是业务定时入口

配置（`ywf.ddd.outbox.*`）：

| 键 | 默认 | 说明 |
|----|------|------|
| `enabled` | `true` | 总开关。`false` 时自动配置整体退位——聚合一旦注册事件即 fail-fast |
| `relay.fixed-delay` | `1000` | 排空轮询间隔（毫秒） |
| `relay.batch-size` | `50` | 单轮排空行数上限 |
| `relay.max-attempts` | `10` | 单行最大重试次数，达到后转死信 |
| `relay.max-backoff` | `5m` | 指数退避封顶 |
| `relay.retention-days` | `7` | 已软删行保留天数 |
| `relay.purge-cron` | `0 0 3 * * *` | 每日物理清除 cron |

## 4. Contract + Publisher — 集成事件翻译 + 捕获

```java
// contract/order/dto/event/integration/OrderPlacedIntegrationEvent.java
@Data @NoArgsConstructor @AllArgsConstructor
public class OrderPlacedIntegrationEvent implements IntegrationEvent, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String orderId;
    private String customerId;
}

// application/order/event/publisher/OrderEventPublisher.java
@Component
public class OrderEventPublisher implements IntegrationEventPublisher {

    private final IntegrationEventOutboxStore integrationEventOutboxStore;

    public OrderEventPublisher(IntegrationEventOutboxStore integrationEventOutboxStore) {
        this.integrationEventOutboxStore = integrationEventOutboxStore;
    }

    /** 翻译领域事件 → 集成事件，在调用方事务内捕获入集成 Outbox（不直发 MQ） */
    public void publishOrderPlaced(OrderPlacedEvent domainEvent) {
        OrderPlacedIntegrationEvent ie = new OrderPlacedIntegrationEvent(
                domainEvent.getOrderId().toString(), domainEvent.getCustomerId());
        integrationEventOutboxStore.appendAll(domainEvent, List.of(ie));
    }
}
```

**Publisher 契约（职责重定义）**：

- 只做**翻译 + 同事务捕获**，不直接投 MQ——实际投递由框架集成排空器经
  `IntegrationEventSender` 完成。集成 outbox → MQ 的排空是框架职责，不是 Publisher 职责
- 由域内反应监听器（在排空事务内）或 Handler 显式调用，不被 AppService 直接调用
- 一个领域事件可 fan-out 为 1..N 个集成事件；集成行 id 是捕获时铸造的新 UUID，
  即未来 MQ 消息的 `messageId`；`source_event_id` 记录源领域事件的 `eventId`
  （血缘；入站集成事件再发出时为 NULL）
- 捕获与「领域行标记完成」同事务原子——关闭「领域事件已派发 → 集成事件投 MQ」之间的
  dual-write 窗口

集成侧捕获端口 `IntegrationEventOutboxStore` 定义在**应用层**（domain 不得依赖
contract、infrastructure 不得回调应用组件，故端口在应用层、缺省实现
`JdbcIntegrationEventOutboxStore` 在基础设施层，与读侧 `QueryRepository` 端口同构）。

## 5. 三个角色 + 框架排空

| 角色 | 位置 | 职责 |
|------|------|------|
| `DomainEventListener` | `application/{agg}/event/listener/` | 消费领域事件（域内反应），可触发出站翻译 |
| `IntegrationEventPublisher` | `application/{agg}/event/publisher/` | 翻译领域事件 → 集成事件并捕获入集成 Outbox（**不是** MQ 排空器） |
| `IntegrationEventConsumer` | `adapter/event/consumer/` | 入站集成事件（MQ → Command → Handler），见 [mq-consumer.md](mq-consumer.md) |
| `OutboxRelay` + `IntegrationEventSender` | 框架 `infrastructure/event/outbox/` | 集成 Outbox → MQ 的排空与投递（框架职责） |

## DomainEvent vs IntegrationEvent

| 维度 | DomainEvent | IntegrationEvent |
|------|-------------|------------------|
| 位置 | `domain/{agg}/event/domain/` | `contract/{agg}/dto/event/integration/` |
| 基类 | `extends DomainEvent`（common-ddd） | `implements IntegrationEvent`（common-contract） |
| 受众 | 服务内部（@EventListener） | 外部服务（MQ 订阅） |
| 内容 | 丰富（领域细节） | 精简（仅外部需要的字段） |
| 可变性 | 不可变（final 字段） | 可变（需序列化框架反序列化） |
| 投递机制 | Outbox 捕获 → 框架排空器在排空事务内进程内派发（Spring Event） | Outbox 捕获 → 框架排空器经 `IntegrationEventSender` 投 MQ（跨服务） |
| 身份 / 幂等键 | `eventId`（跨重投稳定） | `messageId` = outbox 行 id |

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| common-ddd | `infrastructure/event/outbox/DomainEventOutboxStore.java` | 领域捕获 SPI（同事务义务） |
| common-ddd | `infrastructure/event/outbox/JdbcDomainEventOutboxStore.java` | 领域缺省捕获（`ddd_domain_event_outbox`） |
| common-ddd | `application/event/outbox/IntegrationEventOutboxStore.java` | 集成捕获端口（应用层） |
| common-ddd | `infrastructure/event/outbox/JdbcIntegrationEventOutboxStore.java` | 集成缺省捕获（`ddd_integration_event_outbox`） |
| common-ddd | `infrastructure/event/outbox/DomainEventCodec.java` | 载荷编解码 + 身份重建 |
| common-ddd | `infrastructure/event/outbox/IntegrationEventSender.java` | MQ 投递接缝 SPI |
| common-ddd | `infrastructure/event/outbox/scheduler/OutboxRelay.java` | 排空引擎（认领 / 派发 / 标记 / 重试 / 死信 / 清除） |
| common-ddd | `infrastructure/event/outbox/scheduler/OutboxRelayScheduler.java` | 排空调度入口（框架管线） |
| common-ddd | `resources/sql/ddd_domain_event_outbox.sql`、`ddd_integration_event_outbox.sql` | 两张 outbox 表的 PG 标准 DDL |
| domain | `event/domain/OrderCancelledEvent.java` | 领域事件定义 |
| domain | `model/Order.java` | registerEvent() 注册 |
| infrastructure | `repository/OrderRepositoryImpl.java` | 持久化后触发冲刷捕获 |
| application | `event/listener/OrderDomainEventListener.java` | @EventListener 监听（域内反应，加入排空事务） |
| application | `event/publisher/OrderEventPublisher.java` | 集成事件翻译 + 捕获 |
| contract | `dto/event/integration/OrderPlacedIntegrationEvent.java` | 跨服务契约 |
| infrastructure（样例） | `mq/LoggingIntegrationEventSender.java` | `IntegrationEventSender` 日志占位实现（待 common-mq 顶替） |
| adapter | `event/consumer/` ⛔ | 入站 Consumer（未实现，待 common-mq，见 [mq-consumer.md](mq-consumer.md)） |
