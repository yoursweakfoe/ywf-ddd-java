# 施工方案与修卷 delta —— 清册 bill
> 本节回答「怎么做」与「改哪些法」，是这两类内容的唯一居所。P-x 是技术裁量编号；tasks 只引用编号。AC-n 对应 specify 的验收账。

## §1 技术形状（机制、坐标、文件落点）

- **P-1 身份与 commit 节奏**：案卷目录 `knowledge/specs/changes/2026-09-cull-epoch-zero/`；slug 载 `cull` + 正文自称「清册 bill」双标识（C6 臂(a) 在途识别与臂(b) 自焚识别皆起效于 HEAD 内容）。采两 commit（Q1-A）：立卷提四件套一笔；施工-删除-自焚终态一笔。被拒：单笔记终态（bill 不入 HEAD → 自焚后识别无凭，审计链退化）。（← AC-6）
- **P-2 补沉淀落点**：`knowledge/docs/explanation/knowledge-system.md` §「过程件内部为什么再分四段」节，「法条刚性、出口在对话」段之后加一小段无编号散文：为何四段之外不设 constitution / clarify 独立件——本店宪法 = 根 AGENTS 九条 + 法卷体系，归属法 §1 已钉死，再造第五文件即双宪；澄清属拍板前问题，天然住 specify 待裁问句节，独立成件会把一次往返拆成两处维护。UTF-8 BOM 随区例。（← AC-2）
- **P-3 整袋删除**：`git rm -r` 三袋整目录——`knowledge/specs/archive/2026-09-adr-into-bill/`（4 文件）、`2026-09-sdd-four-stage/`（4 文件）、`2026-09-archunit-rule-doc/`（3 文件）；袋外 README 不触。镜像区袋零操作。（← AC-1）
- **P-4 册况行归章程**：框架区 `archive/README.md`「**册况**」行改空袋章程态（大意为「本袋现为空袋：案卷到期整册归零，此后每完成一案折叠迁入；章程常驻，账目不驻」）；镜像区 README 删「历元一案卷已清册归零」账目语，只留「本袋现为空袋」与章程援引。两页其余条款体零触。（← AC-4）
- **P-5 净面逐处账**（活面 date-slug 与孤儿案名，全清单，md 八处 + 代码五处）：
  1. `knowledge/specs/changes/README.md`：门制段删尾指针「（→ 案卷 …§裁决记录）」，主句「放行只出自会话中项目主的明示指令」留。
  2. `knowledge/README.md` 锚表：`⑤⑥ Plan 门` 行法源锚列删「；案卷 …」，留 `new-bill` 步骤 4。
  3. `theory-map.md` 四段账行：删「→ 案卷 …§裁决记录」回指引；行首「2026-09 四段案卷案」无连字形状不犯机器臂，留作无编号史叙。
  4. `theory-map.md` 规则集账行：「ArchUnit 守护，案卷 …§裁决记录。」→「ArchUnit 守护。」
  5. `knowledge-system.md` §四段：句尾「→ 案卷 …§裁决记录」删，「项目主立此品味」收句。
  6. `knowledge-system.md` 页脚：删末句本案回指（「本案（判例为何不再是独立态）的裁决快照 → 案卷 …（决策快照）。」整句）；「旧编号制已随判例归卷案废止」为无编号散文，留。
  7. `architecture-rules.md` 页脚：「…沉淀现行版：→ 案卷 …〔决策快照〕。快照冻结…」→ 删回指读段，留「本页论证是决策快照的沉淀现行版：快照冻结，本页随法演化。」
  8. `.agents/skills/new-bill/SKILL.md` 两处（步骤 4 与反例表）：删括注内「→ 案卷 …§裁决记录」，留「法条刚性/面刚性、出口在对话」散文。
  9. 代码五处（Q2-A）：`common-packages-integration-test` 内 `src/test/resources/application-test.yml`、`ResourceServerIntegrationTest.java`、`PgTestSupport.java`、`MybatisPersistenceTest.java`、模块 `pom.xml`——「法案 2026-09-common-it-consolidation」→「历元旧案」；注释行内零 Java/XML/YML 语义变更。（← AC-3）
- **P-6 终闸次序**：②补沉淀（P-2）→ ③删袋（P-3）→ ④净账（P-4）→ ⑤净面（P-5）→ `check-docs` 七闸 + `check-diagrams` + `mvn -B compile`（删除对 HEAD 可见、bill 在途，臂(a) 验整册归零）→ 自焚 `git rm -r` 本 bill 目录 → 复跑同三闸（臂(a) 转自焚识别：HEAD 内本卷 specify 自称「清册 bill」）→ commit#2 → 净工作区三度复跑（臂(b) 验空册自洽）。（← AC-5/6）
- **P-7 执法器零触**：C6 两臂谓词（自称识别、整册归零、空册 slug 零容忍、ADR 常设零容忍）已配平现行法，本案纯执行。被拒：施工顺手改探测器（装置演化须另案，归属法 §1 scripts 行）。（← AC-7）

## §2 修卷 delta（对 current/ 各卷的修改量）

**无。** 清册是归属法 §4「卷宗清册执行」既有条款的执行，不是修法：`current/` 一字不动，历元重启不产生新条款（← AC-7）。本案全部文档触面为：解读架补沉淀（P-2，地图区随位收编，不属修卷）、章程册况行（P-4）、活面标识符净空（P-5）。

## §3 波及面与回退

- 波及：删除 15 文件（三袋 11 + 本 bill 4）；行级改写 7 个 md；注释级改写 5 个代码文件；`mvn -B compile` 复验。图闸零触面（d2/svg 无案名，仅复跑）。
- 回退：commit#2 前一切可退（checkout/reset 即复原袋与 bill）；条款从未变，故无「法漂移窗」。回滚后再清册须重新过两门。
