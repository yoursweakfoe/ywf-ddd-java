# 提案：common 测试集中入专用集成测试模块 + H2 全仓退役（真 PG 自建立清）

- 状态：已批准并折叠（2026-09-08 施工全绿：试验场 193 ×2 轮真 PG + sample 119 回归；本目录随折叠整袋入 archive/）
- 立卷：2026-09-08
- 辖域：框架区（knowledge/specs/）；触码 = ywf-ddd-common 全部模块 + 新模块
- 前案：本卷是 `archive/2026-09-pg-native-shape-and-real-testdb` 的续案——该案把 sample 换到真 PG 并把「框架测自己，继续 H2 自洽」写进 ADR-0034 豁免句；本案裁决**废除该豁免**并把全部 common 测试集中立法。

## 问题

1. **法卷自相矛盾（C 级病）**：TC-1/TC-9 现行条文写「容器测试只走 test profile + 真 PG 测试库」，但 common-ddd 容器测试仍在 H2（`common-ddd/pom.xml` h2 test 依赖、`src/test/resources/application-test.yml` 双 H2 源）。ADR-0034 的辖域豁免句是唯一的挡箭牌，而它本身与 TC-1 的普适措辞冲突。
2. **测试依赖散落在 library 模块 POM**：common-ddd 的 test scope 背着 dynamic-datasource、h2、common-pg——框架 library 模块替第三方集成教例站岗，消费者虽不受传递污染（test scope 截断），但模块 POM 观感与职责边界被侵蚀。
3. **H2 语义失真**：用户裁决语——「h2 测不出真实结果」；PG 方言、search_path、真约束行为在 H2 PG 兼容模式下只能近似。
4. **测试就近性无资产复用计划**：fixture 教例（聚合根/Converter/Mapper XML 三件套形状演示）散在 common-ddd src/test，本应是全仓框架 API 的演示资产。

## 裁决主张

- **R1 集中模块**：新建第 9 个 common 模块 `common-packages-integration-test`（纯测试模块，`src/main` 空置、全部测试住 `src/test`），**全部** common 测试迁居于此——含互引测试（common-ddd 的 MybatisPersistence 链路、双源路由）与单模块独立测试（contract/pg/security/exception/cloud 的 29 个测试类、18 个 fixture 类、4 个资源）。
- **R2 真 PG 唯一基座**：新模块测试基座 = 真 PostgreSQL（`ywf-infra` postgres 环节）；H2 从全仓退役（common 模块 POM 与测试树清零）。fixture 表落**专用 schema `common_it`**（表名不带限定、靠 search_path 解析）；双源路由教例的「空库」以**专用空 schema `common_it_vacant`** 表达（second 源 currentSchema 指它）。
- **R3 自建自清**：测试库 `ddd_framework_test` 无人工前置——启动时经 maintenance 库 `postgres` 裸连接自动 `CREATE DATABASE IF NOT EXISTS`；JVM 内首次触库时 `DROP SCHEMA IF EXISTS common_it, common_it_vacant CASCADE + CREATE + 执行 DDL`（每 JVM 恰一次，静态门闩）；注册 shutdown hook 尽力清空。崩溃残留由下次启动复位兜底。
- **R4 白盒保留原包**：迁入测试保持与被测类同包名（跨 artifact 同包 = split package，classpath 工程下合法、不涉 JPMS），保住 package-private/protected 白盒访问通道，不为测试搬迁扩大产码 API 面。
- **R5 各 library 模块瘦身**：6 个被测模块的 test-scope 测试栈（common-test、h2、dynamic-datasource、junit、assertj、jackson-test 等）全删，src/test 目录移除；common-test 作为聚合器的角色不变，新模块 compile 引之。

## 边界与不办

- 不动 sample-application（其测试基座已由前案落定，镜像区无涉）。
- 不改任何 common 模块 main/ 产码与公开 API（零破坏性变更 → 无需 api 手册形状修订，只同步依赖描述行）。
- 不引 testcontainers（沿承前案 q3 裁决：真 PG 直连，前置 = postgres 环节）。
- 新模块不发布：`maven.deploy.skip=true`。
- ArchUnit 双端守护不受影响（common 侧本无 ArchUnit 测试类，规则集住 common-test 不动）。

## 待裁决问句（2026-09-08 已裁决，批准施工）

| # | 问句 | 裁决 |
|---|---|---|
| Q1 | 专用库名 `ddd_framework_test`、schema 名可否缩写？ | **库名照推荐；schema 用全称 `integration_test` / `integration_test_vacant`，禁用 "it" 缩写——「我们不缺这点长度」** |
| Q2 | 模块名拼写修正为 `common-packages-integration-test`？ | **修正** |
| Q3 | 白盒迁移保原包 vs 重包名？ | **保持原包名（找起来也方便）** |
| Q4 | 三段生命周期（自建→启动复位→shutdown 清空）？ | **照推荐** |
