# Order 行为契约（现行本）

> **本区=法律**：代码违反它 = 改代码，或走 `../changes/` 流程修法；**禁止为迁就代码偷改本文**。
> 每条断言括注取证依据（文件:行 或 测试名）。源码未覆盖的意图空白用 `<!-- 待 changes/ 补全 -->` 标注，不脑补。

**路径缩写**（均相对仓库根；行号以 2026-09-06 工作区为准）：
- `C/` = `sample-application/sample-service/sample-service-contract/src/main/java/com/yoursweakfoe/sampleapplication/sampleservice/contract/`
- `S/` = `sample-application/sample-service/sample-service-server/src/main/java/com/yoursweakfoe/sampleapplication/sampleservice/`
- `T/` = `sample-application/sample-service/sample-service-server/src/test/java/com/yoursweakfoe/sampleapplication/sampleservice/`
- `CM/` = `ywf-ddd-common/`

## 1. 状态机

- **OS-1** Order 状态值域恰为 7 常量：`PENDING / PAID / CONFIRMED / SHIPPED / DELIVERED / COMPLETED / CANCELLED`，无子状态。（源：`S/domain/order/model/OrderStatus.java`；契约镜像 `C/order/enums/OrderStatus.java:17-39`）
- **OS-2** 每个写状态迁移必须由聚合根 `requireStatus` 守卫先行：当前状态不在允许集内即抛 `BusinessException`，不存在旁路改状态的入口。（源：`S/domain/order/model/Order.java:190-203`；穷尽 switch 编译期锁新增枚举值——`Order.java:188-189` javadoc）
- **OS-3** 新建订单只经 `OrderFactory.create`（包私有构造器收口），返回即 `PENDING` 且已通过不变量校验（「创建即合法」，不存在「构造了但没下单」中间态）。（源：`Order.java:38-48` 注释与构造器、`OrderFactory.java:36-40`、`T/domain/order/model/OrderTest.java:219-234`）
- **OS-4** 持久化重建（`Order.reconstitute`）惰性回填快照，**不校验、不改历史**；它不是状态迁移入口。（源：`Order.java:54-80`、`OrderTest.java:241-256`）
- **OS-5** `place()` 只做不变量校验（`validate()`），状态保持 `PENDING`——它不是迁移。（源：`Order.java:98-100`、`OrderTest.java:30-35`）

**迁移表**（正向=SHALL 允许；未列出的 当前态×动作 组合一律拒绝 422）：

| 当前态 | 动作 | 目标态 | 拒绝时错误键 | 附加副作用 |
|---|---|---|---|---|
| — | create（工厂） | PENDING | 不变量键（见 OW-1.4） | 铸造 UUIDv7 身份 |
| PENDING | pay | PAID | `order:err.status.pending` | —（源：`Order.java:107-110`） |
| PAID | confirm | CONFIRMED | `order:err.status.paid` | —（源：`Order.java:117-120`） |
| CONFIRMED | ship(tn) | SHIPPED | `order:err.status.confirmed` | 记 `trackingNumber`（源：`Order.java:128-132`） |
| SHIPPED | deliver | DELIVERED | `order:err.status.shipped` | —（源：`Order.java:139-142`） |
| DELIVERED | complete | COMPLETED | `order:err.status.delivered` | —（源：`Order.java:149-152`） |
| PENDING / PAID | cancel(reason) | CANCELLED | `order:err.status.cancellable` | 记 `cancelReason` + 同事务回补库存（源：`Order.java:160-164`、`S/application/order/handler/command/CancelOrderHandler.java:35-44`） |

- **OS-6** `COMPLETED` 与 `CANCELLED` 是终态：对二者执行任何迁移动作均 422。（源：迁移表穷尽性；实证 `T/integration/RestEndpointIntegrationTest.java:229-243`——对已取消订单再 cancel → 422 `order:err.status.cancellable`）
- **OS-7** 主链路 `PENDING→PAID→CONFIRMED→SHIPPED→DELIVERED→COMPLETED` 端到端可走通，且每步响应体状态即迁移结果。（源：`RestEndpointIntegrationTest.java:301-330` `lifecycle_confirmShipDeliverComplete_reachesCompleted`）
- **OS-8** 重复执行同一迁移必被拒（非幂等重放安全：第二次 pay 拿 `order:err.status.pending`）。（源：`RestEndpointIntegrationTest.java:287-299` `lifecycle_payAgain_returns422`、`T/domain/order/model/OrderTest.java:110-117`）

