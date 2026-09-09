# 实现清单（按依赖序，[P]=可并行；每条尾巴指回 delta Requirement）
- [x] 1. 项目主批复两问（proposal「待裁决」：① 改元重启 ② 页脚快照账行清册时整行删除）→ 若有分歧先修三件套再施工；批复原文 = ADR-0037 的 Confirmation → 批准门
- [x] 2. `decisions/` 新立 ADR-0037「卷宗清册制」（context=冻库无路+脚手架终局空册+发布态如新仓；decision=可清册存储、自足判据前置、彻底抹除（本体+账行+标识符）、清册即改元、法不考古、仪式产物不豁免于仪式；consequences=解读架必须承载前因后果全文成为唯一因果权威、活面标识符有寿命、历元化编号）+ `decisions/README.md` 登记行 + 纯增一行章程复述（索引表 = 活册期工作表，清册随裁）+ `theory-map.md`「知识系统（自指）」账段 → §1 清册制注
- [x] 3. [P] `docs/explanation/knowledge-system.md` 增「清册制」节：以自足判据写法承载本法全部因果（为何能删——两类活物已独立；为何删干净——残留即假墙；为何改元——历元自洽）；页脚追加 `ADR-0037（决策快照）` → §1 Scenario: 合规彻底清册
- [x] 4. [P] `docs/reference/glossary.md` 新词条「卷宗清册制」「清册即改元」「自足判据」（各一句定位 + 指针，禁定义复述） → §4 清册行步骤⑥
- [x] 5. [P] C6 两臂立法实施 `scripts/check-docs.ps1`：冻结臂=现行为扩展至三区（`knowledge/decisions/`、`knowledge/specs/archive/`、`sample-application/specs/archive/`；本体文件在位 diff 含 minus 行 → 红）；清册臂两判——(a) 本体文件**整删**（`--diff-filter=D`）仅当同 diff 新增 `specs/changes/*/proposal.md` 含「清册」字样（清册 bill 在途）时放行，否则红；(b) **空册自洽**：若 decisions/ 无本体 ⇒ 全活面（除 `changes/`、`archive/`、`decisions/` 自身与豁免白名单）grep `ADR-\d{4}`（`ADR-NNNN`/`ADR-NNN` 占位形除外）= 0，archive 侧同判 date-slug 形，残留即红；输出两臂计数行 → §1 清册制注（机器条款）+ Scenario: 清册臂抓残痕
- [x] 6. 清册演练取证（工具法实证闸）：临时 worktree 造四例跑 C6——(a) 整删+清册 bill 在途 → 绿；(b) 整删无 bill → 红；(c) 在位涂改 → 红；(d) 区已空+活面残 `ADR-0099` → 红——四份输出记入本案卷 tasks 附注转正取证源 → §1 Scenario 1/4
- [x] 7. [P] `docs/reference/doc-guards.md` C6 节重写（两臂行为、巡查三区、演练判据；说明唯一入口随工具改） → §2 机器背书列
- [x] 8. [P] `diagrams/knowledge/README/drive-relations.d2`：archive/decisions 容器标签「封存·永不回改」类措辞 → 「在位不改·到期归零」；⑧ 边标签不动；TALA 重渲 + `check-diagrams.ps1` 绿 → §4 清册行步骤⑦
- [x] 9. [P] `.agents/skills/new-bill/SKILL.md`：步骤 6.3 改「在位只进不改 + 到期整册归零（→ §1 清册注）」双态措辞；反例表增「未立清册 bill 整删 = C6 清册臂红」；L33 体例参照（指向具体在库案卷路径）改指 `_template/` 形状自明 + archive 工作表（防账亡例灭） → §1 清册制注
- [x] 10. [P] 伞 `knowledge/README.md` 复述位同步：§1 三分类表 decisions 行与 specs 行各补「到期整册归零（账净如新）」；§3 树 archive/decisions 注同步；§2.1 锚表 ⑦⁺ 行补「页脚回指仅活册期有效」半句 → §4 清册行步骤⑤（净面之预防性增量）
- [x] 11. 现状确认（不改动）：活面指向两册本体的 markdown 超链 = 0（2026-09-09 摘链 17 处成果防回潮）；具体标识符的**净空改写**义务自本期清册 bill 起执行，本案不提前抹号 → §2 两行（纯文本回指条款）
- [x] 12. 闸验：`mvn -B compile`（不触 Java）+ `check-docs.ps1` 七闸绿 + `check-diagrams.ps1` OK → 全部 Requirement
- [x] 13. 归档折叠（文档同步义务唯一时点）：delta 六处写回（attribution-law §1 段替换+新注、§2 两行、§4 新行；specs/README 行；archive/README 整篇重写[九案清单照登为工作表，本案自记一行]；镜像 README 行）+ 整目录 `git mv` → `archive/` → §1/§2/§4 各 Requirement

---

## 取证附注：C6 两臂五例演练（2026-09-09，临时 fixture 仓 `%TEMP%\opencode\cull-drill-fixture`，脚本 = 本仓 check-docs.ps1 副本 + -RootPath 指向夹具）

| 例 | 场景 | 期望 | 实测 C6 行 | 判定 |
|---|---|---|---|---|
| S0 | 基线：两册在库零改动 | PASS | C6 PASS; residue=0 | ✓ |
| S1 | decisions 正文仅 Status 行 1:1 对替 | PASS（宪章豁免） | "Status-line supersede swap allowed: ...ADR-0099.md (1)" + C6 PASS | ✓ |
| S2 | 正文在位涂改（非 Status 行） | 红（冻结臂） | C6 FAIL(1) - 在位涂改 2 line(s) | ✓ |
| S3 | 本体整删、无清册 bill 在途 | 红（清册臂 a） | C6 FAIL(1) - deleted without a cull bill | ✓ |
| S4 | 整删 + bill 在途 + 整册归零 + 活面净 | PASS | "emptied under in-flight cull bill - 清册臂(a) OK" + C6 PASS | ✓ |
| S5 | 册已空、活面残留 ADR-0099 | 红（清册臂 b） | C6 FAIL(1) - 空册自洽被破：活面残留该期具体标识符 1 处 | ✓ |

真仓回归：C6 PASS（decisions=37 bodies / archive=OCCUPIED / residue=0 / cull-bill-in-flight=True 即本案）；SelfTest ST1–ST6 全 PASS（新增 ST5 编号正则、ST6 slug 正则）。

演练揪出的既有 bug（同批治愈，记为本案连带成果）：① L52 `` 单命中时坍缩为字符串标量，与数组 `+` 变字符串拼接 → 三区巡查列表整体失效（清册功能级致命，真实仓同样中招）；② PS5.1 下 git stderr 警告（CRLF 提示）在 EAP=Stop 被升格为终止异常 → C6 块局部降 Continue 包夹；③ 宪章"旧篇仅 Status 行可改"与闸"任何 minus 即红"长期错位 → 冻结臂按宪章补齐 1:1 Status 豁免。