# 用法规范法卷：写用例链（框架法 · 严格件）

> **身份**：本卷是写用例链**统一用法**的唯一权威——条款与全套规范形状在此，全仓他处不得复写形状；违反本卷=修代码，修卷只走 `../../changes/` 程序。docs 同题篇（`../../../docs/how-to/write-path.md`）为设计卡（选型与边界叙事，零形状代码），冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：C1（`{agg}` 实例化）/ C3（符号）/ C4（中立）扫本卷；教例家族 Reservation（虚构教例，sample 未实现），真实例只准出现在带「真实例」标记的指针位。开册法案：`2026-09-howto-codification`；统一用法归卷：`2026-09-usage-consolidation`。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| WC-1 | 依赖方向单向：`adapter → application → domain ← infrastructure`；domain 零框架运行时依赖（stereotype 豁免，纯 Java + common-ddd） | ArchUnit 双端守护（common-test 规则集） | R1/R2 系 |
| WC-2 | 写侧 Handler 执行形态固定四拍：load 聚合 → 调聚合行为 → save → toDTO；Handler 标 `@Transactional` | `modules/ddd.md` 场景 3；AGENTS 九条 3 | ArchUnit |
| WC-3 | 业务规则（if-throw）封在聚合根内；Handler 内禁止业务分支判断 | AGENTS 九条 4；`modules/ddd.md` 场景 1 | 聚合根守卫测试 |
| WC-4 | Handler 返回 DTO；AppService 返回 CO；Adapter 实现契约接口纯透传（零逻辑） | AGENTS 九条 2；`modules/ddd.md` | R10b（DTO 标记）|
| WC-5 | Assembler（domain→DTO）与 Presenter（DTO→CO）强制分离，禁止一类通吃 | AGENTS 九条 6；`BasicAssembler`/`BasicPresenter` 框架基类 | C3 |
| WC-6 | 异常场景在聚合行为方法内显式 `if + throw BusinessException(messageKey)`，禁止吞错返回 null/布尔 | `modules/exception.md` EV-1 | — |
| WC-7 | 契约边界输入纪律：Command 聚合 ID 全程 `UUID`（禁降级 String，与批量卷 BW-2 同源裁决）；路径参数由 Adapter 注入、`@Schema(hidden = true)` 对请求体隐藏；文本字段 `@Size(max = 列宽)`、金额 `@Digits(integer = 8, fraction = 2)` 对齐 schema 精度——越界在绑定层拦成 400 + fieldErrors，400 先于 422 | 真实例：sample orders 表 customer_id VARCHAR(50)、tracking_number VARCHAR(100)、cancel_reason VARCHAR(500)，products 表 name VARCHAR(100)、price DECIMAL(10,2)（见 db-migration 0001 变更集），对应命令 PlaceOrderCommand / CancelOrderCommand / ShipOrderForm 已按此改型（原篇真实例锚点入法） | 评审项 |
| WC-8 | 契约输出纪律：CO 为外部安全视图，version/审计等内部字段不暴露；status 值域=契约枚举（住 `contract/{agg}/enums/`），与 domain 状态机奇偶镜像由守卫测试锁死；String → 枚举在呈现层 valueOf 收口、脏值当场 fail-fast，映射不外溢；消费方从契约 jar 直接拿合法值域，OpenAPI 自动枚举 | 真实例：sample `ContractEnumParityTest` 奇偶守卫测试（原篇锚点）；CO 内部字段不暴露 <!-- 待 ../../changes/ 补全 --> | 守卫测试 |
| WC-9 | 入口分工：REST 映射与 OpenAPI 文档注解（@Tag/@Operation/@*Mapping）全部住契约接口；实现类仅标记协议 + 透传（`@RestController` + `RestAdapter`），零逻辑 | ArchUnit R8a/R8b（RestAdapter 标记双向锁，原篇载明）+ 本卷 §2.3 形状 | ArchUnit |
| WC-10 | 仓储义务：持久化必走基类通道（saveDomain/updateDomain 系）——save/update 前自动 `validate()` 且经 `AuditFieldFiller` 显式填充审计字段；仓储层不声明事务（边界在 Handler，WC-2 互指）；跨聚合协调 = 同事务直调（跨聚合卷） | `MybatisPersistence` javadoc「内置行为契约」「事务边界（上收至应用层）」节 | C5 同区 |
| WC-11 | 持久化文本形态：PO 为纯 `@Data` POJO、零 ORM 注解；表名、乐观锁版本条件、逻辑删除过滤全在 XML SQL 文本逐条可见——无任何运行时拦截器参与 | `MybatisPersistence` javadoc（乐观锁无运行时拦截器——版本条件由每聚合手写 XML UPDATE 自身携带）+ 本卷 §2.10 形状 | 评审项 |
| WC-12 | 写失败语义三分（绝不静默失败）：UPDATE 0 行且版本条件未命中（实体仍在）→ `OptimisticLockConflictException`（409，可重试，正常并发流）；UPDATE 0 行且目标已并发消失 → 普通 `IllegalStateException`（409，业务竞态，重试无意义、不应被重试器吞掉）；INSERT/DELETE 影响 0 行 → `SilentWriteLossException`（类型在 `com.yoursweakfoe.common.exception.type`；写丢失级不可能状态，500 + ERROR 告警通道，勿重试、需人工介入） | `MybatisPersistence` javadoc「内置行为契约」三分通道条；异常→HTTP 映射唯一完整表在 `GlobalRestExceptionHandler` javadoc（原篇载明），docs 侧 canonical 见 [../../docs/reference/api/common-exception.md](../../../docs/reference/api/common-exception.md) | C5 同区 |

