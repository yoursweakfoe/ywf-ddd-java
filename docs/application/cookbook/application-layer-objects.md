# 应用层内部数据对象

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 为什么需要

Application 层在 Handler（领域 ↔ 内部数据）和 Presenter（内部数据 ↔ 契约 CO）之间，需要**语义明确的后缀**来区分不同用途的数据对象，而不是用泛化的 `DTO`。

三种典型场景：

| 场景 | 后缀 | 方向 | 示例 |
|------|------|------|------|
| 出路径投影 | **`ViewDTO`** | Domain → Contract | `OrderViewDTO` |
| 入路径富化 | **`ParamsDTO`** | Command → Domain | `OrderCreationParamsDTO` |
| 防腐层中间数据 | **`RecordDTO`** | External → Domain | `PaymentCallbackRecordDTO` |

---

## 出路径投影：ViewDTO

**场景**：Handler 从领域模型投影数据，经 Presenter 裁剪为不同 CO。

**代码示例**（示例应用已实现）：

```java
// application/order/dto/OrderViewDTO.java —— 全量内部视图
@Data
public class OrderViewDTO implements Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<OrderItemViewDTO> items;
    private OffsetDateTime createAt, updateAt;  // 内部审计字段
    private Integer version;                     // 不暴露给外部
}

// Presenter 按场景裁剪
@Component
public class OrderPresenter implements BasicPresenter<OrderViewDTO, OrderCO> {
    // 详情：全字段
    public OrderCO present(OrderViewDTO view) { ... }
    // 列表：精简字段
    public OrderSummaryCO presentSummary(OrderViewDTO view) { ... }
}
```

**关键点**：同一 `ViewDTO` → 多个 CO，Presenter 做裁剪，不用为每个视图写单独的 Handler。

---

## 入路径富化：ParamsDTO

**场景**：Handler 在 Command 与领域工厂之间，需要富化入参（查库、查配置、组装上下文），需要中间对象。

**代码示例**（示例应用未实现，展示模式）：

```java
// application/order/dto/OrderCreationParamsDTO.java —— 富化后的入参
@Data
public class OrderCreationParamsDTO {
    private String customerId;
    private List<OrderItemParams> items;
    private String operatorId;      // 从安全上下文注入
    private String region;          // 从配置中心注入
    private BigDecimal totalAmount; // 查库计算
}

// Handler
@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderViewDTO> {
    @Override
    public OrderViewDTO handle(PlaceOrderCommand command) {
        OrderCreationParamsDTO params = enrich(command);
        Order order = OrderFactory.create(params);  // 参数对象，非裸 Command
        orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    private OrderCreationParamsDTO enrich(PlaceOrderCommand cmd) {
        OrderCreationParamsDTO params = new OrderCreationParamsDTO();
        params.setCustomerId(cmd.getCustomerId());
        params.setOperatorId(SecurityUtil.getCurrentUserId());
        params.setRegion(configService.getRegion());
        params.setTotalAmount(priceService.calculate(cmd.getItems()));
        return params;
    }
}
```

**关键点**：`Params` 隔离了"外部契约"（Command）和"内部领域参数"——Command 只含契约字段，Params 可自由富化而不污染契约。

---

## 防腐层中间数据：RecordDTO

**场景**：外部系统推来的数据格式与领域模型截然不同，需要在 Handler 中先转成内部格式，再转领域模型。

**代码示例**（示例应用未实现，展示模式）：

```java
// application/order/dto/PaymentCallbackRecordDTO.java —— 防腐层中间格式
@Data
public class PaymentCallbackRecordDTO {
    private String externalTransactionId;  // 外部系统交易 ID
    private String externalStatus;         // 外部系统状态码（如 "SUCCESS" / "FAILED"）
    private String rawPayload;             // 原始消息体（审计用）
    private OffsetDateTime receivedAt;     // 接收时间
}

// Handler
@Component
public class PaymentCallbackHandler implements EventHandler<PaymentCallbackEvent> {
    @Override
    public void handle(PaymentCallbackEvent event) {
        PaymentCallbackRecordDTO record = toRecord(event);
        Order order = orderRepository.findById(record.toOrderId()).orElseThrow();
        order.reconcilePayment(record.externalTransactionId());  // 领域方法用内部类型
        orderRepository.save(order);
    }
}
```

**关键点**：`Record` 承载"外部系统原始形态"，Handler 做格式转换，领域模型不接触外部格式——这就是防腐层（Anti-Corruption Layer）的落地。

---

## 什么时候用哪种后缀

| 后缀 | 出现路径 | 何时用 |
|------|----------|--------|
| **`ViewDTO`** | Handler → Presenter | 需要多视图或多 CO 输出 |
| **`ParamsDTO`** | Handler → Domain Factory | 入参需要富化（查库/查配置/安全上下文） |
| **`RecordDTO`** | External → Handler → Domain | 外部数据格式与领域模型差异大 |

如果没有多视图、没有富化、没有外部格式差异 → 不需要这些中间对象，Assembler 直接产 CO 即可。