# ADR-0003: 案卷四段制（Specify → Plan → Tasks → Implement）+ 双门刚性批准（法面不设大小豁免）

- Status: Accepted
- Date: 2026-09-10
- 案卷：2026-09-sdd-four-stage（历元二首件**立案即修法**之案：所修之法 = 立法程序自身的案卷形状与门制）。前案核对：ADR-0001 裁论证载体分工（跨区三层），本案裁案卷内部文书分工，正交无撞；本案 supersede 历元一「两阶段批准判例」（案已随清册归零，其常制化即由本案完成）。

## Context（当时快照）

三件套案卷相对业界 spec-driven 标准链（Kiro：Requirements→Design→Tasks→Implement；Spec Kit：specify→plan→tasks→implement）缺「怎么做」正典位。病灶两封活体自证（p6spy 案，审议中）：机制条款（runtime scope、版本 pin）混入 spec-delta 的 SHALL——设计事实冒充行为事实；spy.properties 整串键值塞进 tasks 条项——施工图混进验收清单；施工取证与 AC 映射无账可落。项目主令：按四段引入，delta 归入 plan（「修法行动是计划一环」），回写归 implement。

## Decision

- **案卷 = 四件**：`specify.md`（问题 / 验收 AC 账 / 约束 / 不做 / 裁决节）、`plan.md`（技术决策 P-x + 修卷 delta §2 = 法条正文唯一居所）、`tasks.md`（纯执行清单，条内零设计参数、指回 P-x）、`implement.md`（执行账：勾验 / AC→证据 / 源回填 / 漂移回门 / 收官闸）。
- **三写禁绝**：验收人话摘要住 specify（AC-n），条文正文唯 plan（`← AC-n` 映射），执行记录唯 implement——宽严双份制映射进案卷内部。
- **门制**：Specify 门 → Plan 门，**每案两停、不论案之大小；法条与 skill 不设并审/免停条款**。放行豁免只存在于会话中项目主明示指令——法面刚性，出口在对话（项目主 Q1 裁决原语：「流程规范是必须要严格固定的」）。
- 历史不回改：案卷形状随立案时模板；`archive/` 旧袋与 p6spy 审议袋不追溯（P-7）；执法器 C6 臂开关随折叠同日落地（P-8）。

## 被拒方案

- delta 保持第五件独立：件数膨胀，且 delta 本性 = 施工动作，项目主已裁归 plan。
- 小案并门/免停条款：项目主否决——弹性写进法面即腐蚀刚性，会话层意图不该法条化。
- Kiro 系命名（requirements/design）：「Requirements」与 delta 既有 `### Requirement:` 分节标题撞名，双义即病灶。
- 直接改 `_template/` 不立案：改形状权威 = 改法，须走本案自己的门（元法亦是法）。

## Consequences

25 文件波及面随动（法案义换词、泛称豁免、图与锚表、执法器——明细住案卷 plan §3）；此后一切案卷（含挂起的 p6spy 案收编时）按四件立案；立法成本 +一份 implement 账，换取三层接缝与可审计取证链；`new-bill` 等 7 个 skill 的流程句随折叠改写。

## Confirmation（授权收据）

- 授权人：项目主（Tim）
- 形式：裁决 Q1「都必须停两次，不论大小…你的流程规范是必须要严格固定的」＋双门批准「可以开工」
- 日期：2026-09-10
