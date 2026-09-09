# 用法规范法卷：归属法（知识系统元法 · 严格件）

> **身份**：本卷是知识系统的**宪法**——每类事实恰好一个 home，其余载体只准「一句规范 + 路径指针」。约束对象是文档与代码的维护行为本身，违反=按本卷裁决修容器。前身 `.agents/rules/05`，2026-10「法律入典」改革升格至此（法案 `2026-10-rules-codification`；元法亦是法，住 specs，走同一立法程序——体系里没有不该被程序修改的宪法）。
> **机器对账**：本卷诸行判据由 `knowledge/scripts/check-docs.ps1` 七校验执行（[说明](../../../docs/reference/doc-guards.md)）。

## 1. 诸区分野（一目录一法律）

| 位置 | 态 | 真相源 | 冲突裁决 | 写入时钟 |
|---|---|---|---|---|
| `knowledge/docs/` | 描述（地图） | 代码 | 与代码不符 = 文档是 bug | 代码之后，被动跟随 |
| `knowledge/specs/` | 框架契约（法律） | 意图/约定 | 与代码不符 = 二者之一必修；改法必须走 `changes/`，禁止迁就代码偷改 | 代码之前，发起者主动 |
| `knowledge/decisions/` | 判例卷宗 | 过去的决策事件 | 正文永不回改；推翻 = 新立 ADR + 旧篇仅 Status 行变 superseded | 决策定稿当刻 |
| `sample-application/specs/` | 契约镜像（业务法） | 意图/约定（生意） | 同框架法；业务名只在本树合法（契约描述生意，教学描述通识） | 代码之前，发起者主动 |
| `.agents/` | 流程（SOP） | 不适用（约束 worker 的手法） | 不腐烂、不被违反，只被执行；**法不住本树**（2026-10 法律入典，本树只余 skills；根 AGENTS.md 九条为唯一合法的压缩复述位） | 观察犯错后追加流程步骤 |
| `knowledge/scripts/` | 执法（工具链） | 不适用（不描述事实，守护事实） | 文档红=修文档或修工具，按"磁盘实证"裁决；说明唯一入口 = docs 架 `reference/doc-guards.md` | 随探测器演化 |

伞 `knowledge/` 与区 `docs|specs|decisions|scripts` 本身不立法；方法树拒入伞（工具可弃，知识/契约不可弃）。执法工具链居伞内 `knowledge/scripts/`（2026-09-06 内聚裁定）：辖域是每区各自的法律、不是物理相邻。

**案卷与判例分家**（2026-09 `2026-09-archive-dossier-boundary`）：`specs/changes|archive/` 的案卷 = **立法过程件**——问题陈述、条款变化量（delta）、表决行、施工勾验；折叠即使命终、入档封存。`decisions/` 判例卷宗 = **决策论证件**——context / 取舍 / consequences，跨时间被引（新判例前查旧案、supersede 靠旧论证活着）。两栖病灶（本行立前实证：案卷 proposal 裁决表与 ADR Decision Outcome 近同文双写）的裁决一句话：**同一事实，论证归卷宗、过程归案卷**。

## 2. 事实归属表

| 事实类别 | 易变性 | canonical home | 其余载体允许形式 | 机器背书 |
|---|---|---|---|---|
| 包路径/类名/方法签名 | 高 | 源代码本身 | 一句规范 + `→ 见 path`；**md 禁手抄结构树**（包树地图不设二手——2026-09 structure.md 注销判例） | check-docs C1/C3 |
| 异常→HTTP 映射、错误码格式 | 高 | `GlobalRestExceptionHandler` javadoc（代码）+ exception 法卷 EV 系（承诺）+ `reference/api/common-exception.md` §2 表（描述镜像） | 一行 + 指针 | C5 对账 |
| 服务/聚合结构组成 | 高 | `specs/current/patterns/aggregate-blueprint.md`（§1 槽位清单 + §5 通式） | 指针 | C1 |
| 用法规范/规范代码形状（严格件） | 中 | `specs/current/modules/*.md` + `specs/current/patterns/*.md`——条款+取证源+全套规范形状（全仓唯一样本） | docs 同题=设计卡：判据/决策点/边界+指针，**零形状代码**、禁条款编号；冲突法卷赢 | C1/C3/C4 |
| 规范行（必须/禁止） | 中 | 法卷（`specs/current/`——[prohibitions](prohibitions.md) 为禁令对照表，[coding-conventions](coding-conventions.md) 为公约） | 一句复述 + 指针；AGENTS.md 九条例外 | 每行挂 R##/测试名 |
| 设计论证（为什么） | 低 | `docs/explanation/`（含 theory-map 理论账本） | 指针 | —（低易变，允许就近重述） |
| 架构决策（论证） | 冻结 | `decisions/`（全局编号） | `ADR-NNN` 限定名引用；**案卷涉已立 ADR 之决策只准表决行（何人何时批准/否决/裁定，至多附一句因）+ 指针，禁复写论证正文** | C6 diff-scope + Confirmation 必填 |
| 立法/裁决过程事实 | 随案卷 | `specs/changes|archive/` 案卷本体（提案问题单、delta、表决行、tasks 勾验） | 判例卷宗与 docs 只准指针；折叠前过瘦身闸（§4 新行） | ddd-review 人肉抽查（机器化暂缓——语义比对超 C 闸能力，判据同实施内容基准律注） |
| 需求/行为规格 | 随变更 | 框架：`knowledge/specs/`；示例业务：`sample-application/specs/`（同构镜像区） | skill 首步产出 delta | 归档折叠=同步义务唯一时点；C2/C3 扫两区 |
| 任务流程（顺序+清单） | 低 | `.agents/skills/`（≤500 行） | 法卷指过来；**skill 内零法条零模板**（D6 推广）；形状性内容必须锚定法卷节号——**实施内容基准律**，见下表注 | C2/C3 + ddd-review 锚点抽查 |
| 防腐工具行为与用法 | 中 | `knowledge/scripts/` 代码本体（行为即法）+ `reference/doc-guards.md`（说明唯一入口） | 一行 + 指针 | 人肉跑主机 + ddd-review 末步 |
| 术语 → 身份定位 | 中 | `docs/reference/glossary.md`（只准「一句定位 + 指针」，禁定义复述） | 指针 | — |
| 元法（本卷：事实归属本身） | 低 | 本卷 | 一句守则 + 指针（伞 README / docs README 各留一行） | — |

