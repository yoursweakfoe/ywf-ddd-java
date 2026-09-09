# knowledge/ —— 项目知识伞（组织宣言）

这把伞治仓库文档的一个老病：**同一句话住在多处，腐烂时没人知道该谁改**。目录为什么长这样，是五个问题的答案——读完五答，树形不言自明。

## 五问五答

| # | 问题 | 本仓答案 | 落在 |
|---|---|---|---|
| 1 | **谁听谁的？** 一句话腐烂了，轮谁改？ | 三分类：地图（文档跟代码）、法律（代码跟文档）、卷宗（谁也不跟） | `docs/` `specs/` `decisions/` 三区 |
| 2 | **身份何时会变？** 同一念头从讨论到定法到废止，改法完全不同 | 法律分三层：过程稿 → 现行本 → 存档；归档折叠那一刻是文档同步义务**唯一**发生的时点 | 各区 `specs/` 的 changes/ archive/（current/ 首案开册）；业务法同构镜像在 `sample-application/specs/` |
| 3 | **读者打开时想要什么？** | Diátaxis 四象限：教程 / 手册 / 字典 / 解读 | `docs/` 内四书架（§2） |
| 4 | **藏起来还是摆出来？** | 知识全摆明面；教 agent 怎么干活的"方法"藏 `.agents/`（工具原生扫描路径钉死，且工具可弃、知识不可弃） | 伞内 vs 伞外 |
| 5 | **离代码放多近？** | 不贴源码（一纸契约跨五个模块，贴哪都偏心），不分仓（暂无独立治理需求，同 PR 原子同步更值钱）——**根级一伞统一存放，知识不散落多处** | 根级 `knowledge/`；退路：将来需要独立评审时 `specs/` 整袋迁出（先例：PEP、KEP） |

品味说明：本仓同时实现两套经典方案，不冲突因为它们作用在不同层——**三分类（问一）管分区，Diátaxis（问三）只管地图区内部摆架**。法律区与卷宗区不入四象限，它们的分架轴是问二的效力身份。

## 1 · 三分类：一区一法律（问一的答案）

| 区 | 态 | 守则一句话 | 法律全文 |
|---|---|---|---|
| `docs/` | **地图**（描述） | 代码变了它没跟 → 文档是 bug，修文档 | `knowledge/specs/current/patterns/attribution-law.md`（归属法） |
| `specs/` | **法律**（框架契约：行为承诺 + 严格用法规范） | 代码违反它 → 修代码；要改法 → 走 changes/ 程序，禁止迁就代码偷改；docs 同题指引为宽松件，冲突法卷赢 | `specs/README.md` |
| `decisions/` | **判例卷宗**（冻结） | 正文永不回改；推翻 = 新立案 + supersede 旧案 | `decisions/README.md` |
| `scripts/` | **执法**（工具链） | 非知识，是守护前三区的机器检查；行为以代码本体为准，说明住字典架 | [docs/reference/doc-guards.md](docs/reference/doc-guards.md) |

为什么要拆开：地图区守则"烂了修文档"是溶剂——法律若寄居地图区，执法力就被泡掉；卷宗若寄居任何活区，会被"好心"重写。故三区互不相犯、每区法律在自己目录生效，**伞本身不立法**。

**业务不入伞**（2026-09-06 辖域裁定）：伞只装框架/脚手架层的通识。示例业务的行为法住镜像区 [`sample-application/specs/`](../sample-application/specs/README.md)——契约天然记生意，容器跟着内容走。

## 2 · Diátaxis：地图区的摆架法（问三的答案）

两条轴切四格：横轴问"来学习还是来工作"，纵轴问"要动手还是要认知"。

| | 学习 | 工作 |
|---|---|---|
| **动手** | `tutorials/` 教程：零基础练习场，线性步骤带你完成第一次 | `how-to/` 设计卡：该不该用、怎么选（浅层指引；一切用法形状在 specs 法卷） |
| **认知** | `explanation/` 解读：背景、权衡、设计原理——讲"为什么" | `reference/` 字典：描述性事实，结构严格，供查证（用法条款已入法卷） |

