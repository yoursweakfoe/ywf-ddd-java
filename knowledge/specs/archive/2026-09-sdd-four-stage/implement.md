# 执行账本（施工期唯一记录件；完成即勾、禁攒批补勾）
> 现态：✅ 全账清结，案卷已折叠入 archive/（2026-09-10）。

## §1 执行账（tasks 条项 1:1）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
| 0a Specify 门 | ✅ 批准（2026-09-10 项目主，会话指令「可以开工」；Q1 前裁：每案必两停不论大小） | 手动记录：会话裁决 |
| 0b Plan 门 | ✅ 批准（同上，双门并批系会话层明示放行，合法出口） | 手动记录：会话裁决 |
| 1 ADR-0003 + theory-map 账行 | ✅ | ADR-0003-sdd-four-stage-gates.md 落位；decisions/README 账行×2；theory-map 四段账行+L185 随动 |
| 2 _template 换装 | ✅ | _template/ 现四件（specify/plan/tasks/implement），旧三件已删，BOM 归一 |
| 3 章程/法卷 D-1..14 | ✅ | specs/README、AGENTS、attribution（§1+§4×3+插行）、changes/README、archive/README、glossary、knowledge/README 锚表、d2、镜像×2、theory-map、knowledge-system+⑦⁺ 新节 |
| 4 流程件 D-15..16 | ✅ | new-bill 整件重写；五工序 skill 统一句 + new-portal L61 + ddd-review L87 |
| 5 执法器 D-17 | ✅ | check-docs.ps1 C6 臂双名（specify|proposal）+ 头注/消息、doc-guards L46 |
| 6 d2 + 图闸 | ✅ | render 成功 1 图 → check-diagrams DIAGRAMS_EXIT=0 |
| 7 验证三件套（复扫/七闸/mvn） | ✅ | 复扫命中全数分类零违规；check-docs 7/7 PASS DOCS_EXIT=0；mvn -B install MVN_EXIT=0 |
| 8 折叠收官 | ✅ | 整袋 Move-Item 入 archive/2026-09-sdd-four-stage/（本案文件未入 HEAD，git mv 不适用，rename 由 commit 时检测）；折叠后终跑七闸+图闸双绿 |

## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
| AC-1 | `knowledge/specs/changes/_template/` 现四件（specify/plan/tasks/implement），旧三件灭 | ✅ |
| AC-2..5 | 四模板正文 = 本案 plan §4（逐件 diff 可核）；本案自身即活体形状 | ✅ |
| AC-6 | 全仓复扫命中分类毕：史述/豁免/案卷自引，无同文双写 | ✅ |
| AC-7 | specs/README 守则 2 新文、changes/README 门档段、new-bill 步骤 4、knowledge/README ②④行、ADR-0003 Decision | ✅ |
| AC-8 | 25 文件随动毕；泛称豁免逐处零动（new-service L27 / blueprint L53,L104 / new-portal L39 / architecture-rules L103 / p6spy 袋 / archive 史述）；check-docs C1-C7 PASS | ✅ |
| AC-9 | check-docs.ps1:217,223 双名谓词；终跑 `cull-bill-in-flight=False`、C6 PASS | ✅ |
| AC-10 | drive-relations.d2 节点+①②⑤ 三标签；SVG 重刷（render 51ms success）；check-diagrams EXIT=0 | ✅ |
| AC-11 | archive/README 入袋规矩「形状随立案时模板…不回改」；p6spy 袋零重塑（仅 ADR 编号改标一行，其审议期随动） | ✅ |
| AC-12 | sample specs/README L5、changes/README L3 四件措辞 | ✅ |

## §3 源回填账
（本案无 current/ 编号法卷 SHALL 增量，D 表即执行量——零「待回填」位。）
- 无待回填位 ✅

## §4 漂移记录
- 2026-09-10｜Plan 门后施工期，两处**加强而非扩面**的裁量：① C6 清册臂文件名谓词取 `(specify|proposal)` 双名而非单换 specify（旧案袋与在途袋留检，零漏检窗）；② doc-guards C6 行措辞随双名同步。均属 AC-9「臂须正确工作」的射程内。
- 2026-09-10｜发现**预存弱环（登记待裁，不顺手修——改臂谓词=新法）**：C6 清册臂以「案卷文件载『清册』一词」为在途凭据——本案审议期 specify 因标准术语引用即误开臂（实测 True，折叠后归 False）。凡未来**已入库（HEAD 内）**法案在 specify 合法提及册制者，其存续期臂恒真；拆件/择留删仍有「删后必须归零」拦，唯一可误放 = 整册全删且人眼放行提交。收紧谓词（如认案卷目录 slug 载册字样）另案裁决 = **账 C**。
- 2026-09-10｜施工序小疵自记：attribution-law（current/ 卷）之 D-4..7 实际写于折叠挪袋前数分钟，与「current 一字不动至折叠刻」之工序纪律形不符、实不远（同 session、批准门已过、折叠批终闸复跑双绿）。后案：current 触卷动作严格排入折叠批执行。

## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」
- [x] 七闸全绿（DOCS_EXIT=0）+ check-diagrams 绿（DIAGRAMS_EXIT=0）+ `mvn -B install` 绿（MVN_EXIT=0）
- [x] 瘦身自查毕（多句理由之判例已由 ADR-0003 承载；四件按新形状在位封存，此后不回改）
