# 应用层内部数据对象

> 设计原理 → [module-design/application.md](../explanation/application.md)

## 为什么需要

Application 层在 Handler（领域 ↔ 内部数据）和 Presenter（内部数据 ↔ 契约 CO）之间，需要**语义明确的后缀**来区分不同用途的数据对象，而不是用泛化的 `DTO`。

一个写侧基线 `DTO` + 三种扩展场景对象：

| 场景 | 后缀 | 方向 | 示例 |
|------|------|------|------|
| 写侧投影（Command 结果） | **`DTO`** | Domain → Contract | `{Agg}DTO`（含 version） |
| 读侧投影（Query 结果） | **`ViewDTO`** | PO → Contract | `{Agg}ViewDTO`（不含 version） |
| 入路径富化 | **`ParamsDTO`** | Command → Domain | `{Agg}CreationParamsDTO` |
| 防腐层中间数据 | **`RecordDTO`** | External → Domain | `PaymentCallbackRecordDTO`（虚构教例） |

---

## 写/读投影：DTO vs ViewDTO

DTO（内部视图）与 CO（契约输出）的职责分工规范表 canonical 在 `.agents/rules/03-coding-conventions.md`（DTO / CO 强制分离），本文不复制。在其之上，写侧与读侧 DTO 进一步**解耦**（避免"一个肥 DTO 贯穿所有层"的耦合）：

| DTO | 承载 | Presenter | 说明 |
|-----|------|-----------|------|
| 写侧 `DTO` | 含乐观锁 version | `{Agg}Presenter` | Command 执行后的聚合状态投影 |
| 读侧 `ViewDTO` | 不含 version | `{Agg}ViewPresenter` | Query 的 PO 直接投影（绕过 domain） |

**代码示例**（示例应用已实现，模板以 `{Agg}` 通式表述）：

```java
// 写侧 DTO —— Command 执行后的聚合状态投影（含 version）
// application/{agg}/dto/{Agg}DTO.java
@Data
public class {Agg}DTO implements ApplicationDTO, Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<{Agg}ItemDTO> items;
    private OffsetDateTime createAt, updateAt;  // 内部审计字段
    private Integer version;                     // 写侧关注点，不暴露给外部
}

// 读侧 DTO —— Query 的 PO 直接投影（不含 version，绕过 domain）
// application/{agg}/dto/{Agg}ViewDTO.java
@Data
public class {Agg}ViewDTO implements ApplicationDTO, Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<{Agg}ItemViewDTO> items;
    private OffsetDateTime createAt, updateAt;
}

// 读侧 Presenter 按场景裁剪
@Component
public class {Agg}ViewPresenter implements BasicPresenter<{Agg}ViewDTO, {Agg}CO> {
    // 详情：全字段
    public {Agg}CO present({Agg}ViewDTO view) { ... }
    // 列表：精简字段
    public {Agg}SummaryCO presentSummary({Agg}ViewDTO view) { ... }
}
```

> 真实例映射位：sample-application/.../application/order/dto/OrderDTO.java、OrderViewDTO.java（真实例，示例应用已实现，字段形态与上文一致）。

**关键点**：同一读侧 `ViewDTO` → 多个 CO，Presenter 做裁剪；写侧 `DTO` 与读侧 `ViewDTO` 分离，各自独立演进，不互相复用。

---

## 入路径富化：ParamsDTO

**场景**：Handler 在 Command 与领域工厂之间，需要富化入参（查库、查配置、组装上下文），需要中间对象。

**代码示例**（示例应用未实现，展示模式）：

```java
// application/{agg}/dto/{Agg}CreationParamsDTO.java —— 富化后的入参
@Data
public class {Agg}CreationParamsDTO {
    private String customerId;
    private List<{Agg}ItemParams> items;
    private String operatorId;      // 从安全上下文注入
    private String region;          // 从配置中心注入
    private BigDecimal totalAmount; // 查库计算
}

// Handler
@Component
public class {Action}{Agg}Handler implements CommandHandler<{Action}{Agg}Command, {Agg}DTO> {
    @Override
    public {Agg}DTO handle({Action}{Agg}Command command) {
        {Agg}CreationParamsDTO params = enrich(command);
        {Agg} {agg} = {Agg}Factory.create(params);  // 参数对象，非裸 Command
        {agg}Repository.save({agg});
        return {agg}Assembler.toDTO({agg});
    }

    private {Agg}CreationParamsDTO enrich({Action}{Agg}Command cmd) {
        {Agg}CreationParamsDTO params = new {Agg}CreationParamsDTO();
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
// application/payment/dto/PaymentCallbackRecordDTO.java —— 防腐层中间格式
@Data
public class PaymentCallbackRecordDTO {
    private UUID refId;                      // 关联聚合 ID（外部报文经 toRecord 一次解析定型，不在 Handler 里 String→UUID）
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
        Payment payment = paymentRepository.findById(record.getRefId())
                .orElseThrow(() -> new BusinessException("payment:err.notFound"));
        payment.reconcilePayment(record.getExternalTransactionId());  // 领域方法用内部类型
        paymentRepository.update(payment);                            // 已存在聚合走 update
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