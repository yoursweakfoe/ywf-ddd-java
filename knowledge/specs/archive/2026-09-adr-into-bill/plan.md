# 施工方案与修卷 delta（plan · 本案唯一法条正文居所）
> P-x = 技术裁量（tasks 指号抄不回）；D-x = 修卷替换对（旧文以「」锚定原文片段，新文为折叠日逐字执行量；行级锚缺失处标〔施工读原文〕）。AC-n 对齐 specify 验收账。

## §1 技术形状（机制选型、坐标、文件落点、参数值）

- **P-1 §裁决记录节形状**：`specify.md` 末节增设 `## §裁决记录（裁决唯一居所：门次收据与拍板快照；裁一条落一条，落笔不改）`，条目三型——门次收据（`门 <档> ✅/❌ <日期>｜项目主原语摘录`）、Q-x 裁定（`裁 <项>｜当时理由（允许多句，冻结快照）｜落点 AC/P｜supersede 声明（何案§节/无）`）、漂移回门（日期＋门次）；头行「关联 ADR」字段删除。**三写禁绝四归一居**：问题住 specify 前段、**裁决住 specify §裁决记录**、条文住 plan、账住 implement——当时理由只准落裁决节（快照），现行版走 ⑨⁺ 复写解读架，规范文本走 delta 入法卷，三时间属性各一身。被拒：第五件 `decision.md`（件数膨胀；前案 P-2 已立判例「delta 本性=计划一环，非独立件」，裁决同理）；落 implement（时序倒置——裁决先于施工存在）。
- **P-2 无案裁决双载体**：「修码就法」类裁决（无案卷）= 所涉法卷末节「生效登记」一行（✅ 生效＋裁决散文＋日期，卷内本有该节〔施工逐卷定位〕）＋ theory-map 账本行，两载体同案齐落。被拒：强制 lite 案卷（AC-3 已裁）。
- **P-3 存量三案与区体处置**：`git rm` `knowledge/decisions/` 全部 4 文件（ADR-0001/0002/0003/README），不迁不留不回填（Q3-A）；archive 在位案卷内对旧 ADR 的引用是历史语，不回改（AC-11）。执法器换臂与删区**同折叠 commit 一刀**（装置随法走，不留器法漂移窗——前案 P-8 同款操演）。
- **P-4 工序编号重组（①–⑩＋③⁺/⑨⁺）** 旧→新映射：

  | 旧八步 | 新十步 | 变化 |
  |---|---|---|
  | ① 立案起草 | ① 立案起草 | dev→specify 精锚 |
  | ② 分档送审（一箭） | **② Specify 门送审＋③ 裁定落笔**（一箭双向）、**⑤ Plan 门送审＋⑥ 放行裁定**（一箭双向） | 双门各显为一回合 |
  | ③ 拍板沉淀·先查旧案（dev→dec） | ② 边注（翻旧案）＋ **③⁺**（theory-map 账本行，源=§裁决记录） | dec 消解 |
  | ④ 裁定落稿（dec→changes） | **③ 裁定落笔**（入 §裁决记录）＋ **④ 裁定入稿**（specify→plan 内边） | 外挂改内挂 |
  | ⑤ 按约施工 | ⑦（tasks→code 精锚） | 换号 |
  | ⑥ 结果回填 | ⑧（code→implement 精锚） | 换号 |
  | ⑦ 折叠落实 | ⑨（**plan→current**，delta 起量段显形） | 换号＋精锚 |
  | ⑦⁺ 论证沉淀 | ⑨⁺（回指改 `→ 案卷 <slug> §裁决记录`） | 换号 |
  | ⑧ 归档 | ⑩（changes 容器→archive） | 换号 |

  被拒：保号重锚（Q4-B′ 项目主另裁）。
- **P-5 图形结构**：`specs.changes` 字符串节点 → 容器（子块 specify/plan/tasks/implement，label 载各件身份＋关键居所）；`dec` 容器删。**门回合用 `<->` 合边**（②③、⑤⑥各一箭两号）——旧案已付学费「同端点平行边是 TALA 标签漂移病灶」（旧 ③ 行裁例），dev→specify 若同向两箭（①③）必犯此病；全部边 `{ near: center }` 钉中点；静态驱动边与淡虚线引用边一字不动。被拒：①③ 同向双箭（平行边复发）。
- **P-6 supersede 落卷链**：推翻旧裁决 = 新案 §裁决记录 条目点名 `案卷 <date-slug> §裁决记录 Qn` 宣告接管；旧案在位不改由 C6 冻结臂＋archive 只进不改共守；不设机器 supersede 检查（旧 Status 对替豁免臂随区退役）。
- **P-7 C6 换臂**（详单 → D-27/D-28）：$decDirs 与 archive 分支的 `ADR-*.md` 枚举、Status 行豁免分支全删；arm(a) 清册面收缩为两侧 archive；arm(b) 改两式——**废号零容忍常设臂**（活面 Markdown 含 `ADR-\d{4}` 即红，$zoneSelfRx 历史语豁免面保留 changes/archive；不依赖任何册空触发——编号制度已废，残留即鬼）＋ 空册自洽（date-slug 臂原样）；SelfTest r5 正则保留复用；C6 info 行字段随动。
- **P-8 镜像区只随援引**：`sample-application/specs/` 诸行按判据复质，不另立法（辖域铁律）。

## §2 修卷 delta（对 current/ 各卷与章程；折叠即自此起量）