## 2. 全局门面（校验层分界）

- **OG-1** 所有错误响应为 RFC 9457 `application/problem+json`：422/400/409/500 通道齐备。（源：`CM/common-exception/.../handler/GlobalRestExceptionHandler.java` 类级 javadoc + `RestEndpointIntegrationTest.java:36-40`）
- **OG-2** **400=绑定层**：`@Valid` Bean Validation、类型转换、参数缺失，响应携 `fieldErrors` 扩展成员，且**不回显客户端原始值**（防输入回显攻击面）。（源：`GlobalRestExceptionHandler` `fieldValidationProblem`/`handleTypeMismatch` javadoc；实证 `RestEndpointIntegrationTest.java:335-386` 三例均断言 body 含字段名、不含超长值本身、<2000 字节）
- **OG-3** **422=领域层**：`BusinessException` 缺省映射 422，`detail` 即 i18n 位点原文（键格式 `{aggregate}:err.{scene}`，服务端不翻译）。（源：`GlobalRestExceptionHandler.DEFAULT_BUSINESS_STATUS`；实证 `RestEndpointIntegrationTest.java:104-107` 断言 `detail == "product:err.notFound"`、`title == "Business Error"`）
- **OG-4** 服务端成功响应为 2xx + CO JSON（代码未标注 `@ResponseStatus`，走 Spring MVC 缺省；集成测试成功路径均按 2xx 走通）。（源：`C/order/adapter/rest/controller/OrderController.java` 全面无状态码注解、`RestEndpointIntegrationTest.java` 各 success 用例）

> 绑定层与领域层的分界判据在**每条写用例**内分别给 SHALL：超长/非法字面量停在 400，**与资源是否存在无关**（源：`RestEndpointIntegrationTest.java:334` 注释「与订单存在性无关」、`:369` 注释「失败先于任何库存触碰」）。

## 3. 写用例

### 3.1 下单（OW-1）　`POST /api/orders`　`@Valid @RequestBody PlaceOrderCommand` → 200 `OrderCO`

（端点：`C/order/adapter/rest/controller/OrderController.java:41-43`；`@Operation` 语义「创建新订单，初始状态为 PENDING」:41；`/api` 前缀=服务器 context-path：`sample-service-server/src/main/resources/application.yml:13`）

