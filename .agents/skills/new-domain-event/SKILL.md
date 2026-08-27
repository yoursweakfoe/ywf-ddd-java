---
name: new-domain-event
description: 为已有聚合新增领域事件，可选添加监听器和集成事件。当需要实现事件驱动的域内反应或跨服务通知时使用。
---

# 新增领域事件

## 前置阅读

1. `docs/application/cookbook/event-flow.md`（事件全链路代码）
2. `.agents/rules/03-coding-conventions.md`（领域事件节）

## 步骤

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
- 标注 `@Component`
- 方法标注 `@EventListener`——事件在业务事务**提交后**投递（直发路径 = afterCommit 派发；
  业务接 Outbox 后 = 排空器投递），无活动事务，不要用
  `@TransactionalEventListener(AFTER_COMMIT)`（无事务可挂靠，默认不执行）
- 监听器内有数据库写入时追加 `@Transactional(propagation = REQUIRES_NEW, rollbackFor = Exception.class)`
- 投递语义 at-least-once：监听器逻辑按 `eventId` 幂等（重复投递不产生重复副作用）
- 方法签名：`public void on{Agg}{Action}({Agg}{Action}Event event)`
- 薄编排：接事件 → 加载聚合 → 委托 DomainService（不含 if-else 业务判断）

### 4.（可选）创建 Publisher + 集成事件

仅当需要通知外部服务时：

- 集成事件：`contract/{agg}/dto/event/integration/{Agg}{Action}IntegrationEvent.java`
  - 实现 `IntegrationEvent` 标记接口
- Publisher：`application/{agg}/event/publisher/{Agg}EventPublisher.java`
  - 翻译领域事件 → 集成事件 → 投递 MQ
  - 被 DomainEventListener 或 CommandHandler 显式调用

### 5.（可选）外部消费方

- 消费方 adapter 层：`adapter/{agg}/consumer/{Agg}EventConsumer.java`
- 接收 MQ → 反序列化 → 透传 AppService

## 验证

- [ ] DomainEvent 所有字段 final（不可变）
- [ ] 事件注册在状态变迁之后
- [ ] DomainEventListener 注解选择正确（投递已在提交后发生）：
  - 一律 `@EventListener`（不要用 `@TransactionalEventListener(AFTER_COMMIT)`）
  - 带数据库写入 → 追加 `@Transactional(propagation = REQUIRES_NEW)`
  - 完全异步 → `@Async @EventListener`
- [ ] 监听器逻辑按 `eventId` 幂等（Outbox at-least-once 重投契约）
- [ ] 集成事件在 contract 模块（不在 server）
- [ ] Publisher 不被 AppService 直接调用（由 CommandHandler/DomainEventListener 调用）

## 文档同步

- 更新 `docs/application/cookbook/event-flow.md` 文件清单
- 如新增了事件类型对比，更新 module-design/domain.md 事件边界节
