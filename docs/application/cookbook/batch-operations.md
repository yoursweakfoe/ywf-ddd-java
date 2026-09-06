# 批量操作

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 业务场景

> 本文为**虚构教例**（`payment` 聚合，sample 未实现，落地情况见「实现状态」表）；示例应用真实批量场景需要时按本文模板落地。

本文以 **"批量确认支付单"** 为案例，展示批量写操作从 Command 到数据库的完整路径。

**业务需求：**

1. 后台选中多条支付单，一键批量确认
2. 所有支付单必须在同一事务内完成（要么全部成功，要么全部回滚）
3. 部分支付单状态不合法时，整批失败并返回明确错误

## 调用链路

```
REST 请求（BatchConfirmPaymentCommand）
  → adapter/rest/controller/PaymentControllerImpl
    → application/payment/service/PaymentAppService
      → application/payment/handler/command/BatchConfirmPaymentHandler
        → 循环：repository.findById → payment.confirm()
        → RepositoryImpl 继承基类的 updateDomainBatch 批量通道  ← 批量落库（MybatisPersistence 基行为，逐条 validate）
      → application/payment/presenter/PaymentPresenter
  ← List<PaymentCO>
```

## 1. Contract — 批量 Command

```java
// contract/payment/dto/command/BatchConfirmPaymentCommand.java
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

要点：

- ID 集合直接用 `List<UUID>`，Handler 内**不做** `UUID.fromString` 手工转换（类型契约在边界一次定型）
- 真实例参照：`sample-application/.../contract/order/dto/command/CancelOrderCommand.java` 的 `UUID orderId` 字段（真实例，C3 裁决 String→UUID 后的形状）

## 2. Application — 批量 Handler

```java
// application/payment/handler/command/BatchConfirmPaymentHandler.java
@Component
public class BatchConfirmPaymentHandler implements CommandHandler<BatchConfirmPaymentCommand, List<PaymentDTO>> {

    // region 依赖注入
    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;

    public BatchConfirmPaymentHandler(PaymentRepository paymentRepository, PaymentAssembler paymentAssembler) {
        this.paymentRepository = paymentRepository;
        this.paymentAssembler = paymentAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaymentDTO> handle(BatchConfirmPaymentCommand command) {
        // 1. 批量加载聚合根（ID 已是 UUID，直接传）
        List<Payment> payments = command.getPaymentIds().stream()
                .map(id -> paymentRepository.findById(id)
                        .orElseThrow(() -> new BusinessException("payment:err.notFound")))
                .toList();

        // 2. 逐个调用领域行为（业务规则在聚合根内）
        payments.forEach(Payment::confirm);

        // 3. 批量持久化（经基类批量通道，见下文要点与分片契约）
        paymentRepository.updateDomainBatch(payments);

        // 4. 批量转 DTO
        return payments.stream().map(paymentAssembler::toDTO).toList();
    }
}
```

要点：

- `@Transactional` 保证整批原子性（任一支付单 confirm 失败 → 全部回滚）
- 领域行为 `payment.confirm()` 内含状态校验（非可确认状态抛 BusinessException）
- `updateDomainBatch` 属 `MybatisPersistence` 基类（`RepositoryImpl` 继承后暴露的基行为），**不是** domain `Repository` 五方法生命周期契约的成员；其内部逐条循环 `updateDomain`（每条都触发 validate）
- 批量原子性由 Handler 的 `@Transactional` 保证——框架批量方法本身**不标注**事务（事务边界上收至应用层，`MybatisPersistence` javadoc「事务边界」节）

## 3. 事务边界与失败策略

| 策略 | 实现方式 | 适用场景 |
|------|---------|---------|
| 全部成功或全部回滚 | `@Transactional` + 异常传播 | 批量确认、批量取消 |
| 跳过失败项，返回结果 | Handler 内 try-catch + 收集错误 | 批量导入（允许部分失败） |
| 无事务（每条独立） | 去掉 `@Transactional` | 批量通知、日志写入 |

### 消费契约：分片责任在调用方（≤500 条/批）

批量 = 单事务逐条循环（非多行 SQL），耗时与连接占用线性于批量大小。框架侧契约（canonical：`MybatisPersistence` javadoc「消费契约」节，saveDomainBatch / updateDomainBatch 两处载明）：

- 需要整体原子性的批量调用，调用方（Handler）**必须自行分片，建议 ≤500 条/批**
- 框架刻意**不设行数护栏**：批大小上限属业务容量策略（不同聚合行宽、事务预算差异大），写死数字即业务规则渗入技术骨架——故以文档即契约约束调用方
- 未包裹事务时逐条各自提交、中途失败不回滚已写入记录——原子性诉求下漏标 `@Transactional` 比超限更危险

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

## 实现状态

> 各环节在当前示例应用 / 框架中的落地情况；「未实现」的环节待业务需要时按本文模板补全。

| 环节 | 状态 | 落地位置 |
|------|------|---------|
| `updateDomainBatch` / `saveDomainBatch` 基类批量通道 | ✅ 已实现 | common-ddd `MybatisPersistence.java`（写侧批量方法族） |
| ≤500 分片消费契约（javadoc 载明） | ✅ 已实现 | 同文件 `saveDomainBatch` / `updateDomainBatch` javadoc |
| 示例批量用例（Batch* Command / Handler） | ⛔ 未实现 | sample 无任何 Batch* 文件；本文即落地模板 |

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/command/BatchConfirmPaymentCommand.java` | 批量命令（含 ID 列表，⛔ 虚构教例） |
| application | `handler/command/BatchConfirmPaymentHandler.java` | 批量编排（⛔ 虚构教例） |
| application | `service/PaymentAppService.java` | 委托 + 呈现（⛔ 虚构教例） |
| adapter | `rest/controller/PaymentControllerImpl.java` | 透传（⛔ 虚构教例） |
