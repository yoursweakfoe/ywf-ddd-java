# 分布式事务（Seata AT）· 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码（依赖/配置/双端组件/Handler 模板）、选型表与落地模板见法卷 → [../../specs/current/patterns/distributed-tx.md](../../specs/current/patterns/distributed-tx.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md)

## 什么时候需要分布式事务

一个业务动作横跨**不同数据源**（跨服务）且必须整体成败——案例教例：「下单 = 创建订单 + 扣减库存（跨服务）」，扣库存失败时订单必须回滚。同服务内多聚合**不算**（见决策点 1）。示例应用刻意保持单服务，跨服务版是虚构教例示意（法卷 §2 注记）。

## 边界选择即核心决策：本地优先

1. **能用本地事务就不用分布式事务**（铁序，法卷 DT-1）：同服务多聚合同库 → 本地事务即可（sample 两聚合形态即真实例对照，法卷 §2.7）；跨服务 → 全局事务；最终一致可接受（通知、日志）→ 异步消息 + 重试，禁止拉入 Seata（法卷 DT-4）。Seata AT 有全局锁与两阶段（undo_log）开销——完整边界选择表在册（法卷 §2.3）。
2. **XID 链路由谁负责**：框架不内置透传组件，业务侧手写双端组件（出站拦截器 + 入站 bind/unbind filter），缺一环即全局事务断链（法卷 DT-2/DT-7）；业务数据源须经自动代理，旁路即破链（法卷 DT-3）。
3. **全局事务方法怎么摆**：注解标在发起方 Handler，先远程扣减后本地落库于同一事务方法体（法卷 DT-5 形状）；一期东西向为静态 baseUrl 直连、载荷复用 contract CQE/CO，不另造传输模型（法卷 DT-6）。

## 边界与代价

- 依赖经 common-cloud 聚合引入（optional 标记，业务服务可直接依赖或自备声明）——示例应用当前未引入 common-cloud/Seata（法卷 §2.1 注记）
- 分支补偿语义=AT 逆向 undo_log，非 XA 持锁挂起；隔离性代价见工作原理注记（法卷 §2.8）
- 设计原理纵深 → [../explanation/infrastructure.md](../explanation/infrastructure.md)；同服务跨聚合协作 → [cross-aggregate.md](cross-aggregate.md)

## 落地状态

一律以法卷 §3 生效登记为准（边界规则 DT-1/4 ✅ 即时生效 / XID 透传与自动代理 DT-2/3 ⛔ 示意模板未落地——两服务化拆分时必须按本卷落地并对账）。
