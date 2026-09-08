# 变更增量（spec-delta）

> 只描述变化量。取证源 = 立卷时真实存在的 文件:行 / 测试类名。折叠时按本卷写回 `current/`。

## MODIFIED: testing-conformance TC-1（第 4 类定义扩至全仓）

- **今**：集成类 = 「test profile + 真 PG 测试库（TC-9）」，辖域事实上仅 sample（框架容器测试仍 H2，靠 ADR-0034 豁免句挡箭）。
- ** SHALL **：TC-1 集成类措辞明载**全仓一切容器/真库测试（含 common 各包）**只走 test profile + 真 PostgreSQL；H2 自本案起在**全仓任何模块**不得作为测试基座。取证：`common-ddd/pom.xml`（h2 test 依赖）、`common-ddd/src/test/resources/application-test.yml`（双 H2 源）为待清偿的现状债；`testing-conformance.md` L10 表行 TC-1。
- ** SHALL **：ADR-0034 辖域豁免句（「common 模块自身测试树不受本案辖域」）被 ADR-0035 部分推翻；`knowledge/docs/reference/api/common-ddd.md` §4 依赖表 h2 行随迁删除（归属法 §4 强制同步）。

## MODIFIED: testing-conformance TC-9（建立与运行分离——增框架轨）

- ** SHALL **：TC-9 双轨制保持「服务形状权威 = db-migration」，新增**框架测试形状轨**：common 集成测试的形状为**测试 fixture 非服务形状**，不入库 db-migration；其生命周期三段全自动——①建库（maintenance 库 `postgres` 裸连接 `CREATE DATABASE IF NOT EXISTS ddd_framework_test`）、②复位（每 JVM 首次触库：`DROP SCHEMA IF EXISTS integration_test, integration_test_vacant CASCADE` → 重建 → 执行 fixture DDL，静态门闩恰一次）、③清空（shutdown hook 尽力 DROP，崩溃残留由下次启动复位兜底）。测试代码仍零手写 DDL 之外的形状职责、零 `@Transactional` 默认回滚依赖。取证：`db-migration/src/main/resources/db/changelog/ddd_sample_application/changes/0001-init-schema.sql` 管辖域不变（仅服务形状）。
- ** SHALL **：表名在 fixture 中一律**不带 schema 限定**，可见性由数据源 `currentSchema` 决定（master→`integration_test`、second→`integration_test_vacant`）；双源路由教例的「空库」语义 = 空 schema，不得以库级复制或 H2 空 URL 戏法表达。取证：`common-ddd/src/test/resources/schema.sql`（现状 `CREATE SCHEMA orders/products` 限定名教例，随迁改形）。

## ADDED: testing-conformance TC-10（common 测试集中法）

- ** SHALL **：`ywf-ddd-common` 下**一切**测试代码（含单模块独立单元测试）只许住在 `common-packages-integration-test` 模块 `src/test/`；各 library 模块 `src/test/` 目录不得存在，其 POM 不得声明 junit/mockito/assertj/h2/dynamic-datasource 等测试栈依赖。取证：立卷盘点（29 测试类 + 18 fixture 类 + 4 资源分布于 6 模块）。
- ** SHALL **：白盒迁移保原包——迁入类保持与被测类全同包名（跨 artifact split package，classpath 工程下合法；本工程不使用 JPMS module-info）。为保住既有 package-private/protected 访问面，**禁止**为测试搬迁扩大任何产码可见性。取证：`MybatisPersistenceTest`（protected 构造器通道）、`AuditFieldFillerTest`（嵌套 PO）。
- ** SHALL **：fixture 教例树（聚合根/Converter/Mapper XML 演示资产）随迁至新模块，命名与形状不变，仅表名去限定（见 TC-9 条）。

## MODIFIED: prohibitions §9「Common 模块约束」登记表

- ** SHALL **：构件身份三分法（工具库 / 定型装配 / **试验场**）：`common-packages-integration-test` 登记为**试验场**——判据：`src/main` 永空置（出现产码即违规）、必 `maven.deploy.skip`、依赖面 = 全 common compile + 测试栈**无最小化质证义务**（它不发布，污染止于自身）。取证：现行登记段（`prohibitions.md` §9 首段「对 common 作依赖审查判据」二分表）。

## 地图连带（非 delta 条文，折叠时同步义务，归属法 §4）

- `ywf-ddd-common/README.md` 拓扑 8→9 模块 + 新模块身份行；根 `README.md` 仓库结构；`api/common-ddd.md` §4 去 h2 行；`quickstart.md` 测试前置若涉 common 测试措辞核对；`.agents/skills/new-test|test-review/SKILL.md` 指针若指 common 测试位置。