**易变性分层**：低易变教义就近重述是有益冗余；高易变事实重述是纯债——只禁后者。

**实施内容基准律**（2026-09-07 用户裁定）：单一事实源不止于「禁止抄法」（D6 负面义务）——skill 与 docs 内一切**可机械化内容**（代码形状、包结构位、注解、签名、文件槽位）必须是指针，锚定 `specs/current/` 法卷或镜像区法卷的**某卷某节**；**锚不到法源 = 法卷覆盖缺口，走 `changes/` 立案补法，禁止就地自造形状**（正面义务，spec 为实施内容的顶层基准）。流程性内容（顺序、判据、工序、验收动作）不可机械化，住 skill 本树自带权限、不受此律——否则法卷被步骤污染、工作台被降格成链接目录，两头都失去宽严双份的本意。界线仍是那把尺子：这句话能机械化执行吗。检查落点 = `ddd-review` 文档交付约之锚点抽查；机器全自动化暂缓（锚点解析度不足，硬做闸=误伤机），攒案例后再议。

## 3. 教学中立条款（D4 教义）

- 教学文档是通用抽取不是业务镜像：代码围栏与反引号路径/类名内禁 `order/Order/product/Product`；通配 `{agg}`/`{Agg}` 或虚构教例家族（现行两族：Payment=新建聚合教例、Reservation=读写链路教例），虚构首现标「虚构教例，sample 未实现」。`tutorials/` 为真实例操作手册，业务词豁免。
- **真实例指针位**（代码块外 + 标注）是全仓业务名合法位之一。
- 辖域：C4 扫 `knowledge/docs` + `.agents` + `knowledge/specs/current/`（法卷是严格件必须中立形状）；`changes/`、`archive/`、`knowledge/decisions`、`tutorials/` 豁免（案卷记录当时语）。业务契约居 `sample-application/specs/`——扫描面外，业务合法性由辖域保证；框架法卷依法用框架词汇。
- 「模板与框架 API 自洽即可」（不整段编译验证，残余风险接受，判据：D4 裁决）。

## 4. 强制同步规则

| 触发 | 必须动作 |
|---|---|
| 改 `GlobalRestExceptionHandler` 映射 | 同 PR 改其 javadoc + `reference/api/common-exception.md` §2 表 + exception 法卷条款（C5 会红） |
| 改 `MybatisPersistence` 通道行为 | 同 PR 改其 javadoc + `reference/api/common-ddd.md` §2 + OL/BP 相关条款 |
| 重构包结构（sample/common） | 当天按 check-docs C1/C3 报告修全部指名文档行 |
| 新增/变更**框架行为** | 先立 `knowledge/specs/changes/<slug>/` 三件套；归档折叠本区 `current/` |
| 新增/变更**示例业务聚合行为** | 先立 `sample-application/specs/changes/<slug>/` 三件套；归档折叠本区 `current/<agg>.md` |
| 修订法卷条款 | 同 PR 核对 docs 同题设计卡无矛盾；有矛盾改 docs（法卷赢） |
| 新设计决策 | `decisions/` 新立 ADR（MADR 骨架，Confirmation 必填）；`explanation/theory-map.md` 账本登记 |
| 案卷折叠入 archive 前（瘦身闸） | 逐件查 proposal/delta：凡多句决策理由而其判例未立 → 先补 ADR 再折叠；案卷裁至表决行 + `→ ADR-NNN` 指针（判据 → §2「架构决策（论证）」「立法/裁决过程事实」两行） |
| 新增文档 | 仅登记 `knowledge/docs/README.md` 一处（唯一索引；伞 README 只写三态法律） |
| 新增规范行（必须/禁止） | 只能进法卷（走 changes/ 程序）；`.agents/` 内禁止出现裸法条——「方法区不载地图亦不载法」 |

## 5. i18n 错误码管理

- 格式 `{aggregate}:err.{scene}`；messageKey 是前端渲染位点，服务端不维护 messages.properties；条款 → [EV-2](../modules/exception.md)；全仓 key 清单唯一登记账本 = `knowledge/docs/how-to/error-handling.md`（登记簿居设计卡，形状条款在法卷）；禁硬编码可读文案作 key。

## 6. 防复发

- `knowledge/scripts/check-docs.ps1` 七校验（说明唯一入口 `docs/reference/doc-guards.md`）：PR 门 changed-files-only、夜间全量；`ddd-review` 末步必跑，非零即返工。
- 豁免清单外置 `knowledge/scripts/check-docs.whitelist.txt`，新增豁免须 PR 评审写理由——清单只删不增。
- 判例法：被 check-docs 抓过 / 审计定过性的写法，在归属表增行，不另发明新载体。
