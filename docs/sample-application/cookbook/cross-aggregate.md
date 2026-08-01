# 跨聚合协调 + Domain Service

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（领域服务章节）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"下单"** 为案例，展示一个涉及多聚合协调的复杂写操作：

**业务规则：**

1. 下单时需查询商品信息（Product 聚合）获取单价
2. 下单时需扣减多个商品的库存（跨 Product 聚合批量操作）
3. 下单时创建新订单（Order 聚合）并注册 OrderPlacedEvent
4. 库存扣减逻辑不归属于任何单一聚合 → 封装为 **Domain Service**

**为什么需要 Domain Service？**

| 如果放在 Order 聚合内 | 用 Domain Service |
|---|---|
| Order 需要注入 ProductRepository（聚合间耦合） | Order 只管自己的状态变迁 |
| 库存扣减逻辑散落在 Order 的行为方法中 | 跨聚合协调逻辑内聚于一处 |
| 取消订单时的库存回补要复制一份 | deductStock / replenishStock 对称复用 |

## 调用链路

```
REST 请求（PlaceOrderCommand）
  → adapter/facade/OrderServiceImpl
    → application/order/OrderAppService
      → application/order/handler/PlaceOrderHandler（复杂用例，跨 2 个聚合）
        → domain/product/repository/ProductRepository.findById()     ← 查询商品
        → domain/shared/service/InventoryDomainService.deductStock() ← 跨聚合扣库存
        → domain/order/model/Order（创建 + place()）                 ← 创建订单
        → domain/order/repository/OrderRepository.save()             ← 持久化
      → application/order/presenter/OrderPresenter
  ← OrderCO
```

## 1. Domain — Domain Service（零框架依赖）

```java
// domain/shared/service/InventoryDomainService.java
public class InventoryDomainService implements DomainService {

    private final ProductRepository productRepository;

    public InventoryDomainService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 批量扣减库存（下单时调用） */
    public void deductStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new BusinessException("product:err.notFound"));
            product.deductStock(item.quantity());
            productRepository.update(product);
        }
    }

    /** 批量回补库存（取消订单时调用） */
    public void replenishStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new BusinessException("product:err.notFound"));
            product.restoreStock(item.quantity());
            productRepository.update(product);
        }
    }
}
```

关键约束：
- 实现 `DomainService` 标记接口，**零框架注解**（无 @Service/@Component）
- Bean 注册由 `infrastructure/config/DomainServiceConfig` 负责
- 可调用 Repository、可修改实体状态（与 Policy 的区别：Policy 无副作用）

## 2. Infrastructure — Bean 注册配置

```java
// infrastructure/config/DomainServiceConfig.java
@Configuration
public class DomainServiceConfig {

    @Bean
    public InventoryDomainService inventoryDomainService(ProductRepository productRepository) {
        return new InventoryDomainService(productRepository);
    }
}
```

## 3. Application — 复杂 CommandHandler（跨聚合编排）

```java
@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderDTO> {

    private final ProductRepository productRepository;
    private final InventoryDomainService inventoryDomainService;
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(PlaceOrderCommand command) {
        // 1. 构建订单项（查询商品单价）
        List<OrderItem> items = command.getItems().stream()
                .map(dto -> {
                    Product product = productRepository.findById(dto.getProductId())
                            .orElseThrow(() -> new BusinessException("product:err.notFound"));
                    BigDecimal unitPrice = BigDecimal.TEN; // 简化：从商品获取
                    return new OrderItem(dto.getProductId(), dto.getQuantity(), unitPrice);
                })
                .toList();

        // 2. 扣减库存（跨聚合协调，委托 Domain Service）
        inventoryDomainService.deductStock(items);

        // 3. 创建订单并下单（聚合根行为）
        Order order = new Order(UUID.randomUUID(), items, command.getCustomerId());
        order.place();
        orderRepository.save(order);

        // 4. 返回 DTO
        return orderAssembler.toDTO(order);
    }
}
```

## 职责边界对比

| | Domain Service | Policy | Handler |
|--|---------------|--------|---------|
| 层 | Domain | Domain | Application |
| 副作用 | **有**（修改实体、调用 Repository） | **无**（纯计算） | **有**（编排持久化） |
| 框架注解 | **零**（Bean 注册在 infra config） | 可有 @Component（无状态单例） | @Component + @Transactional |
| 职责 | 跨聚合协调 | 可插拔决策规则 | 单用例编排 |

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `api/OrderService.java` | RPC 接口 + JAX-RS 路径映射 |
| contract | `dto/PlaceOrderCommand.java` | 下单命令（含订单项列表） |
| adapter | `facade/OrderServiceImpl.java` | 协议适配（透传） |
| application | `handler/PlaceOrderHandler.java` | 跨聚合编排 |
| domain | `shared/service/InventoryDomainService.java` | 跨聚合库存协调 |
| domain | `order/model/Order.java` | 订单聚合根（place 行为） |
| domain | `product/model/Product.java` | 商品聚合根（deductStock 行为） |
| infrastructure | `config/DomainServiceConfig.java` | Domain Service Bean 注册 |

## 相关模式

- **SecurityUtil 获取当前用户** → 参见 `ywf-ddd-common/docs/common-security.md` + `.agents/rules/03-coding-conventions.md`（SecurityUtil 使用层归属）
- **common-pg TypeHandler** → 参见 `ywf-ddd-common/docs/common-pg.md`（UUID/JSONB/数组自动映射）
- **Factory 复杂创建** → 参见 `docs/sample-application/module-design/domain.md`（Factory 章节）
