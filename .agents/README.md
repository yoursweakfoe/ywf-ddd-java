# .agents/ —— Agent 工作台（流程/SOP）

> **本树只载流程，不载地图亦不载法**（2026-10 法律入典裁决，法案 2026-10 法条归典案）：这里回答「活怎么干」（skills 步骤），不回答「代码必须什么样」（那是法典）。

## 两层加载模型

| 层 | 载体 | 角色 | 加载方式 |
|----|------|------|----------|
| 压缩宪法 | 根目录 `AGENTS.md` | 每次必读的核心约束（九条）+ 路由 | 全兼容**主动加载**（厂商中立标准） |
| 可调用流程 | `.agents/skills/` | 任务 SOP（12 个 SKILL.md），只含步骤+指针 | 8/9 主流工具**原生扫描**（下表） |

**法律与描述的居所**（经 AGENTS 路由按需读，本树零复数副本）：

- 法条正文（用法规范/禁令/公约/归属法）→ `knowledge/specs/current/`（modules 8 卷 + patterns 15 卷，含法律入典改革新增 3 卷）
- 业务行为法 → `sample-application/specs/`（镜像区）
- 项目背景/结构地图 → 根 `README.md`、`knowledge/README.md`、`knowledge/docs/`
- 旧裁决（案卷先行）→ `knowledge/specs/archive/` 封存案卷 specify §裁决记录、`knowledge/docs/explanation/`（现行论证）

原 `.agents/rules/01-05` 五部已依上述归宿拆解注销：描述归地图、规范归法典、叙事归解读、元法归归属法卷。

## 设计原则

核心问题是**在多 AI 编码工具之间保持中立，同时让每个工具都能「现成可读」**。跨工具通用的格式只有两种：`AGENTS.md`（纯 Markdown，事实标准）与 `SKILL.md`（Agent Skills 标准）。其余概念（path-gating、subagents、MCP、hooks）各家私有，不进本目录——留在各工具自己的配置域（`.claude/`、`.opencode/`、`.cursor/`）。

skills 目录选择 `.agents/skills/` 的原因：这是全生态最大公约数——**9 个主流工具里 8 个原生扫描此路径**（Codex 列其为主路径；Cursor、VS Code/Copilot、Gemini CLI、Amp、OpenCode、pi、oh-my-opencode 官方文档或源码实证），唯一例外是 Claude Code（只认 `.claude/skills/`，社区功能请求 #31005 长期未合）。注意**子目录分组只有 OpenCode/Cursor/pi 承诺递归**，故本仓 skills 保持平铺，分类学住根 `AGENTS.md` 路由表。

结构借鉴 [dotagents](https://github.com/bgreenwell/dotagents)（草案倡议）命名，按需裁剪——personas 已并入 skills（评审技能）。

## skills 清单（12）

创建：`new-aggregate` `new-usecase` `new-service` `new-portal`；增量：`batch-operations` `scheduled-task` `new-test` `modify-common-module`；审查：`ddd-review`（末步必跑 check-docs）`ops-review` `test-review`；立法：`new-bill`（起草/推进/折叠法案）。纪律：**skill 内零法条零模板**（步骤与指针而已；D6 推广，归属法 §2）；且**一切形状性内容必须锚定法卷某卷某节——实施内容基准律**（归属法卷 §2 表注）：锚不到法源=法卷覆盖缺口，走 `changes/` 立案补法，禁止就地自造形状；工序、判据、验收动作属不可机械化内容，住本树自带权限，免检。

## 各工具接入方式

| 工具 | 入口（AGENTS.md） | `.agents/skills/` |
|------|-------------------|--------|
| OpenCode | 原生 | **原生扫描**（官方文档+源码，递归支持） |
| Codex | 原生（根→cwd 逐级拼接，全链 32 KiB 上限） | **原生扫描，且列为主路径** |
| Cursor | 原生（根+嵌套合并，近者压过） | **原生扫描**（官方文档；另兼容读 `.claude/`、`.codex/`） |
| VS Code / Copilot | 原生（`chat.useAgentsMdFile`） | **原生扫描** |
| Gemini CLI | 需配置（`context.fileName` 指到 AGENTS.md） | **原生扫描**（文档明言互操作路径） |
| Amp | 原生（cwd+父目录上溯） | **原生扫描** |
| Pi | 原生 | **原生扫描**（且支持分组目录） |
| oh-my-opencode | 原生 | **原生扫描**（自带 loader 硬编码此路径） |
| **Claude Code** | **不读 AGENTS.md**——官方姿势 `CLAUDE.md` 内写 `@AGENTS.md` import | ❌ **唯一不支持**（只认 `.claude/skills/`，#31005 未合）。**勿用软链偏方**（CC 会向共享目录写 `.system/` 内部文件，#20820 实锤）；确需触发时个人级复制 |
| Qoder | 原生（CLI 向上找 + 惰性加载子目录；IDE rules 优先于 AGENTS.md） | skills 支持**未见官方实据**（未证实） |
| 其他 | 对话开头贴 AGENTS.md | 按需 |

> 出处：各工具官方文档与源码逐格核验（2026-09 调研）。工具专属配置文件永远不入库（.gitignore）。法条的读法由 AGENTS 路由决定，工具差异不侵入内容。

## 贡献者指南

- 改 skill 需提 PR；新增 skill 必须附验证步骤
- 禁止在 skill 里新增裸规范句——先立法（走 `knowledge/specs/changes/` 程序），skill 再挂指针
- 核心代码中禁止嵌入工具专属指令（禁令卷 PB-§8）
