# 跨聚合协调 + Domain Service

> 设计原理 → [module-design/domain.md](../module-design/domain.md)（领域服务章节）

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"下单"** 为案例，展示一个涉及多聚合协调的复杂写操作：

**业务规则：**

1. 下单时需查询商品信息（Product 聚合）获取单价
2. 下单时需扣减多个商品的库存（跨 Product 聚合批量操作）
3. 下单时创建新订单（Order 聚合），状态初始化为 PENDING
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
  → adapter/rest/controller/OrderControllerImpl
    → application/order/service/OrderAppService
      → application/order/handler/RetryablePlaceOrderHandler（乐观锁冲突重试包装，见 optimistic-lock-retry.md）
        → application/order/handler/PlaceOrderHandler（复杂用例，跨 2 个聚合，@Transactional）
          → domain/product/repository/ProductRepository.findAllById()   ← 批量查询商品（单次 IN）
          → domain/shared/service/InventoryDomainService.deductStock()  ← 跨聚合扣库存（productId 升序加锁）
          → domain/order/model/Order（创建 + place()）                  ← 创建订单
          → domain/order/repository/OrderRepository.save()              ← 持久化
      → application/order/presenter/OrderPresenter
  ← OrderCO
```

## 1. Domain — Domain Service（@Service 组件扫描注册）

```java
// domain/shared/service/InventoryDomainService.java
@Service
public class InventoryDomainService implements DomainService {

    private final ProductRepository productRepository;

    public InventoryDomainService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** 批量扣减库存（下单时调用）：批量加载 + 同商品数量合并 */
    public void deductStock(List<OrderItem> items) {
        Map<Long, Product> products = loadProducts(items);
        quantitiesByProduct(items).forEach((productId, totalQuantity) -> {
            Product product = requireProduct(products, productId);
            product.deductStock(totalQuantity);
            productRepository.update(product);
        });
    }

    /** 批量回补库存（取消订单时调用） */
    public void replenishStock(List<OrderItem> items) {
        Map<Long, Product> products = loadProducts(items);
        quantitiesByProduct(items).forEach((productId, totalQuantity) -> {
            Product product = requireProduct(products, productId);
            product.restoreStock(totalQuantity);
            productRepository.update(product);
        });
    }
}
```

关键约束：
- 实现 `DomainService` 标记接口（common-ddd），标注 `@Service` 由 Spring 组件扫描自动注册
  （Spring 是生态基座，标注注解即标准做法，不手写注册样板；领域层允许 stereotype 注解，见 A2 规则）
- 可调用 Repository、可修改实体状态（与 Policy 的区别：Policy 无副作用）
- 商品按 ID 集合**单次 IN 批量查询**（`findAllById`），杜绝逐项 `findById` 的 N+1 问题；
  同一商品出现在多个订单项时数量合并为一次聚合行为 + 一次持久化
  （避免对同一聚合连续两次乐观锁 UPDATE 导致版本号踩空）

## 2. Application — 复杂 CommandHandler（跨聚合编排）

```java
@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderDTO> {

    private final ProductRepository productRepository;
    private final InventoryDomainService inventoryDomainService;
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;
    private final OrderFactory orderFactory;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(PlaceOrderCommand command) {
        // 1. 批量加载商品（单次 IN 查询），以真实单价构建订单项
        Map<Long, Product> products = productRepository.findAllById(productIds(command)).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<OrderItem> items = command.getItems().stream()
                .map(dto -> new OrderItem(dto.getProductId(), dto.getQuantity(),
                        requireProduct(products, dto.getProductId()).getPrice()))
                .toList();

        // 2. 扣减库存（跨聚合协调，委托 Domain Service；其内部同样批量加载）
        inventoryDomainService.deductStock(items);

        // 3. 创建订单并下单（OrderFactory：创建即合法——校验一步到位；跨聚合联动已在本步之前同事务直调完成）
        Order order = orderFactory.create(command.getCustomerId(), items);
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
| 框架注解 | `@Service`（组件扫描注册） | 可有 @Component（无状态单例） | @Component + @Transactional |
| 职责 | 跨聚合协调 | 可插拔决策规则 | 单用例编排 |

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `adapter/rest/OrderController.java` | Controller 契约接口 |
| contract | `dto/command/PlaceOrderCommand.java` | 下单命令（含订单项列表） |
| adapter | `rest/OrderControllerImpl.java` | 协议适配（透传） |
| application | `handler/RetryablePlaceOrderHandler.java` | 乐观锁冲突重试包装（AppService 实际注入的是本类） |
| application | `handler/PlaceOrderHandler.java` | 跨聚合编排（@Transactional，被上者包装） |
| domain | `shared/service/InventoryDomainService.java` | 跨聚合库存协调 |
| domain | `order/model/Order.java` | 订单聚合根（place 行为） |
| domain | `product/model/Product.java` | 商品聚合根（deductStock 行为） |

## 相关模式

- **SecurityUtil 获取当前用户** → 参见 `docs/common/common-security.md` + `.agents/rules/03-coding-conventions.md`（SecurityUtil 使用层归属）
- **common-pg TypeHandler** → 参见 `docs/common/common-pg.md`（UUID/JSONB/数组自动映射）
- **Factory 复杂创建** → 参见 `docs/application/module-design/domain.md`（Factory 章节）
