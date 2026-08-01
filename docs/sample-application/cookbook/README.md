# Cookbook — 端到端代码实战

本目录提供**完整可编译的代码走查**，回答"具体怎么写"。
设计原则和规则约束请参阅 [module-design/](../module-design/)。

## 导航

| 文档 | 内容 | 适合场景 |
|------|------|--------|
| [write-path.md](write-path.md) | 写路径全链路：Command → Facade → AppService → Handler → Domain → Repository → PO | 新增写操作用例 |
| [read-path.md](read-path.md) | 读路径全链路：Query → Facade → AppService → QueryHandler → Repository 投影 DTO | 新增查询用例 |
| [cross-aggregate.md](cross-aggregate.md) | 跨聚合协调：Domain Service + Bean 注册 + 复杂 Handler + Contract JAX-RS 注解 | 多聚合协作场景 |
| [event-flow.md](event-flow.md) | 事件全链路：DomainEvent → registerEvent → EventHandler → Publisher → IntegrationEvent | 新增领域事件 |
| [policy-pattern.md](policy-pattern.md) | 领域策略模式：Before/After 对比 + 三种组合形态（互斥/叠加/路由） | 抽离可插拔业务规则 |
| [gateway.md](gateway.md) | Gateway（Portal 实现）：Domain 接口 + Infra ACL 翻译 | 对接外部系统 |
| [new-aggregate.md](new-aggregate.md) | 新聚合 Checklist：从 contract 到 infrastructure 的完整文件清单 + 模板 | 从零创建聚合 |
| [error-handling.md](error-handling.md) | 异常全链路：显式 if-throw → BusinessException → HTTP 422 响应 + 前端对接 | 异常处理与错误码设计 |
| [batch-operations.md](batch-operations.md) | 批量操作：批量 Command + 事务边界 + 部分失败策略 | 批量写操作 |
| [scheduled-task.md](scheduled-task.md) | 定时任务：adapter 层 Scheduler 入口 + 分布式锁提示 | 定时/周期性任务 |
| [mq-consumer.md](mq-consumer.md) | MQ 消费者：adapter 层 Consumer 入口 + 幂等性 + 死信处理 | 消息驱动场景 |
| [distributed-transaction.md](distributed-transaction.md) | 分布式事务：Seata AT 模式 + @GlobalTransactional + 边界选择 | 跨服务数据一致性 |
| [optimistic-lock-retry.md](optimistic-lock-retry.md) | 乐观锁冲突与重试：冲突识别 + ExponentialBackoff + Handler 模板 | 并发冲突处理 |

## 与 module-design 的关系

```
module-design/  → 为什么这么设计？规则是什么？（概念级代码片段）
cookbook/       → 具体怎么写？完整文件长什么样？（可编译代码）
```

两者通过交叉链接互引。阅读建议：先读 module-design 理解设计意图，再对照 cookbook 落地实现。
