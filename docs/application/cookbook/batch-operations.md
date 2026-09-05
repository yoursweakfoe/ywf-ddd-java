# 批量操作

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"批量确认订单"** 为案例，展示批量写操作从 Command 到数据库的完整路径。

**业务需求：**

1. 运营后台选中多条订单，一键批量确认
2. 所有订单必须在同一事务内完成（要么全部成功，要么全部回滚）
3. 部分订单状态不合法时，整批失败并返回明确错误

## 调用链路

```
REST 请求（BatchConfirmOrderCommand）
  → adapter/rest/controller/OrderControllerImpl
    → application/order/service/OrderAppService
      → application/order/handler/BatchConfirmOrderHandler
        → 循环：repository.findById → order.confirm()
        → repository.updateDomainBatch(orders)
      → application/order/presenter/OrderPresenter
  ← List<OrderCO>
```

## 1. Contract — 批量 Command

```java
// contract/order/dto/command/BatchConfirmOrderCommand.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量确认订单命令")
public class BatchConfirmOrderCommand implements Command, Serializable {

    /** 订单 ID 列表 */
    @Schema(description = "订单 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> orderIds;
}
```

## 2. Application — 批量 Handler

```java
// application/order/handler/BatchConfirmOrderHandler.java
@Component
public class BatchConfirmOrderHandler implements CommandHandler<BatchConfirmOrderCommand, List<OrderDTO>> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public BatchConfirmOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderDTO> handle(BatchConfirmOrderCommand command) {
        // 1. 批量加载聚合根
        List<Order> orders = command.getOrderIds().stream()
                .map(id -> orderRepository.findById(UUID.fromString(id))
                        .orElseThrow(() -> new BusinessException("order:err.notFound")))
                .toList();

        // 2. 逐个调用领域行为（业务规则在聚合根内）
        orders.forEach(Order::confirm);

        // 3. 批量持久化（方法内保证事务）
        orderRepository.updateDomainBatch(orders);

        // 4. 批量转 DTO
        return orders.stream().map(orderAssembler::toDTO).toList();
    }
}
```

要点：
- `@Transactional` 保证整批原子性（任一订单 confirm 失败 → 全部回滚）
- 领域行为 `order.confirm()` 内含状态校验（非 PAID 状态抛 BusinessException）
- `updateDomainBatch` 内部循环调用 `updateDomain`（每条都触发 validate）

## 3. 事务边界与失败策略

| 策略 | 实现方式 | 适用场景 |
|------|---------|---------|
| 全部成功或全部回滚 | `@Transactional` + 异常传播 | 批量确认、批量取消 |
| 跳过失败项，返回结果 | Handler 内 try-catch + 收集错误 | 批量导入（允许部分失败） |
| 无事务（每条独立） | 去掉 `@Transactional` | 批量通知、日志写入 |

### 部分失败模式（可选）

```java
// 允许部分失败时的处理模式
public BatchResultDTO handle(BatchImportCommand command) {
    List<String> successIds = new ArrayList<>();
    List<String> failedIds = new ArrayList<>();

    for (String id : command.getIds()) {
        try {
            // 单条处理
            successIds.add(id);
        } catch (BusinessException e) {
            failedIds.add(id);
        }
    }
    return new BatchResultDTO(successIds, failedIds);
}
```

> 注意：部分失败模式下**不加** `@Transactional`，否则 catch 后事务不会回滚但语义混乱。

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/command/BatchConfirmOrderCommand.java` | 批量命令（含 ID 列表） |
| application | `handler/BatchConfirmOrderHandler.java` | 批量编排 |
| application | `service/OrderAppService.java` | 委托 + 呈现 |
| adapter | `rest/OrderControllerImpl.java` | 透传 |
