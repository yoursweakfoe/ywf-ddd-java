# .agents/ — Agent-Specific Resources

本目录存放**格式或目的专属于 AI 编码代理**的资源（遵循 [dotagents](https://github.com/bgreenwell/dotagents) 约定）。

共享项目文档（设计原理、代码模板、术语表）留在 `docs/`，本目录不复制。

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
├── skills/                ← 任务技能（Agent Skills 格式）
│   ├── new-aggregate/SKILL.md
│   ├── new-usecase/SKILL.md
│   ├── new-domain-event/SKILL.md
│   ├── new-portal/SKILL.md
│   ├── new-service/SKILL.md
│   ├── new-test/SKILL.md
│   ├── modify-common-module/SKILL.md
│   └── ddd-review/SKILL.md
└── personas/              ← 专家视角
    └── ddd-architect-reviewer.md
```

## 与 docs/ 的关系

| | `.agents/`（本目录） | `docs/` |
|--|--|--|
| 受众 | AI 编码代理 | 人类开发者（agent 也可读） |
| 风格 | 指令式（"你必须/禁止"） | 设计论证 + 代码走查 |
| 内容 | 规则精炼 + 步骤化技能 | 完整原理 + 对比 + 模板 |

## 各工具接入方式

本项目不强制任何 AI 工具。入口文件为根目录 `AGENTS.md`（厂商中立标准）。

| 工具 | 接入方式 |
|------|----------|
| Claude Code | 创建 `.claude/CLAUDE.md`：`请阅读 AGENTS.md 并遵守其路由` |
| Cursor | 创建 `.cursor/rules/ai.md`：内容同上 |
| GitHub Copilot | 创建 `.github/copilot-instructions.md`：内容同上 |
| Qoder | 将 `.agents/rules/*.md` 复制到 `.qoder/rules/` |
| Windsurf | 创建 `.windsurfrules`：内容同上 |
| Aider | `.aider.conf.yml` 中 `read: AGENTS.md` |
| 其他 | 对话开头粘贴 `AGENTS.md` 内容 |

> 所有工具专属配置文件已在 `.gitignore` 中，不会提交到仓库。

## 贡献者指南

- 修改规则或技能需提 PR，经 review 后合入
- 核心代码（`src/` / `sample-application/`）中禁止嵌入工具专属指令
- 新增技能必须附验证步骤
- 工具专属配置目录永远不入库
