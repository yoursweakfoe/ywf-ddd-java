# 实现清单（按依赖序，[P]=可并行）
- [x] 批准门：Q1 分层复写 / Q2 仍成立判据 / Q3 强制回指——2026-09-09 项目主照推荐批准 + 行文法裁决（目录名主词、旧称括注）（→ delta 全部 Requirement 的前提）
^- [x] §1 分家注整段替换（→ MODIFIED「§1 案卷与判例分家注」）
^- [x] §2「设计论证」行整行替换（→ MODIFIED「§2 表设计论证行」）
^- [x] §2「架构决策（论证）」行整行替换 + 更名「架构决策（事件与当时思考）」（→ MODIFIED 对应行）
^- [x] §4 强制同步表新增「案卷折叠时（⑦⁺）」行（→ ADDED「§4 案卷折叠时⑦⁺行」）
^- [x] `decisions/README.md` **纯增一行**冲正（第 5 行「决策论证 canonical 住本区 ADR」受 C6 庇护不可回改，新行宣示三类件分家并指向本案——判例法程序，旧行留痕）
^- [x] [P] 图同步：drive-relations.d2 ⑦⁺ 标签由「同题散文·解释立法原因」升格为「同题散文复写（强制）」、解释架身份注微调；render-diagrams → manifest 重刷（→ ADDED Scenario: 折叠含新论证的法案）
^- [x] [P] glossary 同步：「论证沉淀」词条更新 ⑦⁺ 义务语义、「ADR（判例）」词条补「事件与授权收据，非论证现行居所」、新增「决策快照回指」词条（→ MODIFIED §2 两行）
^- [x] [P] 叙述面同步：knowledge/README §1 decisions 行描述、§2.1 锚表③⑦行与「论证的家在判例」措辞改「卷宗存收据、解读住现行」；docs/README 解释架行「论证的来源=判例」改「源=决策事件，现行居所=本架」（→ §1 分家注 MODIFIED）
^- [x] 本案不触行为代码（纯宣称文本案）：构建验证按判例降档为 N/A，判据 → delta ADDED 行源注（`2026-10-rules-codification` 先例）
^- [x] 验收：check-docs 七闸全绿 + check-diagrams 绿（折叠前跑，同 new-bill 步骤 7）
^- [x] 归档折叠：`spec-delta.md` 合入 `current/patterns/attribution-law.md`，本目录 `git mv` 整袋入 `archive/`——**文档同步义务唯一时点**；新立 ADR 记录本裁决（决策事件件）+ theory-map 登账（→ ADDED 行自指履行）