### MODIFIED（逐字替换对；「施工读原文」者按判据就地裁字，取证回 implement §3）

**D-1 归属法 §1 诸区表 decisions 行删除** ← AC-1
- 删行：「| `knowledge/decisions/` | 判例卷宗 | 过去的决策事件 | 正文永不回改；推翻 = 新立 ADR + 旧篇仅 Status 行变 superseded | 决策定稿当刻 |」

**D-2 归属法 §1 分区枚举行** ← AC-1
- 旧：「伞 `knowledge/` 与区 `docs|specs|decisions|scripts` 本身不立法」→ 新：「伞 `knowledge/` 与区 `docs|specs|scripts` 本身不立法」

**D-3 归属法 §1「三类件分家」注整段替换（改题「两类件分家」）** ← AC-2/3/5
- 新文：**两类件分家**（旧称「三类件分家」，判例独立地位由 2026-09 判例归卷案撤并入案卷；历元一案卷边界案〔2026-09〕立、论证沉淀案〔2026-9〕补沉淀分层、清册案〔2026-09〕补清册制）：`specs/changes/`（审议中）与 `specs/archive/`（折叠后整袋迁入）里的案卷四件套（specify / plan / tasks / implement；2026-09 四段案卷案前为三件套旧形；2026-09 判例归卷案起 specify 增设 §裁决记录节）= **立法过程件**（旧称「案卷」）——问题陈述与验收账、**决策事件与当时思考快照、Confirmation 授权收据与 supersede 宣告（specify §裁决记录，案卷级裁决唯一居所）**、技术形状与条款变化量（plan，含修卷 delta）、施工清单（tasks）、执行账与表决回填（implement）；折叠即使命终、封存只进**不改在位**，到期整册抹除（→「卷宗清册制」注）。`docs/explanation/` = **知识沉淀件**——**论证的现行版**（"这部法今天为什么成立"）的 canonical 居所，折叠时刻（⑨⁺）自案卷 §裁决记录强制复写进来（→ §4「案卷折叠时（⑨⁺）」行），页脚回指 `→ 案卷 <date-slug> §裁决记录（决策快照）` 供活册期内溯源，清册时刻随灭（行灭理存）。案卷与解读的论证重叠**不是双写病灶，是源与流**：案卷裁决节=冻结收据（回答「当时怎么定的、谁批的」），解读=活的现行（回答「今天为什么如此」），时效语义不同、执法闸各异（C6 护前者在位，§4 同步护后者常活）；流必须**先于源灭而独立存在**（自足判据）。旧两栖病灶（案卷裁决表/裁决节与独立 ADR Decision Outcome 近同文双写）随判例区撤并入卷**根治**——裁决已无第二个家。**同一事实，过程与裁决归 `changes|archive/`、沉淀道理（现行版，含前因后果）归 `docs/explanation/`——前者的本体是可清册存储，到期彻底抹除，唯沉淀件永续**。

**D-4 归属法 §1「卷宗清册制」注整段替换** ← AC-3/6
- 新文：**卷宗清册制**（历元一清册案立，项目主裁定基调=彻底抹除；2026-09 判例归卷案把清册面收缩为案卷两袋）：两侧 `specs/archive/`（含镜像区，合称**两册**）是**可清册存储**：于发布节点或项目主宣告时，经清册 bill **彻底抹除**——本体整袋删除（禁拆件、禁在位改、禁择留）+ README 账行同裁（README 常驻的是章程，不是账目）+ 活面**全部具体标识符净空**（date-slug 案名、解读篇页脚快照账行——改写为无编号散文或整行删除，行灭理存）。`ADR-NNNN` 编号制已随判例归卷废止，活面**常设零容忍**（见 C6），不在清册触发列内。**自足判据**：抹除前，每案的**前因后果**（立因、取舍、被拒方案之拒因、生效边界）须已完整住进 `docs/explanation/` 或 current/ 条文——判据一句话：**一个从未见过卷宗的读者，能从解读架复述该案全部因果**；未过判据 = 先补沉淀再清册，缺节不得批。存活件仅两项：**解读架的因果全文 + current/ 的条文现行文本**。git 历史**不是**存活件也不是任何层的存在依赖（**法不考古**：知识的权威面永远在工作树）。**清册即重启案数**：案卷归零不携任何编号制（编号已废，无历元可改）；执行清册的 bill 自身亦是案卷，下期清册随袋归零（**仪式产物不豁免于仪式**）。机器执法：C6 两臂——冻结臂=案卷本体在位涂改红（README 豁免）；清册臂=(a) 本体整删仅清册 bill 在途放行、且删后该册必须整册归零，(b) **废号零容忍＋空册自洽**（活面 Markdown `ADR-\d{4}` 永零命中；两册空 ⇒ date-slug 案名零命中）。

**D-5 归属法 §2「设计论证」行** ← AC-4/8
- 旧锚：「ADR 里的「当时思考」不在此行，归下行」「架构决策类于 ⑦⁺ 折叠时刻强制复写入同题解读篇」→ 新：「案卷 §裁决记录里的「当时思考」不在此行，归下行」「架构决策类于 ⑨⁺ 折叠时刻强制复写入同题解读篇」

