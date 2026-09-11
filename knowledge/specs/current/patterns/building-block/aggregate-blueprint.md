# 用法规范法卷：聚合构建宪（框架法 · 严格件）

> **身份**：本卷规定一个聚合由哪些文件构成、每个文件什么形状。完整清单共 **23 个文件**：21 个最小闭环，加读端口配对 ㉑㉒ 两文件；契约枚举 ㉓ 计入契约段。修改本卷必须走 `../../../changes/` 立案。docs 同题篇 `../../../../docs/how-to/new-aggregate.md` 是设计卡，只讲选型与边界，零形状代码；逐件教学走查全部收在本卷 §4。内容冲突时以本卷为准。
> **机器对账**：C1 把下列 `{agg}` 位以真实聚合代入核验；C3/C4 扫本卷。教例家族 = Payment（虚构教例，sample 未实现）。

## §1 文件清单（①-㉓ 为全局槽位号）

```
sample-service/
├── sample-service-contract/src/main/java/.../contract/
│   └── payment/
│       ├── adapter/rest/controller/PaymentController.java ← ① Controller 契约接口
│       ├── dto/co/PaymentCO.java                    ← ② 契约输出
│       ├── dto/command/CreatePaymentCommand.java    ← ③ Command
│       ├── dto/query/GetPaymentQuery.java           ← ④ Query
│       └── enums/PaymentStatus.java                 ← ㉓ 契约枚举（CO 值域镜像 domain 状态机）
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
    │   └── repository/PaymentQueryRepository.java ← ㉑ 读端口（extends QueryRepository）
    ├── domain/payment/
    │   ├── id/PaymentId.java                    ← ⑫ 聚合身份终类型（record implements Identifier）
    │   ├── model/Payment.java                   ← ⑬ 聚合根
    │   ├── model/PaymentStatus.java             ← ⑭ 枚举
    │   └── repository/PaymentRepository.java    ← ⑮ Repository 接口（写侧）
    └── infrastructure/persistence/master/payment/
        ├── mybatis/po/PaymentPO.java              ← ⑯ PO（纯 POJO，零 ORM 注解）
        ├── converter/PaymentConverter.java        ← ⑰ Converter（框架 BasicConverter 桥）
        ├── mybatis/mapper/PaymentMapper.java      ← ⑱ Mapper（extends DddMapper）
        ├── repository/PaymentRepositoryImpl.java  ← ⑲ RepositoryImpl（继承 MybatisPersistence）
        └── repository/PaymentQueryRepositoryImpl.java ← ㉒ 读实现（PO → DTO 直投，与 ⑲ 同包）

sample-service-server/src/main/resources/
└── mapper/payment/PaymentMapper.xml               ← ⑳ 手写 SQL（DddMapper 七条语句契约）
```

## §2 形状条款

