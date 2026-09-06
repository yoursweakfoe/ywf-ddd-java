# AGENTS.md

## Project purpose

DDD 战术模式微服务框架。修改代码前必须理解分层约束。

## 四态知识地图（谁听谁的）

| 区 | 态 | 守则一句话 |
|---|---|---|
| `knowledge/docs/` | 地图·描述 | 代码变了它必须跟着变，烂了修文档 |
| `knowledge/specs/` | 法律·框架契约 | 代码违反它=修代码；改法走 `changes/`，禁止迁就代码偷改 |
| `knowledge/decisions/` | 判例卷宗 | 正文永不回改；推翻=新立 ADR + supersede 旧案 |
| `.agents/` | 工作台·方法 | 约束干活方式；按需详读，非每次必读 |

法律全文 = `.agents/rules/05`（事实归属法）；伞宣言 = `knowledge/README.md`。业务包契约不入伞，住镜像区 `sample-application/specs/`（框架法=knowledge/specs，业务法=sample 树内）。

## Context routing（按需触发，非全量预读）

- **动手改码前按层详读法条**：分层/依赖 → `rules/02`；命名/惯例 → `rules/03`；禁令全表 → `rules/04`；文档义务 → `rules/05`；项目背景 → `rules/01`
- **新行为先立契约**：写码前在行为所属区的 `changes/<slug>/` 出三件套（框架 → `knowledge/specs/`，示例业务 → `sample-application/specs/`；模板统一在 `knowledge/specs/changes/_template/`），完成后归档折叠回所属区 `current/`——文档同步义务只在那一刻发生
- **执行结构化任务 USE 对应技能**（11 个，全部显式点名）：
  - 创建：新建聚合 `new-aggregate` ｜ 新增用例 `new-usecase` ｜ 新建微服务 `new-service` ｜ 新增外部集成 `new-portal`
  - 增量：批量写操作 `batch-operations` ｜ 定时任务 `scheduled-task` ｜ 编写测试 `new-test` ｜ 修改 common 模块 `modify-common-module`
  - 审查：架构合规自查 `ddd-review`（编码完成必跑）｜ 生产就绪 `ops-review` ｜ 测试充分性 `test-review`
- **查知识（指针驱动，用到才取）**：结构速查→ `knowledge/docs/reference/structure.md`（生成物禁手改）；框架 API→ `reference/api/common-*.md`；为什么→ `explanation/`；当年决策→ `decisions/README.md`（先查旧判例再拍新板）；框架行为→ `knowledge/specs/`（首案开册）、业务行为现状→ `sample-application/specs/current/`；术语→ `knowledge/docs/glossary.md`；全索引→ `knowledge/docs/README.md`
- **进入模块目录时就近读**：`ywf-ddd-common/AGENTS.md`、`sample-application/AGENTS.md`（nearest-wins）
- **文档防腐**：交付前跑 `knowledge/scripts/check-docs.ps1`（六校验；ddd-review 末步已内置）；工具说明见 `knowledge/docs/reference/doc-guards.md`

## Core constraints (quick reference)

每次交互必须遵守的硬约束（完整法条见 `.agents/rules/`）：

1. 分层依赖单向：`adapter → application → domain ← infrastructure`；domain 零框架**运行时**依赖（stereotype 豁免；纯 Java + common-ddd）
2. Handler 返回 DTO，AppService 返回 CO，Adapter 纯透传
3. 写侧：load → 聚合行为 → save → toDTO（Handler 标 `@Transactional`）；读侧绕过聚合根投影 DTO
4. 业务规则封装在聚合根内，Handler 不含 if-else 判断
5. 依赖倒置：Domain 定义 Repository/Portal 接口，Infrastructure 实现；Application 永不直接引用 Mapper/PO
6. Assembler（Domain→DTO）与 Presenter（DTO→CO）强制分离
7. 异常统一 `BusinessException` + i18n 位点（`{aggregate}:err.{scene}`），禁止具名领域异常
8. 时间统一 `OffsetDateTime`；虚拟线程下禁止 `synchronized`（用 `ReentrantLock`）
9. Lombok：Domain 层禁 `@Data`（仅 `@Getter`），PO / DTO / CQE / CO 用 `@Data`
