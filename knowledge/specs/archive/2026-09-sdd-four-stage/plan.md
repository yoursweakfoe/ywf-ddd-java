# 施工方案与修卷 delta（plan · 本案唯一法条正文居所）
> 编号 P-x = 技术决策（tasks 指回此处）；D-x = 修卷替换对（旧文以「」锚定原文片段，新文为折叠日逐字执行量）。AC-n 对应 specify 验收账本。

## §1 技术形状（决策与被拒案）

- **P-1 文件命名**：`specify.md / plan.md / tasks.md / implement.md`（Spec Kit 血统，与项目主四段白话一致）。被拒：Kiro 系 `requirements/design`——「Requirements」与本店 delta 既有 `### Requirement:` 分节标题撞名，双义即病灶。
- **P-2 delta 归位**：修卷 delta 降为 **plan 专节**（项目主已裁「修法行动是计划一环」）。被拒：第五件独立保留（件数膨胀且 delta 本性=施工动作，非问题陈述）。
- **P-3 防双写机制（AC-6 执法对象）**：specify 验收账本 = 人话摘要 + `AC-n` 编号，零条文；plan §2 每条 Requirement 标题挂 `← AC-n`，法条正文全文案卷唯一一份。折叠时条文只从 plan §2 起。
- **P-4 批准门**：Specify 门与 Plan 门**每案各自硬停，不论案之大小，法面不设并审/免停条款**；豁免只可出自会话中项目主明示指令（法条刚性、出口在对话——2026-09-10 项目主拍 Q1）。旧「两阶段批准判例」措辞从各处 REMOVED，由此常制接管（AC-7）。此条为本案核心教义，ADR-0003 论证正文必载。
- **P-5 implement 五段账**：执行账（tasks 条项 1:1 搬入＋状态＋取证）/ AC→证据映射 / 取证源回填账 / 漂移记录（范围扩→回门登记）/ 折叠收官闸。
- **P-6 回填义务迁移**：旧模板「（源：…待施工回填）」句式改为 plan SHALL 初值「（源：待回填 → implement §3）」；**折叠前置条件 = 每条 SHALL 回填为真实 `文件:行`/测试名**（specs/README 守则 3 不换语义、只换账本宿主）。
- **P-7 历史兼容**：案卷形状随立案时模板，在位不回改（archive/README 与归属法 §1 同句改写承载，AC-11）。
- **P-8 执法器开关时机 = 折叠日**（装置随法走，不留器法漂移窗；R11「执法器滞后」教训的反向操演）。p6spy 审议袋持旧名 `proposal.md` 且无「清册」关键词，开关期间清册臂无漏检面。

## §2 修卷 delta（ADDED / MODIFIED / REMOVED）

### MODIFIED（章程/法卷行，逐字替换对）

**D-1 `knowledge/specs/README.md` 守则 2** ← AC-1/AC-7/AC-3
- 旧：「修法唯一通道：`changes/<YYYY-MM-slug>/` 立三件套（proposal → spec-delta → tasks）→ 实现 → **归档折叠**进 `current/`」
- 新：「修法唯一通道：`changes/<YYYY-MM-slug>/` 立四件套（specify → plan → tasks → implement）：Specify 定验收、Plan 定技术形状与修卷 delta、Tasks 拆施工、Implement 记执行账与取证——批准门分档（Specify 门 → Plan 门，**每案两停不论大小**，过门方可施工），实现全绿后**归档折叠**进 `current/`」

**D-2 `knowledge/specs/README.md` 目录表 changes 行** ← AC-1
- 旧：「`_template/` 三件套模板**本区与业务镜像区共用**」→ 新：「`_template/` 四件套模板**本区与业务镜像区共用**」

**D-3 根 `AGENTS.md` 契约先行行** ← AC-1/AC-8
- 旧锚：「`changes/<slug>/` 出三件套（框架 → 」→ 新文：「`changes/<slug>/` 出四件套（specify → plan → tasks → implement；框架 → 」（后续括号内容原样承接，防双括号并置）

**D-4 `attribution-law.md` §1 三类件分家句** ← AC-6/AC-11
- 旧：「`specs/changes/`（审议中）与 `specs/archive/`（折叠后整袋迁入）里的三件套 = **立法过程件**（旧称「案卷」）——问题陈述、条款变化量（delta）、表决行、施工勾验」
- 新：「`specs/changes/`（审议中）与 `specs/archive/`（折叠后整袋迁入）里的案卷四件套（specify / plan / tasks / implement；2026-09 四段案前为三件套旧形）= **立法过程件**（旧称「案卷」）——问题陈述与验收账（specify）、技术形状与条款变化量（plan，含修卷 delta）、施工清单（tasks）、执行账与表决回填（implement）」
- 同句后段：「两栖病灶（changes/archive 的 proposal 裁决表与 ADR Decision Outcome 近同文双写）」→「两栖病灶（changes/archive 的 specify 裁决节与 ADR Decision Outcome 近同文双写）」