| # | SHALL | 取证 |
|---|---|---|
| BP-1 | 每个新聚合按 §1 槽位建满 23 个文件。读端口 ㉑㉒ 允许随首个读用例补齐；除此之外少建任何一个文件即违反本卷。 | 本卷 §1；skill `new-aggregate` 第 0 步对账 |
| BP-2 | 契约枚举 ㉓ 与 domain 枚举 ⑭ 的值域保持镜像；契约段以枚举追节登记两者的奇偶。 | 奇偶守恒锁 = `ContractEnumParityTest` |
| BP-3 | 新建聚合按此顺序：contract（①-④+㉓）→ domain（⑫-⑮）→ infrastructure（⑯-⑳+㉒）→ application（⑥-⑪+㉑）→ adapter（⑤）。 | 依据：接口先行、依赖倒序 |
| BP-4 | HTTP 映射与文档注解 `@Tag`/`@Operation`/`@RequestMapping`/`@Valid` 一律声明在契约接口上。ControllerImpl 只加 `@RestController`、实现 RestAdapter 标记（R8a/R8b），纯透传零逻辑。东西向调用复用同一契约接口，走 HTTP 直连。 | 本卷 §4.①, §4.⑤ |
| BP-5 | CO、Command、Query 三件套一律 `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`，implements Serializable，并各自携带一个标记接口。 | 本卷 §4.②-④ |
| BP-6 | 契约层聚合 ID 引用一律底层原生值（现档 UUID，判据 B12 教义不变）；写侧 domain/DTO 槽位由 BP-13/14 接管；PO 维持原生值；String 形态仍只许出现在 JSON wire 出口（CO 字段与 Presenter toString 位）。 | 本卷 §4.③, §4.⑦, §4.⑯；sample OrderPresenter wire 位实证 |
| BP-7 | 状态字段 wire 三段式：CO 字段 = 契约枚举 ㉓；内部 DTO 字段 = String，Assembler 走 `domain.name()` 出口；`String → 契约枚举` 的转换在 Presenter 以 `valueOf` 收口，未知字面量当场 fail-fast。两个枚举禁止合并，奇偶锁见 BP-2。 | 本卷 §4.㉓, §4.⑨ |
| BP-8 | 聚合的构造入口恒为两扇门：业务构造器负责新建并在内部置初始态；静态 `reconstitute()` 负责重建。除此之外不开任何构造路径。 | 本卷 §4.⑬ |
| BP-9 | DTO 是只读出口视图，可含审计、version 等内部字段，这些字段不进 CO。Assembler 是单向契约：只做 Domain→DTO，批量方法由接口 default 委托。DTO→CO 的转换收口在 Presenter，并在其中过滤内部字段。禁止 DTO→Domain 反向。 | 本卷 §4.⑦, §4.⑧, §4.⑨ |
| BP-10 | 写侧四拍链（new/load → 聚合行为 → save → toDTO）在 CommandHandler 内完成，方法标 `@Transactional(rollbackFor = Exception.class)`。RepositoryImpl 不标事务，事务边界在 Handler。跨聚合协调 = 同一事务内直调。 | 本卷 §4.⑩, §4.⑲ |
| BP-11 | 读侧绕过聚合。QueryHandler 只能经读端口 ㉑：禁触 ⑮ 写端口、不经 ⑧ Assembler、不 reconstitute（R13）。读实现把 PO 直接投影为 DTO，不填 version 等写侧关注字段。读写需要独立演进时拆出 ViewDTO/ViewPresenter，canonical 见 read-path 篇。 | 本卷 §4.⑪, §4.㉑, §4.㉒ |
| BP-12 | 业务规则收口在聚合根的 `validate()`。异常统一抛 `BusinessException`，message 用 i18n 位点 `{aggregate}:err.{scene}`；读侧查不到的抛位同样适用。 | 本卷 §4.⑪, §4.⑬ |
| BP-13 | 聚合根 SHALL 以专属终类型 `{Agg}Id`（`public record`，implements `Identifier<V>` 与 `Serializable`，落位 `domain/{agg}/id/`，命名 `{Agg}Id`，紧凑构造器仅 null 检查、不校验底值，null 闸异常走统一 `BusinessException` + `{agg}:err.idRequired` 位点（§4.⑫ 同形），形状细则住案卷 2026-09-typed-identifier plan P-2／2026-09-id-guard-site plan P-2）为其 `AggregateRoot<ID>` 身份槽实参；原生类型（UUID/Long/String 直用）不再合格。R15 + 负证明探针锁死。场景「混放编译锁」：GIVEN `OrderRepository.findById` 期望 `OrderId` ｜ WHEN 调用位传入 `ProductId` ｜ THEN 编译失败（javac 期望失败取证，实录 → 案卷 2026-09-typed-identifier implement §3）；AND GIVEN 根为裸类型 UUID 的教例 ｜ WHEN R15 扫描 ｜ THEN 必咬。 | `DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS`；实形 `domain/order/id/OrderId.java`、`domain/product/id/ProductId.java`；负证明 `IdentifierRuleProofTest` 四锁 4/4 |
| BP-14 | domain 层跨聚合引用 ID 槽（字段、方法参数、集合与 Map 键）SHALL 使用目标聚合的 `{OtherAgg}Id`；跨聚合 import 仅准入目标 `id` 包。 | `domain/order/model/OrderItem.java` productId=`ProductId`、`domain/shared/service/InventoryDomainService.java` 签名；`OrderTest`/`InventoryDomainServiceTest` 绿 |
| BP-15 | 契约层 CQE/CO、PO、读侧端口与读 DTO、聚合内子实体 PK SHALL 维持原生类型承载，`{Agg}Id` 不入这些槽位；读侧不入的根据 = RC-6 读侧 domain 独立性宪章（读端口禁泄漏 domain 类型，现行法，本案不为其开 id 包例外——§裁决记录 Q6，案卷 2026-09-typed-identifier）。 | `application/order/repository/OrderQueryRepository.java` 与 `OrderViewDTO` 裸 UUID 签名零动、`OrderPO` 原生 id、contract 模块 diff-zero；探针锁三 `IdentifierProbes.BareEntityPk`/`ExemptReadPort` 不误咬绿 |
| BP-16 | 聚合身份来源 SHALL 分三档：①应用铸造（默认姿，经框架铸造唯一入口，创建即合法不破）；②自然键（`{Agg}Id` 包业务值事实，无铸造）；③DB 代铸（insert 省 id + useGeneratedKeys 通道；其「创建即合法」例外语义未立，首用者另案）。`{Agg}Id` 之 V 准任意单值可比较原生类型，机制不绑定 UUID。 | `Identifier<V>` 泛型位＋R15 反射臂只验接口不验承载（`AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS`）；`OrderFactory` 源① `OrderId.of(AggregateIds.mint())`；源③ 半槽=DddMapper javadoc「DB 自增省 id 列 + useGeneratedKeys」契约在库；Long 承载教例为纸面同形证明，见案卷 plan P-7 |
| BP-17 | 纯查询表、配置表、关联中间表及无合适单值代理主键的老表 SHALL 可被业务包声明为非聚合持久对象：不强套聚合蓝图套件、不为过铸造假主键；治理归读端口与 CRUD 旁路条款。反面向：欲入写侧聚合链路者，先须具备三档合法身份之一。 | R15 主语谓词只罩 `AggregateRoot` 具体子类与 `Repository` 端口、豁免位零误伤实证 `IdentifierRuleProofTest.rule_passes_exempt_positions`＋sample 读侧/PO 全绿 |
| BP-X1 | XML 七条语句契约：表名含 schema 前缀，领域词单数（另见 BP-S2）。逻辑删除聚合的 select、update、delete 显式带 `AND is_deleted = false`；`insert` 不枚举 `is_deleted`；`existsById` 恒返一行 boolean；`updateById` 携 `SET version = version + 1 ... AND version = #{version}`。文件放 `resources/mapper/{agg}/`，namespace = Mapper 全限定名。 | `modules/ddd.md` 场景 2；OL-4 互指 |
| BP-X2 | PO 只标 `@Data`，零 ORM 注解。Mapper `extends DddMapper`。RepositoryImpl 继承 `MybatisPersistence`。Converter 的 toDomain 一律经 `reconstitute()` 重建。 | AGENTS 九条 9；`modules/ddd.md`；TC-2 互指 |
| BP-X3 | 读端口 ㉑ 必须 `extends QueryRepository`，且位于 `application/{agg}/repository/`（R13）。 | read-chain RC-1 互指 |
| BP-S1 | 库表形状唯一权威 = `db-migration` 变更集，采 PG 原生形状：`id UUID DEFAULT uuidv7()`、`created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`、`created_by/updated_by UUID`、`is_deleted BOOLEAN NOT NULL DEFAULT FALSE`、`version BIGINT NOT NULL DEFAULT 0`。uuidv7() 为 PG18 内建，工厂铸造 ID 传值覆盖默认。DB 默认只兜手工插入；正常写路径的审计字段归应用层（AuditFieldFiller + Clock）。 | `db-migration/.../0001-init-schema.sql` 双库 SHA256 同形实证；sample PO 换形后 119 测试对账 |
| BP-S2 | schema 命名：聚合边界 = schema 边界，领域词用单数（`product.product`）。与 SQL 保留字冲突时必须升级为行业 UB 术语，如 order→sales_order；禁止用引号或前缀逃逸。schema 名与 Java 聚合包名的单数惯例逐字同构；微服务拆分时整 schema 平移。 | 用户 2026-09 裁决；本卷 §4 教学块与 sample 包树实证 |
| BP-S3 | 外部工具账表各住独立 schema，与业务 schema 互不混列：Liquibase 账表→`liquibase`，由执行器引导件幂等自建；Seata TC 优先分库，`undo_log` 随业务连接落 public。 | `db-migration` `LiquibaseSchemaBootstrapConfig`；双库账表按 schema 清点实证 |
| CC-7 | contract 模块内容有白名单：只放 Controller 契约接口、CQE、CO、枚举；只依赖 common-contract 的标记接口。东西向调用复用同一契约接口：一期走 RestClient 静态直连，Feign 为选项，需引入 common-cloud 才启用。（原号随身，自公约卷整条迁来，一字未改） | [contract 卷](../../modules/contract.md)；案卷 2026-09-pattern-taxonomy 搬家账 2 |

