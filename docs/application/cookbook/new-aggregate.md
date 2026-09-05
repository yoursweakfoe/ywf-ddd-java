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

因此将支付从 Order 聚合中拆出，建立独立的 Payment 聚合。本文列出从 contract 到 infrastructure 的 **19 个文件**完整模板。

## 文件清单总览

```
sample-service/
├── sample-service-contract/src/main/java/.../contract/
│   └── payment/
│       ├── adapter/rest/PaymentController.java     ← ① Controller 契约接口
│       ├── dto/co/PaymentCO.java                    ← ② 契约输出
│       ├── dto/command/CreatePaymentCommand.java        ← ③ Command
│       └── dto/query/GetPaymentQuery.java             ← ④ Query
│
└── sample-service-server/src/main/java/.../
    ├── adapter/rest/controller/
    │   └── PaymentControllerImpl.java           ← ⑤ Controller 实现（REST 入口）
    ├── application/payment/
    │   ├── PaymentAppService.java               ← ⑥ AppService
    │   ├── dto/PaymentDTO.java                  ← ⑦ 内部 DTO
    │   ├── assembler/PaymentAssembler.java      ← ⑧ Assembler
    │   ├── presenter/PaymentPresenter.java      ← ⑨ Presenter
    │   └── handler/
    │       ├── CreatePaymentHandler.java        ← ⑩ CommandHandler
    │       └── GetPaymentHandler.java           ← ⑪ QueryHandler
    ├── domain/payment/
    │   ├── model/Payment.java                   ← ⑫ 聚合根
    │   ├── model/PaymentStatus.java             ← ⑬ 枚举
    │   └── repository/domain/PaymentRepository.java ← ⑭ Repository 接口（写侧）
    └── infrastructure/persistence/master/payment/
        ├── mybatis/po/PaymentPO.java              ← ⑮ PO（纯 POJO，零 ORM 注解）
        ├── mybatis/mapper/PaymentMapper.java      ← ⑰ Mapper（extends DddMapper）
        ├── converter/PaymentConverter.java        ← ⑯ Converter（框架 BasicConverter 桥）
        └── repository/domain/PaymentRepositoryImpl.java ← ⑱ RepositoryImpl（继承 MybatisPersistence）

sample-service-server/src/main/resources/
└── mapper/payment/PaymentMapper.xml               ← ⑲ 手写 SQL（DddMapper 七条语句契约）
```

## ① Contract — Controller 契约接口

```java
package ...contract.payment.adapter.rest;

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

> HTTP 映射 + 文档注解在契约接口声明；服务端 ControllerImpl 仅标记 `@RestController` 并透传（见 ⑤）；
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

## ⑤ Adapter — Controller 实现

```java
// adapter/rest/controller/PaymentControllerImpl.java（实现，仅标记协议 + 透传）
// 注意：实现类需追加实现 ScheduledAdapter 同族的 RestAdapter 标记
// （com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter，规则 R8a/R8b）
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

## ⑥ Application — AppService

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

## ⑦⑧⑨ Application — DTO / Assembler / Presenter

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

    // 最小契约：仅 toDomain / toDTO（+ 集合委托）；toDomain 抛 UnsupportedOperationException（富领域模型走 reconstitute）
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

## ⑩⑪ Application — Handler

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

## ⑫⑬⑭ Domain — 聚合根 / 枚举 / Repository

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
    }

    @Override
    public void validate() {
        if (orderId == null) throw new BusinessException("payment:err.orderIdRequired");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("payment:err.amountPositive");
    }
}

public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }

public interface PaymentRepository extends Repository<Payment, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

## ⑮⑯⑰⑱⑲ Infrastructure — PO / Converter / Mapper / XML / RepositoryImpl

PO 是纯 `@Data` POJO——**零 ORM 注解**，表名、主键策略、版本条件、逻辑删除过滤全部由 XML 的 SQL 文本承担：

```java
@Data
public class PaymentPO {
    private String id;                 // 业务铸造（UUID 文本），INSERT 显式传参
    private String orderId;
    private String status;
    private BigDecimal amount;
    private Integer version;           // 乐观锁：条件由 UPDATE 语句文本携带
    private OffsetDateTime createAt;   // AuditFieldFiller 填充
    private OffsetDateTime updateAt;
    private String createdBy;          // 可选：容器存在 CurrentUserProvider 才填
    private String updatedBy;
}
```

```java
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

    // 最小契约：仅 toDomain / toPO（+ 集合委托）；不定义增量更新方法（富模型走 reconstitute 全量快照）
}
```

```java
@Mapper
public interface PaymentMapper extends DddMapper<PaymentPO> {
    // 通用七条语句由同篇 XML 实现（namespace = 本接口全限定名）；
    // 业务专有查询（如按唯一键单查）在此追加具名方法
}
```