**D-5 `attribution-law.md` §4 触发表两行** ← AC-8
- L59/L60 两处「先立 …三件套」→「先立 …四件套（specify → plan → tasks → implement）」

**D-6 `attribution-law.md` §4 瘦身闸行** ← AC-3/AC-5
- 旧：「逐件查 proposal/delta：凡多句决策理由而其判例未立 → 先补 ADR 再折叠」
- 新：「逐件查 plan（技术形状与修卷 delta）与 specify（why/裁决节）：凡多句决策理由而其判例未立 → 先补 ADR 再折叠」

**D-7 `attribution-law.md` §4 新行（ADDED 性质）** ← AC-5/AC-7
- 在「修订法卷条款」行前插入：「| 施工与验收（案卷执行期） | tasks 条项完成即勾，取证、AC→证据映射、delta 源回填、漂移回门一律记 `implement.md`（施工账本唯一载体）；范围扩 → 回批准门并在 implement 漂移记录留档 |」

**D-8 `knowledge/specs/changes/README.md` 全文三件枚举 + 折叠段** ← AC-1..AC-6
- 「一个变更 = 一个目录：`<YYYY-MM-slug>/`，内含三件套（从 `_template/` 复制起稿）：」及其下三行枚举 → 换四行枚举（各件一句职责 + AC-6 指针句：「三写禁绝：法条正文唯一住 plan §修卷 delta，验收摘要住 specify，执行记录住 implement」）
- 「**折叠即修法**：实现完成、测试全绿后，把 delta 内容合入 current 对应节」→「**折叠即修法**：实现完成、implement 账清（每条 SHALL 源回填为真实取证）后，把 plan §修卷 delta 合入 current 对应节」

**D-9 `knowledge/specs/archive/README.md` 入袋规矩** ← AC-11
- 旧：「折叠完成的案卷**整目录**迁入（`<YYYY-MM-kebab>/` 三件套），不拆件、不留守」
- 新：「折叠完成的案卷**整目录**迁入（`<YYYY-MM-kebab>/`，案卷形状随立案时模板：现行四件套，2026-09 前旧案三件套，在位不回改），不拆件、不留守」

**D-10 `knowledge/docs/reference/glossary.md` 词条** ← AC-8
- 旧行：「| 三件套 | 一份法案 = proposal → spec-delta → tasks（模板 `changes/_template/`）… |」
- 新行：「| 四件套 | 一份法案 = specify → plan → tasks → implement（模板 `changes/_template/`；2026-09 四段案卷法立案，三件套为其前史形） → [changes/README](../../specs/changes/README.md) |」

**D-11 `knowledge/README.md` 锚表行 ①②④⑤⑥⑦** ← AC-10
- ①行：「全名：判断立案·起草三件套」→「全名：判断立案·起草四件（Specify 先行）」
- ②行：锚句末追加「门分档：Specify 门 → Plan 门（本案折叠后生效；前案按当时门制）」
- ④行：「两阶段批准判例（proposal 先批、再补 delta/tasks，步骤 3 末注）」→「门分档常制：Specify 门验验收、Plan 门验方案与修卷量（2026-09 四段案卷法立，supersede 历元一两阶段批准判例）」
- ⑤行：「勾账 + 构建/测试全绿」→「tasks×implement 双账推进：完成即勾、取证入 implement 执行账」
- ⑥行：全名「⑥ 结果回填」→「⑥ 结果回填（implement 账 + delta 源回填）」；锚句「delta 每条 SHALL 取证源折叠后须指真实 文件:行/测试名」→「plan §delta 每条 SHALL 的源须在折叠前经 implement §3 回填为真实 文件:行/测试名」
- ⑦行：锚句「`spec-delta.md` 写回」→「plan §修卷 delta 起出写回」
- 对应 d2 四处（见 D-12）。

