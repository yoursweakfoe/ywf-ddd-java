# 用法规范法卷：聚合构建宪（框架法 · 严格件）

> **身份**：本卷是「一个聚合 = 哪些文件、什么形状」的建筑宪章——**22 个文件**完整清单（20 最小闭环 + 读端口配对 ⑳㉑ 两文件，契约枚举 ㉒ 计入契约段）；修卷走 `../../changes/`。docs 同题篇（`../../../docs/how-to/new-aggregate.md`）为设计卡（选型与边界叙事，零形状代码），逐件教学走查已整体归卷于本卷 §4，冲突以本卷为准。
> **机器对账**：C1（下列 `{agg}` 位以真实聚合代入核验）/ C3 / C4 扫本卷；教例家族 = Payment（虚构教例，sample 未实现）。开册法案：`2026-09-howto-codification`；统一用法归卷：`2026-09-usage-consolidation`。

## §1 文件清单（建筑宪位置表，㉠-㉒ 为全局槽位号）

```
sample-service/
├── sample-service-contract/src/main/java/.../contract/
│   └── payment/
│       ├── adapter/rest/controller/PaymentController.java ← ① Controller 契约接口
│       ├── dto/co/PaymentCO.java                    ← ② 契约输出
│       ├── dto/command/CreatePaymentCommand.java    ← ③ Command
│       ├── dto/query/GetPaymentQuery.java           ← ④ Query
│       └── enums/PaymentStatus.java                 ← ㉒ 契约枚举（CO 值域镜像 domain 状态机）
│
└── sample-service-server/src/main/java/.../
    ├── adapter/rest/controller/
    │   └── PaymentControllerImpl.java           ← ⑤ Controller 实现（REST 入口）
    ├── application/payment/
    │   ├── service/PaymentAppService.java       ← ⑥ AppService
    │   ├── dto/PaymentDTO.java                  ← ⑦ 内部 DTO
    │   ├── assembler/PaymentAssembler.java      ← ⑧ Assembler
    │   ├── presenter/PaymentPresenter.java      ← ⑨ Presenter
    │   ├── handler/
    │   │   ├── command/CreatePaymentHandler.java ← ⑩ CommandHandler
    │   │   └── query/GetPaymentHandler.java      ← ⑪ QueryHandler
    │   └── repository/PaymentQueryRepository.java ← ⑳ 读端口（extends QueryRepository）
    ├── domain/payment/
    │   ├── model/Payment.java                   ← ⑫ 聚合根
    │   ├── model/PaymentStatus.java             ← ⑬ 枚举
    │   └── repository/PaymentRepository.java    ← ⑭ Repository 接口（写侧）
    └── infrastructure/persistence/master/payment/
        ├── mybatis/po/PaymentPO.java              ← ⑮ PO（纯 POJO，零 ORM 注解）
        ├── converter/PaymentConverter.java        ← ⑯ Converter（框架 BasicConverter 桥）
        ├── mybatis/mapper/PaymentMapper.java      ← ⑰ Mapper（extends DddMapper）
        ├── repository/PaymentRepositoryImpl.java  ← ⑱ RepositoryImpl（继承 MybatisPersistence）
        └── repository/PaymentQueryRepositoryImpl.java ← ㉑ 读实现（PO → DTO 直投，与 ⑱ 同包）

sample-service-server/src/main/resources/
└── mapper/payment/PaymentMapper.xml               ← ⑲ 手写 SQL（DddMapper 七条语句契约）
```

## §2 形状条款