**D-6 归属法 §2「架构决策（事件与当时思考）」行整行替换（改题「裁决与当时思考（决策事件）」）** ← AC-2/3/5
- 新行：| 裁决与当时思考（决策事件） | 冻结在位，到期整册抹除 → §1 清册制注 | 案卷 `specs/changes|archive/` 内 `specify §裁决记录` 节（门次收据＋裁定＋当时理由＋supersede 宣告；**不设全局编号**，引用 = date-slug 案名＋节名直指） | 案卷外载体引用只准 `→ 案卷 <date-slug> §裁决记录` 一句表决行，**一律纯文本禁超链**（案卷迁移与清册使超链必成鬼）；禁在其余载体复写论证正文（现行版住 `docs/explanation/`——源流非副本，见 §1 两类件分家注）；清册期后：标识符净空、解读篇自足 | C6 冻结臂 + 清册臂（废号零容忍） |

**D-7 归属法 §2「立法/裁决过程事实」行** ← AC-2
- 旧：canonical 列「案卷本体（提案问题单、delta、表决行、tasks 勾验）」→「案卷本体（问题单与裁决记录〔specify〕、delta、tasks 勾验、执行账）」；其余载体列「判例卷宗与 docs 只准指针」→「docs 其余架只准指针」

**D-8 归属法 §3 C4 辖域行** ← AC-1/6
- 旧：「`changes/`、`archive/`、`knowledge/decisions`、`tutorials/` 豁免」→ 新：「`changes/`、`archive/`、`tutorials/` 豁免」

**D-9 归属法 §4「新设计决策」行整行替换** ← AC-2/3/4
- 新行：| 新设计决策 | 有案卷之裁决 → 本案 specify **§裁决记录**落条（表决行＋当时理由＋Confirmation 收据，P-1 形状）；无案卷「修码就法」裁决 → 所涉法卷「生效登记」节一行＋theory-map 账本行（P-2 双载体）；两者 theory-map 账本行必同案齐落 |

**D-10 归属法 §4 瘦身闸行整行替换** ← AC-2/5
- 新行：| 案卷折叠入 archive 前（瘦身闸） | 逐件查 plan（技术形状与修卷 delta）、specify 前段（问题与 AC 账）与 **§裁决记录**：两门收据齐（每案两停全落笔）、每一 Q-x 裁定有表决行与当时理由、supersede 宣告（若有）点名旧案在位裁决；多句裁决理由只准住 §裁决记录（冻结快照），"今天仍成立"部分必经 ⑨⁺ 复写解读架现行版；条文只住 plan、账只住 implement（判据 → §2「裁决与当时思考」「立法/裁决过程事实」两行；旧「先补 ADR」前置随判例区废止） |

**D-11 归属法 §4「案卷折叠时（⑦⁺）」行整行替换（改题 ⑨⁺）** ← AC-4/8
- 新行：| 案卷折叠时（⑨⁺，2026-09 论证沉淀案立、2026-09 判例归卷案换号） | 本案所裁裁决的**沉淀论证同 PR 复写进 `docs/explanation/` 对应解读篇**，篇脚回指 `→ 案卷 <date-slug> §裁决记录（决策快照）`；无同题篇则立篇并按登记法仅登记 docs/README 一处；事务性决策（不新增"为什么"）theory-map 一行账即算沉淀完成（防灌水档）；**存量不溯及、触发式随位收编**；划界判据：法生效后该理由是否仍成立且指导读法——仍成立→解读有现行版，仅当时情境成立→留案卷 §裁决记录作化石 |

**D-12 归属法 §4「卷宗清册执行」行** ← AC-3/6/8
- 旧锚四处：「`specs/archive/` 案卷或 `decisions/` ADR 本体整册归零」→「两侧 `specs/archive/` 案卷整册归零（`decisions/` 区已废并）」「（⑦⁺ 的追溯执行）」→「（⑨⁺ 的追溯执行）」「⑤ **净面**——全活面 `ADR-NNNN`/date-slug 具体标识符删除」→「⑤ **净面**——全活面 date-slug 具体标识符删除（`ADR-\d{4}` 属常设零容忍，不待清册，见 C6）」「不折叠入 `archive/`、不立 ADR（执行非判例）」→「不折叠入 `archive/`（执行非裁决事件，不落 §裁决记录）」；行内仪式步序 ①–⑦（表内自序列）与工序编号无关，不动

**D-13 specs/README 守则 4** ← AC-4
- 旧：「行为断言与 `knowledge/decisions/` 判例互指不复述」→ 新：「行为断言与案卷 §裁决记录（冻结快照）、`docs/explanation/`（现行论证）互指不复述」

**D-14 specs/README 目录表 archive 行** ← AC-4
- 旧：「过程件，决策论证的家在 `decisions/` 与 `docs/explanation/`（归属法 §1 分家注）」→ 新：「过程件（裁决快照住案卷自身 specify §裁决记录，现行论证住 `docs/explanation/`；归属法 §1 分家注）」

**D-15 specs/changes/README** ← AC-2/5/9
- L5 四件枚举 specify 行：「……约束 / 不做 / 裁决节（零实现、零法条正文）」→「……约束 / 不做 / 裁决问句＋**§裁决记录节**（决策事件、当时思考与门次收据唯一居所，裁一条落一条，落笔不改；零实现、零法条正文）」
- L13：「（法面刚性、出口在对话 → ADR-0003）」→「（法面刚性、出口在对话 → 案卷 2026-09-sdd-four-stage §裁决记录）」
- 三写禁绝句：「问题只住 specify、条文正文只住 plan、账只住 implement」→「问题与裁决只住 specify（前段与 §裁决记录各居其节）、条文正文只住 plan、账只住 implement」

