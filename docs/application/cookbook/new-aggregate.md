# 新聚合 Checklist

> 包结构参考 → [directory-structure/overview.md](../directory-structure/overview.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **"新建 Payment（支付）聚合"** 为案例，演示从零创建一个完整聚合的全部文件。

**业务背景：**

1. 支付有自己的生命周期（PENDING → SUCCESS / FAILED / REFUNDED）
2. 支付需要记录第三方交易号、支付渠道、退款信息等独立数据
3. 支付与订单是多对一关系（一个订单可能多次支付尝试）
4. 未来支付可能拆分为独立微服务

因此将支付从 Order 聚合中拆出，建立独立的 Payment 聚合。本文列出从 contract 到 infrastructure 的 **20 个文件**完整模板。

## 文件清单总览

```
sample-service/
├── sample-service-contract/src/main/java/.../contract/
│   └── payment/
│       ├── controller/PaymentController.java     ← ① Controller 契约接口
│       ├── dto/co/PaymentCO.java                    ← ② 契约输出
│       ├── dto/command/CreatePaymentCommand.java        ← ③ Command
│       ├── dto/query/GetPaymentQuery.java             ← ④ Query
│       └── dto/event/PaymentCreatedIntegrationEvent.java  ← ⑤ 集成事件（可选）
│
└── sample-service-server/src/main/java/.../
    ├── adapter/rest/
    │   └── PaymentControllerImpl.java           ← ⑥ Controller 实现（REST 入口）
    ├── application/payment/
    │   ├── PaymentAppService.java               ← ⑦ AppService
    │   ├── dto/PaymentDTO.java                  ← ⑧ 内部 DTO
    │   ├── assembler/PaymentAssembler.java      ← ⑨ Assembler
    │   ├── presenter/PaymentPresenter.java      ← ⑩ Presenter
    │   └── handler/
    │       ├── CreatePaymentHandler.java        ← ⑪ CommandHandler
    │       └── GetPaymentHandler.java           ← ⑫ QueryHandler
    ├── domain/payment/
    │   ├── model/Payment.java                   ← ⑬ 聚合根
    │   ├── model/PaymentStatus.java             ← ⑭ 枚举
    │   ├── model/event/PaymentCreatedEvent.java ← ⑮ DomainEvent（可选）
    │   └── repository/PaymentRepository.java    ← ⑯ Repository 接口
    └── infrastructure/persistence/master/payment/
        ├── po/PaymentPO.java                    ← ⑰ PO
        ├── converter/PaymentConverter.java      ← ⑱ Converter
        ├── mapper/PaymentMapper.java            ← ⑲ Mapper
        └── repository/PaymentRepositoryImpl.java← ⑳ RepositoryImpl
```

## ① Contract — Controller 契约接口

```java
package ...contract.payment.rest;

@Tag(name = "支付服务", description = "支付创建与查询")
@RequestMapping("/payments")
public interface PaymentController {

    @Operation(summary = "创建支付", description = "创建支付记录")
    @PostMapping("")
    PaymentCO createPayment(@Valid @RequestBody CreatePaymentCommand command);

    @Operation(summary = "查询支付详情", description = "根据 ID 获取支付信息")
    @GetMapping("/{paymentId}")
    PaymentCO getPayment(@PathVariable("paymentId") String paymentId);
}
```

> HTTP 映射 + 文档注解在契约接口声明；服务端 ControllerImpl 仅标记 `@RestController` 并透传（见 ⑥）；
> 东西向调用复用同一契约接口（HTTP 直连），无需额外定义。

## ②③④ Contract — 数据类（CO / Command / Query）

三者模式相同：`@Data` + `implements Serializable`。差异仅在标记接口：

| 类 | 标记接口 | 用途 |
|----|---------|------|
| `PaymentCO` | 无（纯输出） | 契约输出，仅含消费方需要的字段 |
| `CreatePaymentCommand` | `Command` | 写操作入参 |
| `GetPaymentQuery` | `Query` | 读操作入参 |

```java
// CO 示例
@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentCO implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String id;
    private String status;
    private BigDecimal amount;
}

// Command 示例
@Data @NoArgsConstructor @AllArgsConstructor
public class CreatePaymentCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String orderId;
    private BigDecimal amount;
}
```

## ⑤ Contract — Integration Event（可选）

```java
public class PaymentCreatedIntegrationEvent implements Event, Serializable {
    private String paymentId;
    private String orderId;
    // 仅含外部服务需要的字段
}
```

## ⑥ Adapter — Controller 实现

```java
// adapter/rest/PaymentControllerImpl.java（实现，仅标记协议 + 透传）
@RestController
public class PaymentControllerImpl implements PaymentController {

    private final PaymentAppService paymentAppService;

    public PaymentControllerImpl(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @Override
    public PaymentCO createPayment(CreatePaymentCommand command) {
        return paymentAppService.createPayment(command);
    }

    @Override
    public PaymentCO getPayment(String paymentId) {
        return paymentAppService.getPayment(new GetPaymentQuery(paymentId));
    }
}
```

## ⑦ Application — AppService

```java
@Service
public class PaymentAppService {

    private final PaymentPresenter presenter;
    private final CreatePaymentHandler createPaymentHandler;
    private final GetPaymentHandler getPaymentHandler;

    // 构造器注入（省略）

    public PaymentCO createPayment(CreatePaymentCommand command) {
        return presenter.present(createPaymentHandler.handle(command));
    }

    public PaymentCO getPayment(GetPaymentQuery query) {
        return presenter.present(getPaymentHandler.handle(query));
    }
}
```

## ⑧⑨⑩ Application — DTO / Assembler / Presenter

**DTO**：内部视图，可含审计字段（CO 不暴露）：

```java
@Data
public class PaymentDTO {
    private String id;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private OffsetDateTime createAt;  // 内部字段
    private Integer version;         // 内部字段
}
```

**Assembler**（Domain → DTO）：

```java
@Component
public class PaymentAssembler implements BasicAssembler<Payment, PaymentDTO> {

    @Override
    public PaymentDTO toDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId().toString());
        dto.setOrderId(payment.getOrderId());
        dto.setStatus(payment.getStatus().name());
        dto.setAmount(payment.getAmount());
        dto.setCreateAt(payment.getCreateAt());
        dto.setVersion(payment.getVersion());
        return dto;
    }

    // toDomain / updateDomain / updateDTO 抛 UnsupportedOperationException（富领域模型走 reconstitute）
}
```

**Presenter**（DTO → CO，过滤内部字段）：

```java
@Component
public class PaymentPresenter implements BasicPresenter<PaymentDTO, PaymentCO> {

    @Override
    public PaymentCO present(PaymentDTO dto) {
        PaymentCO co = new PaymentCO();
        co.setId(dto.getId());
        co.setStatus(dto.getStatus());
        co.setAmount(dto.getAmount());
        // createAt / version 不暴露
        return co;
    }
}
```

## ⑪⑫ Application — Handler

**CommandHandler**（写侧）：

```java
@Component
public class CreatePaymentHandler implements CommandHandler<CreatePaymentCommand, PaymentDTO> {

    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentDTO handle(CreatePaymentCommand command) {
        Payment payment = new Payment(UUID.randomUUID(), command.getOrderId(), command.getAmount());
        payment.create();
        paymentRepository.save(payment);
        return paymentAssembler.toDTO(payment);
    }
}
```

**QueryHandler**（读侧）：

```java
@Component
public class GetPaymentHandler implements QueryHandler<GetPaymentQuery, PaymentDTO> {

    private final PaymentRepository paymentRepository;
    private final PaymentAssembler paymentAssembler;

    // 构造器注入（省略）

    @Override
    public PaymentDTO handle(GetPaymentQuery query) {
        Payment payment = paymentRepository.findById(UUID.fromString(query.getPaymentId()))
                .orElseThrow(() -> new BusinessException("payment:err.notFound"));
        return paymentAssembler.toDTO(payment);
    }
}
```

## ⑬⑭⑮⑯ Domain — 聚合根 / 枚举 / 事件 / Repository

```java
public class Payment extends AggregateRoot<UUID> {

    private UUID id;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private OffsetDateTime createAt;
    private Integer version;

    /** 业务构造器 */
    public Payment(UUID id, String orderId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    /** 重建构造器（Converter 使用） */
    public static Payment reconstitute(UUID id, String orderId, PaymentStatus status,
                                       BigDecimal amount, OffsetDateTime createAt, Integer version) {
        Payment p = new Payment(id, orderId, amount);
        p.status = status;
        p.createAt = createAt;
        p.version = version;
        return p;
    }

    @Override
    public UUID getId() { return id; }

    public void create() {
        validate();
        registerEvent(new PaymentCreatedEvent(id, orderId, amount));
    }

    @Override
    public void validate() {
        if (orderId == null) throw new BusinessException("payment:err.orderIdRequired");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("payment:err.amountPositive");
    }
}

public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }

public class PaymentCreatedEvent extends DomainEvent {
    private final UUID paymentId;
    private final String orderId;
    private final BigDecimal amount;
    // 构造器 + getter
}

public interface PaymentRepository extends Repository<Payment, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

## ⑰⑱⑲⑳ Infrastructure — PO / Converter / Mapper / RepositoryImpl

```java
@Data
@TableName("payments.payments")  // schema 前缀必须
public class PaymentPO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String orderId;
    private String status;
    private BigDecimal amount;
    @Version
    private Integer version;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
    @TableLogic
    private Boolean isDelete;
}

