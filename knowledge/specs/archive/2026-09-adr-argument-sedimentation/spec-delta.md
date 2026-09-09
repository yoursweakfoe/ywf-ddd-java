# 对 current/patterns/attribution-law.md 的增删改
> 只描述变化量，不描述全世界（delta 教义）。每条带 SHALL + 场景。本案为纯宣称文本案（不触行为代码），取证源 = 被改条款现行文本自身。行文法定稿（2026-09-09 项目主裁决）：**目录名主词，旧指代词（案卷/卷宗）括注辅助**。

## MODIFIED Requirements

### Requirement: §1「案卷与判例分家」注   <!-- 折叠时整段替换 current/ 同名段 -->
**三类件分家**（旧称「案卷与判例分家」；2026-09 `2026-09-archive-dossier-boundary` 立，2026-09 `2026-09-adr-argument-sedimentation` 补沉淀分层）：

- `specs/changes/`（审议中）与 `specs/archive/`（折叠后整袋迁入）里的三件套 = **立法过程件**（旧称「案卷」）——问题陈述、条款变化量（delta）、表决行、施工勾验；折叠即使命终、封存只进不改。
- `decisions/`（`ADR-NNNN` 本体）= **决策事件件**（旧称「判例卷宗」）——决策本身、当时的 context / 取舍 / consequences（思考快照）、**Confirmation 授权收据**；全局编号、append-only、跨时间被引（新案先查旧案、supersede 靠旧案快照活着）。
- `docs/explanation/` = **知识沉淀件**——**论证的现行版**（"这部法今天为什么成立"）的 canonical 居所；折叠时刻（⑦⁺）自 ADR 强制复写进来（→ §4 新行），页脚回指 `ADR-NNN（决策快照）` 供溯源。

卷宗与解读的论证重叠**不是双写病灶，是源与流**：卷宗=冻结收据（回答"当时怎么定的、谁批的"），解读=活的现行（回答"今天为什么如此"），时效语义不同、执法闸各异（C6 护前者、§4 同步护后者）。两栖病灶（changes/archive 的 proposal 裁决表与 ADR Decision Outcome 近同文双写，本行立前有实证）的裁决一句话同步升级：**同一事实，过程归 `changes|archive/`、事件与当时思考归 `decisions/`、沉淀道理（现行版）归 `docs/explanation/`**。
（源：attribution-law.md:19 现文，本 delta 全文替换）

### Requirement: §2 表「设计论证（为什么）」行   <!-- 整行替换 -->
| 设计论证（为什么；**含架构决策的沉淀论证现行版**——ADR 里的"当时思考"不在此行，归下行） | 低 | `docs/explanation/`（含 theory-map 理论账本） | 指针；**架构决策类于 ⑦⁺ 折叠时刻强制复写入同题解读篇**（§4 新行执法；事务性决策一行账即满足，判据见彼行注） | 低易变允许就近重述；强制复写部分由 §4 执法 |
（源：attribution-law.md:30 现文，本 delta 全文替换）

### Requirement: §2 表「架构决策（论证）」行 → 更名「架构决策（事件与当时思考）」   <!-- 整行替换含更名 -->
| 架构决策（事件与当时思考） | 冻结 | `decisions/`（`ADR-NNNN` 全局编号，Confirmation 必填） | `ADR-NNN` 限定名引用；**`changes/`+`archive/` 内涉已立 ADR 之决策只准表决行（何人何时批准/否决/裁定，至多附一句因）+ `→ ADR-NNN` 指针，禁在过程件复写论证正文**（正文的**现行沉淀版**住 `docs/explanation/`——源流非副本，见 §1 三类件分家注；过程件永非其 home） | C6 diff-scope + Confirmation 必填 |
（源：attribution-law.md:31 现文，本 delta 全文替换含更名）

## ADDED Requirements

### Requirement: §4 强制同步表「案卷折叠时（⑦⁺）」行
法案折叠时，本案所立/所改判例（`decisions/` ADR）的**沉淀论证 SHALL 同 PR 复写进 `docs/explanation/` 对应解读篇**，篇脚 SHALL 回指 `ADR-NNN（决策快照）`；无同题篇则立篇并按登记法仅登记 `docs/README` 一处。事务性决策（仅改默认值/更名/修文案，不新增"为什么"）以 theory-map 一行账即算沉淀完成（防灌水档）。**存量不溯及**——既往判例于其专题解读篇下次改写或新法案涉及时随位收编（触发式）。当时思考/沉淀道理之划界判据：**法生效后该理由是否仍成立且指导读法**——仍成立→解读有现行版；仅当时情境成立→留 `decisions/` 作化石，不复写。
（源：本 delta 折叠后由 §1/§2 三处改文自证；纯宣称文本案，判据同 `2026-10-rules-codification` 先例）
#### Scenario: 折叠含新论证的法案
- GIVEN 一部新法案折叠，其 ADR 论证构成对"现行法为何如此"的增量回答
- WHEN 执行 `new-bill` 步骤 6 折叠
- THEN 同 PR 内对应解读篇获得该论证的现行版复写 + 页脚回指 ADR；过程件（changes/archive 三件套）仍只留表决行 + 指针
- AND check-docs 七闸绿（C1 验回指与登记面，C6 验 `decisions/` 零删除）
#### Scenario: 纯事务性决策折叠
- GIVEN 法案仅改默认值/更名/修文案，其决策不新增"为什么"
- WHEN 折叠
- THEN theory-map 账本行即沉淀完成，不强开解读篇
