# 读路径全链路

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **“查询订单详情”** 和 **“分页查询订单列表”** 为案例，展示读操作从 REST 入口到 DTO 投影的完整代码路径。

**业务需求：**

1. 用户点击订单详情页 → 根据订单 ID 查询单条订单（含订单项、状态、金额）
2. 用户打开订单列表页 → 按状态/客户筛选 + 分页浏览
3. 查询是只读操作，不需要加载完整聚合根（无行为可调用），直接投影 DTO 即可
4. 返回给前端的是 CO（契约输出），不暴露 version、deleted 等内部字段

**为什么读侧可以绕过聚合根？**

写侧需要聚合根是因为要调用行为方法（`order.pay()`）执行业务规则。读侧不做任何状态变更，只需“把数据查出来给前端看”，因此无需 reconstitute 完整领域模型，直接从 PO 投影 DTO 性能更优。

```
REST 请求
  → adapter/rest/OrderControllerImpl（参数包装）
    → application/order/OrderAppService（委托 + 呈现）
      → application/order/handler/GetOrderHandler（查询编排）
        → domain/order/repository/OrderRepository（读优化方法）
          → infrastructure/...（Mapper 投影 DTO，不 reconstitute 聚合）
      → application/order/presenter/OrderPresenter（DTO → CO）
  ← OrderCO
```

## 1. Contract — Query 定义

### 单条查询

```java
// contract/order/dto/query/GetOrderQuery.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询订单详情")
public class GetOrderQuery implements Query, Serializable {

    /** 订单 ID */
    @Schema(description = "订单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;
}
```

### 分页查询（record 实现）

```java
// contract/order/dto/query/GetOrderPageQuery.java
public record GetOrderPageQuery(
        String status,       // 订单状态过滤（可选）
        String customerId,   // 客户 ID 过滤（可选）
        int pageNum,         // 页码（从 1 开始）
        int pageSize         // 每页大小
) implements PageableQuery {
}
```

要点：
- 单条查询实现 `Query`，分页查询实现 `PageableQuery`（common-contract）
- `PageableQuery` 继承 `Query`，带 pageNum/pageSize 约束
- record 天然不可变，适合简单 Query

## 2. Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/order/controller/OrderController.java（契约接口，承载 HTTP 映射）
@RequestMapping("/orders")
public interface OrderController {

    @Operation(summary = "查询订单详情", description = "根据 ID 获取订单完整信息")
    @GetMapping("/{orderId}")
    OrderCO getOrder(@PathVariable("orderId") String orderId);
}

// adapter/rest/OrderControllerImpl.java（实现，仅标记协议 + 透传）
@RestController
public class OrderControllerImpl implements OrderController {

    private final OrderAppService orderAppService;

    @Override
    public OrderCO getOrder(String orderId) {
        // REST 路径参数 → Query 包装 → 透传
        return orderAppService.getOrder(new GetOrderQuery(orderId));
    }
}
```

## 3. Application — AppService

```java
// application/order/OrderAppService.java（节选）
@Service
public class OrderAppService {

    private final OrderPresenter orderPresenter;
    private final GetOrderHandler getOrderHandler;

    public OrderCO getOrder(GetOrderQuery query) {
        return orderPresenter.present(getOrderHandler.handle(query));
    }
}
```

## 4. Application — QueryHandler

### 单条查询（经过聚合根）

```java
// application/order/handler/GetOrderHandler.java
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public GetOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }

    @Override
    public OrderDTO handle(GetOrderQuery query) {
        Order order = orderRepository.findById(UUID.fromString(query.getOrderId()))
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        return orderAssembler.toDTO(order);
    }
}
```

### 分页查询（绕过聚合根，推荐）

```java
// application/order/handler/GetOrderPageHandler.java
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderDTO>> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public GetOrderPageHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }

    @Override
    public PageResult<OrderDTO> handle(GetOrderPageQuery query) {
        // Repository 读优化方法：Mapper 直接投影，不 reconstitute 聚合根
        return orderRepository.findDtoPage(query)
                .map(orderAssembler::toDTO);  // PageResult.map() 逐层转换
    }
}
```

要点：
- 实现 `QueryHandler<Q, R>`（common-ddd）
- 无需 `@Transactional`（只读操作）
- 分页返回 `PageResult<DTO>`，不返回 CO
- **读侧不加载聚合根**：通过 Repository 读优化方法直接投影

## 5. Domain — Repository 读优化方法

```java
// domain/order/repository/OrderRepository.java
public interface OrderRepository extends Repository<Order, UUID> {