**D-12 `knowledge/diagrams/knowledge/README/drive-relations.d2`** ← AC-10
- `specs.changes` 节点 label 追加第二行：`specify → plan → tasks → implement`
- ① label →「① 立案起草（Specify：做什么/验收）」；② label →「② 分档送审（Specify 门 → Plan 门）」；⑤ label →「⑤ 按约施工（Tasks×Implement）」；⑥ label 不动（语义在锚表 D-11 承载）
- ③⁺/⑦⁺、静态驱动边、淡虚线引用边：**一字不动**

**D-13 镜像区两援引行** ← AC-12
- `sample-application/specs/README.md` L5：「三件套模板 = …」→「四件套模板 = …」
- `sample-application/specs/changes/README.md` L3：「三件套形状、折叠即修法」→「四件套形状、折叠即修法」

**D-14 解读架两篇（随动 + ⑦⁺ 复写）** ← AC-8
- `theory-map.md` L185：「（三件套，封存）」→「（四件案卷，封存）」
- `knowledge-system.md` L11 立法过程件行：「`specs/changes/` →（折叠后）`specs/archive/` 三件套 | 当初为什么要改这部法、改了哪些条款、谁批的、施工勾验」→「…四件案卷 | 问题与验收（specify）、方案与修卷量（plan）、施工清单（tasks）、执行账与表决（implement）」；同篇补一节「四段分工的为什么」（⑦⁺ 强制复写，篇脚回指 ADR-0003）

### 流程件改写（`.agents/`，方法区不载法、只挂指针——按 C7 技能闸合规形状执行）

**D-15 `new-bill/SKILL.md` 重写要点**（frontmatter description 三件套→四件套；前置阅读模板行；步骤 3 标题「铺四件套」+ 各件职责一句 + delta 入 plan §2 指针；**步骤 4 = 两门两硬停（Specify 门 → Plan 门），条文不设大小豁免——会话中项目主明示放行属对话层人工意图，skill 不载免停条款**；步骤 5 补 implement 记账纪律（完成即勾/取证/漂移回门）；步骤 6.1 折叠源 = plan §修卷 delta、6.4 瘦身闸措辞随 D-6；步骤 7 加「implement §5 收官闸全勾」；反例表：旧「spec-delta 写成全量描述」行保留改称 plan §delta，新增「tasks 条内写设计参数（→指 P-x）」「验收条文抄两份（→唯一住 plan §2）」「以案小为由并门/跳门（→法面刚性，两停）」三行）← AC-1..AC-7

**D-16 五工序 skill 统一句**（new-aggregate L17 / new-usecase L17 / new-portal L16+L61 / batch-operations L16 / scheduled-task L15，现文同形者一律整句替换）：
- 旧式：「动手实现前，在 `…/changes/<YYYY-MM-slug>/` 立三件套（模板在 `knowledge/specs/changes/_template/`，首案时开册、目录未立属正常）：proposal（why/what/不做）→ spec-delta（对 current 的 ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks。…」
- 新式：「动手实现前，在 `…/changes/<YYYY-MM-slug>/` 立四件套（模板在 `knowledge/specs/changes/_template/`，首案时开册、目录未立属正常）：specify（问题/验收 AC 账/边界/裁决问句）→ plan（技术形状 + 对 current 的修卷 delta：ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks（纯清单）→ implement（执行账）。…」
- **new-aggregate L17 变体**（内联枚举语序不同：「立三件套（proposal → spec-delta → tasks，模板 = …`）」）：整括号换「立四件套（specify → plan → tasks → implement，模板 = `knowledge/specs/changes/_template/`）」，旁文不动。
- `ddd-review/SKILL.md` L87 勾项：「三件套并归档折叠」→「四件套并归档折叠」← AC-8

**D-17 执法器** ← AC-9
- `knowledge/scripts/check-docs.ps1`：L217-218 `-Filter 'proposal.md'` → `'specify.md'`；L220-223 注释与 `-match '(^|/)proposal\.md$'` → `specify`；L255 消息文案 proposal → specify；头部注释行 9 同步
- `knowledge/docs/reference/doc-guards.md`：C6 节凡提「案卷 proposal 载『清册』」处 → 「案卷 specify 载『清册』」（施工时按 grep 实扫逐处改）

## §3 波及面总账与豁免

**法案义 25 文件**：上列 D-1..D-17 尽数枚举（specs/README、AGENTS、attribution、changes/README、archive/README、glossary、knowledge/README、d2、镜像×2、theory-map、knowledge-system、_template×3→×4、new-bill、工序 skill×5、ddd-review、check-docs.ps1、doc-guards）。折叠日先全仓 `grep 三件套|spec-delta|proposal` 复扫，表外命中逐个裁决入 D 表或豁免清单——**账外露红即施工失败**。

**泛称豁免清单（一字不动，AC-8 另一半）**：
1. `new-service/SKILL.md` L27「CQE 三件套」；2. `aggregate-blueprint.md` L53/L104「CO/Command/Query 三件套」；3. `new-portal/SKILL.md` L39「职责三件套」；4. p6spy 案卷内「激活三件套」语（该案自决）；5. `new-service` 其余业务构件计数语。判据：所指若是**代码构件组**者豁免；所指若是**案卷文书组**者随动。

## §4 新模板全文（折叠日 `_template/` 一次换装；旧三文件 git rm、新四文件落位）

### specify.md
```
# <变更一句话标题>
- Slug: <YYYY-MM-slug> ｜ 日期: <YYYY-MM-DD> ｜ 关联 ADR: <ADR-NNN / 无 / 待立（③ 裁决时落卷宗）>
## Why（为什么现在改）
<现状一句 + 痛点/驱动力一句>
## What changes（做什么，非怎么做）
<行为级 bullet：为谁改、改出什么可观测差异>
## 验收标准（AC 账本 — 人话摘要 + 编号；法条正文唯一住 plan §delta，此处零条文）
- AC-1 <可断言验收句>
- AC-2 …
## 约束
- <辖域/姿态/兼容/环境硬约束>
## 不做（范围边界）
- <显式排除项，防 worker 顺手扩面>
## 待你裁决的 N 问
- Q1 <分叉 A｜B + 默认倾向 + 一句理由>
```

### plan.md
```
# 施工方案与修卷 delta
> 「怎么做」+「改哪些法」的唯一居所。P-x = 技术决策编号（tasks 只指号不抄参）；AC-n 对齐 specify 验收账。

