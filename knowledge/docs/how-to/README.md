# how-to —— 操作指南子索引（how-to guides · 工作 × 动手）

> **本架 = 操作指南（how-to guides）**：按 Diátaxis（Daniele Procida 四象限文档法）标准定义，回答"我怎么做 X"——何时需要、决策点在哪、怎么走、边界代价是什么。现 13 篇多在前半（选型判据与边界），步骤走查按需补进。
> 条款编号、取证源与规范代码形状的统一权威在法卷 → [`../../specs/current/`](../../specs/current/)；与本架冲突时以法卷为准。操作指南可带走查式示例，不得整表复写条款编号与参数表。
> 历史：本架曾名"设计卡"、被定义为法条的"宽松副本"，2026-09-15 项目主裁定废该认知、回归标准定义。

设计原理和规则约束见 [explanation/](../explanation/)。

## 导航（任务 → 操作指南 → governing 法卷）

| 操作指南 | 管什么任务 | 法卷 |
|------|------|------|
| [write-path.md](write-path.md) | 写用例要不要走这条链；规则、拦截、并发、契约暴露怎么选 | `../../specs/current/patterns/chain/write-chain.md` |
| [read-path.md](read-path.md) | 读查询走投影还是走聚合 | `../../specs/current/patterns/chain/read-chain.md` |
| [cross-aggregate.md](cross-aggregate.md) | 多聚合协作放哪一层 | `../../specs/current/patterns/collaboration/cross-aggregate.md` |
| [policy-pattern.md](policy-pattern.md) | 规则要不要抽成策略 | `../../specs/current/patterns/building-block/domain-policy.md` |
| [gateway.md](gateway.md) | 外部系统在哪层接进来 | `../../specs/current/patterns/boundary/external-gateway.md` |
| [new-aggregate.md](new-aggregate.md) | 该不该立新聚合 | `../../specs/current/patterns/building-block/aggregate-blueprint.md` |
| [error-handling.md](error-handling.md) | 错误语义怎么设计；i18n key 登记账本在本篇 | `../../specs/current/modules/exception.md` |
| [batch-operations.md](batch-operations.md) | 批量写的三档原子性怎么选 | `../../specs/current/patterns/chain/batch-write.md` |
| [scheduled-task.md](scheduled-task.md) | 定时任务要不要建、重复执行谁防 | `../../specs/current/patterns/chain/scheduler.md` |
| [distributed-transaction.md](distributed-transaction.md) | 本地事务和分布式事务怎么划界 | `../../specs/current/patterns/boundary/distributed-tx.md` |
| [application-layer-objects.md](application-layer-objects.md) | 应用层中间对象该不该建 | `../../specs/current/patterns/building-block/application-objects.md` |
| [optimistic-lock-retry.md](optimistic-lock-retry.md) | 冲突要不要重试 | `../../specs/current/patterns/collaboration/optimistic-lock.md` |
| [testing.md](testing.md) | 测试分四类，各测什么 | `../../specs/current/patterns/discipline/testing-conformance.md` |

## 与 explanation / specs 的三架分工

```
specs/current/  → 怎么用才对？（条款 + 规范形状，机器遵循的唯一权威）
how-to/         → 我怎么做 X？（操作指南：任务步骤、判据、代价与指针，人类向）
explanation/    → 为什么这么设计？（解读：原理与权衡，无操作性）
```

怎么用这三架：先读操作指南定方向与走法，再进法卷照形状施工。还有疑问就读 explanation 区；归档案卷的 §裁决记录记着当年为什么这么定。
