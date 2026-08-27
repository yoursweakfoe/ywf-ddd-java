# 写路径全链路

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 业务场景

示例应用是一个简化的电商系统，包含两个聚合：

- **Order（订单）**：生命周期为 `PENDING → PAID → CONFIRMED → SHIPPED → DELIVERED → COMPLETED`，可从 PENDING/PAID 状态取消
- **Product（商品）**：管理商品信息和库存

本文以 **"支付订单"** 为案例，展示一个写操作从 REST 入口到数据库落盘的完整代码路径。

**业务规则：**

1. 只有 PENDING 状态的订单才能支付（状态机约束）
2. 支付成功后订单状态变为 PAID，并触发 `OrderPaidEvent` 领域事件
3. 支付失败（状态不合法）时抛出 BusinessException，前端收到 422 + i18n 错误码
4. 乐观锁保护并发支付（两人同时点"支付"只有一人成功）

## 调用链路

```
REST 请求
  → adapter/rest/controller/OrderControllerImpl（@RestController，参数包装）
    → application/order/service/OrderAppService（委托 Handler + Presenter 呈现）
      → application/order/handler/PayOrderHandler（编排领域逻辑）
        → domain/order/model/Order.pay()（业务规则 + 状态变迁）
        → domain/order/repository/OrderRepository.update()（持久化抽象）
          → infrastructure/persistence/.../OrderRepositoryImpl（MyBatis-Plus 落盘）
      → application/order/presenter/OrderPresenter（DTO → CO）
  ← OrderCO（返回调用方）
```

## 1. Contract — Command / CO

```java
// Command：写操作意图
@Data @NoArgsConstructor @AllArgsConstructor
public class PayOrderCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private UUID orderId;
}

// CO：契约输出（外部安全视图，不暴露 version/审计字段）
@Data @NoArgsConstructor @AllArgsConstructor
public class OrderCO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private String id;
    private String status;
    private BigDecimal totalAmount;
    private String customerId;
    private String trackingNumber;
    private String cancelReason;
    private List<OrderItemCO> items;
}
```

## 2. Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/order/adapter/rest/OrderController.java（契约接口，承载 HTTP 映射 + 文档注解）
@Tag(name = "订单服务", description = "订单生命周期管理")
@RequestMapping("/orders")
public interface OrderController {

    @Operation(summary = "支付订单", description = "将 PENDING 订单标记为已支付")
    @PutMapping("/{orderId}/pay")
    OrderCO payOrder(@PathVariable("orderId") UUID orderId);
}

// adapter/rest/controller/OrderControllerImpl.java（实现，仅标记协议 + 透传）
@RestController
public class OrderControllerImpl implements OrderController {

    private final OrderAppService orderAppService;

    public OrderControllerImpl(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    @Override
    public OrderCO payOrder(UUID orderId) {
        return orderAppService.payOrder(new PayOrderCommand(orderId));
    }
}
```

## 3. Application — AppService / Handler / Assembler / Presenter

**AppService**（聚合入口，委托 + 呈现）：

```java
@Service
public class OrderAppService implements ApplicationService {

    private final OrderPresenter orderPresenter;
    private final PayOrderHandler payOrderHandler;

    public OrderCO payOrder(PayOrderCommand command) {
        return orderPresenter.present(payOrderHandler.handle(command));
    }
}
```

**CommandHandler**（用例执行，固定模式：load → 行为 → save → toDTO）：

```java
@Component
public class PayOrderHandler implements CommandHandler<PayOrderCommand, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(PayOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.pay();
        orderRepository.update(order);
        return orderAssembler.toDTO(order);
    }
}
```

**Assembler**（Domain → DTO，逐字段显式映射）：

```java
@Component
public class OrderAssembler implements BasicAssembler<Order, OrderDTO> {

    @Override
    public OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId().toString());
        dto.setStatus(order.getStatus().name());
        dto.setItems(order.getItems().stream()
                .map(item -> new OrderDTO.OrderItemDTO(
                        item.productId(), item.quantity(), item.unitPrice()))
                .toList());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCustomerId(order.getCustomerId());
        dto.setTrackingNumber(order.getTrackingNumber());
        dto.setCancelReason(order.getCancelReason());
        dto.setCreateAt(order.getCreateAt());
        dto.setVersion(order.getVersion());
        return dto;
    }

    // 最小契约：仅 toDomain / toDTO（+ 集合委托）；toDomain 抛 UnsupportedOperationException（富领域模型走 reconstitute）
}
```

**Presenter**（DTO → CO，过滤内部字段）：

```java
@Component
public class OrderPresenter implements BasicPresenter<OrderDTO, OrderCO> {

    @Override
    public OrderCO present(OrderDTO dto) {
        OrderCO co = new OrderCO();
        co.setId(dto.getId());
        co.setStatus(dto.getStatus());
        co.setItems(presentItems(dto.getItems()));
        co.setTotalAmount(dto.getTotalAmount());
        co.setCustomerId(dto.getCustomerId());
        co.setTrackingNumber(dto.getTrackingNumber());
        co.setCancelReason(dto.getCancelReason());
        // createAt / updateAt / version 不暴露
        return co;
    }
}
```

## 4. Domain — 聚合根 + 值对象

```java
public class Order extends AggregateRoot<UUID> {