## §2 规范形状（统一用法唯一样本）

教例设定（虚构聚合 **Reservation 预约单**，D4 教学中立教义，sample 未实现）：生命周期 `PENDING → CONFIRMED → FULFILLED → COMPLETED`，可从 PENDING/CONFIRMED 状态取消；单内挂多条预约明细（服务项 × 数量 × 单价），以 JSON 物化存储。以 **"确认预约"** 为案例走查一个写操作从 REST 入口到数据库落盘的完整代码路径。业务规则及其条款归属：只有 PENDING 才能确认、确认后变 CONFIRMED（状态机守卫在聚合根，WC-3）；状态不合法抛 BusinessException，前端收到 422 + i18n 错误码（WC-6）；并发确认由乐观锁保护（WC-11/WC-12）。

### 2.1 调用链路

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

### 2.2 Contract — Command / CO

```java
// Command：写操作意图（聚合 ID 引用一律 UUID）
@Data @NoArgsConstructor @AllArgsConstructor
public class ConfirmReservationCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @NotNull
    private UUID reservationId;                              // WC-7：ID 类型 UUID，不降级 String
}

// CO：契约输出（外部安全视图，不暴露 version/审计字段；
// status 值域 = 契约枚举，紧接本围栏后有专段说明）
@Data @NoArgsConstructor @AllArgsConstructor
public class ReservationCO implements CO, Serializable {     // WC-8：外部安全视图
    @Serial private static final long serialVersionUID = 1L;

    private UUID id;
    private ReservationStatus status;                        // WC-8：值域 = 契约枚举
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private List<ReservationItemCO> items;
}
```

`ReservationStatus` 位于 `contract/reservation/enums/`：契约层枚举镜像 domain 状态机值域，二者奇偶由奇偶守卫测试锁死（真实例：sample 的 ContractEnumParityTest）。消费方从契约 jar 直接拿到合法值域，OpenAPI 自动枚举。

**命令字段校验（输入上界对齐 schema 列宽）**：命令的文本/数值字段一律携带输入上界——文本 `@Size(max = 列宽)`、金额 `@Digits(integer = 8, fraction = 2)`（对齐 DECIMAL(10,2)）——超长、超界输入在绑定层被 `@Valid` 拦成 **400 + fieldErrors**，不再穿透到 DB 变成 500 噪音；400（参数校验）先于 422（业务规则违反）发生。真实例锚点：sample 的 orders 表 customer_id VARCHAR(50)、tracking_number VARCHAR(100)、cancel_reason VARCHAR(500)，products 表 name VARCHAR(100)、price DECIMAL(10,2)（见 db-migration 0001 变更集），对应命令 PlaceOrderCommand / CancelOrderCommand / ShipOrderForm 已按此改型。