| # | SHALL | 取证 |
|---|---|---|
| BP-1 | 每个新聚合按 §1 槽位建满 22 文件（读端口 ⑳㉑ 可随首个读用例补齐；除此之外缺一即违反建筑宪） | 本卷 §1；skill `new-aggregate` 第 0 步对账 |
| BP-2 | 契约枚举 ㉒ 与 domain 枚举 ⑬ 值域镜像，契约段以枚举追节登记奇偶 | 奇偶守恒锁 = `ContractEnumParityTest` |
| BP-3 | 创建顺序：contract（①-④+㉒）→ domain（⑫-⑭）→ infrastructure（⑮-⑲+㉑）→ application（⑥-⑪+⑳）→ adapter（⑤） | 原篇顺序节入法（接口先行、依赖倒序） |
| BP-4 | HTTP 映射与文档注解（`@Tag`/`@Operation`/`@RequestMapping`/`@Valid`）声明在契约接口上；ControllerImpl 仅加 `@RestController` + RestAdapter 标记（R8a/R8b）纯透传零逻辑；东西向复用同一契约接口（HTTP 直连） | 本卷 §4.①, §4.⑤ |
| BP-5 | CO / Command / Query 三件套一律 `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`，implements Serializable 并各携标记接口 | 本卷 §4.②-④ |
| BP-6 | 契约层聚合 ID 引用一律 UUID（B12 教义）；String 形态只存在于 JSON wire 序列化出口（CO 字段、Presenter 映 wire 位 toString()），PO / DTO / 领域层禁止以 String 承载 id 或手工 fromString/toString 跨层桥接（PO 层 String 通道随 BP-S1 关闭） | 本卷 §4.③, §4.⑦, §4.⑮；sample OrderPresenter wire 位实证 |
| BP-7 | 状态 wire 三段式：CO 字段 = 契约枚举 ㉒、内部 DTO 字段 = String（Assembler 走 `domain.name()` 出口）、`String → 契约枚举` 在 Presenter 以 `valueOf` 收口且未知字面量当场 fail-fast（奇偶锁见 BP-2，禁止合并两个枚举） | 本卷 §4.㉒, §4.⑨ |
| BP-8 | 聚合构造入口恒两扇门：业务构造器（新建，内部置初始态）+ 静态 `reconstitute()`（重建）；不开放其他构造路径 | 本卷 §4.⑫ |
| BP-9 | DTO 是只读出口视图（可含审计/版本内部字段，CO 不暴露）；Assembler 单向契约（仅 Domain→DTO，批量 default 委托），DTO→CO 收口在 Presenter 并过滤内部字段，禁止 DTO→Domain 反向 | 本卷 §4.⑦, §4.⑧, §4.⑨ |
| BP-10 | 写侧四拍链（new/load → 聚合行为 → save → toDTO）在 CommandHandler 完成并标 `@Transactional(rollbackFor = Exception.class)`；RepositoryImpl 不标事务（事务边界在 Handler）；跨聚合协调 = 同事务直调 | 本卷 §4.⑩, §4.⑱ |
| BP-11 | 读侧绕过聚合：QueryHandler 只经读端口 ⑳（禁触 ⑭ 写端口、不经 ⑧ Assembler、不 reconstitute，R13），读实现 PO→DTO 直接投影、不填写侧关注字段（version）；读写需独立演进时拆 ViewDTO/ViewPresenter（canonical = read-path 篇） | 本卷 §4.⑪, §4.⑳, §4.㉑ |
| BP-12 | 业务规则收口在聚合根 `validate()`；异常统一 `BusinessException` + i18n 位点 `{aggregate}:err.{scene}`（含读侧 miss 抛位） | 本卷 §4.⑪, §4.⑫ |
| BP-X1 | XML 七条语句契约：表名含 schema 前缀（单数领域词，BP-S2）；select/update/delete（逻辑删除聚合）显式 `AND is_deleted = false`；`insert` 不枚举 `is_deleted`；`existsById` 恒返一行 boolean；`updateById` 携 `SET version = version + 1 ... AND version = #{version}`；文件位 `resources/mapper/{agg}/` 且 namespace = Mapper 全限定名 | `modules/ddd.md` 场景 2；OL-4 互指 |
| BP-X2 | PO 纯 `@Data` 零 ORM 注解；Mapper `extends DddMapper`；RepositoryImpl 继承 `MybatisPersistence`；Converter.toDomain 一律经 `reconstitute()` 重建 | AGENTS 九条 9；`modules/ddd.md`；TC-2 互指 |
| BP-X3 | 读端口 ⑳ 必须 `extends QueryRepository` 且位于 `application/{agg}/repository/`（R13） | read-chain RC-1 互指 |
| BP-S1 | 库表形状唯一权威 = `db-migration` 变更集（PG 原生形状法）：`id UUID DEFAULT uuidv7()`（PG18 内建，工厂铸造传值覆盖）、`created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`、`created_by/updated_by UUID`、`is_deleted BOOLEAN NOT NULL DEFAULT FALSE`、`version BIGINT NOT NULL DEFAULT 0`；DB 默认只兜手工插入，正常写路径审计归应用层（AuditFieldFiller + Clock） | `db-migration/.../0001-init-schema.sql` 双库 SHA256 同形实证；sample PO 换形后 119 测试对账 |
| BP-S2 | schema 命名法：聚合边界 = schema 边界，领域词单数（`product.product`）；SQL 保留字冲突 SHALL 升级行业 UB 术语（order→sales_order），禁止引号/前缀逃逸；schema 名与 Java 聚合包名单数惯例逐字同构，微服务拆分整 schema 平移 | 用户 2026-09 裁决（ADR-0033）；本卷 §4 教学块与 sample 包树实证 |
| BP-S3 | 外部工具账表住各自独立 schema（Liquibase 账表→`liquibase`，执行器引导件幂等自建；Seata TC 优先分库、`undo_log` 随业务连接落 public），与业务 schema 互不混列 | `db-migration` `LiquibaseSchemaBootstrapConfig`；双库账表按 schema 清点实证 |

