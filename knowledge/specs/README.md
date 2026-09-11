# knowledge/specs/ —— 框架行为契约区（法律）

本区只立**框架/脚手架层**（common 模块自身）的行为法。业务包的行为法不住这里，住镜像区 [`../../sample-application/specs/`](../../sample-application/specs/README.md)——契约写的是生意，业务名只在那棵树合法；本区承载通识。

本区四条例外守则（全法见归属法卷 `knowledge/specs/current/patterns/meta/attribution-law.md`，业务镜像区援引同一套，不复述）：

1. **代码违反本法 → 改代码**。想改法，走 `changes/` 程序。禁止为了迁就现状偷改法条——那是把法律当地图。
2. 改法唯一通道：`changes/<YYYY-MM-slug>/` 立四件套（specify 定验收 → plan 定技术形状与修卷 delta → tasks 拆施工 → implement 记执行账）。两道批准门（Specify 门、Plan 门），每案两停、不论大小，过门才能施工。实现全绿后折叠归档进 `current/`——文档同步义务只在那一刻发生。
3. 每条 SHALL 括注取证源（实现文件:行 或 测试类名）。没有证据的意图不写进来，用 `<!-- 待 changes/ 补全 -->` 留缺口。
4. 行为断言、案卷 §裁决记录（当时的决策快照）、`docs/explanation/`（今天的论证）三者互指不复述。机器背书（ArchUnit R##、测试类名）写在句尾，由 check-docs 对账。

## 目录：一个名字一种身份

| 目录 | 身份 | 改法 |
|---|---|---|
| `current/modules/<module>.md` | **模块法卷**（8 卷）：单个 common 模块的用法规范，与模块同名 | 只能经 changes/ 折叠写入 |
| `current/patterns/<摊>/<pattern>.md` | **模式法卷**（19 卷，十个摊目录）：跨模块的横切规范，条款编号 + 生效登记；摊卡、空穴登记、搬家账 → 摆架法卷 `patterns/meta/pattern-taxonomy.md`；**卷籍以路径为唯一名册**，册内无卷的摊（diplomacy/、deploy-ops/）有牌面待法 | 同上 |
| `changes/<slug>/` | **审议中**：还没生效的修订案 | 定稿前随便改。`_template/` 四件套模板两区共用（立法流程是业务无关通识，住知识区） |
| `archive/` | **已归档**：折叠完成的案卷（date-slug 命名）。裁决快照住案卷自己的 specify §裁决记录；现行论证复写在 `docs/explanation/` | 只进不改（连错字都不改）。到期时由清册 bill 整册彻底删除——删除前必须满足自足判据（见归属法卷）。README 常驻，只记章程不记账目 |

## 宽严双份（2026-09-06 裁定）

同一份规范允许写两份，但权威只有一个：

- **法卷 = 严格件**：条款编号 + 取证源 + 规范代码形状。机器遵循以它为准。
- **docs = 宽松件**：面向人类的语感指引 + 节级指针。不写条款编号、不写精确参数表。

冲突时法卷赢，docs 改——docs 本来就是地图。归属判据一句话：**这句话能机械化执行吗？能 → 法卷；不能（依赖语气、步骤、语境）→ docs。** 本仓早有同构先例：javadoc（严格）↔ api 文档 §2 表（宽松），C5 每日对账。