```java
// 示例（同一虚构家族）：取消命令——ID 引用统一 UUID；
// reservationId 由 Adapter 从路径参数注入、不在请求体重复携带；
// 文本字段 @Size 对齐原因列 VARCHAR(500)
@Data @NoArgsConstructor @AllArgsConstructor
public class CancelReservationCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /** 预约 ID（由 Adapter 从路径参数注入，客户端无需传递） */
    @Schema(hidden = true)                                   // WC-7：路径参数注入，请求体隐藏
    private UUID reservationId;

    /** 取消原因（上界对齐 cancel_reason 列 VARCHAR(500)） */
    @NotBlank @Size(max = 500)                               // WC-7：上界对齐列宽
    private String reason;
}
```

### 2.3 Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/reservation/adapter/rest/controller/ReservationController.java（契约接口，承载 HTTP 映射 + 文档注解）
@Tag(name = "预约服务", description = "预约生命周期管理")
@RequestMapping("/reservations")
public interface ReservationController {                      // WC-9：映射与文档注解全住契约接口

    @Operation(summary = "确认预约", description = "将 PENDING 预约标记为已确认")
    @PutMapping("/{reservationId}/confirm")
    ReservationCO confirmReservation(@PathVariable("reservationId") UUID reservationId);
}

// adapter/rest/controller/ReservationControllerImpl.java（实现，仅标记协议 + 透传；RestAdapter 标记见规则 R8a/R8b）
@RestController
public class ReservationControllerImpl implements ReservationController, RestAdapter {   // WC-9：R8a/R8b 双向锁

    private final ReservationAppService reservationAppService;

    public ReservationControllerImpl(ReservationAppService reservationAppService) {
        this.reservationAppService = reservationAppService;
    }

    @Override
    public ReservationCO confirmReservation(UUID reservationId) {   // WC-4：Adapter 零逻辑透传
        return reservationAppService.confirmReservation(new ConfirmReservationCommand(reservationId));   // WC-7：路径参数 → Command
    }
}
```

### 2.4 Application — AppService（聚合入口，委托 + 呈现）

```java
@Service
public class ReservationAppService implements ApplicationService {

    private final ReservationPresenter reservationPresenter;
    private final ConfirmReservationHandler confirmReservationHandler;

    public ReservationCO confirmReservation(ConfirmReservationCommand command) {   // WC-4：AppService 返回 CO
        return reservationPresenter.present(confirmReservationHandler.handle(command));   // WC-5：委托 Handler + Presenter
    }
}
```

### 2.5 Application — CommandHandler（四拍链：load → 行为 → save → toDTO）

```java
@Component
public class ConfirmReservationHandler implements CommandHandler<ConfirmReservationCommand, ReservationDTO> {

    private final ReservationRepository reservationRepository;
    private final ReservationAssembler reservationAssembler;

    // 构造器注入（省略）

    @Override
    @Transactional(rollbackFor = Exception.class)            // WC-2：事务边界在 Handler（仓储不标，WC-10）
    public ReservationDTO handle(ConfirmReservationCommand command) {   // WC-4：Handler 返回 DTO
        Reservation reservation = reservationRepository.findById(command.getReservationId())
                .orElseThrow(() -> new BusinessException("reservation:err.notFound"));   // WC-2：load（i18n 位点见 WC-6）
        reservation.confirm();                                        // WC-3：业务规则在聚合根内，Handler 零分支
        reservationRepository.update(reservation);                    // WC-10：基类通道（自动 validate + 审计填充）
        return reservationAssembler.toDTO(reservation);               // WC-2：toDTO 收尾
    }
}
```

### 2.6 Application — Assembler（Domain → DTO，逐字段显式映射）

```java
@Component
public class ReservationAssembler implements BasicAssembler<Reservation, ReservationDTO> {   // WC-5：与 Presenter 强制分离

