# AGENTS.md

## Project purpose

DDD 战术模式微服务框架。修改代码前必须理解分层约束。

## Context routing

- **每次交互前（必读）：** READ `.agents/rules/`（01-05，硬约束）
- **项目术语不确定时：** READ `docs/glossary.md`
- **执行结构化任务时：** USE 对应技能（逐一显式路由，不做目录清扫）
  - 新建聚合 → `.agents/skills/new-aggregate/SKILL.md`
  - 新增用例 → `.agents/skills/new-usecase/SKILL.md`
  - 新增批量写操作 → `.agents/skills/batch-operations/SKILL.md`
  - 新增外部集成 → `.agents/skills/new-portal/SKILL.md`
  - 新增定时任务 → `.agents/skills/scheduled-task/SKILL.md`
  - 新建微服务 → `.agents/skills/new-service/SKILL.md`
  - 编写测试 → `.agents/skills/new-test/SKILL.md`
  - 修改 common 公共模块 → `.agents/skills/modify-common-module/SKILL.md`
- **完成编码后自查：** USE `.agents/skills/ddd-review/SKILL.md`（架构合规）
- **生产就绪 / 部署前审查：** USE `.agents/skills/ops-review/SKILL.md`
- **测试充分性审查：** USE `.agents/skills/test-review/SKILL.md`
- **需要设计原理时：** CONSULT `docs/application/module-design/`（contract/adapter/application/domain/infrastructure.md 五篇）
- **需要完整代码模板时：** CONSULT `docs/application/cookbook/README.md`（实战篇子索引，按篇进入）
- **需要文档树导航时：** CONSULT `docs/README.md`（唯一文档总索引）

## Core constraints (quick reference)

每次交互必须遵守的硬约束（完整法条见 `.agents/rules/`）：

1. 分层依赖单向：`adapter → application → domain ← infrastructure`；domain 零框架依赖（纯 Java + common-ddd）
2. Handler 返回 DTO，AppService 返回 CO，Adapter 纯透传
3. 写侧：load → 聚合行为 → save → toDTO（Handler 标 `@Transactional`）；读侧绕过聚合根投影 DTO
4. 业务规则封装在聚合根内，Handler 不含 if-else 判断
5. 依赖倒置：Domain 定义 Repository/Portal 接口，Infrastructure 实现；Application 永不直接引用 Mapper/PO
6. Assembler（Domain→DTO）与 Presenter（DTO→CO）强制分离
7. 异常统一 `BusinessException` + i18n 位点（`{aggregate}:err.{scene}`），禁止具名领域异常
8. 时间统一 `OffsetDateTime`；虚拟线程下禁止 `synchronized`（用 `ReentrantLock`）
9. Lombok：Domain 层禁 `@Data`（仅 `@Getter`），PO / DTO / CQE / CO 用 `@Data`
