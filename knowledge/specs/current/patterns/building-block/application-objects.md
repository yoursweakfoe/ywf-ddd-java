# 用法规范法卷：应用层内部对象（框架法 · 严格件）

> **身份**：本卷是应用层内部数据对象统一用法的唯一权威，对象共四种：DTO、ViewDTO、Params、Record。规范形状只写在本卷，全仓其他文档不得复写。违反本卷就改代码；修改本卷必须走 `../../../changes/` 立案。docs 同题篇 `../../../../docs/how-to/application-layer-objects.md` 是设计卡，只讲选型与边界的叙事，零形状代码。
> **机器对账**：C1/C3/C4 扫本卷；教例家族 = {Agg} 通式 + Payment（虚构教例）。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| AO-1 | 三种中间对象各有准入门槛。没有契约投影需求、没有聚合行为需求、也没有外部格式需求时，一律走 DTO + CO 标准链路（WC-4/WC-5），不得发明第四种载体。 | `how-to/application-layer-objects.md` 结论段：「没有契约视图、没有复杂参数、没有外部格式 → 不需要这些中间对象」 | — |
| AO-2 | ViewDTO 只承载读侧多视图投影，放 `application/{agg}/dto/`，必须实现 `ApplicationDTO` 标记。ViewPresenter 专职 DTO→CO。 | R10b；RC-4 互指 | ArchUnit |
| AO-3 | Params 只在方法签名需要聚合超过 3 个参数时使用；不跨 Handler 边界传播。 | docs 设计卡「适用场景」段 | — |
| AO-4 | Record 是不可变状态载体：唯一入口是私有构造 + 静态工厂，与聚合重建的 `reconstitute()` 同型。禁止裸 setter 构造。 | `BasicConverter` 重建契约 → `modules/ddd.md` 场景 2；TC-2 互指 | — |
| AO-5 | 乐观锁 version 是写侧关注点：写侧 DTO 承载它，Presenter 不得把它暴露进 CO。读侧 ViewDTO 一律不含 version。写侧 DTO 与读侧 ViewDTO 分离，不互相复用，各自独立演进。 | docs 设计卡写/读投影对比表与关键点段；本卷 §2.1 形状；真实例确认：sample OrderDTO / OrderViewDTO 与模板一致 | — |
| AO-6 | 读侧多视图：同一个 ViewDTO 呈现多个 CO，详情版全字段，列表版精简字段；Presenter 按场景提供方法裁剪。ViewPresenter 实现 `BasicPresenter`，与写侧 Presenter 平行。 | docs 设计卡写/读投影关键点段；本卷 §2.1 形状 | — |
| AO-7 | 领域工厂的入参是 Params 参数对象，不是裸 Command。Command 只含契约字段；安全上下文、配置中心、查库计算这些富化结果一律装进 Params 再入厂。 | docs 设计卡入路径富化关键点段：「Params 隔离外部契约与内部领域参数」；本卷 §2.2 形状 | — |
| AO-8 | 外部报文先经 `toRecord` 一次解析定型，再进主流程。ID 在防腐入口定型成 UUID，不在 Handler 内手工 String→UUID。领域方法只接收内部类型。对已存在的聚合，持久化走 `update`。 | docs 设计卡防腐层教学注释与关键点段：领域模型不接触外部格式，即 Anti-Corruption Layer 的落地；本卷 §2.3 形状 | BW-2 同类互指 |

## §2 规范形状

本节是全仓统一的唯一形状样本。落地状态逐件登记于 §3：§2.1 示例应用已实现；§2.2、§2.3 示例应用未实现，为展示模式（虚构教例）。

### 2.1 写/读投影：DTO vs ViewDTO vs Presenter

```java
// 写侧 DTO —— Command 执行后的聚合状态投影（含 version）
// application/{agg}/dto/{Agg}DTO.java
@Data
public class {Agg}DTO implements ApplicationDTO, Serializable {
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<{Agg}ItemDTO> items;
    private OffsetDateTime createdAt, updatedAt;  // 内部审计字段
    private Long version;                     // AO-5：写侧关注点，不暴露给外部
}

// 读侧 DTO —— Query 的 PO 直接投影（不含 version，绕过 domain）
// application/{agg}/dto/{Agg}ViewDTO.java
@Data
public class {Agg}ViewDTO implements ApplicationDTO, Serializable {   // AO-2：ApplicationDTO 标记（R10b）
    private String id, status, customerId, trackingNumber, cancelReason;
    private BigDecimal totalAmount;
    private List<{Agg}ItemViewDTO> items;
    private OffsetDateTime createdAt, updatedAt;                        // AO-5：读侧一律无 version
}

// 读侧 Presenter 按场景裁剪
@Component
public class {Agg}ViewPresenter implements BasicPresenter<{Agg}ViewDTO, {Agg}CO> {   // AO-6
    // 详情：全字段
    public {Agg}CO present({Agg}ViewDTO view) { ... }
    // 列表：精简字段
    public {Agg}SummaryCO presentSummary({Agg}ViewDTO view) { ... }    // AO-6：同一 ViewDTO → 多个 CO
}
```