## §3 验收单（交付闸，逐条可机械化）

- [ ] `mvn compile` 通过（无循环依赖）
- [ ] ArchUnit 测试通过（common-test 规则集）
- [ ] domain 层零框架注解（纯 Java + common-ddd）
- [ ] 应用层 DTO 实现 `ApplicationDTO` 标记（R10b）；CO 实现 `CO` 标记
- [ ] BP-X1~X3 逐条勾验
- [ ] BP-4~BP-12 逐条勾验（逐件形状对照 = 本卷 §4 走查）
- [ ] 新行为断言 = delta Scenario（TC-3）

## §4 全套规范形状（①-㉒ 逐件教学走查）

> 本节走查整体自 docs 同题篇归卷而来（统一用法归卷 `2026-09-usage-consolidation`）。全套为**虚构教例，sample 未实现**（Payment 家族——教学世界观：Payment 家族 = 聚合构建场景，Reservation 家族 = 写/读路径走查）。各件代码即槽位文件的规范形状唯一样本，条款注记见 §2 各 BP 行取证指针。

### 4.① Contract — Controller 契约接口

```java
package ...contract.payment.adapter.rest.controller;

@Tag(name = "支付服务", description = "支付创建与查询")
@RequestMapping("/payments")
public interface PaymentController {

    @Operation(summary = "创建支付", description = "创建支付记录")
    @PostMapping("")
    PaymentCO createPayment(@Valid @RequestBody CreatePaymentCommand command);

    @Operation(summary = "查询支付详情", description = "根据 ID 获取支付信息")
    @GetMapping("/{paymentId}")
    PaymentCO getPayment(@PathVariable("paymentId") UUID paymentId);
}
```

> HTTP 映射 + 文档注解在契约接口声明；服务端 ControllerImpl 仅标记 `@RestController` 并透传（见 §4.⑤）；
> 东西向调用复用同一契约接口（HTTP 直连），无需额外定义。

### 4.② Contract — CO（契约输出；含 ②③④ 三件套共用形状对照）

三者模式相同：`@Data` + `implements Serializable`。差异仅在标记接口：

| 类 | 标记接口 | 用途 |
|----|---------|------|
| `PaymentCO` | `CO` | 契约输出，仅含消费方需要的字段 |
| `CreatePaymentCommand` | `Command` | 写操作入参 |
| `GetPaymentQuery` | `Query` | 读操作入参 |

```java
// CO 示例
@Data @NoArgsConstructor @AllArgsConstructor
public class PaymentCO implements CO, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private UUID id;
    private PaymentStatus status;   // 契约枚举（㉒，见下节），非 String——值域镜像 domain PaymentStatus
    private BigDecimal amount;
}
```

