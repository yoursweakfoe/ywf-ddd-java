---
name: new-bill
description: 为知识体系起草、推进并归档一部法案（案卷四件套 specify → plan → tasks → implement，经两门批准施工、折叠归档）。当需要修改 knowledge/specs/current 法卷（框架法卷或 sample-application/specs 业务镜像法卷，含归属法卷自身）、新增/变更/废止规范条款、修订框架行为或示例业务行为契约、或把已完成的案卷折叠归档时使用。改法唯一合法通道是立 bill；业务契约不入伞，立到镜像区。禁止为迁就代码偷改法卷——那必须走本流程裁决。
---

# 法案起草与归档（立法 SOP）

> 定位：改法的唯一通道。制度本身（诸区裁决、归属表、强制同步规则、审议中/定稿身份）在归属法卷与两区 README，本 skill 只载工序与关卡，条文一律指针。

## 前置阅读（动笔前）

- 元法：`knowledge/specs/current/patterns/attribution-law.md` —— §1 诸区分野（冲突裁决表）、§4 强制同步规则（「要不要立案」的触发源）
- 法区宪章：`knowledge/specs/README.md` —— current / changes / archive 一名一身份，宽严双份
- 工位纪律：`knowledge/specs/changes/README.md` —— 案卷目录制、三写禁绝与「折叠即修法」
- 四件套模板：`knowledge/specs/changes/_template/` —— specify.md / plan.md / tasks.md / implement.md（形状权威在模板本身，本 skill 不复述）

## 第 1 步：判要不要立案

对照归属法卷 §4 强制同步表逐行判触发；对不上表的分支即停，不立空案：

| 情形 | 走法 |
|---|---|
| 新增/变更/废止**框架行为或规范条款** | 立案 → `knowledge/specs/changes/{slug}/` |
| 新增/变更**示例业务聚合行为** | 同流程，落区换镜像区 → `sample-application/specs/changes/{slug}/`（首案开册，目录未立属正常） |
| 代码与现行法冲突、法本身不改 | 不立案——修代码就法（裁决 → 归属法卷 §1） |
| docs 地图腐烂 | 不立案——直接修文档（地图区被动跟随，同上） |
| 记一个新的设计决策 | 不是法案——走 `knowledge/decisions/` 新立 ADR（卷宗法另成程序） |
| 想在 skill 里落一句裸「必须/禁止」 | 先立案（本 SOP），法生效后 skill 再挂指针 |

## 第 2 步：选所属区、定 slug

- **所属区**：框架法 → `knowledge/specs/`；业务法 → `sample-application/specs/`。辖域裁决 → 归属法卷 §1（把业务契约立进伞内即辖域违规）。模板两区共用，住框架区 `_template/`。
- **slug**：目录名 `<YYYY-MM-kebab>`——立卷年月 + 望文生义的一两个英文词。形状以 `_template/` 骨架自明；历内在库例卷见 `knowledge/specs/archive/` 工作清单（清册归零后例随册灭，届时唯模板与 git 史可鉴——法不考古，模板为权威）。
- 案卷一经立定身份即冻结规则生效：`changes/` 内=审议稿随便改，`current/` =现行法——折叠时刻前两者不一致**以 current 为准**（工位纪律 → changes/README）。

## 第 3 步：铺四件套

从 `_template/` 复制四文件到 `changes/{slug}/` 填空，一件一身份（形状细则以模板原文为准，不自创变体）：

- `specify.md` —— 做什么与算不算成：Why / What changes / 验收 AC 账（人话摘要）/ 约束 / 不做 / 裁决问句；零实现、零法条正文
- `plan.md` —— 怎么做与改哪些法：技术裁量（P-x，含被拒案）+ 修卷 delta（ADDED/MODIFIED/REMOVED，**法条正文唯一居所**，每条标 ← AC-n）+ 波及面与回退
- `tasks.md` —— 纯执行清单：条项 = 一个可验收动作，条内零设计参数（指回 P-x），尾巴指 AC-n
- `implement.md` —— 执行账本：完成即勾、取证、AC→证据映射、delta 源回填、漂移回门、收官闸

**三写禁绝**：同一文字全文案卷恰出现一次（AC-n / P-x 指针不算正文）→ changes/README。
每条 SHALL 括注取证源；无证据的意图写「待回填 → implement §3」留缺口、不硬写 → `knowledge/specs/README.md` 守则 3。

大案可先只递 specify 供裁决，批准后再补 plan/tasks/implement（门分档：Specify 门 → Plan 门）。

## 第 4 步：批准门（两门两硬停）

- **Specify 门**：项目主裁验收账（AC-n）、边界与裁决问句——方向对不对。
- **Plan 门**：项目主裁技术裁量（P-x）、修卷 delta 条文与模板/波及量——方案行不行；**过此门方可施工**。
- **每案两停、不论案之大小；法面不设并审或豁免条款**——放行只出自会话中项目主的明示指令（法条刚性、出口在对话 → ADR-0003）。
- agent 递案必须硬停等人，批准前禁止自行施工——无论结论看起来多明确。

