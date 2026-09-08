# 实现清单（镜像案 · 按依赖序，[P]=可并行）

> 施工解锁前置：框架案与本案批准门通过 + 五问句拍板（本案侧主要是 ⚖Q⑤；Q①② 的落点在本册 tasks 1~8 生效）。业务代码批与框架案 tasks 第 3~8 步合流，同 PR 折叠。

- [ ] 1. pom：`sample-service-server` 显式引入 `common-pg`（common-pg 的 AutoConfiguration 自动注册 UUID/JSONB 等 TypeHandler，兑现主配置 yml 注释的既有宣称；驱动随之传递），`h2` 依赖退役 → TC-9（框架案）
- [ ] 2. PO 换形：`OrderPO`/`ProductPO`——`id String→UUID`、`version Integer→Long`、`createAt/updateAt→createdAt/updatedAt`、`isDelete→isDeleted`、`createdBy/updatedBy String→UUID`；javadoc 表名指针换全名 → BP-S1/BP-6（框架案）
- [ ] 3. [P] Converter 桥接拆除：`UUID.fromString`/`toString()` 两跳删除，直传 UUID → BP-6
- [ ] 4. [P] RepositoryImpl：`toPersistenceId` 覆写拆除（基类恒等通道即正确形状）→ BP-6
- [ ] 5. Mapper XML 两篇全量换形：表全名（`sales_order.sales_order`/`product.product`）、列名（`created_at/updated_at/is_deleted`）、参数位（`#{createdAt}`/`#{updatedAt}`）、排序位（`ORDER BY created_at DESC`）、delete 审计参数列名 → BP-X1/OR-9/OI-3/PW-2.3/PR-5
- [ ] 6. 域模型随形：`Order`/`Product` 字段与 getter `createAt→createdAt`/`updateAt→updatedAt`、`version Integer→Long`；`reconstitute` 签名随改；调用面传播（Assembler/Presenter/内部 DTO 字段名同改，CO 投影面不变量维持 OR-2/PW-1.3 现状语义）→ OI-10/OR-2
- [x] 7. ~~配置桥接~~（✅Q① 裁决改判：框架缺省直接改 `createdAt`/`updatedAt`，ADR-0033 承载，消费方**零桥接配置**——本条作废，sample yml 不增 audit 键）→ 框架案 tasks 第 7 步
- [ ] 8. 测试基座落地（⚖Q② 推荐案）：`src/test/resources/schema.sql` 删除；`TestOrders`/`OrderFixtures`/各单测随域类签名换形（version long 字面量等）；D 型隔离双轨——`RestEndpointIntegrationTest` 等非事务类挂 `@Sql TRUNCATE ... RESTART IDENTITY` 清场，事务类验证回滚路径 → TC-9
- [ ] 9. 契约 javadoc 上限引用换全名：`PlaceOrderCommand`/`ShipOrderForm`/`CancelOrderCommand`/`CreateProductCommand` 中 `orders.orders.*`/`products.products.*` 括注 → OW-1.1/OW-4.2/PW-1.1
- [ ] 10. 闸门：`mvn -B test` 真 PG 全绿（两库；含并发教例守恒律复验）+ `ddd-review`（末步 check-docs）→ delta 全部
- [ ] 11. 折叠：本 delta 合入 `current/order.md`/`current/product.md`（含前言行号基线刷新 + OS-S/PS-S 入册），本目录整袋移 `sample-application/specs/archive/`（镜像区 archive 首册开册）——**文档同步义务唯一时点**，与框架案同 PR
