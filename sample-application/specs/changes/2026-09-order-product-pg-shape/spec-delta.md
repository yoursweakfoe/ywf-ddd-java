# 对 current/order.md 与 current/product.md 的增删改

> 只描述变化量（delta 教义）。本案为**纯形状随动案**：业务行为语义零变化——改动仅涉三类表面：表全名引用、审计/软删列名 token、取证锚点重定向（sample `schema.sql` 退役）。
> ⚖Q⑤ 绑定声明：按推荐答案起草——Java 类名**保留** `Order`/`Product`，`sales_order` 仅作库层 UB 全称；`OrderCO.id` 维持 String（PR-4 记录的两聚合历史差异，本 bill 不统型）。裁决若改，本 delta 随裁改写后再施工。
> 行号锚点：折叠时以实施态工作区重定基线（现行册前言「2026-09-06 为准」行随之刷新）。

## ADDED Requirements

### Requirement: OS-S/PS-S（形状权威引用条款，两册各落一行）
两册现行本 SHALL 不复述表形状/命名条款——形状权威链：框架卷 BP-S1~S3（`aggregate-blueprint.md`）→ `db-migration/src/main/resources/db/changelog/ddd_sample_application/changes/*.sql`（唯一 DDL 事实，双库实证同形）。本册仅在「上限对齐」括注中引用全名，不常驻形状。
#### Scenario: 形状再演进时本册的动作
- GIVEN 未来需要新列/改列/索引优化
- WHEN 形状经 db-migration 新变更集 + 框架法案通道演进
- THEN 本册仅随动全名引用行（如 OW-1.1 的 VARCHAR 上限括注），形状本体永不入册

## MODIFIED Requirements

### Requirement: OW-1.1（400 层上限引用换全名）
- **OW-1.1（400 层）** `customerId` 必填且 ≤50 字符（对齐 `sales_order.sales_order.customer_id VARCHAR(50)`）；`items` 非空；每项 `productId` 必填、`quantity ≥ 1`。越界 → 400 `fieldErrors`，不落库不触库存。（源：`C/order/dto/command/PlaceOrderCommand.java`（行号折叠定基）；实证 `RestEndpointIntegrationTest.java` 51 字符 → 400 用例）

### Requirement: OW-4.2（同上换全名）
- **OW-4.2（400 层）** `trackingNumber` 必填（`@NotBlank`）且 ≤100（对齐 `sales_order.sales_order.tracking_number VARCHAR(100)`）；超长 → 400，**与订单存在性无关**，不再穿透 DB 变 500。（源：`ShipOrderForm.java`；实证 `shipOversizedTrackingNumber_returns400_noPayloadEcho`）

### Requirement: OR-2（内部 DTO 审计字段名随 BP-S1；id(String) 维持）
- **OR-2** `OrderCO` 投影面恰为 `{id(String), status(契约枚举), items[{productId,quantity,unitPrice}], totalAmount, customerId, trackingNumber, cancelReason}`；**审计时间（内部 DTO 字段 `createdAt`/`updatedAt`，随 BP-S1 更名）与乐观锁 version 不外泄**（Presenter 不映射即不暴露）。（源：`C/order/dto/co/OrderCO.java` 字段面、`S/application/order/presenter/OrderViewPresenter.java`）

### Requirement: OR-9（排序列名 token）
- **OR-9** 过滤条件可选（`status`/`customerId` 均 null=不过滤），结果按 `created_at DESC` 排序；取数与计数两条语句共享同一 WHERE 片段（防两口径漂移）。（源：`sample-service-server/src/main/resources/mapper/order/OrderMapper.xml` `pageCondition`/`selectPageByCondition`/`countByCondition`）

### Requirement: OR-11（软删列名 token）
- **OR-11** 已逻辑删除行（`is_deleted=true`）对所有读不可见。（源：`OrderMapper.xml` 全部 select/delete 条件含 `is_deleted = false`）