- **OW-1.1（400 层）** `customerId` 必填且 ≤50 字符（对齐 `orders.orders.customer_id VARCHAR(50)`）；`items` 非空；每项 `productId` 必填、`quantity ≥ 1`。越界 → 400 `fieldErrors`，不落库不触库存。（源：`C/order/dto/command/PlaceOrderCommand.java:30,31,36,50,55`；实证 `RestEndpointIntegrationTest.java:369-386` `placeOversizedCustomerId_returns400_noPayloadEcho`（51 字符 → 400））
- **OW-1.2（语义）** 订单项单价**取自商品当时真实 `price`**，客户端无传价通道。（源：`S/application/order/handler/command/PlaceOrderHandler.java:61-66`；实证 `T/application/order/handler/command/PlaceOrderHandlerTest.java:60-75` `handle_shouldCreatePendingOrderWithRealUnitPrice`——断言落库订单 unitPrice=25.50 而非命令侧值）
- **OW-1.3（422 层）** 商品不存在 → `product:err.notFound`；库存不足 → `product:err.insufficientStock`（携 `params: {productId, required, available}`）。（源：`PlaceOrderHandler.java:98`、`S/domain/product/model/Product.java:78-80`；实证 `RestEndpointIntegrationTest.java:149-163`、`:131-147`）
- **OW-1.4（原子性）** 多商品单中任一失败（不存在/不足）→ **整单不持久化**（同一 `@Transactional` 内先扣库存后建单，任何异常整体回滚）。（源：`PlaceOrderHandler.java:58,69-73`；实证 `PlaceOrderHandlerTest.java:86-107` `handle_shouldNotSaveWhenAnyStockInsufficient`——`verify(orderRepository, never()).save(any())`）
- **OW-1.5（不变量兜底）** 创建即合法：订单项为空 / customerId 缺失 / 总金额 ≤0 在工厂处抛 `order:err.itemsEmpty` / `order:err.customerIdRequired` / `order:err.totalMustBePositive`（422）。（源：`S/domain/order/model/Order.java:170-180`、`OrderFactory.java:36-40`；实证 `OrderTest.java:227-234`）
- **OW-1.6（乐观锁重试，仅本用例）** 下单是唯一经重试包装的写用例：捕获 `OptimisticLockConflictException` 至多 3 次尝试、指数退避（基数 100ms）；每次尝试独立事务，冲突尝试已整体回滚；`BusinessException`（如 422 库存不足）与「实体消失」类 `IllegalStateException` **不重试原样上抛**；重试耗尽 → 上抛 → HTTP 409。（源：`S/application/order/service/OrderAppService.java:43,55` 注入 `RetryablePlaceOrderHandler`；`S/application/order/handler/command/RetryablePlaceOrderHandler.java:37-38,63-77`；实证 `T/application/order/handler/command/RetryablePlaceOrderHandlerTest.java:43-88` 四例）
- **OW-1.7（结果）** 成功 → 200 `OrderCO`，`status=PENDING`，含铸造的 `id`。（实证 `RestEndpointIntegrationTest.java:112-129`）

### 3.2 支付（OW-2）　`PUT /api/orders/{orderId}/pay`（无请求体）→ 200 `OrderCO`

（端点：`OrderController.java:51-54`；Adapter 以路径段组装 `PayOrderCommand(UUID orderId)`：`S/adapter/rest/controller/OrderControllerImpl.java:43-45`）

- **OW-2.1（400 层）** `orderId` 非法 UUID → 400 类型转换拒绝，不进领域。（源：`C/order/dto/query/GetOrderQuery.java:23` javadoc 同型约定 + `GlobalRestExceptionHandler.handleTypeMismatch`）
- **OW-2.2（422 层）** 订单不存在 → `order:err.notFound`；状态非 PENDING → `order:err.status.pending`。（源：`S/application/order/handler/command/PayOrderHandler.java:33-34`、`Order.java:108`；实证 `RestEndpointIntegrationTest.java:287-299`）
- **OW-2.3（边界声明）** 本端点是**裸状态迁移**：无支付凭证、无网关对账参数——真实支付集成不在现行契约内。<!-- 待 changes/ 补全：支付需携带交易凭证（如 thirdPartyTradeNo）与回调确认流 -->

### 3.3 确认（OW-3）　`PUT /api/orders/{orderId}/confirm` → 200 `OrderCO`

- **OW-3.1** 仅 PAID 可确认（`order:err.status.paid`），成功 → CONFIRMED；不存在 → `order:err.notFound`。（源：`Order.java:117-120`、`ConfirmOrderHandler.java:33-34`；实证 `T/application/order/handler/command/ConfirmOrderHandlerTest.java:37-64`、`RestEndpointIntegrationTest.java:306-310`）

### 3.4 发货（OW-4）　`PUT /api/orders/{orderId}/ship?trackingNumber=...` → 200 `OrderCO`

