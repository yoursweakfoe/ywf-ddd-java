---
name: new-bill
description: 为知识体系起草、推进并归档一部法案（案卷三件套 proposal → spec-delta → tasks，经批准门施工、折叠归档）。当需要修改 knowledge/specs/current 法卷（框架法卷或 sample-application/specs 业务镜像法卷，含归属法卷自身）、新增/变更/废止规范条款、修订框架行为或示例业务行为契约、或把已完成的案卷折叠归档时使用。改法唯一合法通道是立 bill；业务契约不入伞，立到镜像区。禁止为迁就代码偷改法卷——那必须走本流程裁决。
---

# 法案起草与归档（立法 SOP）

> 定位：改法的唯一通道。制度本身（诸区裁决、归属表、强制同步规则、施工中/定稿身份）在归属法卷与两区 README，本 skill 只载工序与关卡，条文一律指针。

## 前置阅读（动笔前）

- 元法：`knowledge/specs/current/patterns/attribution-law.md` —— §1 诸区分野（冲突裁决表）、§4 强制同步规则（「要不要立案」的触发源）
- 法区宪章：`knowledge/specs/README.md` —— current / changes / archive 一名一身份，宽严双份
- 工位纪律：`knowledge/specs/changes/README.md` —— 案卷目录制与「折叠即修法」
- 三件套模板：`knowledge/specs/changes/_template/` —— proposal.md / spec-delta.md / tasks.md（形状权威在模板本身，本 skill 不复述）

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
- **slug**：目录名 `<YYYY-MM-kebab>`——立卷年月 + 望文生义的一两个英文词。体例参照已生效判例：`knowledge/specs/archive/2026-10-rules-codification/`、`knowledge/specs/archive/2026-09-pagequery-default-claim/`。
- 案卷一经立定身份即冻结规则生效：`changes/` 内=审议稿随便改，`current/` =现行法——折叠时刻前两者不一致**以 current 为准**（工位纪律 → changes/README）。

## 第 3 步：铺三件套

从 `_template/` 复制三文件到 `changes/{slug}/` 填空，结构要求看模板原文、不自创变体：

1. `proposal.md` —— 只写问题与边界，不写实现。待批态样本 → `knowledge/specs/changes/2026-11-agents-workspace/proposal.md`
2. `spec-delta.md` —— 只描述变化量不描述全世界（delta 教义）；每条 SHALL 括注取证源（真实 文件:行 / 测试类名），无证据的意图用「待 changes/ 补全」留缺口、不硬写 → `knowledge/specs/README.md` 守则 3
3. `tasks.md` —— 实现清单按依赖序，每条尾巴指回 delta 的 Requirement 名

大案可先只递 proposal 供裁决，批准后再补 delta/tasks（两阶段批准的现行判例即上列 2026-11 案卷首注）。

## 第 4 步：批准门（硬停）

agent 递出案卷后**必须停下等人工**：批准、否决、待裁决问句的拍板全是人的事（问句样式 → 2026-11 案卷「待你裁决的三问」节）。批准前禁止自行施工——无论结论看起来多明确。

## 第 5 步：施工（审议期纪律）

- 按 tasks.md 推进，**完成一条勾一条**，禁攒批补勾
- 施工中发现新工作：先补 tasks 条目再动手；范围要扩：回第 4 步重新求批
- 代码与 docs 侧修订可并行进行；但 `current/` 法卷各节在本时刻前**一字不动**，法的全部修改量暂住 delta

## 第 6 步：折叠归档（文档同步义务唯一时点）

前置：实现完成、按变更性质构建/测试全绿（判例参照：纯宣称文本案 `mvn compile` 即可，触行为案全量绿）。此刻、且只在此刻履行同步（归属法卷 §4）：

1. 把 `spec-delta.md` 写回 `current/` 对应卷：ADDED 入位、MODIFIED 整节替换同名节、REMOVED 删节并在归档记录一句原因
2. **整目录** `git mv` 到 `archive/`——不拆件、不留守
3. 只进不改：本案与 archive/ 既有案卷此后永不回改（含错字）；要推翻旧案 = 新立案，旧案即历史记录（同卷宗法理 → `knowledge/specs/archive/README.md`）
4. 连带义务：计数宣称、glossary、docs 同题设计卡若被本次改法波及，同 PR 修齐（冲突裁决法卷赢 → 归属法卷 §1）

## 第 7 步：验收

- [ ] 七闸全绿：`powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1`（各检查治什么病、变红怎么裁决 → `knowledge/docs/reference/doc-guards.md`）
- [ ] delta 每条 SHALL 的取证源在折叠后指向真实存在的 文件:行 / 测试名
- [ ] 新建 md 与 knowledge 区现行编码一致（现存文件均带 UTF-8 BOM，归一由主控统一执行）
- [ ] 案卷触码时，施工收尾并跑 `ddd-review`（其末步内置同一闸）

## 常见反例

| 反例 | 正解 |
|---|---|
| 直接改 `current/` 某节让它「和代码一致」 | 想改法就立案，否则修代码（§1 裁决，C 闸拦不住时靠人拦） |
| proposal 递出未批就开工 | 第 4 步硬停 |
| 归档后回改 archive/ 旧案卷 | 只进不改，新案接管 |
| 把业务契约立进 `knowledge/specs/` | 辖域违规 → 镜像区 `sample-application/specs/` |
| spec-delta 写成对全世界的全量描述 | delta 教义：只写变化量 |
| 施工完一口气补勾 tasks | 完成即勾 |
| 折叠时 delta 合了、目录没挪（或只挪一半） | 整目录进 `archive/` |
