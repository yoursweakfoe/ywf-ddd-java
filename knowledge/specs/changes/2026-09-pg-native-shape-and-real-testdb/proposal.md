# PG 原生形状权威与真库测试基座——框架规范修订

- Slug: 2026-09-pg-native-shape-and-real-testdb ｜ 日期: 2026-09-08 ｜ 关联 ADR: ADR-0033/0034（拟新立，本案批准时同立判例）

## Why（为什么现在改）

现行法卷把一组「H2 能力天花板下的旧形」钉成了规范：审计列 `create_at`/`update_at`（畸形缩写英文）、软删列 `is_delete`、`version INTEGER`、id `VARCHAR(36)`、教例双名表 `orders.orders`；测试基座条款（TC-1/D 型）更把「H2 MODE=PostgreSQL、零外部基础设施、clone 即全绿」写死为法律。

2026-09 用户三连裁决判定：这些不是设计意图而是**对测试工具的迁就**——「工具的局限不得影响业务设计」。事实层已先行落地：`db-migration` 工程（仓库根下独立 Spring Boot Job）已建 `ddd_sample_application` / `ddd_sample_application_test` 双库，形状为 PG 原生（`uuid DEFAULT uuidv7()`、`created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`、`created_by/updated_by uuid`、`is_deleted boolean`、`version bigint`、聚合 schema 单数命名 `sales_order`/`product`、账表独立 schema `liquibase`），同一变更集双库实跑字节级同形。**法卷与已落地事实的裂缝每多一天，check-docs C1/C3 对账与 {agg} 模板的误导性就多一天。**

## What changes（改什么行为，非如何实现）

映射到未来 spec-delta 的各 ADDED/MODIFIED：

- **testing-conformance 卷**：D 型集成测试基座由「test profile + H2 内存库」改为「test profile + 真 PG 测试库（`ddd_sample_application_test`）」；「零外部基础设施」承诺改写为「前置 = ywf-infra postgres 环节 + db-migration 建形」（隔离与种子策略、免基础设施逃生门 → 见待裁决问句）。
- **aggregate-blueprint 卷 §5 骨架组成法**：聚合根表模板形状换为上述 PG 原生形状；`{agg}` 双名示例换为单数领域词示例（`sales_order.sales_order`，命名法入条款）。
- **编码公约域（涉 naming 各卷）**：新立「schema/表/列命名法」条款——领域词单数；SQL 保留字冲突时升级行业通用术语（order→sales_order），禁引号/前缀逃逸；外部工具账表住各自独立 schema（liquibase 案例），与业务聚合 schema 不混列。
- **横切卷教学引用同步**：write-chain / read-chain / optimistic-lock / prohibitions / modules/pg 各卷内 `create_at`/`is_delete`/`orders.orders` 形状引用逐处换形（纯形状跟随，无行为语义变化）。
- **框架消费指引**：审计列名与框架默认（`createAt`/`updateAt`）分歧时经 `ywf.ddd.audit.*` 配置桥接——该桥接路径写入模块法卷 pg/ddd 相关条款的消费示例。

## 不做（范围边界）

- **不改框架代码默认值**：`AuditProperties` 缺省保持 `createAt`/`updateAt`（改默认 = 破坏所有消费方的独立裁决，本案只立配置桥接的正当性；是否换默认 → 问句①）。
- **不动 common 模块自身测试树**：框架模块单测/双源路由测试继续 H2 自洽（框架的测试对象是自己，不是 PG 方言生态；「真库测试」裁决辖域 = sample 集成测试基座）。
- **不动 `archive/` 与 `decisions/` 既有卷宗**：旧案卷记录当时法，不回改（卷宗法）。
- **不含 sample 业务契约换形**：order/product 现行册的表名/字段/上限引用修订另案（镜像区 `2026-09-order-product-pg-shape`），两案独立批准。
- **H2 不出框架生态**：仅从 sample 测试基座退役；sample pom 的 h2 依赖与 `src/test/resources/schema.sql` 删除属实施细节，进 tasks。

## 待裁决问句（批准前请一并拍板）

1. **`AuditProperties` 默认值**：保持 `createAt`/`updateAt`（消费方配置桥接），还是随新形改默认为 `createdAt`/`updatedAt`（破坏性，全下游波及）？起草人倾向：保持默认 + 桥接（默认值动一发牵全局，本案仅 sample 一个消费方时不值得预支）。
2. **真库测试的数据隔离策略**：a) `@Transactional` 测试级回滚（现有习惯延续）；b) `@Sql TRUNCATE ... RESTART IDENTITY` 测试前清场（防已提交残留）；c) 组合（推荐：D 型基类挂 a，非事务并发教例 OptimisticLockConcurrencyTest 类挂 b）。delta 阶段展开为条款。
3. **「clone 即全绿」逃生门**：无 PG 的贡献者要不要补一个可选 testcontainers profile？（推荐：本案不做，留 README 前置说明；testcontainers 引入 = 新依赖新裁决，独立案。）
