# 用法规范法卷：分布式事务（框架法 · 严格件）

> **身份**：本卷是 Seata AT 使用边界与 XID 透传形态**统一用法**的唯一权威——全套规范形状（依赖/配置/双端组件/Handler 模板）在此，全仓他处不得复写形状；违反本卷=修代码，修卷走 `../../changes/`。docs 同题篇（`../../../docs/how-to/distributed-transaction.md`）为设计卡（选型与边界叙事，零形状代码），冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷；教例家族 Invoice/Inventory 跨服务示意系（虚构，与 cross-aggregate 同系；PlaceOrderHandler 为真实例对照）。开册法案：`2026-09-howto-codification`；统一用法归卷：`2026-09-usage-consolidation`。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| DT-1 | 边界选择铁序：同一数据源的多聚合一致性**必须**用本地 `@Transactional`；仅跨数据源的一致性场景方可启用 `@GlobalTransactional`（Seata AT 有全局锁开销） | 原篇「能用本地事务就不用分布式事务」原则入法；`modules/cloud.md` 互指 | — |
| DT-2 | 启用 Seata 时，东西向 HTTP 调用**必须**双端透传 XID：发起端拦截器写 `RootContext.getXID()` 入约定 header；接收端 filter 读取并 bind/unbind——缺一环即全局事务断链 | 原篇 XID 透传节入法 | 模板见宽松件 |
| DT-3 | 业务数据源必须经 Seata 自动代理（DataSource proxy）方可参与 AT；禁止手工注册旁路数据源 | 原篇自动代理节；`modules/cloud.md` 条件装配条款 | — |
| DT-4 | 最终一致可接受的场景（通知、日志）禁止拉入分布式事务，改走异步消息 + 重试 | 原篇边界表第三行入法 | — |
| DT-5 | `@GlobalTransactional` 标注在**发起方**（TC 协调入口）Handler 方法；跨服务调用与本地写在同一方法体内完成（模板序：先远程扣减、后本地落库），任一分支失败 → TC 通知所有分支回滚（undo_log 逆向补偿） | 本卷 §2.4 形状；原篇「标注在发起方」要点入法 | — |
| DT-6 | 一期东西向调用为 RestClient 静态 baseUrl 直连；请求/响应类型复用 contract 中的 CQE/CO，禁止另造传输模型 | 根 README 技术栈声明（东西向 HTTP/RestClient 直连，一期静态地址）；本卷 §2.4 形状 | — |
| DT-7 | XID 透传双端组件由业务侧按本卷模板手写——框架（common-cloud）**不内置**透传组件；Spring Cloud Alibaba 路线不适用（本项目未引入） | 原篇「框架不内置透传组件」注记入法 | — |

## §2 规范形状（统一用法唯一样本）

> 教例：跨服务示意版为**虚构教例**（`invoice` / `inventory` 聚合系，与 [cross-aggregate.md](cross-aggregate.md) 同一虚构系）；同服务真实形态有 sample 落位（见 §2.7 与 §3）。案例为「下单 = 创建订单 + 扣减库存（跨服务）」：扣库存失败时订单必须回滚。

### 2.1 依赖引入

```xml
<!-- common-cloud 聚合引入 seata-spring-boot-starter（optional 标记——业务服务依赖 common-cloud 或直接自备声明；示例应用当前未引入 common-cloud/Seata） -->
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```

### 2.2 配置

```yaml
# application.yml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER:127.0.0.1:8848}
```

### 2.3 边界选择表：本地事务 vs 分布式事务（用法侧规范，随形状入卷）

| 场景 | 选择 | 理由 |
|------|------|------|
| 同一服务内多聚合（如 sample 两聚合同库形态，真实例落位见 §2.7） | `@Transactional`（本地） | 同一数据源，本地 ACID 即可 |
| 跨服务调用（订单服务调支付服务） | `@GlobalTransactional`（Seata） | 跨数据源，需分布式协调 |
| 最终一致性可接受（通知、日志） | 异步消息 + 重试 | 无需强一致，避免分布式事务开销 |

> **原则（DT-1）：能用本地事务就不用分布式事务。** Seata AT 有全局锁开销，仅跨服务数据一致性场景使用。

### 2.4 跨服务场景 —— @GlobalTransactional 发起方模板

```java
// application/invoice/handler/command/CreateInvoiceGlobalTxHandler.java（跨服务示意版，虚构教例）
@Component
public class CreateInvoiceGlobalTxHandler
        implements CommandHandler<CreateInvoiceCommand, InvoiceDTO> {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceAssembler invoiceAssembler;
    private final InvoiceFactory invoiceFactory;
    private final RestClient inventoryRestClient;  // 远程服务（HTTP，静态 baseUrl 直连）（DT-6）

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)  // Seata 全局事务（DT-5：标注在发起方 TC 协调入口）
    public InvoiceDTO handle(CreateInvoiceCommand command) {
        // 1. 远程扣减额度（跨服务 HTTP 调用，XID 经出站拦截器写入 TX_XID header 透传，Seata 分支事务）
        inventoryRestClient.post()
                .uri("/inventory/internal/deduct-stock")
                .body(new DeductStockCommand(command.getRefId(), command.getQuantity()))  // DT-6：请求对象复用 contract CQE
                .retrieve()
                .toBodilessEntity();

        // 2. 本地创建订单（InvoiceFactory 创建即合法；Seata 分支事务，同一全局事务内）
        Invoice invoice = invoiceFactory.create(command.getCustomerId(), command.toItems());
        invoiceRepository.save(invoice);

        return invoiceAssembler.toDTO(invoice);
    }
}
```