**D-16 specs/archive/README** ← AC-4/6/8
- 「入档前义务」段整段换：「入档前义务：瘦身闸（两门收据与各裁决条目全落 specify §裁决记录，形状 → 归属法 §4 瘦身闸行）；⑨⁺ 论证沉淀（现行版复写 `docs/explanation/` 同题篇、篇脚回指 `→ 案卷 <date-slug> §裁决记录（决策快照）`，回指仅活册期有效，清册时整行随灭，行灭理存）——两道闸条文与豁免 → [归属法](../current/patterns/attribution-law.md) §4 强制同步表。」
- 「终态 = 清册归零」段旧锚：「本袋（连同镜像区 archive 与 `decisions/`）」「账行同裁不留痕；清册 = 合法整袋归零」→「本袋（连同镜像区 archive；`decisions/` 区已废并 2026-09 判例归卷案）」「（`ADR-NNNN` 编号零容忍属常设 C6，不待清册）」；「清册 bill 末步自焚、不立 ADR」→「清册 bill 末步自焚」（其余字不动）

**D-17 `_template/specify.md` 全文换装** ← AC-2（新全文 → §4-b）
**D-18 `_template/implement.md` §5 行** ← AC-2
- 旧：「- [ ] 瘦身自查毕（多句理由之判例已立 ADR）」→ 新：「- [ ] 瘦身自查毕（§裁决记录条目全：两门收据齐、各 Q-x 裁定有表决行与当时理由、supersede 宣告（若有）点名旧案）」

**D-19 根 `AGENTS.md`** ← AC-1/4
- 题：「## 四态知识地图（谁听谁的）」→「## 三态知识地图（谁听谁的）」；表删判例行（「| `knowledge/decisions/` | 判例卷宗（历元） | 正文在位不回改；推翻=新立 ADR + supersede 旧文；两册到期彻底抹除、编号改元重启（见归属法 §1 清册注） |」）；specs 行「`archive/` 在位不改、到期整册归零（清册制）」后补「裁决快照随案卷同生死」一句〔读原文嵌〕
- 路由行旧锚：「当年决策→ `decisions/README.md`（先查旧判例再拍新板）」→「旧裁决→ `specs/archive/` 各案卷 §裁决记录＋`explanation/`（先查旧案再拍新板——案卷先行）」；「为什么→ `explanation/`」不动
- 四件套行、伞宣言行内 `decisions` 字样〔施工读原文逐处〕：「业务包契约不入伞，住镜像区…」句若含三区枚举随动

**D-20 伞 `knowledge/README.md`** ← AC-1/4/8（新锚表全文 → §4-c）
- 五问一答行：「三分类：地图（文档跟代码）、法律（代码跟文档）、卷宗（谁也不跟）」→「两分类：地图（文档跟代码）、法律（代码跟文档）——裁决事件随案卷 §裁决记录，不再独立成态」；落在列「`docs/` `specs/` `decisions/` 三区」→「`docs/` `specs/` 两区（案卷住 `specs/changes|archive/`）」
- §1 题「三分类：一区一法律」→「两分类：一区一法律」；表删 decisions 行；scripts 行「守护前三区」→「守护前两区」；「为什么要拆开」段尾随 D-3 判例归卷理由补一句〔读原文嵌，措辞：判例不复独立成区——裁决快照与立法过程同件同封、同袋同灭，别区双写是旧两栖病灶（根治见 2026-09 判例归卷案）〕
- §2.1 叙事段：「带圈编号 ①–⑧ = 立法流程的先后序」→「①–⑩」；「不改主线八步」→「不改主干十步」；涉 decisions/拍板沉淀语重写〔读原文〕；锚表整表替换 → §4-c；SVG/图源两行注不动
- §3 树：删「├── decisions/ 卷宗 | ADR-NNNN 历元编号…」行；specs 行括注补「案卷含 §裁决记录」

**D-21 根 `README.md`** ← AC-1
- 「knowledge/ # 知识伞：docs 地图 / specs 法律 / decisions 判例卷宗 / scripts 执法工具链」→「knowledge/ # 知识伞：docs 地图 / specs 法律（案卷含裁决记录）/ scripts 执法工具链」
- 「架构决策判例卷宗在 [knowledge/decisions/](knowledge/decisions/README.md)」→「历史裁决在 [knowledge/specs/archive/](knowledge/specs/archive/README.md) 各案卷 §裁决记录」

**D-22 `.agents/README.md`** ← AC-4
- 「- 判例 → `knowledge/decisions/`」→「- 旧裁决（案卷先行）→ `knowledge/specs/archive/` 各案 §裁决记录 与 `knowledge/docs/explanation/`」

**D-23 `ywf-ddd-common/AGENTS.md` L9** ← AC-4（新文全文见 D 旧锚替换对）
- 旧：「须在 `knowledge/decisions/` 新立 ADR（改判走 supersede，不偷改旧卷宗）+ 更新 `reference/api/` 类表 + 登记 `knowledge/specs/changes/`。」→ 新：「须经 `knowledge/specs/changes/` 立案（破坏性裁决落该案 specify §裁决记录；推翻旧裁 = 新案点名旧案宣告 supersede，不回改封卷）+ 更新 `reference/api/` 类表。」

