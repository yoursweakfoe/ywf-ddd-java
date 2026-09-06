# how-to —— 设计卡子索引（宽松架）

> **本架=设计卡（宽松件）**：每篇只回答"该不该用、怎么选"——何时需要、决策点、边界代价。一切**具体如何用**（条款、规范代码形状、选择表、生效登记）统一住法卷 → [`../../specs/current/`](../../specs/current/)（宽严双份，2026-09-06 裁定）。形状全仓唯一样本在法卷，本架零形状代码。

设计原则和规则约束请参阅 [explanation/](../explanation/)。

## 导航（任务 → 设计卡 →  governing 法卷）

| 设计卡 | 管什么决策 | 法卷 |
|------|------|------|
| [write-path.md](write-path.md) | 写用例该不该拆这一刀 | `../../specs/current/patterns/write-chain.md` |
| [read-path.md](read-path.md) | 读路径选型（投影 vs 聚合） | `../../specs/current/patterns/read-chain.md` |
| [cross-aggregate.md](cross-aggregate.md) | 多聚合协作怎么落 | `../../specs/current/patterns/cross-aggregate.md` |
| [policy-pattern.md](policy-pattern.md) | 规则要不要抽策略 | `../../specs/current/patterns/domain-policy.md` |
| [gateway.md](gateway.md) | 外部系统在哪层接 | `../../specs/current/patterns/external-gateway.md` |
| [new-aggregate.md](new-aggregate.md) | 该不该立新聚合 | `../../specs/current/patterns/aggregate-blueprint.md` |
| [error-handling.md](error-handling.md) | 错误语义怎么设计（+ i18n key 登记账本） | `../../specs/current/modules/exception.md` |
| [batch-operations.md](batch-operations.md) | 批量三档位怎么选 | `../../specs/current/patterns/batch-write.md` |
| [scheduled-task.md](scheduled-task.md) | 定时任务的幂等预算 | `../../specs/current/patterns/scheduler.md` |
| [distributed-transaction.md](distributed-transaction.md) | 本地 vs 分布式边界 | `../../specs/current/patterns/distributed-tx.md` |
| [application-layer-objects.md](application-layer-objects.md) | 中间对象准入门 | `../../specs/current/patterns/application-objects.md` |
| [optimistic-lock-retry.md](optimistic-lock-retry.md) | 冲突要不要重试 | `../../specs/current/patterns/optimistic-lock.md` |
| [testing.md](testing.md) | 测试投资怎么分型 | `../../specs/current/patterns/testing-conformance.md` |

## 与 explanation / specs 的三架分工

```
specs/current/  → 怎么用才对？（严格件：条款 + 规范形状，机器遵循的唯一权威）
how-to/         → 该不该用、怎么选？（设计卡：判据 + 代价 + 指针，人类向）
explanation/    → 为什么这么设计？（解读：原理与权衡，无操作性）
```

阅读建议：先设计卡定方向 → 进法卷照形状施工 → 有疑问读解释区与判例卷宗。