### Requirement: OI-3（版本条件 SQL 文本 token）
- **OI-3（版本条件 SQL 文本）** 乐观锁由手写 XML 的 `SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_deleted = false` 文本自身承担（无运行时拦截器）；每次 UPDATE 全列覆写。（源：`resources/mapper/order/OrderMapper.xml` updateById；全量 UPDATE 理由见 decisions/ ADR-0002，不复述）

### Requirement: OI-4（UUIDv7 取证锚点重定向——schema.sql 退役）
- **OI-4（创建即合法 + 双扇门）** 聚合构造恒两扇门：新建=`OrderFactory.create`（铸 UUIDv7 + 立即 place() 校验），重建=`Order.reconstitute`（惰性）；业务构造器包私有，「谁能 new 订单」由包结构编译期锁死。（源：`Order.java` javadoc、`OrderFactory.java`；UUIDv7 策略：`OrderFactory.java` javadoc + `db-migration/src/main/resources/db/changelog/ddd_sample_application/changes/0001-init-schema.sql` id 列定义 `DEFAULT uuidv7()`（DB 默认兜手工插入，正常路径工厂铸造——旧 `schema.sql:2-4` 注记锚点随 H2 退役重定向于此））

### Requirement: OI-10（审计列名 token + 填充事实精确化）
- **OI-10（时间/操作人）** 时间字段统一 `OffsetDateTime`；`created_at`/`updated_at` 由应用层 `AuditFieldFiller` 在持久化前填充（DB 列带 `DEFAULT now()` 仅兜手工插入，正常写路径填充权在应用，非 DB 触发器），时钟经注入 `Clock`。（源：`Order.java` 字段面、`db-migration .../0001-init-schema.sql` 审计列定义、`MybatisPersistence.java` fillInsert/fillUpdate；理由见 decisions/ ADR-0006 与框架卷 BP-S1，不复述）

### Requirement: PW-1.1（上限全名 + 取证锚点重定向）
- **PW-1.1（400 层）** `name` 必填 ≤100（对齐 `product.product.name VARCHAR(100)`）；`price` 必填、**严格 >0**（`@DecimalMin(0, inclusive=false)`）、精度 `@Digits(integer=8, fraction=2)`（对齐 `NUMERIC(10,2)`，BP-S1 惯用名）；`stock ≥ 0`。越界 → 400 `fieldErrors`。（源：`C/product/dto/command/CreateProductCommand.java`；`db-migration .../0001-init-schema.sql` product.product 列宽佐证——旧 `schema.sql:31-42` 锚点重定向）

### Requirement: PW-1.3（审计列名 token）
- **PW-1.3（结果）** 成功 → 200 `ProductCO{id, name, price, stock}`，`created_at/updated_at/version` 不外泄（Presenter 不映射）。（源：`C/product/dto/co/ProductCO.java` 字段面、`S/application/product/presenter/ProductPresenter.java`；实证 `createProduct_returns200`——命令未传 id，响应含服务端铸造 id）

### Requirement: PW-2.3（软删 token）
- **PW-2.3（持久化路径）** 每次扣/补经 `productRepository.update` 走乐观锁全列 UPDATE（`SET version = version + 1 WHERE ... AND version = #{version} AND is_deleted = false`），影响 0 行按三分通道处理（见 PI-3）。（源：`sample-service-server/src/main/resources/mapper/product/ProductMapper.xml` updateById、`ProductRepositoryImpl.java`）

### Requirement: PR-5（审计/软删 token）
- **PR-5** 投影面恰为 `{id, name, price, stock}`；`created_at/updated_at/version` 不外泄；已逻辑删除行（`is_deleted=true`）不可见。（源：`ProductViewPresenter.java`、`ProductMapper.xml` selectById `WHERE id AND is_deleted = false`）

## REMOVED Requirements

（无条款删除。旧 `sample-service-server/src/test/resources/schema.sql` 的取证锚点全部重定向至 `db-migration` 变更集——H2 基座退役属框架案 TC-5/TC-9 通道，非本册行为条款废止。）
