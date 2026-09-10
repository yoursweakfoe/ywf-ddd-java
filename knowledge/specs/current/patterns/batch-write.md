# 用法规范法卷：批量写（框架法 · 严格件）

> **身份**：本卷是批量写**统一用法**的唯一权威——全套规范形状在此，全仓他处不得复写形状；违反本卷=修代码，修卷走 `../../changes/`。docs 同题篇（`../../../docs/how-to/batch-operations.md`）为设计卡（选型与边界叙事，零形状代码）。
> **机器对账**：C1/C3/C4 扫本卷；教例家族 Payment（虚构教例，sample 未实现）。开册法案：2026-09 设计卡降格案；统一用法归卷：2026-09 用法归卷案。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| BW-1 | 批量写 = 单一 Command 携带 ID 集合 + 单一 Handler 整批处理；禁止为批量另发明新层 | 本卷 §2 形状 | C1 |
| BW-2 | ID 集合在契约边界一次定型（`List<UUID>`），禁止降级 String 后在 Handler 手工 `fromString` | 真实例映射：`sample-application/.../contract/order/dto/command/CancelOrderCommand.java`（C3 裁决 String→UUID 后的形状） | C3 |
| BW-3 | 整批原子性由 Handler 的 `@Transactional(rollbackFor = Exception.class)` 保证；框架批量通道（`updateDomainBatch`/`saveDomainBatch`）**刻意不标事务**——事务边界上收应用层 | `MybatisPersistence` javadoc「事务边界」节 | C5 同区 |
| BW-4 | 批量落库一律经基类批量通道（内部逐条循环 `updateDomain`，每条触发 validate）；领域行为调用不得被绕过 | `MybatisPersistence` javadoc；WC-2/WC-3 互指 | 守恒测试 |
| BW-5 | 需要整体原子的调用方**必须自行分片，建议 ≤500 条/批**；框架刻意不设行数护栏（上限属业务容量策略，写死即业务规则渗入骨架） | `MybatisPersistence` javadoc「消费契约」节（两处载明） | 文档即契约 |
| BW-6 | 部分成功模式为独立形态：不标 `@Transactional`、逐条 try-catch 收集、返回双列表结果；两种模式禁止混用（catch 后事务不回滚但语义混乱） | 本卷 §2.3 形状 | 评审项 |
| BW-7 | 批量内单条处理仍走聚合行为（WC-2/WC-3），禁止绕聚合直改表 | WC 卷互指 | ArchUnit |

## §2 规范形状（统一用法唯一样本）

### 2.1 Command（contract 段）

```java
// contract/{agg}/dto/command/BatchConfirmPaymentCommand.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量确认支付命令")
public class BatchConfirmPaymentCommand implements Command, Serializable {

    /** 支付单 ID 列表（领域 ID 类型，不降级为 String） */
    @Schema(description = "支付单 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<UUID> paymentIds;
}
```

### 2.2 Handler（application 段，四拍链的批量形态）

```java
// application/{agg}/handler/command/BatchConfirmPaymentHandler.java
@Component
public class BatchConfirmPaymentHandler implements CommandHandler<BatchConfirmPaymentCommand, List<PaymentDTO>> {

    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;

    public BatchConfirmPaymentHandler(PaymentRepository paymentRepository, PaymentAssembler paymentAssembler) {
        this.paymentRepository = paymentRepository;
        this.paymentAssembler = paymentAssembler;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)          // BW-3：原子性在此层，框架通道不标
    public List<PaymentDTO> handle(BatchConfirmPaymentCommand command) {
        List<Payment> payments = command.getPaymentIds().stream()   // BW-2：ID 已是 UUID，直接传
                .map(id -> paymentRepository.findById(id)
                        .orElseThrow(() -> new BusinessException("payment:err.notFound")))
                .toList();
        payments.forEach(Payment::confirm);                          // BW-4/WC-3：行为在聚合根内
        paymentRepository.updateDomainBatch(payments);               // BW-4：基类批量通道，逐条 validate
        return payments.stream().map(paymentAssembler::toDTO).toList();
    }
}
```

### 2.3 部分失败形态（BW-6，与 2.2 互斥选用）

```java
// 注意：不加 @Transactional——逐条自治，收集双列表
public BatchResultDTO handle(BatchImportCommand command) {
    List<String> successIds = new ArrayList<>();
    List<String> failedIds = new ArrayList<>();
    for (String id : command.getIds()) {
        try {
            /* 单条处理 */
            successIds.add(id);
        } catch (BusinessException e) {
            failedIds.add(id);
        }
    }
    return new BatchResultDTO(successIds, failedIds);
}
```

### 2.4 失败策略选择表（用法侧规范，随形状入卷）

| 策略 | 实现方式 | 适用场景 |
|------|---------|---------|
| 全部成功或全部回滚 | `@Transactional` + 异常传播（§2.2） | 批量确认、批量取消 |
| 跳过失败项，返回结果 | try-catch + 收集（§2.3，**无事务**） | 批量导入（允许部分失败） |
| 无事务（每条独立） | 去掉 `@Transactional` | 批量通知、日志写入 |

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| `updateDomainBatch`/`saveDomainBatch` 基类批量通道 | ✅ | common-ddd `MybatisPersistence` |
| ≤500 分片消费契约 | ✅ | 同文件 javadoc「消费契约」节 |
| 业务批量件（Batch* Command/Handler/AppService/Controller） | ⛔ 虚构教例，sample 未落地 | §2 即落地模板；设计判断见 docs 设计卡 |