**D-24 `.agents/skills/modify-common-module/SKILL.md` 三处** ← AC-4
- L12「破坏性变更立 ADR」→「破坏性变更立案」；L41→「新设计决策 → 本案 specify §裁决记录落条（无案「修码就法」→ 法卷生效登记＋theory-map，→ 归属法 §4）；决策正文不入 api 手册（其 §6「设计决策」已迁出案卷）」；L72→「破坏性公开 API 变更已立案并于 §裁决记录落条（→ `ywf-ddd-common/AGENTS.md`）」

**D-25 `.agents/skills/new-bill/SKILL.md`** ← AC-2/3/4/5/8〔工序属流程件，按 C7 形状零法条〕
- 前置阅读四件套行随模板措辞；步骤 1 表行「| 记一个新的设计决策 | 不是法案——走 `knowledge/decisions/` 新立 ADR（卷宗法另成程序） |」→「| 记一个新的设计决策 | 有案卷 → 本案 specify §裁决记录落条；无案卷「修码就法」→ 法卷生效登记＋theory-map 账本行（→ 归属法 §4）；不另立区 |」
- 步骤 2 尾补「拍板前翻旧案 = 查 `archive/` 各案 §裁决记录与 `docs/explanation/`（案卷先行，2026-09 判例归卷案）」
- 步骤 3 specify 职责句补「§裁决记录（裁一条落一条）」；三写禁绝句 → D-15 同形
- 步骤 4：「（法条刚性、出口在对话 → ADR-0003）」→「→ 案卷 2026-09-sdd-four-stage §裁决记录」；补「两门收据均落 §裁决记录（门次＋日期＋原语摘录）」
- 步骤 6.2 清册自焚括注「该 bill 亦不立 ADR，执行非判例」→「执行非裁决事件」；6.4 瘦身自查 → D-10 同形指针；6.5 题与内文 ⑦⁺→⑨⁺、回指形 → D-11
- 反例表：涉「判例」行随 D-10 措辞；新增反例行「裁决散记在 plan/implement 对话摘要里（→ 唯一住 §裁决记录）」

**D-26 `docs/reference/doc-guards.md`** ← AC-6
- L3 辖域段旧锚「C6 只扫两册卷宗区〔decisions + 两侧 archive，历元制后扩〕」→「C6 只扫案卷区〔两侧 archive〕＋全活面废号零容忍（`ADR-\d{4}` 永禁，2026-09 判例归卷案）」
- L35 豁免枚举删 `decisions/`；C6 行（L46）机制列按 P-7 全重写（冻结臂：两侧 archive 本体在位 minus 即红，README 豁免、Status 对替豁免废止；清册臂(a)：册枚举缩短；臂(b)：废号常设臂 + 空册自洽两支；变红样例：「改旧案卷正文 = 冻结臂红；活面残留 `ADR-00NN` = 废号臂红」）；L47「案卷 历元一工位案 L2」系史叙事无编号，不动

**D-27 `knowledge/scripts/check-docs.ps1`** ← AC-6/9（P-7 之逐处执行量）
- L55 `$decDirs = @('knowledge/decisions', 'docs/adr')` 及一切引用删；`$dossierZones` = 两侧 archive 常量；`Get-DossierBodyNames` 非 archive 分支（`ADR-*.md` 枚举）删；冻结臂 `isDec`／Status 豁免分支删（两型消息合一为「案卷本体在位不改」）；`$adrIdRx` 保留、arm(b) 改常设：`$idTargets`（全 md 减 $zoneSelfRx）中命中即红，脱离 `$decEmpty` 触发；`$zoneSelfRx` 中 `decisions|` 与 `^docs/adr/` 备选删（changes/archive 历史语豁免面**保留**——本案卷自身亦在其内，属过渡豁免面）；info 行字段随动（`decisions=` 项删）；头部与注释史行随动；SelfTest r5 保留

**D-28 `diagrams/knowledge/README/drive-relations.d2` 全文换装** ← AC-7/9（新全文 → §4-a；施工按 doc-guards 判例五诫：编辑工具改、BOM 剥、near:center 全钉、网格键名、render→check 连跑）

**D-29 `docs/reference/glossary.md`** ← AC-1/3/4/8
- 知识伞行「三类知识三个法律区」→「两类知识两个法律区」；诸区分野行改题「诸区分野（三态）」：「地图 / 契约（法律）/ 判例卷宗 / 方法」→「地图 / 契约（法律）/ 方法（裁决随案卷 §裁决记录）」
- 「ADR（判例）」词条 **REMOVED**（原因：判例区废止）；新词条「§裁决记录（案卷）」：案卷级决策事件唯一居所——门次收据＋裁定与当时理由＋supersede 宣告；落笔不改，随案入 archive、清册同灭（行灭理存）｜→ `_template/specify.md`、归属法 §2「裁决与当时思考」行
- 「立法八步（①–⑧）」→「立法十步（①–⑩）」枚举全换（→ §4-c 序）；「决策快照回指」canonical 与例形换（`→ 案卷 <slug> §裁决记录`；法源行名 ⑨⁺）；「判例先行」改写（翻 archive §裁决记录＋解读架；旧案撞案=新案 §裁决记录宣告 supersede）＋改题「案卷先行」；「论证沉淀」③⁺⑦⁺→③⁺⑨⁺；「卷宗清册制」两册定义随 D-4；「清册即改元」**REMOVED**（原因：编号制已废，无元可改；语义并入「卷宗清册制」行）

