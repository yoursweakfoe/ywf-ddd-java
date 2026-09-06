# .agents/ —— Agent 工作台（流程/SOP）

> **本树只载流程，不载地图亦不载法**（2026-10 法律入典裁决，法案 `2026-10-rules-codification`）：这里回答「活怎么干」（skills 步骤），不回答「代码必须什么样」（那是法典）。

## 两层加载模型

| 层 | 载体 | 角色 | 加载方式 |
|----|------|------|----------|
| 压缩宪法 | 根目录 `AGENTS.md` | 每次必读的核心约束（九条）+ 路由 | 全兼容**主动加载**（厂商中立标准） |
| 可调用流程 | `.agents/skills/` | 任务 SOP（12 个 SKILL.md），只含步骤+指针 | opencode / Pi **原生扫描** |

**法律与描述的居所**（经 AGENTS 路由按需读，本树零复数副本）：

- 法条正文（用法规范/禁令/公约/归属法）→ `knowledge/specs/current/`（modules 8 卷 + patterns 15 卷，含法律入典改革新增 3 卷）
- 业务行为法 → `sample-application/specs/`（镜像区）
- 项目背景/结构地图 → 根 `README.md`、`knowledge/README.md`、`knowledge/docs/`
- 判例 → `knowledge/decisions/`

原 `.agents/rules/01-05` 五部已依上述归宿拆解注销：描述归地图、规范归法典、叙事归解读、元法归归属法卷。

## 设计原则

核心问题是**在多 AI 编码工具之间保持中立，同时让每个工具都能「现成可读」**。跨工具通用的格式只有两种：`AGENTS.md`（纯 Markdown，事实标准）与 `SKILL.md`（Agent Skills 标准）。其余概念（path-gating、subagents、MCP、hooks）各家私有，不进本目录——留在各工具自己的配置域（`.claude/`、`.opencode/`、`.cursor/`）。

skills 目录选择 `.agents/skills/` 的原因：opencode 与 Pi **原生扫描**此路径（零配置），Claude Code / Cursor / Qoder 经复制、软链或引用接入。这是唯一「中立位置 + 多工具原生支持」重合的目录。

结构借鉴 [dotagents](https://github.com/bgreenwell/dotagents)（草案倡议）命名，按需裁剪——personas 已并入 skills（评审技能）。

## skills 清单（12）

创建：`new-aggregate` `new-usecase` `new-service` `new-portal`；增量：`batch-operations` `scheduled-task` `new-test` `modify-common-module`；审查：`ddd-review`（末步必跑 check-docs）`ops-review` `test-review`；立法：`new-bill`（起草/推进/折叠法案）。纪律：**skill 内零法条零模板**（步骤与指针而已；D6 推广，归属法 §2）。

## 各工具接入方式

| 工具 | 入口（AGENTS.md） | skills |
|------|-------------------|--------|
| Claude Code | `CLAUDE.md` 内 `@AGENTS.md` | 复制/软链到 `.claude/skills/` |
| opencode | 原生 | **原生扫描 `.agents/skills/`** |
| Cursor | 原生 | 支持 Agent Skills，指向或复制 |
| Qoder | 原生 | 复制到 `.qoder/skills/` |
| Pi | 原生 | **原生扫描** |
| 其他 | 对话开头贴 AGENTS.md | 按需 |

> 工具专属配置文件永远不入库（.gitignore）。法条的读法由 AGENTS 路由决定，工具差异不侵入内容。

## 贡献者指南

- 改 skill 需提 PR；新增 skill 必须附验证步骤
- 禁止在 skill 里新增裸规范句——先立法（走 `knowledge/specs/changes/` 程序），skill 再挂指针
- 核心代码中禁止嵌入工具专属指令（禁令卷 PB-§8）