## §3 验收单（交付闸，逐条可机械化）

- [ ] `mvn compile` 通过，无循环依赖
- [ ] ArchUnit 测试通过（common-test 规则集）
- [ ] domain 层零框架注解，纯 Java + common-ddd
- [ ] 应用层 DTO 实现 `ApplicationDTO` 标记（R10b）；契约 CO 实现 `CO` 标记
- [ ] BP-X1~X3 逐条勾验
- [ ] BP-4~BP-17 逐条勾验，逐件形状对照本卷 §4 走查
- [ ] 新行为断言 = delta Scenario（TC-3）

## §4 全套规范形状（①-㉓ 逐件走查）

> 本节是 §1 全部槽位规范形状的唯一样本，各件代码即对应槽位文件的对照原型。整套均为**虚构教例，sample 未实现**。教学世界观：Payment 家族演示聚合构建，Reservation 家族演示写/读路径走查。条款注记见 §2 各 BP 行的取证列。

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

> HTTP 映射与文档注解声明在契约接口上。服务端的 ControllerImpl 只标 `@RestController` 并透传，见 §4.⑤。东西向调用复用同一契约接口，HTTP 直连，无需额外定义。

### 4.② Contract — CO（契约输出）

②③④ 三件套共用一张形状对照表。三者模式相同：`@Data` + `implements Serializable`，差异仅在标记接口：

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
    private PaymentStatus status;   // 契约枚举（㉓，见下节），非 String——值域镜像 domain PaymentStatus
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