**D-30 `docs/explanation/theory-map.md`** ← AC-3/9
- L140「（R11 严格化，2026-09，ADR-0002）」→「（R11 严格化，2026-09，「修码就法」无案裁决——见 OL/WC 卷生效登记）」；知识体系节各行：L185 题与文 → D-3 两类件形（旧「三类件分家」留括注为前史）；L186 尾「ADR-0003 决策快照」→「案卷 2026-09-sdd-four-stage §裁决记录」；L187 两册定义与「编号历元重启」clause 删；L188「历元二首案 ADR-0001」→「案卷 2026-09-archunit-rule-doc（该案裁决记录）」
- 「## ADR 总索引（已迁出）」节整节替换：「## 裁决索引（案卷先行）｜活的裁决 = `specs/current/` 法卷条文与 `docs/explanation/` 现行论证；历史裁决 = `specs/archive/` 各案卷 specify §裁决记录（随册清册归零，法不考古——无总索引、无旧号映射，跨期同号歧义不存在，因编号制已废）。本账本仅登理论模式与治理制度的采纳/未采纳裁决（不复述案卷——归属法）。」

**D-31 `docs/explanation/knowledge-system.md` 随法修订** ← AC-4/10〔史叙事段允许提及 ADR/卷宗之名（无编号），现行制度段全换〕
- 题「（三类件：过程、事件、道理）」→「（两类件：过程含裁决、道理）」；L3「`docs/specs/decisions` 三区」→「`docs/specs` 两区」；「一个决策事实的三份分身」节 →「一个决策事实的两份分身」（表删决策事件件行，其「谁在何时授权」并入过程件行 specify §裁决记录位；「三者不冗余」段改二者源流文）
- 「过程件内部为什么再分四段」节：判据句补裁决节四归一居（P-1 形）；L23 尾「ADR-0003 决策快照」→「案卷 2026-09-sdd-four-stage §裁决记录」；「为什么"当时思考"不能冒充"现行道理"」节内「留在卷宗当化石」→「留在案卷 §裁决记录当化石」；L29「每次 ⑦⁺ 折叠」→「每次 ⑨⁺ 折叠」；清册制节：「两册」定义→案卷两袋（decisions 入「立因」史叙事）；L55「瘦身闸、⑦⁺、supersede 旧制全部原样有效」→「瘦身闸、⑨⁺、supersede（案卷链）原样有效」；L57 页脚随动；**本案 ⑨⁺ 义务：本篇即同题解读篇，折叠当刻把本案裁决的现行论证（判例为何不再是独立态）复写入分身节，页脚回指本案卷 §裁决记录**

**D-32 `docs/README.md`** ← AC-1/4
- 题注「契约在 `../specs/`、判例卷宗在 `../decisions/`——法律与卷宗**不登本索引**」→「契约（含封存案卷与其 §裁决记录）在 `../specs/`——法律**不登本索引**（区自持索引，防双登记）」
- 解读架行「〔⑦⁺ 复写，快照留 `decisions/`〕」→「〔⑨⁺ 复写，快照住案卷 §裁决记录〕」

**D-33 解读三篇就近复质** ← AC-9
- `architecture-rules.md`：L36/L129 涉「ADR-0001」处 → 「案卷 2026-09-archunit-rule-doc §裁决记录」形；`security.md` L17「空号判例」散文留（无编号）；`domain.md` L88 涉判例载体语〔读原文随 D-3〕
- **D-34 `reference/api/common-*.md` ×8** ← AC-9：「旧号映射见该文 §migration」小句整删（该节不存在的假宣称；映射随判例区废止永不建立——「历元一编号随史卷宗俱灭，法不考古」一句散文可留〔各篇同形〕）
- **D-35 how-to 三处** ← AC-4：`scheduled-task.md` L16 旧案判例 → decisions 指针改指法卷生效登记/theory-map；`testing.md` L22「判例：…」散文形留、指针改；`how-to/README.md` L33「解读区与判例卷宗」阅读建议 → 「解读区与封存案卷」
- **D-36 镜像区六件** ← AC-10（P-8）：`sample-application/specs/README.md` L11、`changes/README.md` L7、`archive/README.md` L3/L5、`current/order.md` L94/120/122/128/129、`current/product.md` L58/61——「卷宗/旧案/decisions」指针逐处复质为镜像区 archive 案卷 §裁决记录或本区法卷；业务法不引框架判例区（辖域）
- **D-37 代码注释五处（零行为）** ← AC-9：`RestAdapter.java` L21 重合同义裸号「ADR-0003」→ 删号留「重契约」（或指 contract 法卷锚）；`AuditProperties.java` L12/L40 「ADR-0033」→ 指 observability 法卷；`sample-service-server/pom.xml` L50 「ADR-0027 判例」→ 散文「历元旧案」；`DddArchitectureRules.java` L351/L383 与 `TransactionBoundaryRuleProofTest.java` L16 「ADR-0002」→「2026-09 修码就法裁决（无案卷，OL/WC 卷生效登记）」〔施工读原文嵌，注释行内零 Java 语义变更〕

**D-38 区体删除** ← AC-1
- `git rm -r knowledge/decisions/`（ADR-0001/0002/0003/README 共 4 文件），与 D-27 换臂同 commit（P-3 器法一刀）；删后除本案卷（changes/ 历史语豁免面）外全仓 `knowledge/decisions` 路径字样零命中

### REMOVED 汇总（折叠时归档记录一句原因）
D-1 判例区行、D-29 ADR 词条与「清册即改元」词条、D-30 ADR 总索引节（旧形态）、D-31 决策事件件分身、D-38 区本体——原因一律「判例归卷：裁决记录住案卷 specify §裁决记录，编号制废止无继承」。