> 真实例映射位：sample-application/.../application/order/dto/OrderDTO.java、OrderViewDTO.java。真实例，示例应用已实现，字段形态与上文一致。

### 2.2 入路径富化：ParamsDTO

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
        {Agg}CreationParamsDTO params = enrich(command);   // AO-7：Command（契约）→ Params（内部富化）
        {Agg} {agg} = {Agg}Factory.create(params);  // 参数对象，非裸 Command   ← AO-7
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

### 2.3 防腐层中间数据：RecordDTO

> ⚠ 未决冲突登记：落地一律以 AO-4 为准，即不可变、私有构造 + 静态工厂唯一入口。下方教学模板是可 set 形态；模板与法条的差异如何裁决，尚未定案：`<!-- 待 ../../../changes/ 补全 -->`。

```java
// application/payment/dto/PaymentCallbackRecordDTO.java —— 防腐层中间格式（虚构教例）
@Data   // ⚠ AO-4：法卷要求私有构造 + 静态工厂唯一入口，禁止裸 setter 构造——本模板形态与法条冲突，以 AO-4 为准
public class PaymentCallbackRecordDTO {
    private UUID refId;                      // 关联聚合 ID（外部报文经 toRecord 一次解析定型，不在 Handler 里 String→UUID）← AO-8
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
        PaymentCallbackRecordDTO record = toRecord(command);   // 外部报文 → 内部格式   ← AO-8：一次解析定型
        Payment payment = paymentRepository.findById(record.getRefId())
                .orElseThrow(() -> new BusinessException("payment:err.notFound"));
        payment.reconcilePayment(record.getExternalTransactionId());  // 领域方法用内部类型   ← AO-8：领域模型不接触外部格式
        paymentRepository.update(payment);                            // 已存在聚合走 update   ← AO-8
        return null;
    }
}
```

### 2.4 四对象角色总表

Handler 负责领域与内部数据互转，Presenter 负责内部数据与契约 CO 互转。两者之间的数据对象用语义明确的后缀区分用途，不许只叫泛化的 `DTO`。写侧基线是 `DTO`，另有三种扩展场景对象：

| 场景 | 后缀 | 方向 | 示例 |
|------|------|------|------|
| 写侧投影（Command 结果） | **`DTO`** | Domain → Contract | `{Agg}DTO`（含 version） |
| 读侧投影（Query 结果） | **`ViewDTO`** | PO → Contract | `{Agg}ViewDTO`（不含 version） |
| 入路径富化 | **`ParamsDTO`** | Command → Domain | `{Agg}CreationParamsDTO` |
| 防腐层中间数据 | **`RecordDTO`** | External → Domain | `PaymentCallbackRecordDTO`（虚构教例） |

### 2.5 写/读投影承载对比

DTO（内部视图）与 CO（契约输出）的职责分工规范表，canonical 在 `knowledge/specs/current/patterns/discipline/coding-conventions.md`，即 DTO/CO 强制分离条款，本卷不复制。本卷在它之上再加一层解耦：写侧 DTO 与读侧 DTO 分开，避免一个肥 DTO 贯穿所有层。

| DTO | 承载 | Presenter | 说明 |
|-----|------|-----------|------|
| 写侧 `DTO` | 含乐观锁 version | `{Agg}Presenter` | Command 执行后的聚合状态投影 |
| 读侧 `ViewDTO` | 不含 version | `{Agg}ViewPresenter` | Query 的 PO 直接投影（绕过 domain） |

### 2.6 后缀准入选择表

| 后缀 | 出现路径 | 何时用 |
|------|----------|--------|
| **`ViewDTO`** | Handler → Presenter | 需要多视图或多 CO 输出 |
| **`ParamsDTO`** | Handler → Domain Factory | 入参需要富化（查库/查配置/安全上下文） |
| **`RecordDTO`** | External → Handler → Domain | 外部数据格式与领域模型差异大 |

三种需求都没有时，不需要这些中间对象：沿用 Handler 产 DTO、Presenter 产 CO 的标准链路即可。见 AO-1。另记一条：Assembler 不得跨层直产 CO。

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| AO-1~4 形态条款（框架法） | ✅ | §1；AO-2 在 sample 已有现行 ViewDTO |
| AO-5~8 形态条款（框架法） | ✅ | §1；形状见 §2.1~§2.3 |
| 写/读投影 DTO / ViewDTO / ViewPresenter（真实例：sample OrderDTO、OrderViewDTO 已实现） | ✅ | §2.1 真实例映射位 |
| ParamsDTO 富化教例件 | ⛔ 示例应用未实现（展示模式） | §2.2 即落地模板 |
| RecordDTO 防腐教例件 | ⛔ 示例应用未实现（展示模式） | §2.3 即落地模板；模板 @Data 形态与 AO-4 的冲突待 changes/ 裁决 |
| AO-3/AO-4 未引入件 | ✅（真空满足） | 按需引入件。未引入就不存在违反，不挂 ⛔ |