    private UUID id;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private Integer version;
    // ...

    public void pay() {
        requireStatus("order:err.status.pending", OrderStatus.PENDING);
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(id));
    }

    @Override
    public void validate() {
        if (items == null || items.isEmpty())
            throw new BusinessException("order:err.itemsEmpty");
        if (customerId == null)
            throw new BusinessException("order:err.customerIdRequired");
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("order:err.totalMustBePositive");
    }
}

// 值对象：首选 record，天然不可变
public record OrderItem(Long productId, int quantity, BigDecimal unitPrice) implements ValueObject {

    public OrderItem {
        if (productId == null) throw new BusinessException("order:err.productIdRequired");
        if (quantity <= 0) throw new BusinessException("order:err.quantityMustBePositive");
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
```

## 5. Domain — Repository 接口

```java
public interface OrderRepository extends Repository<Order, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

## 6. Infrastructure — PO / Converter / RepositoryImpl

```java
@Data
@TableName("orders.orders")
public class OrderPO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String status;
    private String items;          // JSON 序列化
    private BigDecimal totalAmount;
    private String customerId;
    private String trackingNumber;
    private String cancelReason;
    @Version
    private Integer version;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
    @TableLogic
    private Boolean isDelete;
}

@Component
public class OrderConverter implements BasicConverter<Order, OrderPO> {

    @Override
    public Order toDomain(OrderPO po) {
        return Order.reconstitute(
                UUID.fromString(po.getId()), OrderStatus.valueOf(po.getStatus()),
                deserializeItems(po.getItems()), po.getTotalAmount(),
                po.getCustomerId(), po.getTrackingNumber(), po.getCancelReason(),
                po.getCreateAt(), po.getUpdateAt(), po.getVersion());
    }

    @Override
    public OrderPO toPO(Order domain) {
        OrderPO po = new OrderPO();
        po.setId(domain.getId().toString());
        po.setStatus(domain.getStatus().name());
        po.setItems(serializeItems(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        po.setTrackingNumber(domain.getTrackingNumber());
        po.setCancelReason(domain.getCancelReason());
        po.setVersion(domain.getVersion());
        return po;
    }

    // 最小契约：仅 toDomain / toPO（+ 集合委托）；不定义增量更新方法（富模型走 reconstitute 全量快照）
}

@Component
public class OrderRepositoryImpl
        extends MybatisPlusPersistence<OrderMapper, OrderPO, Order, UUID>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper,
                               ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
                               ObjectProvider<OutboxStore> outboxStoreProvider,
                               OrderConverter converter) {
        super(mapper, domainEventPublisherProvider, outboxStoreProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Order, OrderPO> getConverter() { return converter; }
    @Override protected Serializable toPersistenceId(UUID id) { return id.toString(); }
    @Override public Optional<Order> findById(UUID id) { return findDomainById(id); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) { saveDomain(domain); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) { updateDomain(domain); }
    @Override public boolean exists(UUID id) { return existsDomainById(id); }
    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

> 领域事件随持久化经框架 Outbox 捕获契约交付（`OutboxStore` SPI，见
> `docs/common/common-ddd.md` Outbox 节），仓储构造注入的 `OutboxStore`
> Provider 即捕获的接线点；框架不提供缺省实现——业务提供 Store Bean 时激活捕获，
> 入箱后的投递归业务排空器，未提供时自动回退直发路径。

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/command/PayOrderCommand.java` | 写操作意图 |
| contract | `dto/co/OrderCO.java` | 契约输出 |
| contract | `adapter/rest/OrderController.java` | Controller 契约接口 |
| adapter | `rest/OrderControllerImpl.java` | 协议适配（透传） |
| application | `service/OrderAppService.java` | 聚合入口 |
| application | `handler/PayOrderHandler.java` | 用例编排 |
| application | `assembler/OrderAssembler.java` | Domain → DTO |
| application | `presenter/OrderPresenter.java` | DTO → CO |
| application | `dto/OrderDTO.java` | 内部视图 |
| domain | `model/Order.java` | 聚合根（业务规则） |
| domain | `model/OrderItem.java` | 值对象 |
| domain | `repository/OrderRepository.java` | 持久化抽象 |
| infrastructure | `mybatisplus/po/OrderPO.java` | 持久化对象（MyBatis-Plus 注解载体） |
| infrastructure | `converter/OrderConverter.java` | Domain ↔ PO（框架 BasicConverter 桥） |
| infrastructure | `mybatisplus/mapper/OrderMapper.java` | MyBatis-Plus Mapper |
| infrastructure | `repository/OrderRepositoryImpl.java` | 仓储实现（继承 MybatisPlusPersistence） |