## §3 波及面总账与豁免

**法案义文件 ≈ 46**：归属法（D-1..12）、三章程 README（D-13..16）、模板两件（D-17/18）、路由与伞（D-19..22）、skills×2（D-24/25）、执法器（D-26/27）、图（D-28）、字典（D-29/32/34）、解读（D-30/31/33）、how-to×3（D-35）、镜像×6（D-36）、代码注释×5（D-37）、区删（D-38）。ddd-review skill 经 grep 确认无判例字样（零触面，留档）。折叠日先全仓 `grep -E 'ADR-|decisions|判例|卷宗|四态|八步|⑦⁺|③⁺|清册即改元|立法八步'` 复扫，表外命中逐个裁决入 D 表或豁免清单——**账外露红即施工失败**。

**泛称豁免判据（一字不动）**：① 喻义/泛文史「判例」——「判例法」（归属法 §6 行）、「施工判例五条」「判例否决」（doc-guards 工具史语）、「判据/判例：BOM」「历元旧案」散文；② explanation 史叙事段提及 ADR 之**名**（无编号）——讲制度前史合法，提编号即犯 AC-9；③ 本案卷自身（changes/ 历史语豁免面）；④ 「三件套」代码构件组泛称（CQE 三件套等，前案豁免清单原样延续）。所指为**判例区制度义**者随动，为**史话/喻义**者豁免。

**回退**：折叠前任一门否决 → 审议稿留 changes/ 零痕迹；折叠后撞法 → 新立案 supersede（案卷链），不回改。

## §4 执行量全文

### a) `drive-relations.d2` 新全文（折叠日整文件替换，render 重刷 SVG+manifest）

```d2
direction: right

# 发起与裁决同人：开发者 = 项目主（图外无第三方）；四段 = changes 容器内四块独立段位，
# 工序边精锚到段：裁决写在哪、delta 从哪起量、账在哪回填，图上直读（2026-09 判例归卷案：判例不再独立成区）
dev: {
  label: "开发者"
  role: "立案意图（新增行为/现实撞法）\n批准门（批准/否决/拍板问句）"
}

code: {
  label: "code 代码库（源码树 = 事实）"
  repo: "ywf-ddd-java"
}

specs: {
  label: "specs/ 三身份"
  changes: "changes/ 审议稿（唯一立法通道）" {
    specify: "specify\n问题 + AC 账\n§裁决记录（裁定快照+门次收据）"
    plan: "plan\n技术形状 P-x\n§修卷 delta（条文唯一居所）"
    tasks: "tasks\n纯执行清单"
    implement: "implement\n执行账 + 取证 + 收官闸"
  }
  current: "current/ 法卷\n（主内容·严格件）"
  archive: "archive/ 存档案卷\n（在位不改·到期整册归零）" { style.stroke-dash: 3 }
}

docs: {
  label: "docs 四象限（每架一驱动）"
  grid-columns: 2
  grid-rows: 2
  grid-gap: 45
  tutorials: "教程架(tutorials)：学习 × 动手"
  how-to: "设计卡架(how-to)：工作 × 动手"
  explanation: "解读架(explanation)：学习 × 认知\n（论证 canonical 库·被全系统引用）"
  reference: "字典架(reference)：工作 × 认知"
}

# ── 立法流程 主干①–⑩（编号=先后序；全量语义在 README 锚表，图上只留工序名；门回合一箭双号，
#    同端点同向平行边是 TALA 标签漂移病灶——旧案学费，见 doc-guards 图管线节）──
# near: center = 标签钉死本线中点
dev -> specs.changes.specify: "① 立案起草（Specify：做什么/验收）" { near: center }
specs.changes.specify <-> dev: "② Specify 门送审 · ③ 裁定落笔（裁先翻 archive 案卷§裁决记录与解读架；裁定写进 §裁决记录）" { near: center }
specs.changes.specify -> specs.changes.plan: "④ 裁定入稿（裁决转写 P-x 与 delta 草稿）" { near: center }
specs.changes.plan <-> dev: "⑤ Plan 门送审（技术形状+修卷量） · ⑥ 放行裁定（批准=开工，收据落 §裁决记录）" { near: center }
specs.changes.tasks -> code: "⑦ 按约施工（完成即勾）" { near: center }
code -> specs.changes.implement: "⑧ 取证回填（账+delta 源回填+漂移回门）" { near: center }
specs.changes.plan -> specs.current: "⑨ 折叠落实（delta 起量）" { near: center }
specs.changes -> specs.archive: "⑩ 归档整袋（瘦身闸）" { near: center }
# 论证沉淀=挂在时刻上的分支义务（上标⁺，不改主干十步）：③⁺ 裁定当刻 theory-map 账本行（与 §裁决记录同案）；
# ⑨⁺ 折叠当刻沉淀论证复写入同题解读篇、篇脚回指案卷 §裁决记录——
# 案卷=冻结的事件与当时思考，解读=活的现行道理，重叠是源流非双写（归属法 §1 两类件分家）
specs.changes.specify -> docs.explanation: "设计（修改与决策构成一次设计）驱动文档修改：论证沉淀\n③⁺ theory-map 账本行\n⑨⁺ 同题散文强制复写（解释立法原因）" { near: center }

# ── 四象限静态驱动源（无编号 = 常态归属，非流程）──
code -> docs.tutorials: "代码驱动文档修改" { near: center }
code -> docs.reference: "代码驱动文档修改" { near: center }
specs.current -> docs.how-to: "法卷驱动文档修改·宽松副本" { near: center }

# ── 解读架引用面（淡虚线 = 阅读时引用，非驱动；时效仍经论证中转）──
docs.how-to -> docs.explanation: "引用·原理槽" { style.stroke-dash: 3; style.opacity: 0.4 }
docs.reference -> docs.explanation: "引用·见行" { style.stroke-dash: 3; style.opacity: 0.4 }
docs.tutorials -> docs.explanation: "引用·延伸" { style.stroke-dash: 3; style.opacity: 0.4 }
specs.current -> docs.explanation: "引用·论证指针" { style.stroke-dash: 3; style.opacity: 0.4 }
```

