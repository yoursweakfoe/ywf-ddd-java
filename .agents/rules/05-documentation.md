# 05 — 文档维护与事实归属法

> 本文件是「知识伞」的宪法：每类事实恰好一个 home，其余载体只允许「一句规范 + 路径指针」。


## 1. 四态分野（一目录一法律）

| 位置 | 态 | 真相源 | 冲突裁决 | 写入时钟 |
|---|---|---|---|---|
| `knowledge/docs/` | 描述（地图） | 代码 | 与代码不符 = 文档是 bug | 代码之后，被动跟随 |
| `knowledge/specs/` | 契约（法律） | 意图/约定 | 与代码不符 = 二者之一必修；改法必须走 `changes/`，禁止迁就代码偷改 | 代码之前，发起者主动 |
| `knowledge/decisions/` | 判例卷宗 | 过去的决策事件 | 正文永不回改；推翻 = 新立 ADR + 旧篇仅 Status 行变 superseded | 决策定稿当刻 |
| `.agents/` | 方法 | 不适用（约束 worker） | 不腐烂、不被违反，只被执行 | 观察犯错后追加 |

伞 `knowledge/` 与区 `docs|specs|decisions` 本身不立法；方法树拒入伞（工具可弃，知识/契约不可弃）。

## 2. 事实归属表

| 事实类别 | 易变性 | canonical home | 其余载体允许形式 | 机器背书 |
|---|---|---|---|---|
| 包路径/类名/方法签名 | 高 | 源代码本身 | 一句规范 + `→ 见 path`；**md 禁手抄结构树** | check-docs C1/C3 |
| 异常→HTTP 映射、错误码格式 | 高 | `GlobalRestExceptionHandler` javadoc（代码）+ `reference/api/common-exception.md` §2 表（docs 唯一法源） | 一行 + 指针 | C5 对账（表行↔处理器覆盖） |
| 结构速查（树图） | 高 | `reference/structure.md`（**生成物，禁手改**） | 指针 | 重跑生成脚本即对齐 |
| 代码模板（教学用） | 中 | `how-to/` 篇内联——业务无关独立教例，永不挂钩 sample 源码；命名中立 `{agg}` 占位或虚构聚合，禁业务词 | skill 指针引用，**skill 内零模板** | C4 扫描 + C2 计数 |
| 规范行（必须/禁止） | 中 | rules/（本目录） | 一句复述 + 指针；AGENTS.md 九条例外 | 每行挂 R##/测试名 |
| 设计论证（为什么） | 低 | `explanation/` | 指针 | —（低易变，允许就近重述） |
| 架构决策 | 冻结 | `decisions/`（全局编号） | `ADR-NNN` 限定名引用 | C6 diff-scope + Confirmation 节必填 |
| 需求/行为规格 | 随变更 | `specs/capabilities/`（真相）+ `changes/`（工作区） | skill 首步产出 delta | 归档折叠 = 同步义务唯一时点 |
| 任务流程（顺序+清单） | 低 | `skills/`（≤500 行） | rules 指过来 | C2/C3 |

**易变性分层**：低易变教义就近重述是有益冗余（agent 执行时看得见）；高易变事实重述是纯债——只禁后者。

## 3. 教学中立条款（D4 教义）

- 教学文档是通用抽取不是业务镜像：代码围栏与反引号路径/类名内禁 `order/Order/product/Product`；通配 `{agg}`/`{Agg}` 或虚构教例家族（现行两族：Payment=新建聚合教例、Reservation=读写链路教例），虚构首现标「虚构教例，sample 未实现」。`tutorials/` 为真实例操作手册，业务词豁免。
- **真实例指针位**（代码块外 + 标注）是全仓业务名合法位之一。
- 辖域：C4 仅扫 `knowledge/docs` + `.agents`；**`knowledge/specs`、`knowledge/decisions`、`tutorials/` 豁免**（契约与判例天然记业务）。
- ~~「代码示例必须与 sample 实际实现保持一致」~~ → 新文：「模板与框架 API 自洽即可」；整段编译验证明文不做（残余风险接受，判据：D4 裁决）。

## 4. 强制同步规则（伞化修订）

| 触发 | 必须动作 |
|---|---|
| 改 `GlobalRestExceptionHandler` 映射 | 同 PR 改 `reference/api/common-exception.md` §2 表（C5 会红） |
| 改 `MybatisPersistence` 通道行为 | 同 PR 改其 javadoc + `reference/api/common-ddd.md` §2 |
| 重构本树包结构（sample/common） | 当天按 check-docs C1/C3 报告修全部指名文档行 |
| 新增/变更聚合行为 | 先立 `knowledge/specs/changes/<slug>/` 三件套；归档折叠 `capabilities/` |
| 新设计决策 | `decisions/` 新立 ADR（MADR 骨架，Confirmation 必填）；`explanation/theory-map.md` 账本登记 |
| 新增文档 | 仅登记 `knowledge/docs/README.md` 一处（唯一索引；`knowledge/README.md` 只写三态法律） |


## 5. i18n 错误码管理（沿用）

- 格式 `{aggregate}:err.{scene}`；messageKey 是前端渲染位点，服务端不维护 messages.properties；清单唯一登记处 = `knowledge/docs/how-to/error-handling.md`（本条为指针；禁硬编码可读文案作 key）。

## 6. 防复发

- `scripts/check-docs.ps1` 六校验：PR 门跑 changed-files-only、夜间全量；`ddd-review` 末步必跑，非零即返工。
- 豁免清单外置于 `scripts/check-docs.whitelist.txt`，**新增豁免须 PR 评审并写理由**——防白名单膨胀复现本文件要治的病。
- 判例法：被 check-docs 抓过 / 审计定过性的写法，在归属表增行，不另发明第五种载体。