手写 XML（`src/main/resources/mapper/payment/PaymentMapper.xml`）——七条语句逐条可见。逻辑删除列 `is_delete` 不入 INSERT（靠 DB 默认值）、出现在每条 select/update/delete 的 WHERE 条件里；`updateById` 携带版本条件；删除语句消费基类传入的 `now` / `updatedBy` 审计参数（操作人列以 `<if>` 守卫）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="...infrastructure.persistence.master.payment.mybatis.mapper.PaymentMapper">

    <sql id="columns">
        id, order_id, status, amount, version, create_at, update_at, created_by, updated_by, is_delete
    </sql>

    <!-- 枚举全部业务列；version 写字面量 0（新建聚合初始版本）；is_delete 不枚举 → DB 默认 FALSE -->
    <insert id="insert">
        INSERT INTO payments.payments (id, order_id, status, amount, version,
                                       create_at, update_at, created_by, updated_by)
        VALUES (#{id}, #{orderId}, #{status}, #{amount}, 0,
                #{createAt}, #{updateAt}, #{createdBy}, #{updatedBy})
    </insert>

    <!-- 全量 UPDATE + 乐观锁版本条件 + 逻辑删除过滤；update_at 由 AuditFieldFiller 刷新 -->
    <update id="updateById">
        UPDATE payments.payments
        SET order_id   = #{orderId},
            status     = #{status},
            amount     = #{amount},
            version    = version + 1,
            update_at  = #{updateAt}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE id = #{id}
          AND version = #{version}
          AND is_delete = false
    </update>

    <select id="selectById" resultType="...infrastructure.persistence.master.payment.mybatis.po.PaymentPO">
        SELECT <include refid="columns"/>
        FROM payments.payments
        WHERE id = #{id}
          AND is_delete = false
    </select>

    <select id="selectByIds" resultType="...infrastructure.persistence.master.payment.mybatis.po.PaymentPO">
        SELECT <include refid="columns"/>
        FROM payments.payments
        WHERE is_delete = false
          AND id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </select>

    <!-- 逻辑删除 = UPDATE 置位 + 审计刷新（now / updatedBy 由基类经 Clock / CurrentUserProvider 传入） -->
    <update id="deleteById">
        UPDATE payments.payments
        SET is_delete = true,
            update_at = #{now}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE id = #{id}
          AND is_delete = false
    </update>

    <update id="deleteByIds">
        UPDATE payments.payments
        SET is_delete = true,
            update_at = #{now}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE is_delete = false
          AND id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </update>

    <!-- 轻量存在性探测：恒返回一行 boolean（UPDATE 行数 0 时的冲突分类依赖它） -->
    <select id="existsById" resultType="boolean">
        SELECT EXISTS (
            SELECT 1 FROM payments.payments WHERE id = #{id} AND is_delete = false
        )
    </select>
</mapper>
```

> 不需要逻辑删除的聚合：`deleteById` 写物理 `DELETE FROM ... WHERE id = #{id}`、各 select 省略 `is_delete` 条件即可——语义选择落在 XML 文本，聚合之间互不影响。

```java
@Component
public class PaymentRepositoryImpl
        extends MybatisPersistence<PaymentMapper, PaymentPO, Payment, UUID>
        implements PaymentRepository {

    private final PaymentConverter converter;

    public PaymentRepositoryImpl(PaymentMapper mapper,
                                 PaymentConverter converter,
                                 Clock clock,
                                 AuditProperties auditProperties,
                                 ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Payment, PaymentPO> getConverter() { return converter; }
    @Override protected Serializable toPersistenceId(UUID id) { return id.toString(); }
    @Override public Optional<Payment> findById(UUID id) { return findDomainById(id); }
    @Override public void save(Payment domain) { saveDomain(domain); }
    @Override public void update(Payment domain) { updateDomain(domain); }
    @Override public boolean exists(UUID id) { return existsDomainById(id); }
    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

> 构造器注入四件框架依赖（`Clock` / `AuditProperties` / `ObjectProvider<CurrentUserProvider>` 加业务 Mapper 与 Converter）；`save/update` 自动 `validate()` 并经 `AuditFieldFiller` 显式填充审计字段；事务边界在 Handler（本类不标 `@Transactional`）；跨聚合协调 = 同事务直调。

## 创建顺序建议

1. **contract**（①-④）：先定义公开契约，确定接口边界
2. **domain**（⑫-⑭）：核心模型，零依赖，可独立编译验证
3. **infrastructure**（⑮-⑲）：持久化实现（PO → Mapper 接口 → XML → RepositoryImpl）
4. **application**（⑥-⑪）：编排层，串联 domain + infrastructure
5. **adapter**（⑤）：最后接入协议层

## 验证清单

- [ ] `mvn compile` 通过（无循环依赖）
- [ ] ArchUnit 测试通过（`common-test` 规则）
- [ ] Domain 层无框架注解（零 Spring / MyBatis 依赖）
- [ ] XML 语句表名含 schema 前缀（如 `payments.payments`）
- [ ] PO 纯 `@Data` 零 ORM 注解；`updateById` 语句携带 `SET version = version + 1 ... AND version = #{version}`
- [ ] 每条 select/update/delete 语句（逻辑删除聚合）显式携带 `AND is_delete = false`
- [ ] `insert` 不枚举 `is_delete`（DB 默认值）；`existsById` 恒返回一行 boolean
- [ ] Mapper XML 位于 `resources/mapper/{agg}/` 且 namespace = Mapper 接口全限定名
- [ ] Converter.toDomain 使用 `reconstitute()` 重建
