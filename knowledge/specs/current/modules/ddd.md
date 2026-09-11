# 模块用法法卷：common-ddd（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-ddd.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-ddd</artifactId>
</dependency>
```

### 身份词汇发行：`Identifier<V>`

系统 SHALL 于 common-ddd `domain/id` 发行纯 Java 身份词汇接口 `Identifier<V>`（唯一方法 `V value()`，零 JDK 外依赖），作为聚合身份终类型的唯一类型学锚点；架名段与业务落位 `domain/{agg}/id/` 镜像对偶；框架不发行抽象基类、解析器或任何运行期设施。

> Scenario: 域纯度不破 —— GIVEN 新接口文件 ｜ WHEN R4 现行域纯度规则扫描 ｜ THEN 绿，零新增依赖边。取证：`common-ddd/.../domain/id/Identifier.java` 在库；框架扫描 `DddArchitectureTest` r3/r4 绿 11/11＝域纯度不破。

### 场景 1：聚合根（状态机 + 不变量校验）

> §3 场景 1–4 共用同一套虚构教例：Payment 家族，与 `knowledge/docs/how-to/new-aggregate.md` 同族。全部是虚构教例，sample 未实现；真实例形态见 sample-application，两者同构。字段形状与该篇现状对齐，每个场景只取所需子集。

```java
public class Payment extends AggregateRoot<PaymentId> {   // 身份槽实参 = 聚合身份终类型（蓝图 BP-13，见上节词汇发行）
    private PaymentId id;
    private UUID orderId;   // BP-14：跨聚合引用槽准目标币种（教例未铸 OrderId，实形见真实例订单项商品槽）
    private PaymentStatus status;
    private BigDecimal amount;
    private Long version;

    /** 业务构造器（创建新支付） */
    public Payment(PaymentId id, UUID orderId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    /** 重建构造器（Converter 使用） */
    public static Payment reconstitute(PaymentId id, UUID orderId, PaymentStatus status,
                                       BigDecimal amount, Long version) {
        Payment payment = new Payment(id, orderId, amount);
        payment.status = status;
        payment.version = version;
        return payment;
    }

    @Override public PaymentId getId() { return id; }

    /** 支付成功：状态机校验 + 状态变迁 */
    public void succeed() {
        if (status != PaymentStatus.PENDING) {
            throw new BusinessException("payment:err.statusPending");
        }
        this.status = PaymentStatus.SUCCESS;
    }

    /** 不变量校验（仓储 save/update 前自动调用） */
    @Override public void validate() {
        if (orderId == null) {
            throw new BusinessException("payment:err.orderIdRequired");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("payment:err.amountPositive");
        }
    }
}
```

### 场景 2：PO + Mapper + XML + RepositoryImpl（仓储实现）

PO 是纯 `@Data` POJO，**零 ORM 注解**。表名、主键、版本条件、逻辑删除过滤全部写在 XML 的 SQL 文本里：

```java
@Data
public class PaymentPO {
    private UUID id;                 // 业务铸造（UUIDv7 文本），INSERT 显式传参
    private UUID orderId;              // FK 列取原生 uuid 类型，common-pg UUIDTypeHandler 自动直映射（零转换代码）
    private String status;
    private BigDecimal amount;
    private Long version;           // 版本条件由 UPDATE 语句文本携带
    private OffsetDateTime createdAt;   // AuditFieldFiller 填充
    private OffsetDateTime updatedAt;
}
```

Mapper 扩展框架契约接口即可。通用七条语句和业务查询放在同一篇 XML 里承载：

```java
@Mapper
public interface PaymentMapper extends DddMapper<PaymentPO> {
    // 分页双语句 + 业务专有查询，实现在 resources/mapper/payment/PaymentMapper.xml
    List<PaymentPO> selectPageByCondition(@Param("status") String status, @Param("offset") long offset, @Param("limit") long limit);
    long countByCondition(@Param("status") String status);
}
```

RepositoryImpl 继承 `MybatisPersistence`，构造器注入四件依赖：Mapper / Clock / AuditProperties / CurrentUserProvider。

```java
@Component
public class PaymentRepositoryImpl
        extends MybatisPersistence<PaymentMapper, PaymentPO, Payment, PaymentId>
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

    /** 领域 ID（PaymentId）→ PO 主键原生值（UUID）：唯一转换位，每仓储恰一处（PO/Mapper/XML/schema 零修改） */
    @Override protected Serializable toPersistenceId(PaymentId id) { return id.value(); }

    @Override public Optional<Payment> findById(PaymentId id) { return findDomainById(id); }

    @Override public void save(Payment domain) { saveDomain(domain); }

    @Override public void update(Payment domain) { updateDomain(domain); }

    @Override public void deleteById(PaymentId id) { removeDomainById(id); }
}
```

### 场景 3：批量操作

`saveDomainBatch` / `updateDomainBatch` 的语义是**单事务循环**：逐条 insert/update，不是多行 VALUES SQL。原因是每条聚合都必须独立走一遍 `validate()`，多行 UPDATE/INSERT 触发不了逐聚合行为。批量原子性由调用方保证：Handler 标 `@Transactional`。

```java
repository.saveDomainBatch(List.of(payment1, payment2, payment3));      // 批量保存
repository.updateDomainBatch(List.of(payment1, payment2));              // 批量更新
List<Payment> payments = repository.findDomainsByIds(List.of(id1, id2)); // 批量写侧加载

payment.refund();
repository.removeDomain(payment);                        // 实体删除（0 命中抛 SilentWriteLossException——写丢失告警，勿重试）
repository.removeDomainByIds(List.of(id1, id2));         // 按 ID 批量删除（BEST_EFFORT：部分未命中静默跳过，整批 0 命中抛 SilentWriteLossException）
```

### 场景 4：PageResult 分页链路（读侧，绕过 domain）

```java
// application 层：读端口 extends QueryRepository 空标记，返回读 DTO（PO → DTO 直接投影，不经过 domain）
public interface PaymentQueryRepository extends QueryRepository {
    PageResult<PaymentViewDTO> findPage(GetPaymentPageQuery query);
}

// application 层：Handler 整体传入 Query 对象——分页参数由实现侧经 safePageNum()/safePageSize() 统一钳制
@Component
public class GetPaymentPageHandler implements QueryHandler<GetPaymentPageQuery, PageResult<PaymentViewDTO>> {
    @Override
    public PageResult<PaymentViewDTO> handle(GetPaymentPageQuery query) {
        return paymentQueryRepository.findPage(query);
    }
}
```

> 读侧完全绕过 domain 层：不 reconstitute 聚合根，也不建领域读模型，直接从 PO 投影读 DTO。读端口由基础设施层实现，方向是 infrastructure → application，即写侧依赖倒置的读侧镜像。读侧没有业务判断；派生值全部在写侧计算，并物化到 PO 列。详见 `knowledge/docs/how-to/read-path.md`。

写侧完整示例见 `knowledge/docs/how-to/write-path.md`。
