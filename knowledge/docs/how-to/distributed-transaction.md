# 分布式事务（Seata AT）· 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该用、怎么选"。形状与代码（依赖/配置/双端组件/Handler 模板）、选型表、落地模板全在法卷 → [../../specs/current/patterns/distributed-tx.md](../../specs/current/patterns/distributed-tx.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md)

## 什么时候需要分布式事务

一个业务动作横跨**不同数据源**、也就是跨服务，而且必须整体成败时用。案例教例：下单等于创建订单加扣减库存，扣库存失败时订单必须回滚。

同服务内的多聚合不算跨服务，见决策点 1。示例应用刻意保持单服务，跨服务那一版是虚构教例示意，见法卷 §2 注记。

## 边界选择即核心决策：本地优先

1. **能用本地事务就不用分布式事务**，这是铁序，见法卷 DT-1。同服务多聚合同库 → 本地事务即可，sample 的两聚合形态就是真实例对照，见法卷 §2.7。跨服务 → 全局事务。最终一致可以接受，比如通知、日志 → 异步消息加重试，禁止拉入 Seata，见法卷 DT-4。Seata AT 的代价是全局锁与两阶段开销，两阶段指 undo_log。完整边界选择表在册，见法卷 §2.3。
2. **XID 链路由谁负责**：框架不内置透传组件，双端组件由业务侧手写：出站拦截器，加上入站 bind/unbind filter。缺一环全局事务就断链，见法卷 DT-2/DT-7。业务数据源必须经自动代理，旁路即破链，见法卷 DT-3。
3. **全局事务方法怎么摆**：注解标在发起方 Handler 上。先远程扣减、后本地落库，两步在同一个事务方法体内，形状见法卷 DT-5。一期东西向是静态 baseUrl 直连，载荷复用 contract CQE/CO，不另造传输模型，见法卷 DT-6。

## 边界与代价

- 依赖经 common-cloud 聚合引入，带 optional 标记，业务服务可直接依赖或自备声明。示例应用当前未引入 common-cloud/Seata，见法卷 §2.1 注记。
- 分支补偿语义 = AT 的逆向 undo_log，不是 XA 那种持锁挂起。隔离性的代价见工作原理注记，法卷 §2.8。
- 设计原理纵深 → [../explanation/infrastructure.md](../explanation/infrastructure.md)；同服务跨聚合协作 → [cross-aggregate.md](cross-aggregate.md)。

## 落地状态

一律以法卷 §3 生效登记为准。边界规则 DT-1/4 ✅ 即时生效。XID 透传与自动代理 DT-2/3 ⛔ 示意模板未落地，两服务化拆分时必须按本卷落地并对账。
