# 文档防腐工具链（knowledge/scripts/ 三文件说明书）

> **辖域**：`knowledge/scripts/` 是伞下的**执法区**——工具本体是装置不是知识（非描述/非契约/非判例），但守护知识的东西与知识同伞才内聚（2026-09-06 用户裁定，自顶层迁入；辖域隔离靠"每区各自的法律"——C6 只扫 `decisions/`，物理同居不产生自我执法，同日初裁"居伞外"已 supersede）。工具的**说明**属于知识 → 住本区字典架，本页即唯一说明入口（登记法见 [../README.md](../README.md)；法源：`knowledge/specs/current/patterns/attribution-law.md` §1 执法行）。

## 三文件一览

| 文件 | 是什么 | 何时动它 |
|---|---|---|
| [../../scripts/check-docs.ps1](../../scripts/check-docs.ps1) | 七校验主机，唯一可执行 | 每次文档交付前跑；`ddd-review` 末步内置；夜间全量 |
| [../../scripts/check-docs.whitelist.txt](../../scripts/check-docs.whitelist.txt) | C3 豁免清单（外置数据，非代码） | C3 误伤时查；新增须 PR 评审并写理由 |
| [../../scripts/lychee.toml](../../scripts/lychee.toml) | 外联检查器 lychee 的配置文件（本体不在仓内） | 装了 lychee 二进制随时跑 |

## check-docs.ps1 —— 七校验主机

### 运行

```powershell
# 全量跑；退出码 = FAIL 检查项数（0 = 全绿），可直接做 CI 门
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1

# 附加探测器自检：ST1-3 注入三类已知违规、ST4 验技能名正则，验证检测器没睡（四项应全 PASS）
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1 -SelfTest
```

依赖 Windows PowerShell 5.1+（脚本头 `# requires -Version 5.1` 钉死）。仓库根默认从脚本位置逐级上溯至根 `AGENTS.md` 标记（对再次搬家免疫），可用 `-RootPath` 显式指定。

### 扫描面（三档）

| 档 | 覆盖 | 用于 |
|---|---|---|
| 教学区 | `knowledge/docs/` + `.agents/` + 根 `AGENTS.md` + `knowledge/specs/current/` | C1 / C4 |
| 全区 | 伞内全部 + `.agents/` + 根 README + AGENTS.md | C2 / C3 |
| 定点 | 框架源码 ↔ 异常文档；`git diff HEAD`（只读）；`.agents/skills/` 实地 | C5 / C6 / C7 |

`changes/`、`archive/`、`decisions/` 参与符号与计数校验但**豁免教学中立扫描**（案卷记录当时语，辖域条款见 `归属法卷` §3）；`specs/current/` 法卷自 2026-09-06 宽严双份裁定起**入列 C1/C4**——严格件必须保持 `{agg}`/虚构家族的中立形状，由机器担保而非自觉。

### 七项检查各治什么病

| 检查 | 治的病 | 机制 | 变红样例 |
|---|---|---|---|
| **C1 模式实例化** | 幽灵路径 | 教学模板里的 `{agg}` 占位目录，对 sample 真实聚合名逐个代入、与源码目录 glob 实例化比对 | 文档写了个 sample 里根本不存在的目录层级 |
| **C2 计数对账** | 文件数宣称腐烂 | 各 md 带圈数字盘点 vs 同文 "N 个文件"宣称，不一致即红 | 聚合从 20 文件扩到 22，旧宣称没人回头改 |
| **C3 框架符号** | 改名鬼魂 | `com.yoursweakfoe.common` 全限定名、以及以 Exception/Mapper/Presenter/Persistence/Fixtures 等结尾的类 token，必须在源码可查（或白名单） | 类改名/删除后文档照引旧名 |
| **C4 教学中立** | 文档沦为业务镜像 | 教学代码围栏与反引号内禁真实例聚合词；标注「真实例/如/sample」的行与登记文件豁免 | 通用教例里混进了具体业务类名 |
| **C5 映射表对账** | 异常文档漏更 | `GlobalRestExceptionHandler` 每个 `@ExceptionHandler` 处理的类必须出现在 `reference/api/common-exception.md` | 处理器加了新通道，文档表没加行 |
| **C6 卷宗防篡改** | 判例回改 | `knowledge/decisions/` 在 `git diff HEAD` 中出现删除行（README 除外）即红——正文 append-only，推翻=新立案 | 有人直接改了旧 ADR 正文 |
| **C7 技能闸** | skill 逸出 spec 纪律 | `.agents/` 一级住民白名单（`README.md` + `skills/` + gitignored `memory/`/`logs/`）；每个技能目录必有 `SKILL.md`，其 frontmatter `name`==目录名（kebab，≤64）、`description` 非空 ≤1024、正文 ≤500 行（Agent Skills 规范上限；案卷 `2026-11-agents-workspace` L2） | skills/ 混进散文件或白名单外目录；`name:` 与目录漂移 |

