# 写路径全链路

> 设计原理 → [module-design/application.md](../explanation/application.md)

## 业务场景

本文与 [new-aggregate.md](new-aggregate.md)、[read-path.md](read-path.md) 同属**虚构教例**（教学中立教义，D4）：教学代码不与 sample 源码挂钩，aggregate 与字段均为虚拟设定。

本文使用虚构聚合 **Reservation（预约单）**（虚构教例，sample 未实现）走查写路径。业务设定：生命周期 `PENDING → CONFIRMED → FULFILLED → COMPLETED`，可从 PENDING/CONFIRMED 状态取消；单内挂多条预约明细（服务项 × 数量 × 单价），以 JSON 物化存储。

以 **"确认预约"** 为案例，展示一个写操作从 REST 入口到数据库落盘的完整代码路径。

**业务规则：**

1. 只有 PENDING 状态的预约才能确认（状态机约束）
2. 确认成功后状态变为 CONFIRMED
3. 确认失败（状态不合法）时抛出 BusinessException，前端收到 422 + i18n 错误码
4. 乐观锁保护并发确认（两人同时点"确认"只有一人成功）

真实例锚点：本文形状与 sample-application 的写路径实现同构（对照 OrderAppService.java:38、PayOrderHandler.java 的 load → 行为 → save → toDTO、Order.java:107-110 的 requireStatus 守卫），仅作对照、不逐字镜像。

## 调用链路

```
REST 请求
  → adapter/rest/controller/ReservationControllerImpl（@RestController，参数包装）
    → application/reservation/service/ReservationAppService（委托 Handler + Presenter 呈现）
      → application/reservation/handler/command/ConfirmReservationHandler（编排领域逻辑）
        → domain/reservation/model/Reservation.confirm()（业务规则 + 状态变迁）
        → domain/reservation/repository/ReservationRepository.update()（持久化抽象）
          → infrastructure/.../repository/ReservationRepositoryImpl（纯 MyBatis + 手写 XML 落盘）
      → application/reservation/presenter/ReservationPresenter（DTO → CO）
  ← ReservationCO（返回调用方）
```

## 1. Contract — Command / CO

```java
// Command：写操作意图（聚合 ID 引用一律 UUID）
@Data @NoArgsConstructor @AllArgsConstructor
public class ConfirmReservationCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @NotNull
    private UUID reservationId;
}

// CO：契约输出（外部安全视图，不暴露 version/审计字段；
// status 值域 = 契约枚举，紧接本围栏后有专段说明）
@Data @NoArgsConstructor @AllArgsConstructor
public class ReservationCO implements CO, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String id;
    private ReservationStatus status;
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private List<ReservationItemCO> items;
}
```

`ReservationStatus` 位于 `contract/reservation/enums/`：契约层枚举镜像 domain 状态机值域，二者奇偶由奇偶守卫测试锁死（真实例：sample 的 ContractEnumParityTest）。消费方从契约 jar 直接拿到合法值域，OpenAPI 自动枚举。

**命令字段校验（输入上界对齐 schema 列宽）**：命令的文本/数值字段一律携带输入上界——文本 `@Size(max = 列宽)`、金额 `@Digits(integer = 8, fraction = 2)`（对齐 DECIMAL(10,2)）——超长、超界输入在绑定层被 `@Valid` 拦成 **400 + fieldErrors**，不再穿透到 DB 变成 500 噪音；400（参数校验）先于 422（业务规则违反）发生。真实例锚点：sample 的 orders 表 customer_id VARCHAR(50)、tracking_number VARCHAR(100)、cancel_reason VARCHAR(500)，products 表 name VARCHAR(100)、price DECIMAL(10,2)（见 schema.sql），对应命令 PlaceOrderCommand / CancelOrderCommand / ShipOrderForm 已按此改型。

