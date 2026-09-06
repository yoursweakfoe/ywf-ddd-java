# knowledge/docs/ —— 描述区索引（地图区 · 全仓唯一文档登记处）

> **本区=地图（描述）**：代码变了这里必须跟着变，与代码不符即文档 bug。守则全文见 `.agents/rules/05`。
> 契约在 `../specs/`、判例卷宗在 `../decisions/`——法律与卷宗**不登本索引**（各区自持索引，防双登记）。
> 人类总入口 = 根 `README.md`；AI 入口 = `AGENTS.md`。

## 四书架（按读者处境分架）

| 架 | 用途 | 入口 |
|---|---|---|
| `tutorials/` | 从零跑通（教程） | [quickstart.md](tutorials/quickstart.md) |
| `how-to/` | 带着任务来（手册；教学模板=业务无关教例，`{agg}`/虚构聚合，永不挂钩 sample 源码） | 子索引与 12+1 篇清单见 [how-to/README.md](how-to/README.md)（testing.md 为 D6 迁入新篇） |
| `reference/` | 查事实（字典） | [structure.md](reference/structure.md)（**生成物禁手改**，重跑脚本对齐）+ [api/](reference/api/) 框架模块文档 8 篇（common-contract/ddd/exception/cloud/pg/security/observability/test） |
| `explanation/` | 搞懂为什么（解读） | 层设计 5 篇（[contract](explanation/contract.md) / [adapter](explanation/adapter.md) / [application](explanation/application.md) / [domain](explanation/domain.md) / [infrastructure](explanation/infrastructure.md)）+ [theory-map.md](explanation/theory-map.md)（理论账本） |

## 其他

| 文件 | 用途 |
|---|---|
| [glossary.md](glossary.md) | 术语表：术语→canonical 指针 + 命名映射 + 订单域通用语言（业务词汇合法位） |

## 登记法

新增/迁移/重命名文档**仅登记本文件一处**；根 README 与 AGENTS.md 只放指针，不维护树。
