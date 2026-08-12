# 分布式事务（Seata AT）

> 设计原理 → [module-design/infrastructure.md](../module-design/infrastructure.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"下单 = 创建订单 + 扣减库存（跨服务）"** 为案例，展示 Seata AT 模式分布式事务的使用方式。

**业务需求：**

1. 下单操作涉及两个聚合（Order + Product），可能部署在不同服务
2. 扣库存失败时订单必须回滚（数据一致性）
3. 同一服务内优先使用本地事务（`@Transactional`），仅跨服务时才启用分布式事务

## Dependencies

```xml
<!-- common-cloud 已聚合引入 seata-spring-boot-starter -->
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```

## Configuration

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

## 边界选择：本地事务 vs 分布式事务

| 场景 | 选择 | 理由 |
|------|------|------|
| 同一服务内多聚合（如 sample 的 Order + Product） | `@Transactional`（本地） | 同一数据源，本地 ACID 即可 |
| 跨服务调用（Order 服务调 Payment 服务） | `@GlobalTransactional`（Seata） | 跨数据源，需分布式协调 |
| 最终一致性可接受（通知、日志） | 领域事件 + 重试 | 无需强一致，避免分布式事务开销 |

> **原则：能用本地事务就不用分布式事务。** Seata AT 有全局锁开销，仅跨服务数据一致性场景使用。

## 1. 跨服务场景 — @GlobalTransactional

```java
// application/order/handler/PlaceOrderHandler.java（跨服务版本）
@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;
    private final RestClient productRestClient;  // 远程服务（HTTP，静态 baseUrl 直连）

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)  // Seata 全局事务
    public OrderDTO handle(PlaceOrderCommand command) {
        // 1. 远程扣库存（跨服务 HTTP 调用，XID 经出站拦截器写入 TX_XID header 透传，Seata 分支事务）
        productRestClient.post()
                .uri("/products/internal/deduct-stock")
                .body(new DeductStockCommand(command.getProductId(), command.getQuantity()))
                .retrieve()
                .toBodilessEntity();

        // 2. 本地创建订单（Seata 分支事务，同一全局事务内）
        Order order = new Order(UUID.randomUUID(), command.toItems(), command.getCustomerId());
        order.place();
        orderRepository.save(order);

        return orderAssembler.toDTO(order);
    }
}
```

要点：
- `@GlobalTransactional` 标注在发起方（TC 协调入口）
- 消费方 RestClient 以静态 baseUrl 构建（一期静态地址直连），请求/响应类型复用 contract 中的 CQE/CO
- 远程服务（Product）的扣库存端点自动注册为分支事务（Seata Agent 拦截 DataSource）
- 任一分支失败 → TC 通知所有分支回滚（undo_log 逆向补偿）

### XID 透传（HTTP）

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

> 框架（common-cloud）不内置透传组件，业务侧按本配方实现；Spring Cloud Alibaba 路线不适用（本项目未引入）。

## 2. 同服务场景 — 本地事务即可

```java
// 当前 sample 的实际做法（Order + Product 在同一服务/同一数据源）
@Override
@Transactional(rollbackFor = Exception.class)  // 本地事务，无需 Seata
public OrderDTO handle(PlaceOrderCommand command) {
    inventoryDomainService.deductStock(command.getProductId(), command.getQuantity());
    Order order = new Order(UUID.randomUUID(), command.toItems(), command.getCustomerId());
    order.place();
    orderRepository.save(order);
    return orderAssembler.toDTO(order);
}
```

## 3. Seata AT 模式工作原理

```
TM（Transaction Manager）—— @GlobalTransactional 标注的方法
  → 开启全局事务（TC 分配 XID）
  → RM（Resource Manager）—— 各分支的 DataSource 代理
    → 一阶段：正常提交本地事务 + 写 undo_log
    → 二阶段提交：异步删除 undo_log
    → 二阶段回滚：根据 undo_log 逆向补偿
```

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| application | `handler/PlaceOrderHandler.java` | @GlobalTransactional 入口 |
| contract | `product/dto/DeductStockCommand.java` | 东西向请求对象（HTTP 载荷，复用同一契约） |
| infrastructure | `config/SeataXidClientInterceptor.java` | 出站：RootContext.getXID() 写入 TX_XID header |
| infrastructure | `config/SeataXidBindFilter.java` | 入站：读取 header 并 bind/unbind RootContext |
| infrastructure | Seata 自动代理 DataSource | 无需手写代码 |