```java
// 示例（同一虚构家族）：取消命令——ID 引用统一 UUID；
// reservationId 由 Adapter 从路径参数注入、不在请求体重复携带；
// 文本字段 @Size 对齐原因列 VARCHAR(500)
@Data @NoArgsConstructor @AllArgsConstructor
public class CancelReservationCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 预约 ID（由 Adapter 从路径参数注入，客户端无需传递） */
    @Schema(hidden = true)
    private UUID reservationId;

    /** 取消原因（上界对齐 cancel_reason 列 VARCHAR(500)） */
    @NotBlank @Size(max = 500)
    private String reason;
}
```

## 2. Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/reservation/adapter/rest/controller/ReservationController.java（契约接口，承载 HTTP 映射 + 文档注解）
@Tag(name = "预约服务", description = "预约生命周期管理")
@RequestMapping("/reservations")
public interface ReservationController {

    @Operation(summary = "确认预约", description = "将 PENDING 预约标记为已确认")
    @PutMapping("/{reservationId}/confirm")
    ReservationCO confirmReservation(@PathVariable("reservationId") UUID reservationId);
}

// adapter/rest/controller/ReservationControllerImpl.java（实现，仅标记协议 + 透传；RestAdapter 标记见规则 R8a/R8b）
@RestController
public class ReservationControllerImpl implements ReservationController, RestAdapter {

    private final ReservationAppService reservationAppService;

    public ReservationControllerImpl(ReservationAppService reservationAppService) {
        this.reservationAppService = reservationAppService;
    }

    @Override
    public ReservationCO confirmReservation(UUID reservationId) {
        return reservationAppService.confirmReservation(new ConfirmReservationCommand(reservationId));
    }
}
```

## 3. Application — AppService / Handler / Assembler / Presenter

**AppService**（聚合入口，委托 + 呈现）：

```java
@Service
public class ReservationAppService implements ApplicationService {

    private final ReservationPresenter reservationPresenter;
    private final ConfirmReservationHandler confirmReservationHandler;

    public ReservationCO confirmReservation(ConfirmReservationCommand command) {
        return reservationPresenter.present(confirmReservationHandler.handle(command));
    }
}
```

**CommandHandler**（用例执行，固定模式：load → 行为 → save → toDTO）：

```java
@Component
public class ConfirmReservationHandler implements CommandHandler<ConfirmReservationCommand, ReservationDTO> {

    private final ReservationRepository reservationRepository;
    private final ReservationAssembler reservationAssembler;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDTO handle(ConfirmReservationCommand command) {
        Reservation reservation = reservationRepository.findById(command.getReservationId())
                .orElseThrow(() -> new BusinessException("reservation:err.notFound"));
        reservation.confirm();
        reservationRepository.update(reservation);
        return reservationAssembler.toDTO(reservation);
    }
}
```

**Assembler**（Domain → DTO，逐字段显式映射）：

```java
@Component
public class ReservationAssembler implements BasicAssembler<Reservation, ReservationDTO> {

    @Override
    public ReservationDTO toDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId().toString());
        dto.setStatus(reservation.getStatus().name());
        dto.setItems(reservation.getItems().stream()
                .map(item -> new ReservationDTO.ReservationItemDTO(
                        item.serviceId(), item.quantity(), item.unitPrice()))
                .toList());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setCustomerId(reservation.getCustomerId());
        dto.setConfirmationCode(reservation.getConfirmationCode());
        dto.setCancelReason(reservation.getCancelReason());
        dto.setCreateAt(reservation.getCreateAt());
        dto.setVersion(reservation.getVersion());
        return dto;
    }

    // BasicAssembler 是单向契约：仅声明 toDTO，List/Set 批量方法由接口 default 委托。
    // DTO 只是只读出口视图——聚合构造入口恒为 Factory / reconstitute() 两扇门，
    // 接口不声明 DTO → Domain 方法（最小契约见 new-aggregate.md ⑧）。
}
```

**Presenter**（DTO → CO，过滤内部字段）：

```java
@Component
public class ReservationPresenter implements BasicPresenter<ReservationDTO, ReservationCO> {