### 变红之后的裁决纪律

红 ≠ 一定是文档烂。先判性质：**文档错 → 修文档；工具误伤 → 修工具**（check-docs 自己是代码，可重构，不是禁区——但它管的三个知识区各自有各自的修法，见 归属法卷 §1）。每次误伤若靠白名单兜底，要回头想是不是检测器逻辑该收窄。

## check-docs.whitelist.txt —— C3 豁免清单

格式：一行一条正则，`#` 开头为注释；空行忽略。只被 C3 读取。现存两类：

1. **JDK / Spring / Jakarta 标准类**（如 `^BindException$`）——它们不在仓库源码里，C3 的"源码可查"判据会误伤；
2. **虚构教例家族前缀**（如 `^Invoice`）——文档已标"虚构教例未实现"，故意出现的合法类名。**教例家族一旦真实落地，对应行必须删除——清单只删不增**。

注意：**C4 的豁免不住这里**，在 ps1 内 `$fileSkipC4` 硬编码（tutorials 真实例手册、glossary 通用语言位等——每条对应 归属法卷 的辖域裁定，属法源而非偏好，故不外置）。

## lychee.toml —— 外联检查器配置

check-docs 管**结构一致**（路径/符号/计数/词面），不管 `https://` 外链死活——那是 [lychee](https://github.com/lycheeverse/lychee) 的活（二进制未随仓安装，配置已就位）：

```powershell
lychee --config knowledge/scripts/lychee.toml "knowledge/**/*.md" ".agents/**/*.md" "AGENTS.md" "README.md"
```

关键取舍：`exclude` 掉 localhost/127.0.0.1（文档里的 curl 示例是虚构本地端点，探测必死）；`include_fragments = false`（页内锚点脆弱度太高，v1 不验）；`cache = true` 加速重复跑。

## render-diagrams.ps1 / check-diagrams.ps1 —— D2 图管线

`knowledge/diagrams/` 载图的全部真相：`.d2` 源按**被注文档的仓库相对路径**镜像入册（如 `diagrams/knowledge/README/drive-relations.d2` 注 `knowledge/README.md`），产物 SVG 落 `diagrams/gen/<同镜像路径>/`，与源同入库（GitHub 网页渲染 SVG；ignore 产物=远程裂图，判例否决）。

```powershell
# 改完 .d2 源后重刷（默认 --layout tala，v0.9.0 bundled 引擎；三引擎肉眼对比裁决 2026-09-09）
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/render-diagrams.ps1
# 防陈旧三方对账（源↔产物↔manifest 哈希，不依赖 d2 本体；孤儿 SVG 亦红）
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-diagrams.ps1
```

产物是**新的可腐烂面**——入库即欠同步债，故独立闸与 check-docs 同点跑（ddd-review 末步）。施工判例三条：PS 双引号串不解释 `\n`（用编辑工具改源，勿脚内 Replace）；`label.near:` 非法、边标签位移用裸键 `near:`；EAP=Stop 下 d2 的 stderr 成功横幅会被包成终止错误，脚本内已按 2>&1+ErrorRecord 还原姿势处理。

---

*本页属 `reference/` 字典架：工具文件行为变了（新增检查、改豁免类别、换 lychee 策略、动图管线），本页必须跟着变——地图区守则，见 [../../specs/README.md](../../specs/README.md) 上方的归属法指针与 `归属法卷` §1。*
