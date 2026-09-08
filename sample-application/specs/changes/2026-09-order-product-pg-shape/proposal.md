# 订单/产品业务契约随 PG 原生形状与命名法换形

- Slug: 2026-09-order-product-pg-shape ｜ 日期: 2026-09-08 ｜ 关联 ADR: 框架案 `knowledge/specs/changes/2026-09-pg-native-shape-and-real-testdb` 所指 ADR-0033 ｜ 镜像区首案（本目录开册）

## Why（为什么现在改）

order/product 两册现行法记录的形状是旧形：表 `orders.orders`/`products.products`、上限引用 `orders.orders.cancel_reason VARCHAR(500)` 式、审计字段 `createAt`/`updateAt`。库形状权威已迁至 `db-migration` 工程并落地新形（`sales_order.sales_order`/`product.product`、`created_at/updated_at`、`is_deleted`、`version bigint`、id `uuid DEFAULT uuidv7()`）——业务镜像法与库内事实冲突，聚合契约的「上限锁进 javadoc」纪律（契约自述的上游 = DB 列定义）指向了不存在的表。

## What changes（行为级）

- **表名/列全名引用换形**：两册内所有 `orders.orders`→`sales_order.sales_order`、`products.products`→`product.product`，列上限引用随行（`sales_order.sales_order.cancel_reason VARCHAR(500)` 等，数值不变仅全名变）。
- **聚合根审计字段行为条款换名**：`createAt`/`updateAt` → `createdAt`/`updatedAt`（领域模型与 PO 同步；`version` 类型条款 `Integer`→`Long`）。
- **契约面时间字段名**：OrderDTO/ProductDTO（CO 链路）字段 `createAt`/`updateAt` → `createdAt`/`updatedAt`（sample 即教材，契约面与库形状同名是教学一致性；本仓无真实下游，破坏无痛）。
- **id 策略条款改写**：「应用侧工厂铸造 UUIDv7 字符串」→「应用侧工厂铸造 UUIDv7（`java.util.UUID`），DB 列原生 `uuid DEFAULT uuidv7()` 兜手工插入」；上限 `VARCHAR(36)` 相关校验条款随之改「UUID 文本形态」。
- **状态/软删谓词**：读侧条款中「`is_delete = FALSE` 过滤」措辞换 `is_deleted`。

## 不做（范围边界）

- 不改业务行为语义（状态机、校验规则、上限数值一律不动——纯形状/命名/类型跟随）。
- 不预支 Seata 接入（undo_log 落位属 seata 案）。
- 不动框架法（横切形状法的母案在框架区，本案是业务镜像侧随动执行件）。

## 待裁决问句

1. **聚合类名是否随 schema 更名**（`Order`→`SalesOrder`、Java 包 `domain/order`→`domain/salesorder`？）：schema 层已定名 `sales_order`，Java 层保留 `Order` 并在册内记「UB 全称 sales_order，简称 order 用于类名」的映射，或彻底统一。**起草人倾向：类名保留 Order**——「订单」在类/命令语境是自然口语，sales 限定是库/上下文层信息；映射一句写进册子即可。
