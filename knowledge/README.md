# knowledge/ —— 项目知识伞

三类知识、三个法律区，外加一套执法工具链，同伞收纳。**伞本身不立法**——每区的守则在其自己目录内生效：

| 区 | 是什么 | 守则一句话 | 法律全文 |
|---|---|---|---|
| `docs/` | **地图**（描述）：教程/手册/字典/解读四书架 | 代码变了它必须跟着变，烂了修文档 | `.agents/rules/05`（归属法） |
| `specs/` | **法律**（契约）：capabilities 现行本 + changes 过程稿 | 代码违反它=改代码或走流程修法，禁止迁就代码偷改 | `specs/README.md` + `.agents/rules/05` |
| `decisions/` | **判例卷宗**（冻结）：全局编号 ADR 日志 | 正文永不回改；推翻=新立案 + supersede 旧案 | `decisions/README.md` |
| `scripts/` | **执法工具链**（机器检查；非知识三态） | 本体代码即法、不载论述；检查行为变了说明页必须跟 | `docs/reference/doc-guards.md` + `.agents/rules/05` §1 |

方法类（rules/skills，教 agent 怎么干活）住在伞外的 `.agents/`——工具可弃，方法可换，知识与契约不可弃。

机器执法：`scripts/check-docs.ps1`（六校验：模式实例化/计数/框架符号/教学中立/映射表对账/卷宗防篡改；三文件说明见 [docs/reference/doc-guards.md](docs/reference/doc-guards.md)）。
