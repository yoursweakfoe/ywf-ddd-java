# docs

项目级文档。

```
docs/
├── glossary.md                          # 项目术语表
├── references.md                        # 架构理论参考（采纳 / 未采纳及原因）
└── sample-application/
    ├── module-design/                   # 应用架构设计（分层职责、组件、规则）
    │   ├── contract.md
    │   ├── adapter.md
    │   ├── application.md
    │   ├── domain.md
    │   └── infrastructure.md
    ├── directory-structure/             # 目录结构参考（包结构速查）
    │   ├── overview.md
    │   ├── contract/contract.md
    │   └── server/{adapter,application,domain,infrastructure}.md
    └── cookbook/                        # 端到端代码实战（完整可编译示例）
        ├── README.md
        ├── write-path.md
        ├── read-path.md
        ├── event-flow.md
        ├── policy-pattern.md
        ├── gateway.md
        └── new-aggregate.md
```

- 想了解**每层怎么设计** → [sample-application/module-design/](application/module-design/)
- 想了解**包怎么组织** → [sample-application/directory-structure/](application/directory-structure/)
- 想了解**具体怎么写** → [sample-application/cookbook/](application/cookbook/)
- 想了解**为什么这么选** → [references.md](references.md)

- 想了解**术语定义** → [glossary.md](glossary.md)

## 与 .agents/ 的关系

本目录（`docs/`）面向**人类开发者**，提供完整的设计原理和代码走查。

[`.agents/`](../.agents/) 目录面向 **AI 编码代理**，提供精炼的指令式规则和任务技能（遵循 [dotagents](https://github.com/bgreenwell/dotagents) 约定）。
`.agents/` 中的规则从本目录的文档中提炼而来，并通过路径引用本目录获取详细原理。

| | `docs/`（本目录） | `.agents/` |
|--|--|--|
| 受众 | 人类（agent 也可读） | AI 代理 |
| 风格 | 设计论证 + 代码走查 | “你必须/禁止”指令 |
| 修改频率 | 设计决策变更时 | 规则精化时 |