Query 没有独立模板，形状与 ②③ 同模式：`@Data` + Serializable，差异只在标记接口 `Query`。对照表见 §4.②；构造与取值的实际形态见 §4.⑤ 和 §4.⑪。

### 4.㉓ Contract — 契约枚举（CO status 值域）

㉓ 号为顺延追加，物理归属 contract 段，故本节排在 ②③④ 之后：

```java
// contract/payment/enums/PaymentStatus.java（契约枚举：wire 值域解码器，与 ⑭ domain 状态机枚举同名同值域、两个编译单元并存）
public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
```

`PaymentCO.status` 的类型是该契约枚举，不是 `String`。消费方从 contract jar 直接拿到合法值域，OpenAPI 自动枚举合法值。它与 ⑭ `domain/payment/model/PaymentStatus` 是同一值域的两个并存化身：domain 不依赖 contract，contract 不依赖 server 内部，分层规则双向封死引用方向，所以两份枚举无法消除。正确的收口不是合并，而是**奇偶锁**：两个枚举的常量名集合由守卫测试锁死，任一侧增删或改名常量，构建期即红。真实例：sample 的 `contract/ContractEnumParityTest.java`。内部 DTO 仍为 `String`，Assembler 走 `domain.name()` 出口；`String → 契约枚举` 在 Presenter 层用 `valueOf` 收口，示例见 §4.⑨。未知字面量当场 fail-fast，含义是消费方需要升级 contract jar。当前形状的完整走查对照 → [write-path.md](../../../../docs/how-to/write-path.md)，Reservation 家族已按此形状示范。

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

DTO 是内部视图，可含审计字段；这些字段不进 CO：

```java
@Data
public class PaymentDTO implements ApplicationDTO, Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private UUID id;   // 最小模板兼作读投影（§4.⑪ 复用）：按 RC-6/BP-15 维持原生；写专用 DTO 准案卷 plan P-3 携带 PaymentId（实形见真实例 OrderDTO）
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
        dto.setId(payment.getId().value());                             // 根为终类型（BP-13）：装配位拆箱进本兼读最小模板的裸 id（写专用 DTO 直传 PaymentId，案卷 plan P-3）
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
        co.setId(dto.getId());   // wire 出口原生 UUID（BP-6：契约保持原生值）
        // 内部 DTO 恒为 String（Assembler 走 domain.name()）；String → 契约枚举（㉓）在呈现层收口，
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
        Payment payment = new Payment(PaymentId.of(AggregateIds.mint()), command.getOrderId(), command.getAmount());   // BP-16 源① 应用铸造：铸造唯一入口 + of() 定型
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

    private final PaymentQueryRepository paymentQueryRepository;   // application 层读端口（见 ㉑）

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

> 读侧不经 ⑮ `PaymentRepository`，也不经 ⑧ Assembler，由读端口直接投影 DTO；⑮ 是 domain 的写侧契约。读侧教义与分页/多视图完整形态以 [read-chain 法卷](../chain/read-chain.md) 为 canonical。本最小模板直接复用 ⑦ `PaymentDTO` 作读投影，version 等内部字段由 Presenter 过滤。需要读写独立演进时，按 read-path.md 拆出 `PaymentViewDTO` 和 `PaymentViewPresenter`。

### 4.⑫ Domain — 聚合身份终类型（`{Agg}Id`）

```java
// domain/payment/id/PaymentId.java —— 本聚合专属币种：根身份槽 / 写端口 ID 槽实参（BP-13）
public record PaymentId(UUID value) implements Identifier<UUID>, Serializable {

