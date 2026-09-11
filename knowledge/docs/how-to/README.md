# how-to —— 设计卡子索引（宽松架）

> **本架 = 设计卡（宽松件）**：每篇只回答"该不该用、怎么选"，也就是三件事：何时需要、决策点在哪、边界代价是什么。
> 具体怎么用不在本架。条款、规范代码形状、选择表、生效登记统一住法卷 → [`../../specs/current/`](../../specs/current/)。这就是宽严双份制：形状在全仓只有法卷那一份样本，本架零形状代码，两者冲突时以法卷为准。

设计原则和规则约束见 [explanation/](../explanation/)。

## 导航（任务 → 设计卡 →  governing 法卷）

| 设计卡 | 管什么决策 | 法卷 |
|------|------|------|
| [write-path.md](write-path.md) | 写用例要不要走这条链；规则、拦截、并发、契约暴露怎么选 | `../../specs/current/patterns/write-chain.md` |
| [read-path.md](read-path.md) | 读查询走投影还是走聚合 | `../../specs/current/patterns/read-chain.md` |
| [cross-aggregate.md](cross-aggregate.md) | 多聚合协作放哪一层 | `../../specs/current/patterns/cross-aggregate.md` |
| [policy-pattern.md](policy-pattern.md) | 规则要不要抽成策略 | `../../specs/current/patterns/domain-policy.md` |
| [gateway.md](gateway.md) | 外部系统在哪层接进来 | `../../specs/current/patterns/external-gateway.md` |
| [new-aggregate.md](new-aggregate.md) | 该不该立新聚合 | `../../specs/current/patterns/aggregate-blueprint.md` |
| [error-handling.md](error-handling.md) | 错误语义怎么设计；i18n key 登记账本在本篇 | `../../specs/current/modules/exception.md` |
| [batch-operations.md](batch-operations.md) | 批量写的三档原子性怎么选 | `../../specs/current/patterns/batch-write.md` |
| [scheduled-task.md](scheduled-task.md) | 定时任务要不要建、重复执行谁防 | `../../specs/current/patterns/scheduler.md` |
| [distributed-transaction.md](distributed-transaction.md) | 本地事务和分布式事务怎么划界 | `../../specs/current/patterns/distributed-tx.md` |
| [application-layer-objects.md](application-layer-objects.md) | 应用层中间对象该不该建 | `../../specs/current/patterns/application-objects.md` |
| [optimistic-lock-retry.md](optimistic-lock-retry.md) | 冲突要不要重试 | `../../specs/current/patterns/optimistic-lock.md` |
| [testing.md](testing.md) | 测试分四类，各测什么 | `../../specs/current/patterns/testing-conformance.md` |

## 与 explanation / specs 的三架分工

```
specs/current/  → 怎么用才对？（严格件：条款 + 规范形状，机器遵循的唯一权威）
how-to/         → 该不该用、怎么选？（设计卡：判据 + 代价 + 指针，人类向）
explanation/    → 为什么这么设计？（解读：原理与权衡，无操作性）
```

怎么用这三架：先读设计卡定方向，再进法卷照形状施工。还有疑问就读 explanation 区；归档案卷的 §裁决记录记着当年为什么这么定。
