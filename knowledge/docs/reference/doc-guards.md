# 文档防腐工具链（knowledge/scripts/ 三文件说明书）

> `knowledge/scripts/` 是伞下的执法区：工具本身不是知识，它守护另外三个区。工具行为以代码本体为准，本区的**说明书**属于知识，住字典架，本页是唯一说明入口（登记法见 [../README.md](../README.md)，法源见归属法卷 `knowledge/specs/current/patterns/attribution-law.md` §1 执法行）。
> 执法区放在伞内是辖域安排：C6 要读案卷区，也就是两侧 `specs/archive/`，并在全仓维持废号零容忍。物理同居不产生自我执法，C6 的规则边界由归属法卷划定。本页前三个脚本是主体，图管线的 render/check-diagrams 两个脚本在末节。

## 三文件一览

| 文件 | 是什么 | 何时动它 |
|---|---|---|
| [../../scripts/check-docs.ps1](../../scripts/check-docs.ps1) | 七校验主机，唯一可执行 | 每次文档交付前跑；`ddd-review` 末步内置；夜间全量 |
| [../../scripts/check-docs.whitelist.txt](../../scripts/check-docs.whitelist.txt) | C3 豁免清单，外置数据文件，不是代码 | C3 误伤时查它；新增须 PR 评审并写理由 |
| [../../scripts/lychee.toml](../../scripts/lychee.toml) | 外联检查器 lychee 的配置文件，二进制不在仓内 | 装了 lychee 二进制随时可跑 |

## check-docs.ps1 —— 七校验主机

### 运行

```powershell
# 全量跑；退出码 = FAIL 检查项数（0 = 全绿），可直接做 CI 门
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1

# 探测器自检：ST1 幽灵路径、ST2 业务词、ST3 鬼类名、ST4 技能名正则、ST5 废号正则、ST6 案名正则，六项应全 PASS
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1 -SelfTest
```

依赖 Windows PowerShell 5.1 及以上，脚本头 `# requires -Version 5.1` 已钉死。仓库根默认从脚本所在位置逐级上溯，找到根 `AGENTS.md` 标记即认定，搬家免疫；可用 `-RootPath` 显式指定。

### 扫描面（三档）

| 档 | 覆盖 | 用于 |
|---|---|---|
| 教学区 | `knowledge/docs/` + `.agents/` + 根 `AGENTS.md` + `knowledge/specs/current/` | C1 / C4 |
| 全区 | 伞内全部 + `.agents/` + 根 README + AGENTS.md | C2 / C3 |
| 定点 | 框架源码 ↔ 异常文档；`git diff HEAD`，只读；`.agents/skills/` 实地 | C5 / C6 / C7 |

`changes/` 与 `archive/` 参与符号和计数校验，但豁免教学中立扫描：案卷记录当时的语言，辖域条款见归属法卷 §3。`knowledge/specs/current/` 法卷在 C1/C4 扫描面内：严格件必须保持 `{agg}` 与虚构家族的中立形状，这件事由机器担保，不靠自觉。

### 七项检查各治什么病

| 检查 | 治的病 | 机制 | 变红样例 |
|---|---|---|---|
| **C1 模式实例化** | 幽灵路径 | 扫教学区 md 里含 `{agg}` 占位的目录路径，把 sample 真实聚合名逐个代入，与源码目录树比对是否存在。只核目录段，`.java` 文件名段先剥掉；`AGENTS.md` 豁免 | 文档写了个 sample 里根本不存在的目录层级 |
| **C2 计数对账** | 文件数宣称腐烂 | 每份 md 内盘点出现过的带圈数字种类，对照同文的 "N 个文件"、"(N + 2"、"= N + 2" 宣称；两边都有记录且数值不一致即红 | 聚合从 20 文件扩到 22，旧宣称没人回头改 |
| **C3 框架符号** | 改名鬼魂 | 全区 md 扫两类 token：`com.yoursweakfoe.common` 开头的限定名尾段；以 Exception/Mapper/AutoConfiguration/TypeHandler/Assembler/Presenter/Persistence/Fixtures 结尾的裸类名。源码文件名查得到、属虚构教例前缀、或在白名单，三者占一即过 | 类改名或删除后，文档照引旧名 |
| **C4 教学中立** | 文档沦为业务镜像 | 教学区的代码围栏行、正文反引号片段内，出现 order/Order/product/Product 即红。三类豁免：含真实例、实现状态、sample、如、e.g. 或 SQL 保留字语境的行；登记过的真实例符号与路径清单；文件级豁免 tutorials/、glossary.md、各级 README | 通用教例里混进了具体业务类名 |
| **C5 映射表对账** | 异常文档漏更 | `GlobalRestExceptionHandler` 每个 `@ExceptionHandler` 处理的异常类，名字必须出现在 `reference/api/common-exception.md`；缺一个报一个 | 处理器加了新通道，文档表没加行 |
| **C6 案卷冻结 + 清册自洽（两臂）** | 封存档回改、销账式偷删、废号残留 | **冻结臂**：两侧 archive 内被修改的案卷正文文件，diff 含 minus 行即红，archive 的 README 豁免。**清册臂 (a)**：archive 正文被整删时，必须有一支「清册 bill」在途才放行；bill 须自报身份——specify 正文载「清册 bill」字样，或案卷目录 slug 含 `cull`；在途指磁盘 `changes/` 下有它，或自焚识别：本轮 diff 删掉的 specify/旧 proposal，其 HEAD 版满足同一自名判据。删后该册必须整册归零，禁拆件、禁择留。**清册臂 (b)**：废号零容忍常设，不等册空——活面 Markdown 残留 `ADR-\d{4}` 样式的编号即红，changes/archive 三区豁免；两册清空后，活面也不得残留 date-slug 案名。输出含 residue 计数与两臂 info 行 | 改封存档正文 = 冻结臂红；没有清册 bill 偷删 = 臂 (a) 红；活面残留旧编号制编号 = 臂 (b) 红；册已清空但案名引用还在 = 空册自洽红 |
| **C7 技能闸** | skill 逸出 spec 纪律 | `.agents/` 一级居民限 `README.md`、`skills/`、gitignored 的 `memory/` 与 `logs/`；`skills/` 下只准技能目录，禁散文件；每个目录必有 `SKILL.md`，frontmatter 的 `name` 为 kebab 式且等于目录名、不超 64 字符，`description` 非空、不超 1024 字符，正文不超 500 行，这是 Agent Skills 规范上限 | skills/ 混进散文件或白名单外目录；`name:` 与目录漂移 |