- **OW-4.1（线上形状，如实入约）** 物流单号经 **URL 查询参数**绑定到 `ShipOrderForm`（`@Valid @ModelAttribute`），wire 形态 `?trackingNumber=...`（自 `@RequestParam` 时代保持不变）；完整 `ShipOrderCommand`（含 orderId）由 Adapter 组装——`ShipOrderForm` **不是 CQE**，只是运输层绑定载体。（源：`OrderController.java:70-82`、`C/order/dto/command/ShipOrderForm.java:10-19`、`OrderControllerImpl.java:53-56`；实证 `RestEndpointIntegrationTest.java:312-317` 以查询参数发货成功）
- **OW-4.2（400 层）** `trackingNumber` 必填（`@NotBlank`）且 ≤100（对齐 `orders.orders.tracking_number VARCHAR(100)`）；超长 → 400，**与订单存在性无关**，不再穿透 DB 变 500。（源：`ShipOrderForm.java:14,18-19`；实证 `RestEndpointIntegrationTest.java:334-349` `shipOversizedTrackingNumber_returns400_noPayloadEcho`——对随机不存在 UUID 发货仍先 400）
- **OW-4.3（422 层）** 状态非 CONFIRMED → `order:err.status.confirmed`；不存在 → `order:err.notFound`。（源：`Order.java:129`、`ShipOrderHandler.java:33-34`；实证 `T/application/order/handler/command/ShipOrderHandlerTest.java:51-57`）
- **OW-4.4（结果）** 成功 → SHIPPED，`trackingNumber` 入 CO。（实证 `RestEndpointIntegrationTest.java:312-317`、`OrderTest.java:55-63`）

### 3.5 签收（OW-5）　`PUT /api/orders/{orderId}/deliver` → 200 `OrderCO`

- **OW-5.1** 仅 SHIPPED 可签收（`order:err.status.shipped`）→ DELIVERED；不存在 → `order:err.notFound`。（源：`Order.java:139-142`、`DeliverOrderHandler.java:33-34`；实证 `T/application/order/handler/command/DeliverOrderHandlerTest.java:36-52`）

### 3.6 完成（OW-6）　`PUT /api/orders/{orderId}/complete` → 200 `OrderCO`

- **OW-6.1** 仅 DELIVERED 可完成（`order:err.status.delivered`）→ COMPLETED（终态）；不存在 → `order:err.notFound`。（源：`Order.java:149-152`、`CompleteOrderHandler.java:33-34`；实证 `T/application/order/handler/command/CompleteOrderHandlerTest.java:37-63`）

### 3.7 取消（OW-7）　`PUT /api/orders/{orderId}/cancel`　`@Valid @RequestBody CancelOrderCommand` → 200 空体

- **OW-7.1（身份单源）** 订单 ID 唯一事实源是**路径参数**；请求体只需 `reason`——命令体内的 `orderId` 字段标 `@Schema(hidden)`，由 Adapter 注入，客户端传了也不采信。（源：`C/order/dto/command/CancelOrderCommand.java:14-36`、`OrderControllerImpl.java:69-72`；实证 `RestEndpointIntegrationTest.java:211-219` 请求体 orderId=null 仍成功）
- **OW-7.2（400 层）** `reason` 必填 ≤500（对齐 `cancel_reason VARCHAR(500)`）；501 字符 → 400 `fieldErrors`，与订单存在性无关。（源：`CancelOrderCommand.java:35-36`；实证 `RestEndpointIntegrationTest.java:351-367` `cancelOversizedReason_returns400_noPayloadEcho`）
- **OW-7.3（422 层）** 仅 PENDING/PAID 可取消，否则 `order:err.status.cancellable`；不存在 → `order:err.notFound`。（源：`Order.java:160-164`、`CancelOrderHandler.java:38-39`；实证 `RestEndpointIntegrationTest.java:229-243`、`T/application/order/handler/command/CancelOrderHandlerTest.java:73-90`）
- **OW-7.4（补偿原子化）** 取消与库存回补**同事务直调**（`InventoryDomainService.replenishStock`）：回补失败整体回滚，接口返回即已回补，无异步等待；成功后商品库存回到下单前值。（源：`CancelOrderHandler.java:35-44` javadoc「同生共死」；实证 `RestEndpointIntegrationTest.java:245-259` `afterCancelOrder_stockReplenished`（100→扣2→回补→100））
- **OW-7.5（缺口）** PAID 单取消后无任何退款/对账动作——仅状态与库存事实。<!-- 待 changes/ 补全：PAID 态取消的退款流程契约位 -->

