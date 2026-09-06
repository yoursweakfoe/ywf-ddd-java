# Product 行为契约（现行本）

> **本区=法律**：代码违反它 = 改代码，或走 `../changes/` 流程修法；**禁止为迁就代码偷改本文**。
> 每条断言括注取证依据（文件:行 或 测试名）。源码未覆盖的意图空白用 `<!-- 待 changes/ 补全 -->` 标注，不脑补。

**路径缩写**同 `order.md` 前言（`C/` `S/` `T/` `CM/`；行号以 2026-09-06 工作区为准）。

## 1. 状态机

- **PS-1** Product **无状态机**：聚合不持 status 字段，CO/命令面亦无——商品生命周期只有「创建后存在」与「库存数量变化」两个事实。（源：`S/domain/product/model/Product.java` 全字段面无 status、`C/product/dto/co/ProductCO.java` 字段面）
- **PS-2** 因此本聚合的行为守卫不是状态守卫而是**数量守卫**：`deductStock` 前校验 `quantity > 0` 与 `stock >= quantity`，违反即拒（无静默部分扣减）。（源：`Product.java:73-83`）

## 2. 全局门面（校验层分界）

同 order.md §2（OG-1…OG-4 为全仓框架级通道，Product 面适用证据：`T/integration/RestEndpointIntegrationTest.java:95-108`——不存在商品 → 422 `product:err.notFound`，RFC 9457 形）。不再复述。

## 3. 写用例

### 3.1 创建商品（PW-1）　`POST /api/products`　`@Valid @RequestBody CreateProductCommand` → 200 `ProductCO`

（端点：`C/product/adapter/rest/controller/ProductController.java:24-35`，`@Operation`「新增商品并初始化库存」；`/api` 前缀：`application.yml:13`）

- **PW-1.1（400 层）** `name` 必填 ≤100（对齐 `products.products.name VARCHAR(100)`）；`price` 必填、**严格 >0**（`@DecimalMin(0, inclusive=false)`）、精度 `@Digits(integer=8, fraction=2)`（对齐 `DECIMAL(10,2)`）；`stock ≥ 0`。越界 → 400 `fieldErrors`。（源：`C/product/dto/command/CreateProductCommand.java:29-42`；`sample-service-server/src/test/resources/schema.sql:31-42` 列宽佐证）
- **PW-1.2（创建即合法）** 新建只经 `ProductFactory.create`：铸造 UUIDv7 身份 + 立即 `validate()`（`product:err.nameRequired / priceRequired / priceNegative / stockNegative`），业务构造器包私有；id 在持久化之前即存在，**自增反查路径已消亡**（无 findByName）。（源：`S/domain/product/model/ProductFactory.java:29-33`、`Product.java:16-19,39-40` javadoc、`S/application/product/handler/command/CreateProductHandler.java:36-39` 注释；实证 `T/application/product/handler/command/CreateProductHandlerTest.java:41-73`——含 `id.version()==7` 断言）
- **PW-1.3（结果）** 成功 → 200 `ProductCO{id, name, price, stock}`，`create_at/update_at/version` 不外泄（Presenter 不映射）。（源：`C/product/dto/co/ProductCO.java` 字段面、`S/application/product/presenter/ProductPresenter.java:19-26`；实证 `RestEndpointIntegrationTest.java:63-79` `createProduct_returns200`——注意命令未传 id，响应含服务端铸造 id）
- **PW-1.4（命令严于领域，如实记载）** 命令层要求 `price > 0`，领域不变量只要求 `price >= 0`（`priceNegative` 仅在 `< 0` 时触发）——价格恰为 0 的商品**无法经 REST 面创建**，但领域与存储层容许其存在。（源：`CreateProductCommand.java:36` vs `Product.java:106-108`）<!-- 待 changes/ 补全：两层价格下界以谁为准（免费商品是否合法）尚无裁决 -->

### 3.2 扣减 / 回补库存（PW-2）——**内部用例，无直接 REST 面**

`Product.deductStock/restoreStock` 只被 `InventoryDomainService` 在 Order 用例事务内调用：下单扣减（order.md OW-1）、取消回补（OW-7.4）。契约面（`ProductController`）不暴露这两个动作——库存变更的唯一线上入口是订单。

- **PW-2.1（扣减守卫）** `quantity ≤ 0` → `product:err.quantityMustBePositive`（422）；`stock < quantity` → `product:err.insufficientStock`（422，携 `params {productId, required, available}` 供前端渲染）；通过则 `stock -= quantity`。（源：`Product.java:73-83`；实证 `T/integration/RestEndpointIntegrationTest.java:131-147`、`T/domain/shared/service/InventoryDomainServiceTest.java:55-62`）
- **PW-2.2（回补守卫）** `quantity ≤ 0` → 同键拒绝；通过则 `stock += quantity`，**无上界校验**。（源：`Product.java:89-93`；实证 `InventoryDomainServiceTest.java:65-72`）<!-- 待 changes/ 补全：restoreStock 无库存上界/溢出防线，是否需要容量语义未裁决 -->
- **PW-2.3（持久化路径）** 每次扣/补经 `productRepository.update` 走乐观锁全列 UPDATE（`SET version = version + 1 WHERE ... AND version = #{version} AND is_delete = false`），影响 0 行按三分通道处理（见 PI-3）。（源：`sample-service-server/src/main/resources/mapper/product/ProductMapper.xml` updateById、`S/infrastructure/persistence/master/product/repository/ProductRepositoryImpl.java:71-75`）
- **PW-2.4（批量协调契约）** 多商品扣/补：单次 IN 加载、同商品数量合并为一次聚合调用+一次 UPDATE、UPDATE 顺序按 productId 全局升序（锁序契约）。详述见 order.md OI-7（同一事实唯一登记位：`S/domain/shared/service/InventoryDomainService.java:26-38,76-93`；实证 `InventoryDomainServiceTest.java:90-102`）。