    @Override
    public ReservationCO present(ReservationDTO dto) {
        ReservationCO co = new ReservationCO();
        co.setId(dto.getId());
        // 内部 DTO 恒为 String（Assembler 走 domain.name()）；String → 契约枚举在呈现层收口，
        // 值域奇偶由守卫测试锁死、脏值当场 fail-fast，映射不外溢
        co.setStatus(ReservationStatus.valueOf(dto.getStatus()));
        co.setItems(presentItems(dto.getItems()));
        co.setTotalAmount(dto.getTotalAmount());
        co.setCustomerId(dto.getCustomerId());
        co.setConfirmationCode(dto.getConfirmationCode());
        co.setCancelReason(dto.getCancelReason());
        // createAt / updateAt / version 不暴露
        return co;
    }
}
```

## 4. Domain — 聚合根 + 值对象

```java
public class Reservation extends AggregateRoot<UUID> {

    private UUID id;
    private ReservationStatus status;
    private List<ReservationItem> items;
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private Integer version;
    // ...

    public void confirm() {
        requireStatus("reservation:err.status.pending", ReservationStatus.PENDING);
        this.status = ReservationStatus.CONFIRMED;
    }

    @Override
    public void validate() {
        if (items == null || items.isEmpty())
            throw new BusinessException("reservation:err.itemsEmpty");
        if (customerId == null)
            throw new BusinessException("reservation:err.customerIdRequired");
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("reservation:err.totalMustBePositive");
    }
}

// 值对象：首选 record，天然不可变
public record ReservationItem(UUID serviceId, int quantity, BigDecimal unitPrice) implements ValueObject {

