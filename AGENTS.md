# AGENTS.md

## Project purpose

DDD 战术模式微服务框架。修改代码前必须理解分层约束。

## Context routing

- **每次交互前（必读）：** READ `.agents/rules/`（01-05，硬约束）
- **项目术语不确定时：** READ `docs/glossary.md`
- **执行结构化任务时：** USE `.agents/skills/` 中对应技能
  - 新建聚合 → `.agents/skills/new-aggregate/SKILL.md`
  - 新增用例 → `.agents/skills/new-usecase/SKILL.md`
  - 新增领域事件 → `.agents/skills/new-domain-event/SKILL.md`
  - 新增外部集成 → `.agents/skills/new-portal/SKILL.md`
  - 新建微服务 → `.agents/skills/new-service/SKILL.md`
- **完成编码后自查：** USE `.agents/skills/ddd-review/SKILL.md`
- **需要设计原理时：** CONSULT `docs/sample-application/module-design/`
- **需要完整代码模板时：** CONSULT `docs/sample-application/cookbook/`
- **审查架构合规性时：** ADOPT `.agents/personas/ddd-architect-reviewer.md`

## Core constraints (quick reference)

- Domain 层零框架依赖（纯 Java + common-ddd）
- Handler 返回 DTO，AppService 返回 CO，Adapter 纯透传
- 异常统一 BusinessException + i18n 位点，禁止具名异常
