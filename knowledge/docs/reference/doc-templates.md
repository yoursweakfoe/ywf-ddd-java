# 文档体裁模板集（生态位地图）

> **这是什么**：全仓文档体裁（生态位）的登记页——有哪些体裁、模板底稿各住在哪个 `_template/`、现有文档还有哪些形状债。模板本体按四件套 `_template/` 的同款做法：一类一份底稿，放到它管的目录旁边。本页只做登记与指针，不复述模板内容（零复制）。
> **身份声明**：评审稿。模板正文属形状规范，正式生效时应按归属法折进法卷承载；届时本页保留为宽松副本。
> **怎么用**：写新文档 → 下表找生态位 → 复制对应底稿到目标目录改名填空。审旧文档 → 按底稿尾部硬规则自查 + §4 形状债清单。

## 1. 生态位地图

| # | 生态位 | 实例位置（数量） | 模板底稿 | 什么时候用 |
|---|--------|-----------------|---------|-----------|
| 1 | 模式法卷·通式 | `specs/current/patterns/`（12 卷） | [pattern.md](../../specs/current/patterns/_template/pattern.md) | 新增一条链/一个模式的横切规范 |
| 2 | 模式法卷·构建宪 | 同上（1 卷） | [blueprint.md](../../specs/current/patterns/_template/blueprint.md) | 新卷核心资产是文件槽位清单 |
| 3 | 模式法卷·禁令表 | 同上（1 卷） | [prohibitions.md](../../specs/current/patterns/_template/prohibitions.md) | 否定条款为主、正身在他卷 |
| 4 | 模式法卷·元法 | 同上（1 卷） | [attribution.md](../../specs/current/patterns/_template/attribution.md) | 管其他文档的文档 |
| 5 | 模块法卷 | `specs/current/modules/`（8 卷） | [module.md](../../specs/current/modules/_template/module.md) | 新增 common 模块的用法规范 |
| 6 | 业务现行册 | `sample-application/specs/current/`（2 册） | [aggregate.md](../../../sample-application/specs/current/_template/aggregate.md) | 示例业务新聚合定行为契约 |
| 7 | 设计卡 | `docs/how-to/`（13 篇） | [card.md](../how-to/_template/card.md) | 新增"该不该用、怎么选"指引 |
| 8 | API 字典 | `docs/reference/api/`（8 篇） | [api.md](api/_template/api.md) | common 模块的查表文档 |
| 9 | 解读·层篇 | `docs/explanation/`（5 篇） | [layer.md](../explanation/_template/layer.md) | 一个架构层一篇的深读 |
| 10 | 解读·专论 | 同上（8 篇） | [treatise.md](../explanation/_template/treatise.md) | 跨层的"为什么"长篇 |
| 11 | 解读·账本 | 同上（1 篇） | [ledger.md](../explanation/_template/ledger.md) | 理论采纳/拒绝台账 |
| 12 | 教程 | `docs/tutorials/`（1 篇） | [tutorial.md](../tutorials/_template/tutorial.md) | 从零跑通的操作手册 |
| 13 | 术语表 | `docs/reference/`（1 篇） | [glossary.md](_template/glossary.md) | 术语 → canonical 指针表 |
| 14 | 工具说明书 | 同上（1 篇） | [tool-guide.md](_template/tool-guide.md) | 脚本/工具链行为描述 |
| 15 | 区章程 | `specs/README` 类（5 篇） | [charter.md](../../specs/_template/charter.md) | 声明一个目录区的规则与身份 |
| 16 | 区子索引 | `docs/README`、`how-to/README` | [shelf-index.md](../_template/shelf-index.md) | 一区的导航页 |
| 17 | 伞宣言 | `knowledge/README`（1 篇） | [umbrella.md](../../_template/umbrella.md) | 知识库自身的组织宣言 |
| 18 | 案卷四件套 | `specs/changes/<slug>/` | 权威已在 [changes/_template/](../../specs/changes/_template/)（四件） | 立案即复制，本表只登记 |
| 19 | skill SOP | `.agents/skills/`（12 篇，伞外） | 未铺，见 §4 备注 D9 | 待裁决是否纳入 |

## 2. 通用作头纪律（跨体裁公共件）

各底稿已把公共件内嵌在文首，复制即得；此处只作速记：

1. **通用件头**：全文一个 H1（按各体裁命名公式）→ `> **身份**` 一句（这篇是什么、冲突谁赢）→ 法卷类加 `> **机器对账**` 一句（哪道闸扫它）。
2. **节号统一 `## §N`**，不用 `## N.`（现状两式并存，见 §4 D3）。
3. **尾部固定**：法卷/登记类以生效登记节收尾；docs 类以指向权威源的指针行收尾。
4. **占位符**：泛化路径写 `<xxx>` 尖括号；`{agg}`/`{Agg}` 只出现在能被真实聚合实例化核验的位置（C1 代入核验路径存在）。
5. **闸兼容**：不写 `ADR-` + 四位数字；「清册 bill」自谓词出现处不得删；计数宣称与磁盘核对。

## 3. 建模板时确立的取舍（供审阅）

- **一种体裁一份底稿，单实例体裁也建**（元法、禁令表、伞宣言）：哪怕只有一篇，只要还会新增或被仿写，就需要锚。
- **模式法卷拆四体裁**：通式之外，blueprint（清单先行）、prohibitions（分域逐条）、attribution-law（表格即主干）与通式骨架差异过大，强行归一会削足适履。
- **README 类拆三体裁**：章程写规则、子索引给导航、宣言解释结构且不立法，职责与节型各不相同。
- **四件套不重复建**：`specs/changes/_template/` 已是形状权威，本表只登记。
- **skill 暂不铺**：`.agents/` 在伞外，且是否给 skills 动格式由你另行拍板。

## 4. 形状债清单（全库盘点发现，供裁决）

| # | 漂移 | 实例 | 处置选项 |
|---|------|------|---------|
| D1 | 模块法卷无 `##` 级节号（exception 从 §4 起跳号） | 8 卷 | 触及时按 module.md 补齐，或小案一次换装 |
| D2 | observability 卷全文无小节 | 1 卷 | 漂移最重；补节即达标 |
| D3 | 节号两式并存：`## §1`（他卷）vs `## 1.`（attribution-law） | 1 卷 | 统一 `## §N`，标题级轻改 |
| D4 | blueprint 末节"生效登记"缺 § 号 | 1 卷 | 并入 D3 处理 |
| D5 | quickstart 顶部双 H1 | 1 篇 | 删第二个 H1 |
| D6 | 设计卡按需节名不统一（"落地状态" vs "落地顺序与验收"） | 2 卡 | 可容忍；定规：同名节须同内容 |
| D7 | product 册无"§6 错误码位总表" | 1 册 | 先核是否真无键；无则按底稿写一句"本聚合无登记键" |
| D8 | 镜像区两份 README 无目录身份表 | 2 篇 | 按 charter.md 补 |
| D9 | skill SOP 形态未锚定 | 12 篇（伞外） | 若决定铺：`.agents/skills/_template/skill.md`；形态 = frontmatter + 定位引言 + 前置阅读 + 步骤块 + 验证勾选 +（可选）反例表；硬约束 ≤500 行、零法条、形状必锚法卷 |

> 处理 D1–D5、D7、D8 共约 20 个文件的轻改动。现在做、追认进本轮豁免、还是另立小案，等你拍板。