<!-- 待 changes/ 补全：全聚合无所有权/身份校验——customerId 是输入不是凭证，任何调用方可对任意订单执行 pay/cancel（现行「身份不投影」姿势，论证见 decisions/ 判例，不复述；是否收紧归 changes/） -->
<!-- 待 changes/ 补全：下单无幂等键——同参重复提交产生多笔订单（源码无任何去重路径） -->
<!-- 待 changes/ 补全：无订单修改/删除 REST 面（domain Repository 具备 deleteById，契约面刻意不暴露） -->

## 4. 读用例

### 4.1 详情　`GET /api/orders/{orderId}` → 200 `OrderCO`

- **OR-1** 读路径**绕过聚合根**：查询端口 PO→读 DTO 直接投影，不 reconstitute、不在读侧算派生值。（源：`S/application/order/handler/query/GetOrderHandler.java:23-27`、`S/infrastructure/persistence/master/order/repository/OrderQueryRepositoryImpl.java:67-76`）
- **OR-2** `OrderCO` 投影面恰为 `{id(String), status(契约枚举), items[{productId,quantity,unitPrice}], totalAmount, customerId, trackingNumber, cancelReason}`；**审计时间（createAt/updateAt）与乐观锁 version 不外泄**（Presenter 不映射即不暴露）。（源：`C/order/dto/co/OrderCO.java` 字段面、`S/application/order/presenter/OrderViewPresenter.java:19-38` 注释与实现）
- **OR-3** 不存在 → 422 `order:err.notFound`（合法 UUID 形状但不存在）。非法 UUID 形状 → 400（OG-2）。（实证 `RestEndpointIntegrationTest.java:181-192`——全零 UUID → 422）
- **OR-4** 订单项从 `items` TEXT 列 JSON 反序列化直投读 DTO（不经领域值对象）。（源：`OrderQueryRepositoryImpl.java:82-91`、`S/infrastructure/persistence/master/order/converter/OrderConverter.java`「订单项列表以 JSON 格式存储于 TEXT 列」）

### 4.2 分页　`GET /api/orders/page?status=&customerId=&pageNum=&pageSize=` → 200 `PageResult<OrderSummaryCO>`

- **OR-5** 查询条件经 **URL 查询参数**绑定 record `GetOrderPageQuery`（`@Valid`，无请求体——GET 语义可缓存可书签）。（源：`OrderController.java:129-139`；实证 `RestEndpointIntegrationTest.java:194-205`）
- **OR-6（分页教义）** 页码**从 1 开始**、页大小**上限 1000**（`@Max(PageableQuery.MAX_PAGE_SIZE)`）；**无缺省注入**——pageNum/pageSize 缺参绑 0，被 `@Min(1)` 拒 → **400**，须显式传入；`safePageNum()/safePageSize()`（钳 1..1000）是仓储执行侧**第二道防线**（护未走 @Valid 的直调），非默认值机制；`DEFAULT_PAGE_SIZE=20` 仅为建议值，注入与否属消费方策略。〔2026-09-06 折叠自 archive/2026-09-pagequery-default-claim〕（源：`C/order/dto/query/GetOrderPageQuery.java` javadoc/Schema、`CM/common-contract/.../query/PageableQuery.java` 常量注释、`Oq/OrderQueryRepositoryImpl.java:46-52`）
- **OR-7（双通道钳制）** 读仓储实现一律消费 `safePageNum()/safePageSize()`（钳制 `1..1000`）：即使调用点未触发 Bean Validation，也不产生非法分页或超大分页拖库。（源：`PageableQuery.java:78-91`、`OrderQueryRepositoryImpl.java:46-52`；offset 用 long 乘法防大页码 int 溢出 :50-52）
- **OR-8** `status` 过滤类型为契约枚举（非自由字符串）：非法字面量在 binding 层直接 400 typeMismatch——显式失败优于静默空页；实现侧按枚举常量名比对 SQL `status` 列。（源：`C/order/enums/OrderStatus.java:4-8` javadoc、`GetOrderPageQuery.java:8-10` 参数注、`OrderQueryRepositoryImpl.java:53-55`）<!-- 待 changes/ 补全：非法 status 字面量→400 目前只有 javadoc 声明，无集成测试实证 -->
- **OR-9** 过滤条件可选（`status`/`customerId` 均 null=不过滤），结果按 `create_at DESC` 排序；取数与计数两条语句共享同一 WHERE 片段（防两口径漂移）。（源：`sample-service-server/src/main/resources/mapper/order/OrderMapper.xml` `pageCondition`/`selectPageByCondition`/`countByCondition`）
- **OR-10** 出参信封 `PageResult{records, total, pageNum, pageSize}`（契约层 record，不可变 + 防御拷贝）；`records` 为 `OrderSummaryCO{id,status,totalAmount,customerId}`——**列表不含订单项明细**（与详情 CO 刻意分面）。（源：`CM/common-contract/.../query/PageResult.java:44-48` 及其 javadoc、`C/order/dto/co/OrderSummaryCO.java`、`OrderController.java:137` `@Operation` description、`OrderAppService.java:120-121` map→presentSummary）
- **OR-11** 已逻辑删除行（`is_delete=true`）对所有读不可见。（源：`OrderMapper.xml` 全部 select/delete 条件含 `is_delete = false`）


