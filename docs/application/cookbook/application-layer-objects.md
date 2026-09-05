# 应用层内部数据对象

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 为什么需要

Application 层在 Handler（领域 ↔ 内部数据）和 Presenter（内部数据 ↔ 契约 CO）之间，需要**语义明确的后缀**来区分不同用途的数据对象，而不是用泛化的 `DTO`。

一个写侧基线 `DTO` + 三种扩展场景对象：

| 场景 | 后缀 | 方向 | 示例 |
|------|------|------|------|
| 写侧投影（Command 结果） | **`DTO`** | Domain → Contract | `OrderDTO`（含 version） |
| 读侧投影（Query 结果） | **`ViewDTO`** | PO → Contract | `OrderViewDTO`（不含 version） |
| 入路径富化 | **`ParamsDTO`** | Command → Domain | `OrderCreationParamsDTO` |
| 防腐层中间数据 | **`RecordDTO`** | External → Domain | `PaymentCallbackRecordDTO` |

---

## 写/读投影：DTO vs ViewDTO

DTO（内部视图）与 CO（契约输出）的职责分工规范表 canonical 在 `.agents/rules/03-coding-conventions.md`（DTO / CO 强制分离），本文不复制。在其之上，写侧与读侧 DTO 进一步**解耦**（避免"一个肥 DTO 贯穿所有层"的耦合）：

| DTO | 承载 | Presenter | 说明 |
|-----|------|-----------|------|
| 写侧 `DTO` | 含乐观锁 version | `OrderPresenter` | Command 执行后的聚合状态投影 |
| 读侧 `ViewDTO` | 不含 version | `OrderViewPresenter` | Query 的 PO 直接投影（绕过 domain） |

**代码示例**（示例应用已实现）：

```java
// 写侧 DTO —— Command 执行后的聚合状态投影（含 version）
// application/order/dto/OrderDTO.java
@Data
public class OrderDTO implements ApplicationDTO, Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> items;
    private OffsetDateTime createAt, updateAt;  // 内部审计字段
    private Integer version;                     // 写侧关注点，不暴露给外部
}

// 读侧 DTO —— Query 的 PO 直接投影（不含 version，绕过 domain）
// application/order/dto/OrderViewDTO.java
@Data
public class OrderViewDTO implements ApplicationDTO, Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<OrderItemViewDTO> items;
    private OffsetDateTime createAt, updateAt;
}

// 读侧 Presenter 按场景裁剪
@Component
public class OrderViewPresenter implements BasicPresenter<OrderViewDTO, OrderCO> {
    // 详情：全字段
    public OrderCO present(OrderViewDTO view) { ... }
    // 列表：精简字段
    public OrderSummaryCO presentSummary(OrderViewDTO view) { ... }
}
```

**关键点**：同一读侧 `ViewDTO` → 多个 CO，Presenter 做裁剪；写侧 `DTO` 与读侧 `ViewDTO` 分离，各自独立演进，不互相复用。

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
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderDTO> {
    @Override
    public OrderDTO handle(PlaceOrderCommand command) {
        OrderCreationParamsDTO params = enrich(command);
        Order order = OrderFactory.create(params);  // 参数对象，非裸 Command
        orderRepository.save(order);
        return orderAssembler.toDTO(order);
    }

    private OrderCreationParamsDTO enrich(PlaceOrderCommand cmd) {
        OrderCreationParamsDTO params = new OrderCreationParamsDTO();
        params.setCustomerId(cmd.getCustomerId());
        params.setOperatorId(SecurityUtil.getString("uid"));  // 按名取 claim（common-security：字段名无规范，不预定义）
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
    private String orderId;                  // 关联的本地订单 ID（外部报文携带或由 Handler 解析）
    private String externalTransactionId;    // 外部系统交易 ID
    private String externalStatus;           // 外部系统状态码（如 "SUCCESS" / "FAILED"）
    private String rawPayload;               // 原始消息体（审计用）
    private OffsetDateTime receivedAt;       // 接收时间
}

// CommandHandler（对账请求经 adapter 转 Command 后进入）
@Component
public class ReconcilePaymentHandler implements CommandHandler<ReconcilePaymentCommand, Void> {
    @Override
    public Void handle(ReconcilePaymentCommand command) {
        PaymentCallbackRecordDTO record = toRecord(command);   // 外部报文 → 内部格式
        Order order = orderRepository.findById(UUID.fromString(record.getOrderId()))
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.reconcilePayment(record.getExternalTransactionId());  // 领域方法用内部类型
        orderRepository.update(order);                              // 已存在聚合走 update
        return null;
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

如果没有多视图、没有富化、没有外部格式差异 → 不需要这些中间对象，沿用 Handler 产 DTO + Presenter 产 CO 的标准链路即可（Assembler 不得跨层直产 CO）。