    // ===== 写侧（继承自 Repository）=====
    // findById / save / update / exists / deleteById

    // ===== 读优化方法（绕过聚合根）=====

    /** 按 ID 直接投影 DTO（不 reconstitute 聚合） */
    Optional<OrderDTO> findDtoById(UUID id);

    /** 分页投影 DTO */
    PageResult<OrderDTO> findDtoPage(GetOrderPageQuery query);
}
```

要点：
- 读优化方法定义在 Domain 层 Repository 接口
- 返回 DTO 而非 Domain（CQRS 读侧无需完整模型）
- `PageResult<T>` 是框架级分页容器（common-ddd），隔离 MyBatis-Plus `Page<PO>`

## 6. Infrastructure — 读优化实现

```java
// infrastructure/persistence/master/order/repository/OrderRepositoryImpl.java（节选）
@Component
public class OrderRepositoryImpl
        extends MybatisRepositorySupport<OrderMapper, OrderPO, Order>
        implements OrderRepository {

    @Override
    public Optional<OrderDTO> findDtoById(UUID id) {
        // Mapper 直接 SELECT 投影为 DTO，不经过 Converter.toDomain()
        OrderPO po = getById(id.toString());
        if (po == null) return Optional.empty();
        return Optional.of(projectToDto(po));
    }

    @Override
    public PageResult<OrderDTO> findDtoPage(GetOrderPageQuery query) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<OrderPO>()
                .eq(query.status() != null, OrderPO::getStatus, query.status())
                .eq(query.customerId() != null, OrderPO::getCustomerId, query.customerId())
                .orderByDesc(OrderPO::getCreateAt);

        // 使用 MybatisRepositorySupport 的 findDomainPage 分页能力
        PageResult<Order> domainPage = findDomainPage(wrapper, query.pageNum(), query.pageSize());
        // 或直接 Mapper 投影（性能更优）：
        // Page<OrderPO> mpPage = page(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        // return new PageResult<>(projectToDtoList(mpPage.getRecords()), mpPage.getTotal(), ...);
        return domainPage.map(this::projectToDto);
    }

    private OrderDTO projectToDto(OrderPO po) {
        // 轻量投影：仅映射 DTO 需要的字段，不重建领域模型
        OrderDTO dto = new OrderDTO();
        dto.setId(po.getId());
        dto.setStatus(po.getStatus());
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCustomerId(po.getCustomerId());
        return dto;
    }
}
```

要点：
- 读优化方法**不经过 Converter.toDomain()**（不 reconstitute 聚合根）
- 直接 PO → DTO 轻量投影
- `PageResult.map()` 支持链式转换

## 写路径 vs 读路径对比

| 维度 | 写路径（CommandHandler） | 读路径（QueryHandler） |
|------|------------------------|----------------------|
| 事务 | `@Transactional(rollbackFor = Exception.class)` | 可省略（只读） |
| 聚合根 | 必须加载（load → 行为 → save） | 可绕过（直接投影 DTO） |
| 返回类型 | DTO | DTO 或 `PageResult<DTO>` |
| 依赖 | Repository + Assembler + DomainService | Repository（读优化方法） |
| 事件 | 触发 DomainEvent | 不触发 |
| 固定模式 | load → 调用行为 → save → toDTO | findDtoXxx → 返回 |

## 完整文件清单（读路径涉及）

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/query/GetOrderQuery.java` | 单条查询 |
| contract | `dto/query/GetOrderPageQuery.java` | 分页查询 |
| contract | `dto/co/OrderCO.java` | 契约输出 |
| adapter | `rest/OrderControllerImpl.java` | 协议适配 |
| application | `OrderAppService.java` | 聚合入口 |
| application | `handler/GetOrderHandler.java` | 单条查询编排 |
| application | `handler/GetOrderPageHandler.java` | 分页查询编排 |
| application | `presenter/OrderPresenter.java` | DTO → CO |
| domain | `repository/OrderRepository.java` | 读优化方法定义 |
| infrastructure | `repository/OrderRepositoryImpl.java` | 读优化实现（投影） |