### 3.3 无其他写面

Product 契约面无修改（价格/名称不可变）、无删除、无上下架端点。<!-- 待 changes/ 补全：改价/下架若成需求必须走 changes/ 新开用例，现行代码无任何旁路 -->

## 4. 读用例

### 4.1 详情　`GET /api/products/{productId}` → 200 `ProductCO`

- **PR-1** 读路径绕过聚合根：`ProductQueryRepository` PO→`ProductViewDTO` 直投影。（源：`S/application/product/handler/query/GetProductHandler.java:21-25`、`S/infrastructure/persistence/master/product/repository/ProductQueryRepositoryImpl.java:26-44`）
- **PR-2** 不存在 → 422 `product:err.notFound`。（源：`GetProductHandler.java:24`；实证 `RestEndpointIntegrationTest.java:95-108`——断言 `detail == "product:err.notFound"`、`title == "Business Error"`、`type == "about:blank"`）
- **PR-3** 非法 UUID 形状 → 400 绑定层拒绝（OG-2 同型）。（源：`ProductController.java:42-46` `@PathVariable UUID` + `GlobalRestExceptionHandler.handleTypeMismatch`）
- **PR-4** `ProductCO.id` 为 **UUID 类型**（Jackson 序列化为字符串）——与 `OrderCO.id`（String 字段）不同型，系两聚合历史差异的现行事实，如实入约。（源：`C/product/dto/co/ProductCO.java:25` vs `C/order/dto/co/OrderCO.java:27`）
- **PR-5** 投影面恰为 `{id, name, price, stock}`；`create_at/update_at/version` 不外泄；已逻辑删除行（`is_delete=true`）不可见。（源：`ProductViewPresenter.java:18-25`、`ProductMapper.xml` selectById `WHERE id AND is_delete = false`）
- **PR-6** **无分页/列表端点**——分页教义（1 起/上限 1000）现行仅 Order 面适用（order.md §4.2）。（源：`C/product/adapter/rest/controller/ProductController.java` 全部 2 端点）

## 5. 不变量

- **PI-1（库存非负）** `stock >= 0` 恒成立：领域层 `deductStock` 守卫 + 聚合 `validate()`（`product:err.stockNegative`）在每次 save/update 复核双重执法。（源：`Product.java:77-81,110-112`、`MybatisPersistence.java:212,274` `validateIfAggregate`）
- **PI-2（防超卖守恒）** 并发下单下同一商品：剩余库存 ≥0、成功数+剩余=初始（严格守恒）——乐观锁版本条件而非行锁承担并发正确性。（实证 `T/integration/OptimisticLockConcurrencyTest.java:118-125`；SQL 依据 `ProductMapper.xml` updateById 版本条件）
- **PI-3（乐观锁三分通道）** 与 order.md OI-1 同一框架事实：UPDATE 0 行+实体在 → `OptimisticLockConflictException`(409 可重试)；UPDATE 0 行+实体没 → ISE(409 不重试)；INSERT/DELETE 0 行 → `SilentWriteLossException`(500+ERROR)。论证指针：decisions/ ADR-0002、ADR-0013（不复述）。Product 是这条通道的**主要受试方**（下单/取消冲突全部落在 Product 行上，源：`InventoryDomainService.java:54,69` 的 update 调用位）。
- **PI-4（单价唯一来源）** `Product.price` 是下单订单项单价的唯一来源，客户端不可报价。（源：`Product.java:27-29` 字段注释「下单时订单项单价的唯一来源」、`PlaceOrderHandler.java:61-66`；实证 `PlaceOrderHandlerTest.handle_shouldCreatePendingOrderWithRealUnitPrice`）
- **PI-5（批量加载反 N+1）** 跨聚合取商品一律 `findAllById` 单次 IN；不存在 ID 静默缺席、由调用方守 `product:err.notFound`。（源：`S/domain/product/repository/ProductRepository.java:15-20` javadoc、`InventoryDomainService.java:76-81,103-106`）
- **PI-6（错误码登记）** 本聚合全部错误为 `BusinessException` + i18n 位点 `product:err.{scene}`：`notFound` / `insufficientStock`(携 params) / `quantityMustBePositive` / `nameRequired` / `priceRequired` / `priceNegative` / `stockNegative`（抛出点：`Product.java:75,78,91,100-111`、`InventoryDomainService.java:105`、`PlaceOrderHandler.java:98`、`GetProductHandler.java:24`）。位点约定论证见 decisions/ ADR-0011（不复述）。
- **PI-7（无契约枚举镜像）** Product 面**不存在** domain↔contract 枚举对（无 status 值域），`ContractEnumParityTest` 的 PAIRS 现仅 Order 一行——将来为 Product 引入状态值域时，奇偶锁义务随之生效（先例规则见 order.md OI-8）。（源：`T/contract/ContractEnumParityTest.java:26-30`）

---

**首版不完整声明**：本文件从 2026-09-06 工作区源码反推（行号以当日为准），非逐条人工评审产物；允许首批 `changes/` 逐节替换。文中全部 `待 changes/ 补全` 注记 = 已知意图空白清单，裁决前不构成承诺。