    @Override
    public ReservationDTO toDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId().toString());
        dto.setStatus(reservation.getStatus().name());                // WC-8：DTO 恒 String，枚举收口在呈现层
        dto.setItems(reservation.getItems().stream()
                .map(item -> new ReservationDTO.ReservationItemDTO(
                        item.serviceId(), item.quantity(), item.unitPrice()))
                .toList());
        dto.setTotalAmount(reservation.getTotalAmount());
        dto.setCustomerId(reservation.getCustomerId());
        dto.setConfirmationCode(reservation.getConfirmationCode());
        dto.setCancelReason(reservation.getCancelReason());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setVersion(reservation.getVersion());                     // DTO 含 version（CO 不暴露，WC-8）
        return dto;
    }

    // BasicAssembler 是单向契约：仅声明 toDTO，List/Set 批量方法由接口 default 委托。
    // DTO 只是只读出口视图——聚合构造入口恒为 Factory / reconstitute() 两扇门，
    // 接口不声明 DTO → Domain 方法（最小契约见 new-aggregate.md ⑧）。
}
```

### 2.7 Application — Presenter（DTO → CO，过滤内部字段）

```java
@Component
public class ReservationPresenter implements BasicPresenter<ReservationDTO, ReservationCO> {   // WC-5

    @Override
    public ReservationCO present(ReservationDTO dto) {
        ReservationCO co = new ReservationCO();
        co.setId(dto.getId());
        // 内部 DTO 恒为 String（Assembler 走 domain.name()）；String → 契约枚举在呈现层收口，
        // 值域奇偶由守卫测试锁死、脏值当场 fail-fast，映射不外溢
        co.setStatus(ReservationStatus.valueOf(dto.getStatus()));      // WC-8：呈现层收口，脏值 fail-fast
        co.setItems(presentItems(dto.getItems()));
        co.setTotalAmount(dto.getTotalAmount());
        co.setCustomerId(dto.getCustomerId());
        co.setConfirmationCode(dto.getConfirmationCode());
        co.setCancelReason(dto.getCancelReason());
        // createdAt / updatedAt / version 不暴露
        return co;                                                     // WC-4：呈现产物 = 契约输出
    }
}
```

### 2.8 Domain — 聚合根 + 值对象

```java
public class Reservation extends AggregateRoot<UUID> {                 // WC-1：零框架运行时依赖（纯 Java + common-ddd）

    private UUID id;
    private ReservationStatus status;
    private List<ReservationItem> items;
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private Long version;
    // ...

    public void confirm() {
        requireStatus("reservation:err.status.pending", ReservationStatus.PENDING);   // WC-3/WC-6：if-throw 封在聚合根 + i18n 位点
        this.status = ReservationStatus.CONFIRMED;
    }