### 4.③ Contract — Command（写操作入参）

```java
// Command 示例
@Data @NoArgsConstructor @AllArgsConstructor
public class CreatePaymentCommand implements Command, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private UUID orderId;          // 聚合 ID 引用一律 UUID（B12 教义，非 String）
    private BigDecimal amount;
}
```

### 4.④ Contract — Query（读操作入参）

原走查未给 Query 独立模板——形状与 ②③ 同模式（`@Data` + Serializable，差异 = 标记接口 `Query`，对照表见 §4.②）；构造形态见 §4.⑤（`new GetPaymentQuery(paymentId)`）与 §4.⑪（`query.getPaymentId()`）。

### 4.㉒ Contract — 契约枚举（CO status 值域）

编号顺延追加为 ㉒，物理归属 contract 段（本小节紧随 ②③④ 之后）：

```java
// contract/payment/enums/PaymentStatus.java（契约枚举：wire 值域解码器，与 ⑬ domain 状态机枚举同名同值域、两个编译单元并存）
public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
```

`PaymentCO.status` 的类型是该契约枚举而非 `String`：消费方从 contract jar 直接拿到合法值域，OpenAPI 自动枚举合法值。它与 ⑬ `domain/payment/model/PaymentStatus` 是**同一值域的两个化身**——分层规则双向封死引用方向（domain 不依赖 contract、contract 不依赖 server 内部），重复不可消除；正确的收口不是合并，而是**奇偶锁**：两个枚举常量名集合由奇偶守卫测试锁死，任一侧增删/改名常量在构建期即红（真实例：sample 的 `contract/ContractEnumParityTest.java`）。内部 DTO 仍为 `String`（Assembler 走 `domain.name()` 出口），`String → 契约枚举` 在 Presenter 层用 `valueOf` 收口（见 §4.⑨ 示例），未知字面量当场 fail-fast = 消费方需升级 contract jar。当前形状的完整走查对照 → [write-path.md](../../../docs/how-to/write-path.md)（Reservation 家族已按此形状示范）。

### 4.⑤ Adapter — Controller 实现

```java
// adapter/rest/controller/PaymentControllerImpl.java（实现，仅标记协议 + 透传）
// 注意：实现类需追加实现 ScheduledAdapter 同族的 RestAdapter 标记
// （com.yoursweakfoe.common.ddd.adapter.rest.controller.RestAdapter，规则 R8a/R8b）
@RestController
public class PaymentControllerImpl implements PaymentController, RestAdapter {

    private final PaymentAppService paymentAppService;

    public PaymentControllerImpl(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @Override
    public PaymentCO createPayment(CreatePaymentCommand command) {
        return paymentAppService.createPayment(command);
    }

    @Override
    public PaymentCO getPayment(UUID paymentId) {
        return paymentAppService.getPayment(new GetPaymentQuery(paymentId));
    }
}
```

### 4.⑥ Application — AppService

```java
@Service
public class PaymentAppService implements ApplicationService {

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

### 4.⑦ Application — DTO（内部视图）

内部视图，可含审计字段（CO 不暴露）：

```java
@Data
public class PaymentDTO implements ApplicationDTO, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID orderId;
    private String status;
    private BigDecimal amount;
    private OffsetDateTime createdAt;  // 内部字段
    private Long version;         // 内部字段
}
```

### 4.⑧ Application — Assembler（Domain → DTO）

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
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setVersion(payment.getVersion());
        return dto;
    }

    // BasicAssembler 是单向契约：仅声明 toDTO，List/Set 批量方法由接口 default 委托。
    // DTO 只是只读出口视图（聚合构造入口恒为业务构造器/reconstitute() 两扇门），
    // 接口不声明 DTO → Domain 方法——回潮守卫见 BasicAssembler javadoc（单一事实源）。
}
```

### 4.⑨ Application — Presenter（DTO → CO，过滤内部字段）

