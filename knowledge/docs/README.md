# knowledge/docs/ —— 描述区索引（地图区 · 全仓唯一文档登记处）

> 本区是地图：描述代码。代码变了这里必须跟着变，与代码不符就是文档 bug。守则全文在 `knowledge/specs/current/patterns/meta/attribution-law.md`。
> 契约在 `../specs/`，封存案卷和其中的 §裁决记录也在那里。法律不进本索引：各区自持索引，一份文档只登记一处。
> 人类总入口是根 `README.md`，AI 入口是 `AGENTS.md`。

## 四书架（按读者处境分架）

| 架 | 用途 | 入口 |
|---|---|---|
| `tutorials/` | 教程：零基础线性步骤，从头跑到通 | [quickstart.md](tutorials/quickstart.md) |
| `how-to/` | 设计卡：带着任务来，回答该不该用、怎么选。只给浅层指引，零形状代码；具体用法一律由法卷承载 | 子索引在 [how-to/README.md](how-to/README.md)：13 篇，逐篇挂 governing 法卷 |
| `reference/` | 字典：查证描述性事实 | [glossary.md](reference/glossary.md)：术语唯一登记，术语指针、命名映射、订单域通用语言三部分 · [api/](reference/api/)：框架模块文档 8 篇，common-contract/ddd/exception/cloud/pg/security/observability/test · [doc-guards.md](reference/doc-guards.md)：`knowledge/scripts/` 防腐工具链说明书 · [doc-templates.md](reference/doc-templates.md)：文档体裁模板集（生态位地图与形状债清单；模板底稿住各区 `_template/`） · [doc-templates.md](reference/doc-templates.md)：文档体裁模板集（生态位地图，模板底稿住各区 `_template/`） · 包结构直接查源码树，组成法见 `specs/current/patterns/building-block/aggregate-blueprint.md` §5 |
| `explanation/` | 解读：讲为什么这样设计。这里是论证的权威库，全系统被引用最广的一架：how-to 卡片的原理行、字典的"→ 见"行、教程末节延伸、法卷的论证指针，四路都指向此架。架构决策的沉淀论证现行版住这里，⑨⁺ 折叠时刻复写进来，决策原始快照在案卷 §裁决记录；「一次设计 = 修改 + 决策」这个定名的论证同样在此 | 层设计 5 篇：[contract](explanation/contract.md) / [adapter](explanation/adapter.md) / [application](explanation/application.md) / [domain](explanation/domain.md) / [infrastructure](explanation/infrastructure.md)。专论 7 篇：[knowledge-system](explanation/knowledge-system.md) 知识系统三类件 / [cloud-integration](explanation/cloud-integration.md) 云集成 / [security](explanation/security.md) 安全层 / [observability](explanation/observability.md) 可观测 / [testing](explanation/testing.md) 测试 / [architecture-rules](explanation/architecture-rules.md) ArchUnit 规则集设计与载体分工 / [pattern-taxonomy](explanation/pattern-taxonomy.md) 摆架 rationale（模式法卷为什么是十摊）。另有 [theory-map.md](explanation/theory-map.md)：理论账本 |

## 登记法

新增、迁移、重命名文档，只登记本文件一处。根 README 与 AGENTS.md 只放指针，不维护文档树。