    public PaymentId {
        if (value == null) {
            throw new BusinessException("payment:err.idRequired");   // BP-12 统一位点通道
        }
    }

    /** 装箱入口（写 Handler 一点定型位使用）——仅 null 检查，不站岗 */
    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }
}
```

> **只装箱不站岗**：紧凑构造器仅 null 检查，不校验底值——禁格式复校验（非法格式到不了此处：Web 层类型转换 400 判例在先），亦禁查 UUID 版本位（存量行经装载路径照样过 `of()`，查版本＝炸历史数据）；出生保证归铸造入口 `AggregateIds`。**null 闸抛 `BusinessException` + `{agg}:err.idRequired` 统一位点（BP-12 同法——「不站岗」指不校验底值，非不走统一通道；`requireNonNull` 中文硬编码＝NPE 走 500 兜底冒充业务位点，禁）**。`Serializable` 仅满足 `MybatisPersistence` 持久槽既有约束（`ID extends Serializable`），非序列化承诺。底层承载由各聚合自定（UUID / Long 皆可，机制不绑类型，见 BP-16）；币种不入的槽位见 BP-15。真实例：`domain/order/id/OrderId.java`、`domain/product/id/ProductId.java`。

### 4.⑬ Domain — 聚合根

```java
public class Payment extends AggregateRoot<PaymentId> {   // 身份槽实参 = 终类型（BP-13）

    private PaymentId id;
    private UUID orderId;   // BP-14：跨聚合引用槽准目标币种（教例未铸 OrderId，实形见真实例订单项商品槽）
    private PaymentStatus status;
    private BigDecimal amount;
    private OffsetDateTime createdAt;
    private Long version;