## 第 5 步：施工（审议期纪律）

- 按 tasks.md 推进，**完成一条勾一条**，禁攒批补勾；取证、AC→证据映射、delta 源回填一律落 `implement.md`（施工记录唯一载体，tasks 不兼账本）
- 施工中发现新工作：先补 tasks 条目再动手；范围要扩：回第 4 步重新求批并在 implement §4 漂移账记门次与日期
- 代码与 docs 侧修订可并行进行；但 `current/` 法卷各节在本时刻前**一字不动**，法的全部修改量暂住 plan §修卷 delta

## 第 6 步：折叠归档（文档同步义务唯一时点）

前置：实现完成、implement 账清（§3 零「待回填」）、按变更性质构建/测试全绿（判例参照：纯宣称文本案 `mvn compile` 即可，触行为案全量绿）。此刻、且只在此刻履行同步（归属法卷 §4）：

1. 把 `plan.md` §修卷 delta 写回 `current/` 对应卷：ADDED 入位、MODIFIED 整节替换同名节、REMOVED 删节并在归档记录一句原因
2. **整目录** `git mv` 到 `archive/`——不拆件、不留守。**清册 bill 例外（自焚）**：执行卷宗清册的 bill 不折叠——末步整删自身案卷目录，三区零残留是唯一合格终态（→ 归属法卷 §4「卷宗清册执行」行；该 bill 亦不立 ADR，执行非判例）
3. 在位只进不改：本案与 archive/ 既有案卷此后**在位**不回改（含错字）；要推翻旧案 = 新立案，旧案即历史记录——但**不是永存**：两册到期经清册 bill 彻底归零（→ 归属法卷 §1「卷宗清册制」注；`knowledge/specs/archive/README.md`）
4. 连带义务：计数宣称、glossary、docs 同题设计卡若被本次改法波及，同 PR 修齐（冲突裁决法卷赢 → 归属法卷 §1）；**瘦身自查**：入档前逐件裁 plan（技术形状与修卷 delta）与 specify（why/裁决节），多句决策理由而判例未立先补 ADR，案卷只留表决行 + 指针（→ 归属法卷 §4「案卷折叠入 archive 前」行）
5. **⑦⁺ 论证沉淀**：本案所立/所改判例的沉淀论证（现行版）同 PR 复写进 `docs/explanation/` 对应解读篇、篇脚回指 `ADR-NNN（决策快照）`；无同题篇立篇走登记法；事务性决策 theory-map 一行账即止（→ 归属法卷 §4「案卷折叠时（⑦⁺）」行）

## 第 7 步：验收

- [ ] 七闸全绿：`powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1`（各检查治什么病、变红怎么裁决 → `knowledge/docs/reference/doc-guards.md`）
- [ ] plan §delta 每条 SHALL 的取证源在折叠后指向真实存在的 文件:行 / 测试名（implement §3 清账为前置）
- [ ] `implement.md` §5 收官闸全勾（AC→证据映射无空行）
- [ ] 新建 md 与 knowledge 区现行编码一致（现存文件均带 UTF-8 BOM，归一由主控统一执行）
- [ ] 案卷触码时，施工收尾并跑 `ddd-review`（其末步内置同一闸）

## 常见反例

| 反例 | 正例 |
|---|---|
| 直接改 `current/` 某节让它「和代码一致」 | 想改法就立案，否则修代码（§1 裁决，C 闸拦不住时靠人拦） |
| specify 递出未批就开工 | 第 4 步两门两停 |
| 以案小为由并门/跳门 | 法面刚性、出口在对话——豁免只出自会话中项目主明示（ADR-0003） |
| 验收条文在 specify 抄一份、plan 再抄一份 | 三写禁绝：正文唯一住 plan §delta，specify 只登 AC-n 人话摘要 |
| tasks 条项内写参数值 | 条项指 P-x，参数住 plan §1 |
| 修卷 delta 写成对全世界的全量描述 | delta 教义：只写变化量 |
| 施工完一口气补勾 tasks、取证散写各文件 | 完成即勾，账只落 implement |
| 归档后回改 archive/ 旧案卷 | 在位不改，新案接管（到期归零走清册 bill，不在案卷内涂改） |
| 未立清册 bill 就 rm 两册本体（偷删/拆件/择留） | 清册 = 独立 bill 七步仪式（→ 归属法 §4）；C6 清册臂当场红 |
| 把业务契约立进 `knowledge/specs/` | 辖域违规 → 镜像区 `sample-application/specs/` |
| 折叠时 delta 合了、目录没挪（或只挪一半） | 整目录进 `archive/` |