    @Override
    public void validate() {                                            // WC-10：save/update 前由基类自动调用
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

### 2.9 Domain — Repository 接口

```java
public interface ReservationRepository extends Repository<Reservation, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

### 2.10 Infrastructure — PO / Converter / Mapper + XML / RepositoryImpl

PO 是纯 `@Data` POJO——零 ORM 注解（WC-11）；表名、乐观锁版本条件、逻辑删除过滤全部在 XML 的 SQL 文本里（语句模板见 [../../docs/how-to/new-aggregate.md](../../../docs/how-to/new-aggregate.md) ⑲）：

```java
@Data
public class ReservationPO {                                     // WC-11：纯 POJO，零 ORM 注解
    private UUID id;                 // 业务铸造（uuid 原生列直传），INSERT 显式传参
    private String status;
    private String items;              // JSON 序列化
    private BigDecimal totalAmount;
    private String customerId;
    private String confirmationCode;
    private String cancelReason;
    private Long version;           // 条件由 updateById 语句文本携带（WC-11）
    private OffsetDateTime createdAt;   // AuditFieldFiller 填充（WC-10）
    private OffsetDateTime updatedAt;
}

@Mapper
public interface ReservationMapper extends DddMapper<ReservationPO> {
    // DddMapper 七条通用语句由 resources/mapper/reservation/ReservationMapper.xml 手写实现
}

@Component
public class ReservationConverter implements BasicConverter<Reservation, ReservationPO> {   // WC-1：装配在 infra，Domain↔PO 双向

    @Override
    public Reservation toDomain(ReservationPO po) {
        return Reservation.reconstitute(
                po.getId(), ReservationStatus.valueOf(po.getStatus()),
                deserializeItems(po.getItems()), po.getTotalAmount(),
                po.getCustomerId(), po.getConfirmationCode(), po.getCancelReason(),
                po.getCreatedAt(), po.getUpdatedAt(), po.getVersion());
    }

    @Override
    public ReservationPO toPO(Reservation domain) {
        ReservationPO po = new ReservationPO();
        po.setId(domain.getId());
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
        extends MybatisPersistence<ReservationMapper, ReservationPO, Reservation, UUID>   // WC-10：基类通道
        implements ReservationRepository {                       // WC-1：domain 定义接口、infra 实现（依赖倒置）

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
    @Override public void save(Reservation domain) { saveDomain(domain); }       // WC-10：自动 validate + 审计填充
    @Override public void update(Reservation domain) { updateDomain(domain); }   // WC-10/WC-12：0 行走语义三分
    @Override public boolean exists(UUID id) { return existsDomainById(id); }
    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

> 仓储只负责持久化与不变量校验（save/update 前自动 `validate()`，并经 `AuditFieldFiller` 显式填充审计字段）；事务边界在应用层 Handler（WC-10）；跨聚合协调 = 同事务直调（跨聚合规范见 [cross-aggregate.md](cross-aggregate.md) 卷）。

### 2.11 并发写：影响行数 0 的语义三分处置（WC-12）

并发确认的行为等价性由 XML 的 `updateById` 语句保证：`SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_deleted = false`——无任何运行时拦截器参与（WC-11）。影响行数 0 时基类 `MybatisPersistence` 经存在性探测按**语义三分通道**处置（绝不静默失败）：

- **版本条件未命中（实体仍存在）** → `OptimisticLockConflictException`：409，可重试，属正常并发流；
- **更新目标已并发消失** → 普通 `IllegalStateException`：409，业务竞态，重试无意义、不应被重试器吞掉；
- **INSERT / DELETE 影响 0 行** → `SilentWriteLossException`（类型在 `com.yoursweakfoe.common.exception.type`）：写丢失级不可能状态，500 + ERROR 告警通道，勿重试、需人工介入。

真实例锚点：异常 → HTTP 映射的唯一完整表在 GlobalRestExceptionHandler javadoc，docs 侧 canonical 见 [common-exception.md](../../../docs/reference/api/common-exception.md)。

### 2.12 教例文件清单（写路径走查逐文件对位）

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

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| WC-1~6 既有条款 | ✅ | sample 示例聚合现行遵循（ArchUnit 双端守护） |
| WC-2/WC-3 真实例同构 | ✅ | 真实例锚点：原篇形状与 sample-application 的写路径实现同构（对照 OrderAppService.java:38、PayOrderHandler.java 的 load → 行为 → save → toDTO、Order.java:107-110 的 requireStatus 守卫），仅作对照、不逐字镜像 |
| WC-7 输入上界改型 | ✅ | 真实例：db-migration 0001 变更集列宽对账 + PlaceOrderCommand / CancelOrderCommand / ShipOrderForm 已按此改型（原篇锚点入法） |
| WC-8 契约枚举奇偶 | ✅ | 真实例：sample `ContractEnumParityTest` 守卫在册；CO 内部字段不暴露 <!-- 待 ../../changes/ 补全 --> |
| WC-9~12 归卷新增条款（入口分工 / 仓储义务 / 文本形态 / 语义三分，源=原篇 write-path.md 入法） | ✅ | 本卷 §2 形状在册；框架侧 canon 在 `MybatisPersistence` / `GlobalRestExceptionHandler` javadoc |
| 教例家族 Reservation 全链走查模板 | ⛔ 虚构教例，sample 未落地 | §2 即落地模板 + §2.12 文件清单；设计判断见 docs 设计卡 |
