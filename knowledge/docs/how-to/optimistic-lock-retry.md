# 乐观锁冲突与重试 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码（传播路径/重试模板/策略表/409 契约）见法卷 → [../../specs/current/patterns/optimistic-lock.md](../../specs/current/patterns/optimistic-lock.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md) ｜ 分类契约字典 → [../reference/api/common-ddd.md](../reference/api/common-ddd.md) §2「持久化支撑（MybatisPersistence）」

## 什么时候需要关心冲突

两个请求并发改同一聚合实例（案例教例："两人同时对同一支付单扣款"，只有一个成功）。框架默认行为已给出答案：UPDATE 版本条件 0 行且实体仍在 → 抛冲突异常 → 自动映射 HTTP 409 Conflict，失败方收到明确错误而非静默丢失——**用户交互场景到此为止，无需任何代码**，前端提示"操作冲突，请刷新重试"即可。要额外动作的只有系统间调用（MQ Consumer / 定时任务）。

## 三个设计决策点

1. **重试还是甩锅**：REST 前端不重试直接 409；MQ 消费靠状态机幂等天然消化（重复消费=已 PAID→忽略）；定时任务/系统间自动重试；高并发秒杀=乐观锁+重试+网关限流组合——策略选择表在册（法卷 §2.2）。
2. **重试包在哪一层**：应用层 Handler 包装器（法卷 §2.4 模板）——指数退避 + 有界次数 + 耗尽上抛（法卷 OL-5）；重试是编排关注点，聚合根内禁止重试（OL-6）；每次重试重新 load 拿最新 version（OL-3）。真实例：示例应用 PlaceOrder 链路 RetryablePlaceOrderHandler 已落地（法卷 §3）。
3. **哪些异常绝不进重试器**：只 catch 冲突类型——实体消失的普通 IllegalStateException 与 INSERT/DELETE 0 行的 SilentWriteLossException（500+ERROR）一律直接上抛：重试无意义，必须让告警吵醒人（法卷 OL-7 三分通道围栏）。

## 边界与代价

- 识别只走编译期类型通道，新代码禁止依赖消息文本做语义判断（法卷 OL-2；affected 0 rows 字样仅框架侧兼容期过渡）
- 版本号由框架 SQL 维护，业务层禁手工读写（法卷 OL-4）；版本条件由手写 XML 的 UPDATE 语句文本自身携带，无运行时拦截器，框架之外无需感知
- 批量内单条冲突整批回滚、重试需整批重载——批量慎用重试（法卷 §2.5 注意表）；虚拟线程下 sleep 不占载体线程，等待无顾虑（AGENTS 九条 8 语境）

## 落地状态

一律以法卷 §3 生效登记为准（框架冲突/409 通道 OL-1~4 ✅ / 重试形状 OL-5~7 ✅ 模板在册——虚构 payment 教例模板 + 真实例 PlaceOrder 链路件已落地含 4 单测）。