@Component
public class PaymentConverter implements BasicConverter<Payment, PaymentPO> {

    @Override
    public Payment toDomain(PaymentPO po) {
        return Payment.reconstitute(
                UUID.fromString(po.getId()), po.getOrderId(),
                PaymentStatus.valueOf(po.getStatus()),
                po.getAmount(), po.getCreateAt(), po.getVersion());
    }

    @Override
    public PaymentPO toPO(Payment domain) {
        PaymentPO po = new PaymentPO();
        po.setId(domain.getId().toString());
        po.setOrderId(domain.getOrderId());
        po.setStatus(domain.getStatus().name());
        po.setAmount(domain.getAmount());
        po.setVersion(domain.getVersion());
        return po;
    }

    // updateDomain 抛 UnsupportedOperationException；updatePO 合并业务字段
}

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentPO> {}

@Component
public class PaymentRepositoryImpl
        extends MybatisPersistence<PaymentMapper, PaymentPO, Payment>
        implements PaymentRepository {

    private final PaymentConverter converter;

    public PaymentRepositoryImpl(ObjectProvider<DomainEventPublisher> provider,
                                 PaymentConverter converter) {
        super(provider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Payment, PaymentPO> getConverter() { return converter; }
    @Override public Optional<Payment> findById(UUID id) { return findDomainById(id.toString()); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void save(Payment domain) { saveDomain(domain); }
    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Payment domain) { updateDomain(domain); }
    @Override public boolean exists(UUID id) { return existsDomainById(id.toString()); }
    @Override public void deleteById(UUID id) { removeDomainById(id.toString()); }
}
```

## 创建顺序建议

1. **contract**（①-⑤）：先定义公开契约，确定接口边界
2. **domain**（⑬-⑯）：核心模型，零依赖，可独立编译验证
3. **infrastructure**（⑰-⑳）：持久化实现
4. **application**（⑦-⑫）：编排层，串联 domain + infrastructure
5. **adapter**（⑥）：最后接入协议层

## 验证清单

- [ ] `mvn compile` 通过（无循环依赖）
- [ ] ArchUnit 测试通过（`common-test` 规则）
- [ ] Domain 层无框架注解（零 Spring / MyBatis 依赖）
- [ ] `@TableName` 包含 schema 前缀
- [ ] PO 有 `@Version`（乐观锁）和 `@TableLogic`（逻辑删除）
- [ ] Converter.toDomain 使用 `reconstitute()` 重建
- [ ] RepositoryImpl 构造器注入 `ObjectProvider<DomainEventPublisher>`