要点（形状注记）：

- `@GlobalTransactional` 标注在发起方（TC 协调入口）
- 消费方 RestClient 以静态 baseUrl 构建（一期静态地址直连），请求/响应类型复用 contract 中的 CQE/CO
- 远程服务（库存）的扣减端点自动注册为分支事务（Seata Agent 拦截 DataSource，DT-3）
- 任一分支失败 → TC 通知所有分支回滚（undo_log 逆向补偿）

### 2.5 XID 透传·出站拦截器（发起端）

Seata 全局事务的 XID 必须在跨服务 HTTP 调用间透传，否则分支事务无法加入全局事务。配方为两段手写组件：

```java
// infrastructure/config/SeataXidClientInterceptor.java（出站：调用方侧）
@Component
public class SeataXidClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String xid = RootContext.getXID();
        if (xid != null) {
            request.getHeaders().add(RootContext.KEY_XID, xid);  // header 名即 TX_XID
        }
        return execution.execute(request, body);
    }
}
// 注册：RestClient.builder().baseUrl(...).requestInterceptor(new SeataXidClientInterceptor()).build()
```

### 2.6 XID 透传·入站 Filter（提供方端）

```java
// infrastructure/config/SeataXidBindFilter.java（入站：提供方侧）
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeataXidBindFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String xid = ((HttpServletRequest) request).getHeader(RootContext.KEY_XID);
        if (xid != null) {
            RootContext.bind(xid);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (xid != null) {
                RootContext.unbind();
            }
        }
    }
}
```

> 框架（common-cloud）不内置透传组件，业务侧按本配方实现（DT-7）；Spring Cloud Alibaba 路线不适用（本项目未引入）。

### 2.7 同服务场景 —— 本地事务即可（DT-1 正解形态）

```java
// application/invoice/handler/command/CreateInvoiceHandler.java（虚构教例版，同构于 sample 真实形态）——节选，完整形态见 cross-aggregate.md §2
@Override
@Transactional(rollbackFor = Exception.class)  // 本地事务，无需 Seata
public InvoiceDTO handle(CreateInvoiceCommand command) {
    List<InvoiceItem> items = buildItems(command);            // 批量加载取真实单价（cross-aggregate.md §2）
    inventoryDomainService.deductStock(items);                // 跨聚合扣减额度（DomainService 批量契约）
    Invoice invoice = invoiceFactory.create(command.getCustomerId(), items);
    invoiceRepository.save(invoice);
    return invoiceAssembler.toDTO(invoice);
}
```

> 真实例（示例应用即为此形态：两聚合同服务同数据源）：对照 `sample-application/.../application/order/handler/command/PlaceOrderHandler.java`（真实例映射位，`@Transactional` 本地事务、无 Seata）。

### 2.8 Seata AT 模式工作原理（形状语境注记）

```
TM（Transaction Manager）—— @GlobalTransactional 标注的方法
  → 开启全局事务（TC 分配 XID）
  → RM（Resource Manager）—— 各分支的 DataSource 代理
    → 一阶段：正常提交本地事务 + 写 undo_log
    → 二阶段提交：异步删除 undo_log
    → 二阶段回滚：根据 undo_log 逆向补偿
```

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|---|---|---|
| DT-1/4 | ✅ 边界规则即时生效（否定性义务，无落地依赖） | 选择表在册 §2.3 |
| DT-2/3 | ⛔ 未落地——示例应用为单服务（两聚合同数据源，真实例），XID 拦截器/过滤器为跨服务示意模板；两服务化拆分时必须按本卷落地并对账 | §2.5/§2.6 模板 |
| `CreateInvoiceGlobalTxHandler`（`@GlobalTransactional` 入口，跨服务示意版，虚构教例） | ⛔ 未落地 | application 段，§2.4 即落地模板 |
| `DeductStockCommand`（东西向请求对象，HTTP 载荷，复用同一契约） | ⛔ 未落地 | contract 段 |
| `SeataXidClientInterceptor`（出站：`RootContext.getXID()` 写入 TX_XID header） | ⛔ 未落地 | infrastructure/config 段，§2.5 即落地模板 |
| `SeataXidBindFilter`（入站：读取 header 并 bind/unbind RootContext） | ⛔ 未落地 | infrastructure/config 段，§2.6 即落地模板 |
| Seata 自动代理 DataSource | ⛔ 未落地 | 无需手写代码（starter 自动装配，DT-3） |

> **落地状态注记**：示例应用为单服务（两聚合同数据源，真实例）——本卷 §2.4 及 XID 透传、`DeductStockCommand` 均为跨服务**示意模板**，sample 中不存在 `RestClient` 注入与 `@GlobalTransactional` 用法；服务内真实形态见 §2.7（与 [cross-aggregate.md](cross-aggregate.md) 同链路）。