## 5. 不变量

- **OI-1（乐观锁三分通道）** 影响行数 0 的分类处理：①UPDATE 0 行且实体仍在 → `OptimisticLockConflictException`（**409**，唯一可重试类别；仅下单自动重试，见 OW-1.6）；②UPDATE 0 行且实体已消失 → 普通 `IllegalStateException`（**409**，不重试）；③INSERT/DELETE 0 行 → `SilentWriteLossException`（**500+ERROR 告警通道**，重试无意义）。论证与判例：见 `knowledge/decisions/` ADR-0002（全量 UPDATE 决策）、ADR-0013（ISE→409 通道）；SilentWriteLoss→500 独立通道暂无专文 ADR <!-- 待 changes/ 补全：WP-3 若收编 B4 判例则回填编号 -->。（源：`CM/common-ddd/.../persistence/MybatisPersistence.java:211-220,273-303,339` 及 `throwUpdateFailed:289-303`、`CM/common-exception/.../type/OptimisticLockConflictException.java` + `SilentWriteLossException.java` javadoc、`GlobalRestExceptionHandler` 映射表）
- **OI-2（防超卖守恒律）** 并发对同一商品下单：剩余库存 ≥0；成功订单数 ≤ 初始库存；**成功数 + 剩余库存 = 初始库存**（严格守恒）；每请求必被归类（200/409/422/500/传输失败五桶求和=请求数）。（实证 `T/integration/OptimisticLockConcurrencyTest.java:118-125`——20 线程×1 件 vs 库存 10，@Tag("stress")）
- **OI-3（版本条件 SQL 文本）** 乐观锁由手写 XML 的 `SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false` 文本自身承担（无运行时拦截器）；每次 UPDATE 全列覆写。（源：`resources/mapper/order/OrderMapper.xml` updateById；全量 UPDATE 理由见 decisions/ ADR-0002，不复述）
- **OI-4（创建即合法 + 双扇门）** 聚合构造恒两扇门：新建=`OrderFactory.create`（铸 UUIDv7 + 立即 place() 校验），重建=`Order.reconstitute`（惰性）；业务构造器包私有，「谁能 new 订单」由包结构编译期锁死。（源：`Order.java:38-48` javadoc、`OrderFactory.java:12-31`；UUIDv7 策略：`OrderFactory.java:19-22` javadoc + `sample-service-server/src/test/resources/schema.sql:2-4` 注记）
- **OI-5（不变量每次写复核）** `save/update` 持久化前框架自动再调 `validate()`：items 非空、customerId 非空、totalAmount>0 在任何一次落库时都成立（不是仅创建时）。（源：`MybatisPersistence.java:212,274` `validateIfAggregate`、`Order.java:170-180`）
- **OI-6（派生值物化）** `totalAmount = Σ(quantity × unitPrice)` 于创建时计算并物化（写侧算、读侧只投影存储值）；订单项值对象自身守 `order:err.productIdRequired / quantityMustBePositive / unitPriceRequired`。（源：`Order.java:47,209-213`、`S/domain/order/model/OrderItem.java:17-32`；实证 `OrderTest.java:209-216` 25.50 合计）
- **OI-7（跨聚合协调三纪律）** 库存批量操作遵守：①商品**单次 IN 查询**加载（禁 N+1）；②同商品多订单项数量**合并为一次聚合调用+一次 UPDATE**（防对同一聚合连续两次乐观锁踩空）；③全部 Product 更新按 **productId 全局升序**（TreeMap）统一锁序，消除交叉持锁死锁。（源：`S/domain/shared/service/InventoryDomainService.java:26-38,76-93`；实证 `T/domain/shared/service/InventoryDomainServiceTest.java:90-103` 合并仅 update 一次；`S/domain/product/repository/ProductRepository.java:15-20` findAllById 契约）
- **OI-8（枚举奇偶锁）** domain `OrderStatus` 与 contract `OrderStatus` 必须恒为同一值域（任一侧增删/改名 → 构建期红）；wire 值=常量名；消费方遇未知字面量硬失败（反序列化异常），须先升级 contract jar 再消费新值。（源：`T/contract/ContractEnumParityTest.java:26-50`、`C/order/enums/OrderStatus.java:4-15`；呈现层 `valueOf` 收口脏值当场 fail-fast：`S/application/order/presenter/OrderPresenter.java:26`）
- **OI-9（错误码登记）** 本聚合全部错误为 `BusinessException` + i18n 位点 `order:err.{scene}`，无具名领域异常（键族登记见 §6；位点约定论证见 decisions/ ADR-0011，不复述）。
- **OI-10（时间/操作人）** 时间字段统一 `OffsetDateTime`；create_at/update_at 由应用层 `AuditFieldFiller` 填充（非 DB 触发器），时钟经注入 `Clock`。（源：`Order.java:32-34`、`schema.sql:5` 注记、`MybatisPersistence.java:214,276` fillInsert/fillUpdate；理由见 decisions/ ADR-0006，不复述）