### b) `_template/specify.md` 新全文（折叠日换装）

```
# <变更一句话标题>
- Slug: <YYYY-MM-slug> ｜ 日期: <YYYY-MM-DD>
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
## §裁决记录（裁决唯一居所：门次收据与拍板快照；裁一条落一条，落笔不改）
- 门 <Specify 门/Plan 门> ✅/❌ <YYYY-MM-DD｜项目主原语摘录> ｜漂移回门：<日期+门次/无>
- Q1 裁 <项> ｜当时理由：<浓缩快照，允许多句；"今天仍成立"部分 ⑨⁺ 复写解读架> ｜落点 → <AC-n/P-x> ｜supersede：<案卷 <date-slug> §裁决记录 Qn / 无>
```

### c) `knowledge/README.md` §2.1 新锚表（折叠日整表替换；导语段按 D-20 换 ①–⑩）

| 边（编号=工序，一箭双号=门回合；无编号=常态） | 法源锚 |
|---|---|
| ① 开发者 → changes.specify「① 立案起草（Specify：做什么/验收）」 | 立案发起人是**开发者的意图**；工序锚：`new-bill` 步骤 1–3（归属法 §4 触发行逐判；模板 `_template/`） |
| ②③ changes.specify ⇄ 开发者「② Specify 门送审 · ③ 裁定落笔」 | `new-bill` 步骤 4：送审硬停；**裁先翻旧案**（archive 各案 §裁决记录＋explanation——案卷先行，旧「判例先行」收编于此边注）；裁定写进 specify §裁决记录（表决行＋当时理由＋门次收据 → 归属法 §2「裁决与当时思考」行） |
| ④ specify → plan「④ 裁定入稿」 | 审议期纪律：裁决转写 P-x 与 delta 草稿（步骤 3/5）；门验案卷形状与修卷量（2026-09 四段案卷法＋判例归卷案） |
| ⑤⑥ plan ⇄ 开发者「⑤ Plan 门送审 · ⑥ 放行裁定」 | 步骤 4 批准门：门分档 Specify→Plan、每案两停不论大小；法面刚性、出口在对话（→ 案卷 2026-09-sdd-four-stage §裁决记录；收据回 §裁决记录） |
| ⑦ tasks → code「⑦ 按约施工（完成即勾）」 | 步骤 5：tasks 驱动、完成即勾（施工账本唯一载体 implement） |
| ⑧ code → implement「⑧ 取证回填（账+delta 源回填+漂移回门）」 | 步骤 5–7：勾账＋按变更性质定档全绿；plan §delta 每条 SHALL 之源在折叠前经 implement §3 回填为真实 文件:行/测试名 |
| ⑨ plan → current「⑨ 折叠落实（delta 起量）」 | 步骤 6.1（ADDED 入位/MODIFIED 整节替换/REMOVED 删节留因）= 归属法 §4 文档同步义务**唯一时点** |
| ⑩ changes → archive「⑩ 归档整袋（瘦身闸）」 | 步骤 6.2 整目录 `git mv` 不拆件 + 6.3 在位不改 + 6.4 瘦身闸（→ 归属法 §4 瘦身闸行新形） |
| **③⁺/⑨⁺ 分支** changes.specify → 解读架「论证沉淀」 | ③⁺ 裁定当刻 theory-map 账本行（与 §裁决记录同案 → 归属法 §4「新设计决策」行）；⑨⁺ 折叠当刻沉淀论证强制复写同题解读篇、篇脚回指 `→ 案卷 <slug> §裁决记录（决策快照）`（→ 归属法 §4「案卷折叠时（⑨⁺）」行）；旧「decisions → 解读架」边随判例区废止 |
| （沿革注）旧八步 ①–⑧/③⁺/⑦⁺ | 2026-09 判例归卷案换号：映射表住该案 plan §1 P-4；历元一/二诸旧案卷内八步语=历史语不回改 |

### d) `_template/implement.md` §5 换行 → D-18；`drive-relations.svg` 由 render-diagrams.ps1 重刷（禁手改），manifest 自动更新。

## §5 影响面与回退
- 影响：零 Java 行为变更（触码=注释措辞五处＋pom 注释一处，AC-9）；外部读者得「三态＋案卷含裁决」新世界观与十步新编号。
- 验证闸（本案=触流程件＋执法器＋注释措辞案）：check-docs 七闸＋`-SelfTest`（ST5 复用废号正则）＋check-diagrams＋render 成功＋`mvn -B compile` 负证明（注释改动零意外破坏编译）。
- 回退：未折叠=法未动，撤审议稿即净；折叠后撞法=新案 supersede（案卷链），不回改封卷。