### 变红之后的裁决纪律

红不等于文档烂。先判性质：文档错就修文档，工具误伤就修工具。check-docs 自己是代码，可以重构，不是禁区；但它管的三个知识区各有各的修法，见归属法卷 §1。每次误伤若靠白名单兜底，要回头检查检测器逻辑是否该收窄。

## check-docs.whitelist.txt —— C3 豁免清单

格式：一行一条正则，`#` 开头是注释，空行忽略，只有 C3 读它。现存两类条目：

1. **JDK / Spring / Jakarta 标准类**，如 `^BindException$`：它们不在仓库源码里，C3 的"源码可查"判据会误伤；
2. **虚构教例家族前缀**，如 `^Invoice`：文档已标"虚构教例未实现"，这类名字出现是故意的。**教例一旦真实落地，对应行必须删除——清单只删不增**。

注意：C4 的豁免不住这里，硬编码在 ps1 的 `$fileSkipC4`。tutorials 是真实例手册、glossary 是通用语言位，每条豁免对应归属法卷的一次辖域裁定，属法源而非偏好，所以不外置。

## lychee.toml —— 外联检查器配置

check-docs 管结构一致：路径、符号、计数、词面。它不管 `https://` 外链死活，那是 [lychee](https://github.com/lycheeverse/lychee) 的活。二进制未随仓安装，配置已就位：

```powershell
lychee --config knowledge/scripts/lychee.toml "knowledge/**/*.md" ".agents/**/*.md" "AGENTS.md" "README.md"
```

关键取舍三条：`exclude` 掉 localhost 与 127.0.0.1，文档里的 curl 示例是虚构本地端点，探测必死；`include_fragments = false`，页内锚点太脆弱，v1 不验；`cache = true`，重复跑用缓存加速。

## render-diagrams.ps1 / check-diagrams.ps1 —— D2 图管线

`knowledge/diagrams/` 载图的全部真相。`.d2` 源按被注文档的仓库相对路径镜像入册，例如 `diagrams/knowledge/README/drive-relations.d2` 注 `knowledge/README.md`；渲染产物 SVG 落 `diagrams/gen/` 下的同镜像路径。源与产物同入库，因为 GitHub 网页能渲染 SVG，ignore 产物会导致远程裂图。

```powershell
# 改完 .d2 源后重刷，默认 --layout tala，v0.9.0 bundled 引擎
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/render-diagrams.ps1
# 防陈旧三方对账：源、产物、manifest 哈希，不依赖 d2 本体；孤儿 SVG 亦红
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-diagrams.ps1
```

产物是新的可腐烂面，入库即欠同步债，所以 check-diagrams 独立设闸，与 check-docs 同点跑（ddd-review 末步）。施工判例五条，都是踩过的坑：

1. PS 双引号串不解释 `\n`：改源用编辑工具，不要在脚本里 Replace。
2. `label.near:` 是非法键，边标签位移用裸键 `near:`。TALA 默认自动侧移边标签避让连线，密区标签会漂到邻线上误导认读，`{ near: center }` 可钉回本线中点；网格 children 互连边的标签被官方源码强制钉在线中心，不会侧移。
3. 间隙只有网格容器三键 `grid-gap`/`vertical-gap`/`horizontal-gap`；`style.gap` 与 `style.spacing` 不存在，报 invalid style keyword；根级裸写 `gap: 160` 会静默造出一个名为 gap、标着 160 的幽灵节点。非网格容器间的布局间距由引擎自管，OSS 没有旋钮；TALA 可用 `top`/`left` 锁位绕行。
4. EAP=Stop 下，d2 写到 stderr 的成功横幅会被包成终止错误；脚本内已按 2>&1 加 ErrorRecord 还原处理。
5. PS5.1 的 `Set-Content -Encoding UTF8` 必塞 BOM：`.d2`、`.java`、`.xml` 按规矩无 BOM，脚本改过这些文件后必须剥掉，否则 manifest 哈希当场红。

---

*本页属 `reference/` 字典架：工具文件行为变了（新增检查、改豁免类别、换 lychee 策略、动图管线），本页必须跟着变。地图区守则，见 [../../specs/README.md](../../specs/README.md) 的归属法指针与归属法卷 §1。*