一篇文档按读者处境进一个架，不拆写四份——这是摆架的全部意义。**但象限只管描述**：规定句（"必须/禁止/应当这样写"）在四格里没有执照，一律上移一层住 `specs/` 法卷（2026-09-06 宽严双份裁定）。判据：**这句话能机械化执行吗？能→法卷；不能（语气/步骤/语境）→ docs。**

### 2.1 驱动关系（D2 图）

「地图=代码驱动」是 §1 表的压缩口号——四书架的真相源其实分叉。下图把每架画回各自的驱动者；**每条边都可在 [specs/current/patterns/attribution-law.md](specs/current/patterns/attribution-law.md) §1/§2 翻到法源**（锚见表），本图只作导览、不立法（伞不立法）。

![驱动关系图](diagrams/gen/knowledge/README/drive-relations.svg)

> 图源 = [`diagrams/knowledge/README/drive-relations.d2`](diagrams/knowledge/README/drive-relations.d2)（唯一可编辑面）。改源后重刷：`powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/render-diagrams.ps1`（TALA 引擎）；防陈旧对账：同法跑 `check-diagrams.ps1`。

| 边（图上短标签） | 法源锚 |
|---|---|
| 代码 → 教程架「跑不通即bug」 | 归属法 §1 docs 行「与代码不符=文档是 bug；写入时钟：代码之后」；可运行性含环境前置（[docs/README](docs/README.md) 四架表：从零跑通） |
| 代码 → 字典架「漂移即烂」 | §2「包路径/类名/方法签名 → 源代码」「异常→HTTP 映射 → javadoc+法卷+C5」两行 |
| 法卷 → 设计卡架「法卷赢」 | §2「用法规范/规范代码形状」行：docs 同题=设计卡、**冲突法卷赢**（宽严双份制） |
| 判例 → 解读架「沉淀为解释」 | §2「设计论证（为什么）｜低易变｜canonical=`docs/explanation/`（含 theory-map）」行——docs 内唯一**非代码驱动**的架：意图驱动、最冻结；亦是体系蒸馏决策论证的地图侧出口 |
| 判例 → 法卷「判例先行」 | §1 decisions 行「推翻=新立 ADR+supersede」；根 AGENTS 路由「先查旧判例再拍新板」 |
| 法卷 → 代码「违法修码」 | §1 specs 行「与代码不符=二者之一必修」；改法一侧走下行 |
| 代码 ↛ 法卷「违宪·偷改」（虚线✕） | §1 specs 行「禁止迁就代码偷改」；合法改法唯一通道 = `changes/` |

## 3 · 树

```
knowledge/
├── README.md       本页：组织宣言（导览五问三分类，自身不立法）
├── docs/           地图 | README 唯一文档索引
├── diagrams/       配图 | .d2 源按「文档仓库相对路径」镜像入册，gen/ 存 TALA 渲染 SVG 产物（render/check-diagrams.ps1 双件，源为唯一可编辑面）
│   ├── tutorials/  quickstart：clone 到第一次跑通（真实例操作手册）
│   ├── how-to/     设计卡（{agg} 中立教例，零形状代码——规范在法卷）
│   ├── reference/  api/ 框架模块 8 篇 · doc-guards.md 工具说明 · glossary 术语表
│   └── explanation/ 分层设计 5 篇 + theory-map 理论账本
├── specs/          法律·框架 | current/{modules|patterns}/ 法卷 · changes/ 审议稿 · archive/ 存档（业务法→ ../sample-application/specs/）
├── decisions/      卷宗 | ADR-NNNN 全局唯一编号，append-only
└── scripts/        执法 | check-docs.ps1 七校验 + 豁免白名单 + lychee 配置
```

## 4 · 机器执法

靠自觉的守则一定腐烂。`scripts/check-docs.ps1` 七校验——幽灵路径、计数漂移、符号鬼魂、教学词违规、映射表漏更、判例回改、技能闸，全部当场变红；交付前必跑（`ddd-review` 末步已内置），白名单只删不增。