## §1 技术形状（机制选型、坐标、文件落点、参数值）
- P-1 <决策点>：选定 <方案>；被拒 <替代（一句拒因）>

## §2 修卷 delta（对 current/ 各卷与章程；折叠即自此起量）
> MODIFIED 整节/整对替换同名处；REMOVED 删节并在归档记录一句原因。每条标 ← AC-n。
### ADDED
#### Requirement: <名>   ← AC-n
系统 SHALL <可断言行为；规范代码形状在此立法>。（源：待回填 → implement §3）
##### Scenario: <名>
- GIVEN <前置> ｜ WHEN <动作> ｜ THEN <可观测结果> ｜ AND <补充，如适用>
### MODIFIED
#### Requirement: <旧名>   ← AC-n   <!-- 折叠时整节替换 current/ 同名节 -->
<改后全文>
### REMOVED
#### Requirement: <名>   （原因：<一句>）

## §3 波及面与回退
- 文档/执法器/消费方影响清单；施工失败回退手段（未折叠 = 法未动，撤施工即净）
```

### tasks.md
```
# 实现清单（按依赖序，[P]=可并行）
> 纯清单：条项 = 一个可验收动作；条内零设计参数（指 P-x）；尾巴指 AC-n；执行与取证记 implement。
- [ ] <动作>（P-x ｜ → AC-n）
- [ ] 测试：<场景 → 断言落点（单测/集成/手动取证；参照 how-to/testing.md）>（→ AC-n）
- [ ] 折叠：plan §delta 合入 current/，整目录入 archive/，implement §5 收官闸全勾（→ 全部 AC）
```

### implement.md
```
# 执行账本（施工期唯一记录件；完成即勾、禁攒批补勾）
## §1 执行账（tasks 条项 1:1）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
## §3 源回填账（plan §delta 每条 SHALL：「待回填」→ 真实取证位）
## §4 漂移记录（范围扩/新发现：回批准门之日期 + 门次；无漂移=本行写「无」）
## §5 折叠收官闸
- [ ] 全部 AC 有真实证据且 §3 零「待回填」
- [ ] 七闸全绿（触码案加 mvn）；触图案 check-diagrams 绿
- [ ] 瘦身自查毕（多句理由之判例已立 ADR）
```

## §5 影响面与回退
- 影响：本案不触任何 Java 行为与构建产物（check-docs 属文档闸）；外部读者（引入本仓 SOP 者）得新案卷法。
- 回退：批准前任一档否决 → 案卷留审议区不施工，零痕迹；折叠后发现撞法 → 新立案 supersede（判例法轨道），不回改。
- 验证闸（本案=触流程件不触 Java 行为案）：check-docs 七闸 + check-diagrams + render-diagrams 产物刷新 + `mvn -B install` 全量绿（护「意外触码零」的负证明）。