    public ReservationItem {
        if (serviceId == null) throw new BusinessException("reservation:err.serviceIdRequired");
        if (quantity <= 0) throw new BusinessException("reservation:err.quantityMustBePositive");
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
```

## 5. Domain — Repository 接口

```java
public interface ReservationRepository extends Repository<Reservation, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

## 6. Infrastructure — PO / Converter / Mapper + XML / RepositoryImpl

PO 是纯 `@Data` POJO——零 ORM 注解；表名、乐观锁版本条件、逻辑删除过滤全部在 XML 的 SQL 文本里（语句模板见 [new-aggregate.md](new-aggregate.md) ⑲）：

```java
@Data
public class ReservationPO {
    private String id;                 // 业务铸造（UUID 文本），INSERT 显式传参
    private String status;
    private String items;              // JSON 序列化
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private Integer version;           // 条件由 updateById 语句文本携带
    private OffsetDateTime createAt;   // AuditFieldFiller 填充
    private OffsetDateTime updateAt;
}

@Mapper
public interface ReservationMapper extends DddMapper<ReservationPO> {
    // DddMapper 七条通用语句由 resources/mapper/reservation/ReservationMapper.xml 手写实现
}

@Component
public class ReservationConverter implements BasicConverter<Reservation, ReservationPO> {

    @Override
    public Reservation toDomain(ReservationPO po) {
        return Reservation.reconstitute(
                UUID.fromString(po.getId()), ReservationStatus.valueOf(po.getStatus()),
                deserializeItems(po.getItems()), po.getTotalAmount(),
                po.getCustomerId(), po.getConfirmationCode(), po.getCancelReason(),
                po.getCreateAt(), po.getUpdateAt(), po.getVersion());
    }

    @Override
    public ReservationPO toPO(Reservation domain) {
        ReservationPO po = new ReservationPO();
        po.setId(domain.getId().toString());
        po.setStatus(domain.getStatus().name());
        po.setItems(serializeItems(domain.getItems()));
        po.setTotalAmount(domain.getTotalAmount());
        po.setCustomerId(domain.getCustomerId());
        po.setConfirmationCode(domain.getConfirmationCode());
        po.setCancelReason(domain.getCancelReason());
        po.setVersion(domain.getVersion());
        return po;
    }

    // 最小契约：仅 toDomain / toPO（+ 集合委托）；不定义增量更新方法（富模型走 reconstitute 全量快照）
}

@Component
public class ReservationRepositoryImpl
        extends MybatisPersistence<ReservationMapper, ReservationPO, Reservation, UUID>
        implements ReservationRepository {

    private final ReservationConverter converter;

    public ReservationRepositoryImpl(ReservationMapper mapper,
                                     ReservationConverter converter,
                                     Clock clock,
                                     AuditProperties auditProperties,
                                     ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Reservation, ReservationPO> getConverter() { return converter; }
    @Override protected Serializable toPersistenceId(UUID id) { return id.toString(); }
    @Override public Optional<Reservation> findById(UUID id) { return findDomainById(id); }
    @Override public void save(Reservation domain) { saveDomain(domain); }
    @Override public void update(Reservation domain) { updateDomain(domain); }
    @Override public boolean exists(UUID id) { return existsDomainById(id); }
    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

> 仓储只负责持久化与不变量校验（save/update 前自动 `validate()`，并经 `AuditFieldFiller` 显式填充审计字段）；事务边界在应用层 Handler；跨聚合协调 = 同事务直调。

并发确认的行为等价性由 XML 的 `updateById` 语句保证：`SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false`——无任何运行时拦截器参与。影响行数 0 时基类 `MybatisPersistence` 经存在性探测按**语义三分通道**处置（绝不静默失败）：

- **版本条件未命中（实体仍存在）** → `OptimisticLockConflictException`：409，可重试，属正常并发流；
- **更新目标已并发消失** → 普通 `IllegalStateException`：409，业务竞态，重试无意义、不应被重试器吞掉；
- **INSERT / DELETE 影响 0 行** → `SilentWriteLossException`（类型在 `com.yoursweakfoe.common.exception.type`）：写丢失级不可能状态，500 + ERROR 告警通道，勿重试、需人工介入。

真实例锚点：异常 → HTTP 映射的唯一完整表在 GlobalRestExceptionHandler javadoc，docs 侧 canonical 见 [common-exception.md](../reference/api/common-exception.md)。

## 完整文件清单

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/command/ConfirmReservationCommand.java` | 写操作意图 |
| contract | `dto/co/ReservationCO.java` | 契约输出 |
| contract | `enums/ReservationStatus.java` | 契约枚举（值域镜像 domain，奇偶由守卫测试锁死） |
| contract | `adapter/rest/controller/ReservationController.java` | Controller 契约接口 |
| adapter | `rest/controller/ReservationControllerImpl.java` | 协议适配（透传） |
| application | `service/ReservationAppService.java` | 聚合入口 |
| application | `handler/command/ConfirmReservationHandler.java` | 用例编排 |
| application | `assembler/ReservationAssembler.java` | Domain → DTO |
| application | `presenter/ReservationPresenter.java` | DTO → CO |
| application | `dto/ReservationDTO.java` | 内部视图 |
| domain | `model/Reservation.java` | 聚合根（业务规则） |
| domain | `model/ReservationItem.java` | 值对象 |
| domain | `model/ReservationStatus.java` | 状态枚举（domain） |
| domain | `repository/ReservationRepository.java` | 持久化抽象 |
| infrastructure | `mybatis/po/ReservationPO.java` | 持久化对象（纯 POJO，零 ORM 注解） |
| infrastructure | `converter/ReservationConverter.java` | Domain ↔ PO（框架 BasicConverter 桥） |
| infrastructure | `mybatis/mapper/ReservationMapper.java` | Mapper（extends DddMapper，七条通用语句契约） |
| resources | `mapper/reservation/ReservationMapper.xml` | 手写 SQL（表名 / 版本条件 / 逻辑删除过滤逐条可见） |
| infrastructure | `repository/ReservationRepositoryImpl.java` | 仓储实现（继承 MybatisPersistence） |
