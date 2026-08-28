---
name: new-domain-event
description: 为已有聚合新增领域事件，可选添加监听器和集成事件。当需要实现事件驱动的域内反应或跨服务通知时使用。
---

# 新增领域事件

## 前置阅读

1. `docs/application/cookbook/event-flow.md`（事件全链路代码）
2. `.agents/rules/03-coding-conventions.md`（领域事件节）

## 步骤

### 0. 前置检查：Outbox 捕获与排空就绪

- 事件强制经 Outbox 投递：服务必须已注册 `DomainEventOutboxStore` Bean（**框架不提供
  缺省实现**）——缺失时聚合一旦注册事件，捕获 fail-fast 抛错回滚业务写入
- 排空侧需已注册 `OutboxRowAccess` Bean（`kind() = DOMAIN`；集成排空按需再加
  `kind() = INTEGRATION` + `IntegrationEventSender` Bean——缺失则启动 fail-fast）
- 服务尚未接入时，按 sample 参考实现落地：`sample-application/sample-service/
  sample-service-server` 包 `com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.event.outbox`，
  参考 DDL 在其 `src/main/resources/sql/`（outbox 表结构为参考约定，非框架强制）

### 1. 定义领域事件

- 位置：`domain/{agg}/event/domain/{Agg}{Action}Event.java`
- 继承 `DomainEvent`
- 所有字段 `final`（不可变）
- 构造器接收必要业务数据（如聚合根 ID）

### 2. 聚合根注册事件

- 在聚合根行为方法末尾调用 `registerEvent(new {Agg}{Action}Event(...))`
- 事件注册在状态变迁**之后**

### 3. 创建 DomainEventListener（域内反应）

- 位置：`application/{agg}/event/listener/{Agg}DomainEventListener.java`
- 标注 `@Component`，实现 `DomainEventListener` 标记接口
- 方法标注 `@EventListener`——投递发生在框架排空器（`OutboxRelay` 领域实例）的
  **自有事务内**（排空器认领 outbox 行后于该事务中派发）
- 监听器内有数据库写入时追加普通 `@Transactional(rollbackFor = Exception.class)`
  （REQUIRED，**加入**排空事务）——「内部反应 + 集成入箱 + 标记完成」原子提交；
  副作用失败 → 排空事务回滚 → 行保持待投 → 退避重投
- **禁用 `REQUIRES_NEW` 与 `@Async`**——二者撕碎上述原子性，重试时产生双份副作用
- 监听器不做任何非事务副作用（HTTP 调用 / 直发 MQ）——对外通知一律经集成 Outbox 捕获
  （委托 Capture，见步骤 4）
- 投递语义 at-least-once：监听器逻辑按 `eventId` 幂等（重复投递不产生重复副作用）
- 方法签名：`public void on{Agg}{Action}({Agg}{Action}Event event)`
- 薄编排：接事件 → 加载聚合 → 委托 DomainService（不含 if-else 业务判断）

### 4.（可选）创建出站捕获 Capture + 集成事件

仅当需要通知外部服务时：

- 集成事件：`contract/{agg}/dto/event/integration/{Agg}{Action}IntegrationEvent.java`
  - 实现 `IntegrationEvent` 标记接口
- 出站捕获 Capture：`application/{agg}/event/capture/{Agg}IntegrationEventCapture.java`
  - 实现 `IntegrationEventCapture` 标记接口，注入 `IntegrationEventOutboxStore`
  - 翻译领域事件 → 1..N 个集成事件 → `appendAll(source, events)` **同事务捕获入
    集成 Outbox**（参考表 `ddd_integration_event_outbox`，实现归使用方）；**不直发 MQ**——
    实际投递由框架集成排空器经 `IntegrationEventSender` 完成（messageId = outbox 行 id）
  - 被 DomainEventListener（排空事务内）或 CommandHandler 显式调用，不被 AppService 直接调用

### 5.（可选）外部消费方

- 消费方 adapter 层：`adapter/{agg}/consumer/{Agg}EventConsumer.java`
- 接收 MQ → 反序列化 → 透传 AppService

## 验证

- [ ] 服务已注册 `DomainEventOutboxStore` + `OutboxRowAccess`（DOMAIN）Bean（缺失则捕获 fail-fast）
- [ ] DomainEvent 所有字段 final（不可变）
- [ ] 事件注册在状态变迁之后
- [ ] DomainEventListener 注解选择正确（投递发生在排空器事务内）：
  - 一律 `@EventListener`
  - 带数据库写入 → 追加普通 `@Transactional(rollbackFor = Exception.class)`（加入排空事务）
  - **禁用** `REQUIRES_NEW` / `@Async`（撕碎「副作用 + 集成入箱 + 标记完成」原子性）
- [ ] 监听器无非事务副作用（HTTP / 直发 MQ），对外通知经 Capture 捕获入集成 Outbox
- [ ] 监听器逻辑按 `eventId` 幂等（Outbox at-least-once 重投契约）
- [ ] 集成事件在 contract 模块（不在 server）
- [ ] Capture 只做翻译 + 同事务捕获（`IntegrationEventOutboxStore.appendAll`），不投递
  ——出站投递由框架集成排空器经 `IntegrationEventSender` 完成
- [ ] Capture 不被 AppService 直接调用（由 CommandHandler/DomainEventListener 调用）

## 文档同步

- 更新 `docs/application/cookbook/event-flow.md` 文件清单
- 如新增了事件类型对比，更新 module-design/domain.md 事件边界节
