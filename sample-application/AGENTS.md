# sample-application/AGENTS.md

本树=示例业务服务，**全仓唯一「真实例」之家**。进入本目录先读根 `AGENTS.md` 九条；法条在 `knowledge/specs/current/patterns/（禁令卷/编码公约卷）`，此处不复述。

## 本树局部守则（nearest-wins）

- **本树是结构事实的真相源**：`knowledge/` 与 `.agents/` 的每个 `{agg}` 模板路径（代入本树真实聚合名后）、每个类符号，由 `knowledge/scripts/check-docs.ps1` C1/C3 对照本树校验。**重构本树（改包/改名/移类）当天，check-docs 会指名要同步的文档行——修文档是重构的一部分，不是之后的债。**（判例：83ee2ff 扁平化后 docs 滞留 32 处幽灵路径、靠人肉审计发现，本次重组要根除的模式。）
- **业务词在本树合法、出树即违规**：教学文档引用本树真实类只走「真实例」指针位（代码块外+标注，或实现状态表）——D4 教义的另一面。
- **契约先行（spec-first 的落地形态）**：新增/变更行为先立 ``specs/changes/<YYYY-MM-slug>`（本树）/`（proposal→delta→tasks）；实现完成后 delta 归档折叠进本树 `specs/current/`。文档同步义务只在归档那一刻发生。
- **状态表惯例**：教学教例未落地件（batch/scheduler/gateway/distributed-transaction）在对应 how-to 篇维护「实现状态表」（判例 scheduled-task 篇）；新增未实现的教学内容必须同步登记，`⛔ 虚构教例，sample 未实现` 是其身份标记。
- **测试即验收**：三分通道行为有 `OptimisticLockConcurrencyTest` 等实证；契约枚举奇偶由 `ContractEnumParityTest` 锁死；写侧模板在 `knowledge/docs/how-to/testing.md`（skill 内零模板，D6 裁决）。
- 验证闸门：`mvn -B test`（真 PG 测试库 `ddd_sample_application_test`，前置 = ywf-infra postgres 环节 + db-migration 建形；隔离双轨见 TC-9——「离线逻辑零出口、一条前置全绿」是本店演示契约）+ 双端 ArchUnit（挂 common-test 共享常量，零本地覆写）。