    /** 业务构造器 */
    public Payment(PaymentId id, UUID orderId, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    /** 重建构造器（Converter 使用） */
    public static Payment reconstitute(PaymentId id, UUID orderId, PaymentStatus status,
                                       BigDecimal amount, OffsetDateTime createdAt, Long version) {
        Payment p = new Payment(id, orderId, amount);
        p.status = status;
        p.createdAt = createdAt;
        p.version = version;
        return p;
    }

    @Override
    public PaymentId getId() { return id; }

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

> 真实例升级位：sample 的聚合新建已收口到 Factory + 包私有构造，实现「创建即合法」，见真实例 `OrderFactory.java`。本教例保留最小闭环的公开业务构造器形态；Factory 属按需增强件。

### 4.⑭ Domain — 枚举

```java
public enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
```

### 4.⑮ Domain — Repository 接口（写侧）

```java
public interface PaymentRepository extends Repository<Payment, PaymentId> {   // 端口 ID 槽与根槽一致（BP-13/R15）
    // 继承：findById / save / update / exists / deleteById
}
```

### 4.⑯ Infrastructure — PO

PO 是纯 `@Data` POJO，**零 ORM 注解**。表名、主键策略、版本条件、逻辑删除过滤全部由 XML 里的 SQL 文本承担：

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

### 4.⑰ Infrastructure — Converter

```java
@Component
public class PaymentConverter implements BasicConverter<Payment, PaymentPO> {

    @Override
    public Payment toDomain(PaymentPO po) {
        return Payment.reconstitute(
                PaymentId.of(po.getId()), po.getOrderId(),   // PO 原生值 → 终类型：装配位装箱
                PaymentStatus.valueOf(po.getStatus()),
                po.getAmount(), po.getCreatedAt(), po.getVersion());
    }

    @Override
    public PaymentPO toPO(Payment domain) {
        PaymentPO po = new PaymentPO();
        po.setId(domain.getId().value());
        po.setOrderId(domain.getOrderId());
        po.setStatus(domain.getStatus().name());
        po.setAmount(domain.getAmount());
        po.setVersion(domain.getVersion());
        return po;
    }
}
```

### 4.⑱ Infrastructure — Mapper

```java
@Mapper
public interface PaymentMapper extends DddMapper<PaymentPO> {
    // 通用七条语句由同篇 XML 实现（namespace = 本接口全限定名）；
    // 业务专有查询（如按唯一键单查）在此追加具名方法
}
```

### 4.⑲ Infrastructure — RepositoryImpl

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
    /** 领域 ID → 原生主键的唯一转换位：每仓储恰一处（PO / Mapper / XML / schema 零修改） */
    @Override protected Serializable toPersistenceId(PaymentId id) { return id.value(); }
    @Override public Optional<Payment> findById(PaymentId id) { return findDomainById(id); }
    @Override public void save(Payment domain) { saveDomain(domain); }
    @Override public void update(Payment domain) { updateDomain(domain); }
    @Override public boolean exists(PaymentId id) { return existsDomainById(id); }
    @Override public void deleteById(PaymentId id) { removeDomainById(id); }
}
```

> 构造器注入四件框架依赖，外加业务 Mapper 与 Converter：`Clock` / `AuditProperties` / `ObjectProvider<CurrentUserProvider>`。`save/update` 自动 `validate()`，并经 `AuditFieldFiller` 显式填充审计字段。事务边界在 Handler，本类不标 `@Transactional`。跨聚合协调 = 同一事务内直调。

### 4.⑳ Infrastructure — 手写 XML（DddMapper 七条语句）

手写 XML 位于 `src/main/resources/mapper/payment/PaymentMapper.xml`，七条语句逐条可见。各语句的列级语义以 [knowledge/docs/reference/api/common-ddd.md](../../../../docs/reference/api/common-ddd.md) §2 的 DddMapper 七语句契约表为准，例如 INSERT 不枚举 `is_deleted`、UPDATE 携带版本条件、删除消费基类传入的 `now` / `updatedBy` 审计参数。本节只给完整模板：

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

> 不需要逻辑删除的聚合：`deleteById` 改写物理 `DELETE FROM ... WHERE id = #{id}`，各 select 省略 `is_deleted` 条件即可。这个语义选择落在 XML 文本里，聚合之间互不影响。

### 4.㉑ 读端口 — PaymentQueryRepository（application 层，与 ⑪ 配对）

```java
// application/payment/repository/PaymentQueryRepository.java（application 层读端口）
public interface PaymentQueryRepository extends QueryRepository {   // 空标记（common-ddd）：读端口身份

    /** 按 ID 投影支付读 DTO（不存在返回 empty）。 */
    Optional<PaymentDTO> findById(UUID id);
}
```

### 4.㉒ 读端口实现 — PaymentQueryRepositoryImpl（与 ⑲ 同包）

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
- 读端口接口放 `application/payment/repository/` 且 `extends QueryRepository`。在 R13 下（QueryHandler 禁触 domain 仓储）这是唯一合法读路径；R1b 白名单同时放行 infra 对该端口的实现依赖。
- 完整读侧形态含分页双语句、`safe*()` 钳制、ViewDTO/ViewPresenter 多视图，以 [read-chain 法卷](../chain/read-chain.md) 为 canonical。本节只登记新聚合清单所需的最小文件集。

## §5 服务骨架通式（本卷的 service 级扩展）

```text
contract/{agg}/        adapter/rest/controller + dto/{command,query,co} + enums
server adapter/        rest/controller + task/scheduler   —— 不按聚合分包
server application/    service + handler/{command,query} + assembler + presenter + dto + repository（读端口）
server domain/         id（⑫）+ model + repository（写端口）+ portal + service + policy【按需】
server infrastructure/ persistence/{ds}/{agg}/(mybatis/{po,mapper} + converter + repository：写读 Impl 同包) + gateway/{capability} + config
resources/             mapper/**/*.xml（手写 SQL 语句面）
```

真实包树**不设二手地图**：包路径与文件数的 canonical 是源码本身，见归属法卷 §2 第一行；要看包树请直接 glob 源码。本通式只裁定「一个服务该有哪些目录、每个目录住什么」；逐槽形状由 §1 清单和 §2、§4 条款约束。

## 生效登记

全部 ✅。sample 现行两个聚合按本卷建成；槽位真实性由 C1 逐槽代入对账。

本卷 2026-09 换号：旧 ⑫–㉒ → 新 ⑬–㉓，映射全表见案卷 2026-09-typed-identifier