```java
@Component
public class PaymentPresenter implements BasicPresenter<PaymentDTO, PaymentCO> {

    @Override
    public PaymentCO present(PaymentDTO dto) {
        PaymentCO co = new PaymentCO();
        co.setId(dto.getId());
        // 内部 DTO 恒为 String（Assembler 走 domain.name()）；String → 契约枚举（㉒）在呈现层收口，
        // 值域奇偶由守卫测试锁死、脏值当场 fail-fast，映射不外溢
        co.setStatus(PaymentStatus.valueOf(dto.getStatus()));
        co.setAmount(dto.getAmount());
        // createdAt / version 不暴露
        return co;
    }
}
```

### 4.⑩ Application — CommandHandler（写侧）

```java
// application/payment/handler/command/CreatePaymentHandler.java
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

### 4.⑪ Application — QueryHandler（读侧）

```java
// application/payment/handler/query/GetPaymentHandler.java
@Component
public class GetPaymentHandler implements QueryHandler<GetPaymentQuery, PaymentDTO> {

    private final PaymentQueryRepository paymentQueryRepository;   // application 层读端口（见 ⑳）

    public GetPaymentHandler(PaymentQueryRepository paymentQueryRepository) {
        this.paymentQueryRepository = paymentQueryRepository;
    }

    @Override
    public PaymentDTO handle(GetPaymentQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → DTO 投影，不 reconstitute 聚合根（R13：QueryHandler 禁触写侧仓储）；
        // 非法 UUID 已由 Web 层类型转换拦截（400），此处必为合法值
        return paymentQueryRepository.findById(query.getPaymentId())
                .orElseThrow(() -> new BusinessException("payment:err.notFound"));
    }
}
```

> 读侧不经 ⑭ `PaymentRepository`（domain 写侧契约）、不经 ⑧ Assembler——由读端口直接投影 DTO，
> 教义与分页/多视图完整形态见 [read-chain 法卷](read-chain.md)（读侧 canonical）。
> 本最小模板直接复用 ⑦ `PaymentDTO` 作读投影（Presenter 过滤 version 等内部字段）；
> 需要读写独立演进时按 read-path.md 拆出 `PaymentViewDTO` + `PaymentViewPresenter`。

### 4.⑫ Domain — 聚合根

```java
public class Payment extends AggregateRoot<UUID> {

    private UUID id;
    private UUID orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private OffsetDateTime createdAt;
    private Long version;