## 6. 错误码位总表（本聚合登记键）

| 键 | 抛出点 | 缺省 HTTP |
|---|---|---|
| `order:err.status.pending` | `Order.pay` 守卫（`Order.java:108`） | 422 |
| `order:err.status.paid` | `Order.confirm`（:118） | 422 |
| `order:err.status.confirmed` | `Order.ship`（:129） | 422 |
| `order:err.status.shipped` | `Order.deliver`（:140） | 422 |
| `order:err.status.delivered` | `Order.complete`（:150） | 422 |
| `order:err.status.cancellable` | `Order.cancel`（:161） | 422 |
| `order:err.itemsEmpty` / `order:err.customerIdRequired` / `order:err.totalMustBePositive` | `Order.validate`（:170-180） | 422 |
| `order:err.notFound` | 7 个 Handler 加载守卫（如 `PayOrderHandler.java:33`）+ `GetOrderHandler.java:27` | 422 |
| `order:err.productIdRequired` / `order:err.quantityMustBePositive` / `order:err.unitPriceRequired` | `OrderItem` 紧凑构造（`OrderItem.java:17-26`） | 422 |
| `product:err.notFound` / `product:err.insufficientStock` | 下单链路跨界抛点（`PlaceOrderHandler.java:98`、`Product.java:78-80`） | 422（insufficientStock 携 params） |

（跨聚合键登记在 product.md §5，此处只列 order 触发面。）

---

**首版不完整声明**：本文件从 2026-09-06 工作区源码反推（行号以当日为准），非逐条人工评审产物；允许首批 `changes/` 逐节替换。文中全部 `待 changes/ 补全` 注记 = 已知意图空白清单，裁决前不构成承诺。
