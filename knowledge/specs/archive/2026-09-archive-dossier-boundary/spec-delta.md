# 变更增量（spec-delta）

## MODIFIED: attribution-law §1 诸区分野（L12 decisions 行之下列注，紧接 L17 伞注格式）

- **SHALL**：于 §1 表后增分家注一段：案卷（`specs/changes|archive/`）= 立法过程件（问题、delta、表决行、任务勾验），折叠即封存；判例卷宗（`decisions/`）= 决策论证件（context/取舍/consequences），跨时间被引。**同一事实双栖者：论证归卷宗、过程归案卷。** 取证：现状双写实例 `archive/2026-09-common-it-consolidation/proposal.md` 末表 ↔ `ADR-0035` Decision Outcome 近同文（病灶实证）。

## MODIFIED: attribution-law §2 事实归属表

- **SHALL**（改 L29「架构决策」行）：其余载体允许形式扩为——`ADR-NNN` 限定名引用；**案卷内涉已立 ADR 之决策，只准表决行（何人何时批准/否决/裁定，至多附一句因）+ 指针，禁复写论证正文**。
- **SHALL**（L30「需求/行为规格」行后增行）：**立法/裁决过程事实 | 随案卷 | `specs/changes|archive/` 案卷本体 | 判例卷宗与 docs 只准指针；折叠前瘦身（R2 闸）| ddd-review 人肉抽查（机器化暂缓，判据同实施内容基准律注）**。

## MODIFIED: attribution-law §4 强制同步表（增一行）

- **SHALL**：触发行「案卷折叠入 archive 前」→ 必须动作「逐件查 proposal/delta：多句决策理由若判例未立 → 先补 ADR 再折叠；案卷裁至表决行 + 指针」。取证：new-bill skill 第 6 步现为义务时点唯一入口（`.agents/skills/new-bill/SKILL.md`）。

## ADDED（工序树，非条文）: new-bill SKILL

- **SHALL**：第 3 步 proposal 条目补半句「涉既有 ADR 之裁决只登表决行 + 指针 → 归属法 §2」；第 6 步连带义务清单补「瘦身自查：裁掉案卷内 ADR 已载论证（指向归属法 §4 新行）」。skill 零法条纪律不变——句式为工序 + 指针。

## 地图连带（折叠时同步，非 delta 条文）

- `knowledge/specs/README.md` 目录表 `archive/` 身份句、`knowledge/decisions/README.md` 本区法律句各补分家一句；两行均宽松件措辞，不载编号。