    /** 业务构造器 */
    public Payment(UUID id, UUID orderId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    /** 重建构造器（Converter 使用） */
    public static Payment reconstitute(UUID id, UUID orderId, PaymentStatus status,
                                       BigDecimal amount, OffsetDateTime createdAt, Long version) {
        Payment p = new Payment(id, orderId, amount);
        p.status = status;
        p.createdAt = createdAt;
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
```

> 真实例升级位：sample 聚合新建已收口至 Factory + 包私有构造（「创建即合法」），见真实例 `OrderFactory.java`——本教例保留最小闭环的公开业务构造器形态，Factory 属按需增强件。

### 4.⑬ Domain — 枚举

```java
public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
```

### 4.⑭ Domain — Repository 接口（写侧）

```java
public interface PaymentRepository extends Repository<Payment, UUID> {
    // 继承：findById / save / update / exists / deleteById
}
```

### 4.⑮ Infrastructure — PO

PO 是纯 `@Data` POJO——**零 ORM 注解**，表名、主键策略、版本条件、逻辑删除过滤全部由 XML 的 SQL 文本承担：

```java
@Data
public class PaymentPO {
    private UUID id;                 // 业务铸造（uuid 原生列直传），INSERT 显式传参
    private UUID orderId;              // FK 列取原生 uuid 类型，common-pg UUIDTypeHandler 自动直映射（零转换代码）
    private String status;
    private BigDecimal amount;
    private Long version;           // 乐观锁：条件由 UPDATE 语句文本携带
    private OffsetDateTime createdAt;   // AuditFieldFiller 填充
    private OffsetDateTime updatedAt;
    private UUID createdBy;          // 可选：容器存在 CurrentUserProvider 才填
    private UUID updatedBy;
    private Boolean isDeleted;          // 逻辑删除标记（INSERT 不枚举，靠 DB 默认 FALSE）
}
```

### 4.⑯ Infrastructure — Converter

```java
@Component
public class PaymentConverter implements BasicConverter<Payment, PaymentPO> {

    @Override
    public Payment toDomain(PaymentPO po) {
        return Payment.reconstitute(
                po.getId(), po.getOrderId(),
                PaymentStatus.valueOf(po.getStatus()),
                po.getAmount(), po.getCreatedAt(), po.getVersion());
    }

    @Override
    public PaymentPO toPO(Payment domain) {
        PaymentPO po = new PaymentPO();
        po.setId(domain.getId());
        po.setOrderId(domain.getOrderId());
        po.setStatus(domain.getStatus().name());
        po.setAmount(domain.getAmount());
        po.setVersion(domain.getVersion());
        return po;
    }
}
```

### 4.⑰ Infrastructure — Mapper

```java
@Mapper
public interface PaymentMapper extends DddMapper<PaymentPO> {
    // 通用七条语句由同篇 XML 实现（namespace = 本接口全限定名）；
    // 业务专有查询（如按唯一键单查）在此追加具名方法
}
```

### 4.⑱ Infrastructure — RepositoryImpl

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
    @Override public Optional<Payment> findById(UUID id) { return findDomainById(id); }
    @Override public void save(Payment domain) { saveDomain(domain); }
    @Override public void update(Payment domain) { updateDomain(domain); }
    @Override public boolean exists(UUID id) { return existsDomainById(id); }
    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

> 构造器注入四件框架依赖（`Clock` / `AuditProperties` / `ObjectProvider<CurrentUserProvider>` 加业务 Mapper 与 Converter）；`save/update` 自动 `validate()` 并经 `AuditFieldFiller` 显式填充审计字段；事务边界在 Handler（本类不标 `@Transactional`）；跨聚合协调 = 同事务直调。

### 4.⑲ Infrastructure — 手写 XML（DddMapper 七条语句）

手写 XML（`src/main/resources/mapper/payment/PaymentMapper.xml`）——七条语句逐条可见；各语句的列级语义（INSERT 不枚举 `is_deleted`、UPDATE 携带版本条件、删除消费基类 `now` / `updatedBy` 审计参数等）以 [knowledge/docs/reference/api/common-ddd.md](../../../docs/reference/api/common-ddd.md) §2 的 DddMapper 七语句契约表为准，本节只给完整模板：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="...infrastructure.persistence.master.payment.mybatis.mapper.PaymentMapper">

    <sql id="columns">
        id, order_id, status, amount, version, created_at, updated_at, created_by, updated_by, is_deleted
    </sql>

    <!-- 枚举全部业务列；version 写字面量 0（新建聚合初始版本）；is_deleted 不枚举 → DB 默认 FALSE -->
    <insert id="insert">
        INSERT INTO payment.payment (id, order_id, status, amount, version,
                                       created_at, updated_at, created_by, updated_by)
        VALUES (#{id}, #{orderId}, #{status}, #{amount}, 0,
                #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy})
    </insert>

    <!-- 全量 UPDATE + 乐观锁版本条件 + 逻辑删除过滤；updated_at 由 AuditFieldFiller 刷新 -->
    <update id="updateById">
        UPDATE payment.payment
        SET order_id   = #{orderId},
            status     = #{status},
            amount     = #{amount},
            version    = version + 1,
            updated_at  = #{updatedAt}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE id = #{id}
          AND version = #{version}
          AND is_deleted = false
    </update>

    <select id="selectById" resultType="...infrastructure.persistence.master.payment.mybatis.po.PaymentPO">
        SELECT <include refid="columns"/>
        FROM payment.payment
        WHERE id = #{id}
          AND is_deleted = false
    </select>

    <select id="selectByIds" resultType="...infrastructure.persistence.master.payment.mybatis.po.PaymentPO">
        SELECT <include refid="columns"/>
        FROM payment.payment
        WHERE is_deleted = false
          AND id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </select>

    <!-- 逻辑删除 = UPDATE 置位 + 审计刷新（now / updatedBy 由基类经 Clock / CurrentUserProvider 传入） -->
    <update id="deleteById">
        UPDATE payment.payment
        SET is_deleted = true,
            updated_at = #{now}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE id = #{id}
          AND is_deleted = false
    </update>

    <update id="deleteByIds">
        UPDATE payment.payment
        SET is_deleted = true,
            updated_at = #{now}
        <if test="updatedBy != null">
            , updated_by = #{updatedBy}
        </if>
        WHERE is_deleted = false
          AND id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
    </update>

    <!-- 轻量存在性探测：恒返回一行 boolean（UPDATE 行数 0 时的冲突分类依赖它） -->
    <select id="existsById" resultType="boolean">
        SELECT EXISTS (
            SELECT 1 FROM payment.payment WHERE id = #{id} AND is_deleted = false
        )
    </select>
</mapper>
```

> 不需要逻辑删除的聚合：`deleteById` 写物理 `DELETE FROM ... WHERE id = #{id}`、各 select 省略 `is_deleted` 条件即可——语义选择落在 XML 文本，聚合之间互不影响。

### 4.⑳ 读端口 — PaymentQueryRepository（application 层，与 ⑪ 配对）

```java
// application/payment/repository/PaymentQueryRepository.java（application 层读端口）
public interface PaymentQueryRepository extends QueryRepository {   // 空标记（common-ddd）：读端口身份

    /** 按 ID 投影支付读 DTO（不存在返回 empty）。 */
    Optional<PaymentDTO> findById(UUID id);
}
```

### 4.㉑ 读端口实现 — PaymentQueryRepositoryImpl（与 ⑱ 同包）

```java
// infrastructure/persistence/master/payment/repository/PaymentQueryRepositoryImpl.java
@Component
public class PaymentQueryRepositoryImpl implements PaymentQueryRepository {

    private final PaymentMapper paymentMapper;

    public PaymentQueryRepositoryImpl(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    @Override
    public Optional<PaymentDTO> findById(UUID id) {
        PaymentPO po = paymentMapper.selectById(id);
        return po == null ? Optional.empty() : Optional.of(toDTO(po));
    }

    /** PO → 读 DTO 直接投影（不经过 domain、不 reconstitute 聚合根、不经 Converter）。 */
    private PaymentDTO toDTO(PaymentPO po) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(po.getId());
        dto.setOrderId(po.getOrderId());
        dto.setStatus(po.getStatus());
        dto.setAmount(po.getAmount());
        dto.setCreatedAt(po.getCreatedAt());
        return dto;   // version 不填充：读投影不承载写侧关注点
    }
}
```

要点：
- 读端口接口位于 `application/payment/repository/`、`extends QueryRepository`——这是 R13（QueryHandler 禁触 domain 仓储）下的唯一合法读路径，R1b 白名单同时放行 infra 对该端口的实现依赖
- 完整读侧形态（分页双语句 + `safe*()` 钳制 + ViewDTO / ViewPresenter 多视图）以 [read-chain 法卷](read-chain.md) 为 canonical，本节只登记新聚合清单所需的最小文件集

## §5 服务骨架通式（建筑宪的 service 级扩展，自 structure.md 注销迁来）

```text
contract/{agg}/        adapter/rest/controller + dto/{command,query,co} + enums
server adapter/        rest/controller + task/scheduler   —— 不按聚合分包
server application/    service + handler/{command,query} + assembler + presenter + dto + repository（读端口）
server domain/         model + repository（写端口）+ portal + service + policy【按需】
server infrastructure/ persistence/{ds}/{agg}/(mybatis/{po,mapper} + converter + repository：写读 Impl 同包) + gateway/{capability} + config
resources/             mapper/**/*.xml（手写 SQL 语句面）
```

真实包树**不设二手地图**——包路径/文件数的 canonical 是源码本身（归属法卷 §2 第一行），要图直接 glob 源码树；本通式只裁「一个服务该有哪些目录、各目录住什么」，逐槽形状由 §1 清单 + §2/§4 条款约束。

## 生效登记

全部 ✅（sample 两聚合按本宪建成，槽位真实性由 C1 逐槽代入对账）。
