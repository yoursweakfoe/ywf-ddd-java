# 文档防腐工具链（knowledge/scripts/ 三文件说明书）

> **辖域**：`knowledge/scripts/` 是伞下的**执法区**——工具本体是装置不是知识（非描述/非契约/非判例），但守护知识的东西与知识同伞才内聚（2026-09-06 用户裁定，自顶层迁入；辖域隔离靠"每区各自的法律"——C6 只扫 `decisions/`，物理同居不产生自我执法，同日初裁"居伞外"已 supersede）。工具的**说明**属于知识 → 住本区字典架，本页即唯一说明入口（登记法见 [../README.md](../README.md)；法源：`.agents/rules/05` §1 执法行）。

## 三文件一览

| 文件 | 是什么 | 何时动它 |
|---|---|---|
| [../../scripts/check-docs.ps1](../../scripts/check-docs.ps1) | 六校验主机，唯一可执行 | 每次文档交付前跑；`ddd-review` 末步内置；夜间全量 |
| [../../scripts/check-docs.whitelist.txt](../../scripts/check-docs.whitelist.txt) | C3 豁免清单（外置数据，非代码） | C3 误伤时查；新增须 PR 评审并写理由 |
| [../../scripts/lychee.toml](../../scripts/lychee.toml) | 外联检查器 lychee 的配置文件（本体不在仓内） | 装了 lychee 二进制随时跑 |

## check-docs.ps1 —— 六校验主机

### 运行

```powershell
# 全量跑；退出码 = FAIL 检查项数（0 = 全绿），可直接做 CI 门
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1

# 附加探测器自检：注入三类已知违规，验证检测器没睡（三项应全 PASS）
powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1 -SelfTest
```

依赖 Windows PowerShell 5.1+（脚本头 `# requires -Version 5.1` 钉死）。仓库根默认从脚本位置逐级上溯至根 `AGENTS.md` 标记（对再次搬家免疫），可用 `-RootPath` 显式指定。

### 扫描面（三档）

| 档 | 覆盖 | 用于 |
|---|---|---|
| 教学区 | `knowledge/docs/` + `.agents/` + 根 `AGENTS.md` | C1 / C4 |
| 全区 | 伞内全部 + `.agents/` + 根 README + AGENTS.md | C2 / C3 |
| 定点 | 框架源码 ↔ 异常文档；`git diff HEAD`（只读） | C5 / C6 |

specs/decisions 两区参与符号与计数校验，但**豁免教学中立扫描**（契约与判例天然记业务——辖域条款见 `rules/05` §3）。

### 六项检查各治什么病

| 检查 | 治的病 | 机制 | 变红样例 |
|---|---|---|---|
| **C1 模式实例化** | 幽灵路径 | 教学模板里的 `{agg}` 占位目录，对 sample 真实聚合名逐个代入、与源码目录 glob 实例化比对 | 文档写了个 sample 里根本不存在的目录层级 |
| **C2 计数对账** | 文件数宣称腐烂 | 各 md 带圈数字盘点 vs 同文 "N 个文件"宣称，不一致即红 | 聚合从 20 文件扩到 22，旧宣称没人回头改 |
| **C3 框架符号** | 改名鬼魂 | `com.yoursweakfoe.common` 全限定名、以及以 Exception/Mapper/Presenter/Persistence/Fixtures 等结尾的类 token，必须在源码可查（或白名单） | 类改名/删除后文档照引旧名 |
| **C4 教学中立** | 文档沦为业务镜像 | 教学代码围栏与反引号内禁真实例聚合词；标注「真实例/如/sample」的行与登记文件豁免 | 通用教例里混进了具体业务类名 |
| **C5 映射表对账** | 异常文档漏更 | `GlobalRestExceptionHandler` 每个 `@ExceptionHandler` 处理的类必须出现在 `reference/api/common-exception.md` | 处理器加了新通道，文档表没加行 |
| **C6 卷宗防篡改** | 判例回改 | `knowledge/decisions/` 在 `git diff HEAD` 中出现删除行（README 除外）即红——正文 append-only，推翻=新立案 | 有人直接改了旧 ADR 正文 |

### 变红之后的裁决纪律

红 ≠ 一定是文档烂。先判性质：**文档错 → 修文档；工具误伤 → 修工具**（check-docs 自己是代码，可重构，不是禁区——但它管的三个知识区各自有各自的修法，见 rules/05 §1）。每次误伤若靠白名单兜底，要回头想是不是检测器逻辑该收窄。

## check-docs.whitelist.txt —— C3 豁免清单

格式：一行一条正则，`#` 开头为注释；空行忽略。只被 C3 读取。现存两类：

1. **JDK / Spring / Jakarta 标准类**（如 `^BindException$`）——它们不在仓库源码里，C3 的"源码可查"判据会误伤；
2. **虚构教例家族前缀**（如 `^Invoice`）——文档已标"虚构教例未实现"，故意出现的合法类名。**教例家族一旦真实落地，对应行必须删除——清单只删不增**。

注意：**C4 的豁免不住这里**，在 ps1 内 `$fileSkipC4` 硬编码（tutorials 真实例手册、structure 生成树、glossary 通用语言位等——每条对应 rules/05 的辖域裁定，属法源而非偏好，故不外置）。

## lychee.toml —— 外联检查器配置

check-docs 管**结构一致**（路径/符号/计数/词面），不管 `https://` 外链死活——那是 [lychee](https://github.com/lycheeverse/lychee) 的活（二进制未随仓安装，配置已就位）：

```powershell
lychee --config knowledge/scripts/lychee.toml "knowledge/**/*.md" ".agents/**/*.md" "AGENTS.md" "README.md"
```

关键取舍：`exclude` 掉 localhost/127.0.0.1（文档里的 curl 示例是虚构本地端点，探测必死）；`include_fragments = false`（页内锚点脆弱度太高，v1 不验）；`cache = true` 加速重复跑。

---

*本页属 `reference/` 字典架：三文件行为变了（新增检查、改豁免类别、换 lychee 策略），本页必须跟着变——地图区守则，见 [../../specs/README.md](../../specs/README.md) 上方的归属法指针与 `rules/05` §1。*
