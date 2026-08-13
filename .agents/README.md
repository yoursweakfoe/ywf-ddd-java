# .agents/ — Agent-Specific Resources

本目录存放**格式或目的专属于 AI 编码代理**的资源（借鉴 [dotagents](https://github.com/bgreenwell/dotagents) 的命名，按本项目实际需要自定义）。

共享项目文档（设计原理、代码模板、术语表）留在 `docs/`，本目录不复制。

## 设计原则

本目录组织的核心问题是：**在多个 AI 编码工具之间保持中立，同时让每个工具都能「现成可读」。**

### 中立边界 = 格式是否跨工具通用

真正跨工具通用的格式只有两种：

1. **`AGENTS.md`**（纯 Markdown）—— Claude Code / opencode / Cursor / Qoder / Pi / Codex / Copilot 等原生加载的事实标准
2. **`SKILL.md`**（[Agent Skills 标准](https://agentskills.io)）—— Claude Code / opencode / Cursor / Pi 共同遵循

其余概念（rules 的 path-gating、subagents、commands、settings、MCP、hooks）格式各自不同，**不进本目录**——它们应留在各工具自己的目录（`.claude/`、`.opencode/`、`.cursor/`），谁要用谁去配。

### 三层分工

| 层 | 载体 | 角色 | 加载方式 |
|----|------|------|----------|
| 压缩宪法 | 根目录 `AGENTS.md` | 每次必读的核心约束（~10 条） | 全兼容**主动加载** |
| 完整法条 | `.agents/rules/` | 按需详读的规则细节（01-05） | 靠 AGENTS.md 路由按需读 |
| 可调用流程 | `.agents/skills/` | 任务技能（13 个 SKILL.md） | opencode / Pi **原生扫描** |

### 关于 rules 的一个关键约束

中立 `rules/` **无法被任何工具主动加载**——path-gating / alwaysApply / globs 是工具专属能力（Claude Code 用 `.claude/rules/`，Cursor 用 `.cursor/rules/*.mdc` + frontmatter）。中立 md 文件只能靠 AGENTS.md 路由「何时读哪条」触发，或由各 skill 的「前置阅读」引用。因此**「每次必读」的核心约束必须上提到 AGENTS.md**，rules 只承担「按需详读」。

### 关于 skills 的位置选择

`.agents/skills/` 被 opencode 和 Pi **原生扫描**（无需任何配置），Claude Code / Cursor / Qoder 则通过复制、软链或引用接入。这是当前唯一一个「中立位置 + 多工具原生支持」重合的目录。

### 关于 dotagents

本目录结构**借鉴** [dotagents](https://github.com/bgreenwell/dotagents)（一个草案倡议，非被任何工具强制支持的标准）的命名，但按本项目实际需要调整——例如 dotagents 建议的 `personas/` 已并入 `skills/`（personas 转成可调用的 review skill）。

## 目录结构

```
.agents/
├── README.md              ← 本文件
├── rules/                 ← 每次交互必须遵守的硬约束（指令式）
│   ├── 01-project-overview.md
│   ├── 02-architecture.md
│   ├── 03-coding-conventions.md
│   ├── 04-forbidden-patterns.md
│   └── 05-documentation.md
└── skills/                ← 任务技能（Agent Skills 格式，可被 opencode 直接调用）
    ├── new-aggregate/SKILL.md
    ├── new-usecase/SKILL.md
    ├── new-domain-event/SKILL.md
    ├── new-portal/SKILL.md
    ├── new-service/SKILL.md
    ├── new-test/SKILL.md
    ├── batch-operations/SKILL.md
    ├── mq-consumer/SKILL.md
    ├── scheduled-task/SKILL.md
    ├── modify-common-module/SKILL.md
    ├── ddd-review/SKILL.md
    ├── ops-review/SKILL.md
    └── test-review/SKILL.md
```

## 与 docs/ 的关系

| | `.agents/`（本目录） | `docs/` |
|--|--|--|
| 受众 | AI 编码代理 | 人类开发者（agent 也可读） |
| 风格 | 指令式（"你必须/禁止"） | 设计论证 + 代码走查 |
| 内容 | 规则精炼 + 步骤化技能 | 完整原理 + 对比 + 模板 |

## 各工具接入方式

本项目不强制任何 AI 工具。入口文件为根目录 `AGENTS.md`（厂商中立标准）。

| 工具 | 入口（AGENTS.md） | rules（`.agents/rules/`） | skills（`.agents/skills/`） |
|------|-------------------|---------------------------|----------------------------|
| Claude Code | 原生读 `CLAUDE.md`，在其中 `@AGENTS.md` 引入 | 复制/软链到 `.claude/rules/` | 复制/软链到 `.claude/skills/` |
| opencode | 原生读 `AGENTS.md` | `opencode.json` 的 `instructions` 字段 glob 加载 | **原生扫描 `.agents/skills/`** |
| Cursor | 原生读 `AGENTS.md` | 复制到 `.cursor/rules/*.mdc`（加 frontmatter 才有 path-gating） | 支持 Agent Skills，指向或复制 |
| Qoder | 原生读 `AGENTS.md` | 复制到 `.qoder/rules/` | 复制到 `.qoder/skills/` |
| Pi | 原生读 `AGENTS.md` | 无 rules 概念，靠 `AGENTS.md` 路由 | **原生扫描 `.agents/skills/`** |
| GitHub Copilot | `.github/copilot-instructions.md` 引用 | `@file` 引用规则文件 | 支持 skills |
| Windsurf | `.windsurfrules` | `@file` 引用 | — |
| Aider | `.aider.conf.yml` 中 `read: AGENTS.md` | 同上 `read:` 加规则文件 | — |
| 其他 | 对话开头粘贴 `AGENTS.md` 内容 | — | — |

> 粗体 = 该工具**原生支持** `.agents/skills/` 位置，无需任何配置。其余工具的 rules/skills 需复制、软链或引用后接入。

> 所有工具专属配置文件已在 `.gitignore` 中，不会提交到仓库。

## 贡献者指南

- 修改规则或技能需提 PR，经 review 后合入
- 核心代码（`src/` / `sample-application/`）中禁止嵌入工具专属指令
- 新增技能必须附验证步骤
- 工具专属配置目录永远不入库
