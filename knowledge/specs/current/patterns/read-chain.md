# 用法规范法卷：读用例链（框架法 · 严格件）

> **身份**：本卷是读用例链**统一用法**的唯一权威——条款与全套规范形状在此，全仓他处不得复写形状；违反本卷=修代码，修卷只走 `../../changes/` 程序。docs 同题篇（`../../../docs/how-to/read-path.md`）为设计卡（选型与边界叙事，零形状代码），冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：C1（`{agg}` 实例化）/ C3（符号）/ C4（中立）扫本卷；教例家族 Reservation（虚构教例，sample 未实现，D4 教学中立教义），真实例只准出现在带「真实例」标记的指针位。开册法案：`2026-09-howto-codification`；统一用法归卷：`2026-09-usage-consolidation`。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| RC-1 | 读侧绕过聚合根：读端口接口定义于 `application/{agg}/repository/`，`extends QueryRepository`；infra 实现直投 PO → DTO，不加载聚合 | R13；R1b 白名单放行 infra 对该端口的实现依赖 | C1 实例化 |
| RC-2 | QueryHandler 只注入读端口，禁止依赖 domain 侧写 Repository | R13（QueryHandler 禁触 domain 仓储） | ArchUnit |
| RC-3 | 分页链路：Query record 经 `@ModelAttribute` 绑定 → 构造 `PageResult`；缺参绑定为原始类型默认值 → 校验注解 → 400；**无默认分页值**（此承诺自卷 `modules/contract.md` §3.1，本条互指不复述） | `modules/ddd.md` 场景 4；binding 案卷 `archive/2026-09-pagequery-default-claim` | C3 |
| RC-4 | 多视图投影用 ViewDTO/ViewPresenter，规则见 `application-objects.md`（AO-1 准入） | `modules/ddd.md` 场景 4 | — |
| RC-5 | Query 契约形态：单条 = 实现 `Query` + `@NotNull` `UUID` 组件（非法格式由 Web 层类型转换拦截 → 400）；分页 = record 实现 `PageableQuery`（`pageNum()`/`pageSize()` 与 record 组件**同签名**，零覆写样板）；校验注解声明在 record 组件上；过滤字段用**契约枚举**而非裸 String——非法字面量在 Spring 绑定层即 400 typeMismatch，显式失败优于静默空页 | `PageableQuery` javadoc（入参约束由业务 record 在组件上声明；`MAX_PAGE_SIZE` 上限在册）+ 本卷 §2.2 形状；缺参无默认裁决见 RC-3 互指 | C3 |
| RC-6 | 读端口类型纪律：端口接口 `extends QueryRepository`（common-ddd 空标记），供 R1b / R13 架构规则按类型识别，方法签名自由；端口返回 application 层**读 DTO**（无 version 字段），禁泄漏 domain 类型；读侧不经写 Repository、不经 Converter、不经 Assembler | ArchUnit R1b / R13（原篇载明）+ 本卷 §2.6 形状 | ArchUnit |
| RC-7 | 分页实现形态：手写双语句——取数（LIMIT/OFFSET）与计数（同条件）两条具名 Mapper 方法共享 XML `<sql>` 条件片段，杜绝条件漂移；实现侧统一消费 `safePageNum()`/`safePageSize()` 防御性钳制（1..MAX_PAGE_SIZE），即使调用点未经 Bean Validation 也安全；offset 由钳制后页码经 long 乘法换算防大页码溢出；`PageResult<T>`（common-contract，与 `PageableQuery` 同居契约层）隔离底层分页形态；无运行时分页插件参与 | 真实例：sample `OrderMapper.xml` 分页双语句 + `OrderQueryRepositoryImpl.findPage`（原篇载明同构入法） | 评审项 |
| RC-8 | 读侧没有业务判断：业务规则只在写侧（领域聚合根）计算并物化到 PO 列，读侧只投影物化后的值；若某"读"需要现算业务逻辑，那是建模信号——该计算应下沉到写侧物化，禁止在读路径引入领域判断 | <!-- 待 ../../changes/ 补全 --> | 评审项 |
| RC-9 | 只读编排：QueryHandler 无 `@Transactional`（只读操作）、只注入读端口、返回读 DTO / `PageResult<读 DTO>`（不返回 CO）；读 DTO → CO 由 ViewPresenter 收口（WC-4 互指） | 本卷 §2.5 形状；RC-2（R13） | ArchUnit |

## §2 规范形状（统一用法唯一样本）

教例设定（与写链卷共用虚构聚合 **Reservation 预约单**，D4 教学中立教义，sample 未实现）：以 **"查询预约详情"** 和 **"分页查询预约列表"** 为案例，走查读操作从 REST 入口到 DTO 投影的完整代码路径。读侧绕过 domain 是写侧「infrastructure → domain」依赖倒置的读侧镜像「infrastructure → application」：读端口定义在 application 层、基础设施层实现之（RC-1）。

### 2.1 调用链路

```
REST 请求
  → adapter/rest/controller/ReservationControllerImpl（参数包装）
    → application/reservation/service/ReservationAppService（委托 + 呈现）
      → application/reservation/handler/query/GetReservationHandler（查询编排）
        → application/reservation/repository/ReservationQueryRepository（读端口）
          → infrastructure/.../repository/ReservationQueryRepositoryImpl（PO → 读 DTO 直接投影）
      → application/reservation/presenter/ReservationViewPresenter（读 DTO → CO）
  ← ReservationCO
```

### 2.2 Contract — Query 定义

**单条查询：**

```java
// contract/reservation/dto/query/GetReservationQuery.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询预约详情")
public class GetReservationQuery implements Query, Serializable {   // RC-5：单条 = Query 实现
    @Serial private static final long serialVersionUID = 1L;

    /** 预约 ID（非法格式由 Web 层类型转换拦截 → 400） */
    @NotNull
    @Schema(description = "预约 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID reservationId;                                     // RC-5：ID 类型 UUID，与写链 WC-7 同源
}
```

**分页查询（record 实现）：**

```java
// contract/reservation/dto/query/GetReservationPageQuery.java
public record GetReservationPageQuery(                              // RC-5：分页 = record 实现 PageableQuery
        @Schema(description = "状态过滤（可选，值域 = ReservationStatus 契约枚举；非法值 400）")
        ReservationStatus status,                                   // RC-5：过滤用契约枚举，非裸 String
        @Schema(description = "客户 ID 过滤（可选）")
        String customerId,
        @Min(1) @Schema(description = "页码（从 1 开始，须显式传入）")
        int pageNum,                                                // RC-5/RC-3：同签名零覆写；缺省绑定 0 → 400
        @Min(1) @Max(PageableQuery.MAX_PAGE_SIZE) @Schema(description = "每页大小（须显式传入，上限 1000）")
        int pageSize
) implements PageableQuery {
}
```

要点（条款化）：
- 单条查询实现 `Query`，分页查询实现 `PageableQuery`（common-contract）——`PageableQuery` 继承 `Query`，抽象方法 `pageNum()`/`pageSize()` 与 record 组件**同签名**，零覆写样板（RC-5）
- 校验注解（`@Min`/`@Max`）声明在 record 组件上；读侧仓储统一消费 `safePageNum()`/`safePageSize()` 取防御性钳制值（RC-5/RC-7）
- 过滤字段用**契约枚举**而非裸 String：非法字面量在 Spring 绑定层即 400 typeMismatch——显式失败优于静默空页（RC-5；契约枚举纪律同写链 WC-8）
- record 天然不可变，适合简单 Query（RC-5）

### 2.3 Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/reservation/adapter/rest/controller/ReservationController.java（契约接口，承载 HTTP 映射）
@RequestMapping("/reservations")
public interface ReservationController {                            // WC-9：映射注解住契约接口

    @Operation(summary = "查询预约详情", description = "根据 ID 获取预约完整信息")
    @GetMapping("/{reservationId}")
    ReservationCO getReservation(@PathVariable("reservationId") UUID reservationId);
}

// adapter/rest/controller/ReservationControllerImpl.java（实现，仅标记协议 + 透传；RestAdapter 标记见规则 R8b）
@RestController
public class ReservationControllerImpl implements ReservationController, RestAdapter {

    private final ReservationAppService reservationAppService;

    @Override
    public ReservationCO getReservation(UUID reservationId) {
        // REST 路径参数 → Query 包装 → 透传（非法 UUID 由 Web 层类型转换拦截 → 400）
        return reservationAppService.getReservation(new GetReservationQuery(reservationId));   // WC-4：Adapter 零逻辑
    }
}
```

### 2.4 Application — AppService

```java
// application/reservation/service/ReservationAppService.java（节选）
@Service
public class ReservationAppService implements ApplicationService {

    private final ReservationViewPresenter reservationViewPresenter;  // 读侧 Presenter（写侧用 ReservationPresenter，RC-4）
    private final GetReservationHandler getReservationHandler;

    public ReservationCO getReservation(GetReservationQuery query) {  // WC-4：AppService 返回 CO
        return reservationViewPresenter.present(getReservationHandler.handle(query));   // RC-9：ViewPresenter 收口
    }
}
```

### 2.5 Application — QueryHandler（只读编排，RC-9）

**单条查询：**

```java
// application/reservation/handler/query/GetReservationHandler.java
@Component
public class GetReservationHandler implements QueryHandler<GetReservationQuery, ReservationViewDTO> {

    private final ReservationQueryRepository reservationQueryRepository;   // RC-2：只注入读端口

    public GetReservationHandler(ReservationQueryRepository reservationQueryRepository) {
        this.reservationQueryRepository = reservationQueryRepository;
    }

    @Override                                                        // RC-9：无 @Transactional（只读）
    public ReservationViewDTO handle(GetReservationQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 投影，不 reconstitute 聚合根；
        // 非法 UUID 已由 Web 层类型转换拦截（400），此处必为合法值
        return reservationQueryRepository.findById(query.getReservationId())   // RC-1：直投，不经聚合根
                .orElseThrow(() -> new BusinessException("reservation:err.notFound"));
    }
}
```

**分页查询：**

```java
// application/reservation/handler/query/GetReservationPageHandler.java
@Component
public class GetReservationPageHandler implements QueryHandler<GetReservationPageQuery, PageResult<ReservationViewDTO>> {

    private final ReservationQueryRepository reservationQueryRepository;

    public GetReservationPageHandler(ReservationQueryRepository reservationQueryRepository) {
        this.reservationQueryRepository = reservationQueryRepository;
    }

    @Override                                                        // RC-9：只读无事务
    public PageResult<ReservationViewDTO> handle(GetReservationPageQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 分页投影；
        // 分页参数经 Query 双通道 safe*() 在实现侧统一钳制，Handler 不重复处理
        return reservationQueryRepository.findPage(query);           // RC-7：钳制在实现侧，Handler 透传
    }
}
```

要点（条款化）：实现 `QueryHandler<Q, R>`（common-ddd）；无需 `@Transactional`（只读操作）；分页返回 `PageResult<DTO>`，不返回 CO（RC-9）；Handler 只依赖读端口，不经 Assembler、不经聚合根（RC-2/RC-6）。

### 2.6 Application — 读端口（Query Port，RC-6）

```java
// application/reservation/repository/ReservationQueryRepository.java
public interface ReservationQueryRepository extends QueryRepository {   // 空标记（common-ddd）：读端口身份（RC-6：R1b/R13 按类型识别）

    /** 按 ID 投影预约读 DTO（不存在返回 empty）。 */
    Optional<ReservationViewDTO> findById(UUID id);                     // RC-6：返回读 DTO，不泄漏 domain 类型

    /**
     * 分页投影预约读 DTO。
     * 实现侧统一消费 {@code query.safePageNum()} / {@code query.safePageSize()}
     * 双通道防御钳制（1..MAX_PAGE_SIZE）。
     */
    PageResult<ReservationViewDTO> findPage(GetReservationPageQuery query);   // RC-7：PageResult 框架级分页容器
}
```

要点（条款化）：`extends QueryRepository` 空标记供 R1b / R13 架构规则按类型识别，方法签名自由（RC-6）；端口返回**读 DTO**（application 层类型），不返回领域类型（RC-6）；`PageResult<T>` 是框架级分页容器（common-contract，与 `PageableQuery` 同居契约层），业务在 application/infrastructure 用它，消费方从契约直接拿到分页元数据（RC-7）；写侧 `ReservationRepository`（domain 层）只保留聚合生命周期，读侧不经过它（RC-6）。

### 2.7 读侧业务判断放哪（RC-8）

**读侧没有业务判断。** 业务规则只在写侧（领域聚合根）计算并物化：

| 场景 | 落点 | 说明 |
|------|------|------|
| 纯投影读（列表/字段展示） | infra 层 PO → 读 DTO 直投 | 读侧唯一形态（RC-1） |
| 需要派生值（如「是否可取消」） | **写侧聚合根计算 → 物化到 PO 列** | 读侧只投影物化后的值（RC-8） |

若某"读"需要现算业务逻辑，那是建模信号——该计算应下沉到写侧物化（如预约创建时算出 `canCancel` 布尔列），而不是在读路径里引入领域判断。

### 2.8 Infrastructure — 读实现（PO → DTO 直接投影）

```java
// infrastructure/persistence/master/reservation/repository/ReservationQueryRepositoryImpl.java
@Component
public class ReservationQueryRepositoryImpl implements ReservationQueryRepository {   // RC-1：infra 实现 application 定义的读端口

    private static final JsonMapper MAPPER = new JsonMapper();
    private final ReservationMapper reservationMapper;

    public ReservationQueryRepositoryImpl(ReservationMapper reservationMapper) {
        this.reservationMapper = reservationMapper;
    }

    @Override
    public Optional<ReservationViewDTO> findById(UUID id) {
        ReservationPO po = reservationMapper.selectById(id.toString());
        if (po == null) return Optional.empty();
        return Optional.of(toViewDTO(po));
    }

    @Override
    public PageResult<ReservationViewDTO> findPage(GetReservationPageQuery query) {
        // 双通道防御钳制（1..MAX_PAGE_SIZE）：即使调用点未经 Bean Validation 也安全
        int safePageNum = query.safePageNum();                        // RC-7：钳制在实现侧统一发生
        int safePageSize = query.safePageSize();
        // 手写分页：offset 由钳制后的页码换算（long 乘法防大页码溢出）；
        // 取数 / 计数两条具名方法共享 XML 内同一 <sql> 条件片段，无运行时分页插件
        long offset = (safePageNum - 1) * (long) safePageSize;        // RC-7：long 乘法防溢出
        // 契约过滤参数已枚举化（非法字面量在 binding 层即 400，到此必为合法值或 null）；
        // SQL 文本按常量名比对，枚举 → 字符串在此收口
        String statusFilter = query.status() == null ? null : query.status().name();   // RC-5：枚举过滤在此收口
        List<ReservationPO> rows = reservationMapper.selectPageByCondition(
                statusFilter, query.customerId(), offset, safePageSize);               // RC-7：具名取数语句
        long total = reservationMapper.countByCondition(statusFilter, query.customerId());   // RC-7：具名计数语句
        return new PageResult<>(
                rows.stream().map(this::toViewDTO).toList(),
                total, safePageNum, safePageSize);
    }

    /** PO → 读 DTO 直接投影（不经过 domain，不 reconstitute 聚合根）。 */
    private ReservationViewDTO toViewDTO(ReservationPO po) {          // RC-1：直投，不经 Converter/Assembler
        ReservationViewDTO dto = new ReservationViewDTO();
        dto.setId(po.getId());
        dto.setStatus(po.getStatus());
        dto.setItems(deserializeItems(po.getItems()));
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCustomerId(po.getCustomerId());
        dto.setConfirmationCode(po.getConfirmationCode());
        dto.setCancelReason(po.getCancelReason());
        dto.setCreateAt(po.getCreateAt());
        dto.setUpdateAt(po.getUpdateAt());                            // RC-6：读 DTO 无 version 字段
        return dto;
    }
}
```

要点（条款化）：读实现**不经 Converter、不 reconstitute 聚合根**——直接 PO → **读 DTO** 轻量投影，预约明细 JSON 直接反序列化为应用层 DTO（不经过领域值对象）（RC-1/RC-6）；分页 = Mapper 具名方法 + 手写 XML 双语句（LIMIT/OFFSET 取数 + 同条件 COUNT），`PageResult<T>` 隔离底层分页形态，`map()` 支持链式转换（RC-7）。

分页双语句的声明与 SQL 文本（教例形态，与 sample 实际实现同构——真实例：OrderMapper.xml 的分页双语句 + OrderQueryRepositoryImpl.findPage）：

```java
// infrastructure/persistence/master/reservation/mybatis/mapper/ReservationMapper.java（具名分页方法）
List<ReservationPO> selectPageByCondition(@Param("status") String status,
                                          @Param("customerId") String customerId,
                                          @Param("offset") long offset,
                                          @Param("limit") long limit);

long countByCondition(@Param("status") String status,
                      @Param("customerId") String customerId);
```

```xml
<!-- resources/mapper/reservation/ReservationMapper.xml —— WHERE 片段共享，杜绝两条语句条件漂移 -->
<sql id="pageCondition">                                              <!-- RC-7：共享片段=条件漂移免疫 -->
    WHERE is_delete = false
    <if test="status != null">AND status = #{status}</if>
    <if test="customerId != null">AND customer_id = #{customerId}</if>
</sql>

<select id="selectPageByCondition" resultType="...mybatis.po.ReservationPO">
    SELECT <include refid="columns"/>
    FROM reservations.reservations
    <include refid="pageCondition"/>
    ORDER BY create_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>

<select id="countByCondition" resultType="long">
    SELECT COUNT(*)
    FROM reservations.reservations
    <include refid="pageCondition"/>
</select>
```

### 2.9 写路径 vs 读路径对比（选择表，随形状入卷）

| 维度 | 写路径（CommandHandler） | 读路径（QueryHandler） |
|------|------------------------|----------------------|
| 事务 | `@Transactional(rollbackFor = Exception.class)` | 可省略（只读，RC-9） |
| 聚合根 | 必须加载（load → 行为 → save） | 完全绕过（PO → DTO 直投，RC-1） |
| 经过 domain 层 | 是（聚合根 + Repository + Assembler） | **否**（读端口直连 PO） |
| 返回类型 | 写侧 DTO（含 version） | 读侧 DTO（不含 version，RC-6） |
| 依赖 | Repository + Assembler + DomainService | ReservationQueryRepository（读端口） |
| 固定模式 | load → 调用行为 → save → toDTO（WC-2） | findById/findPage → 返回读 DTO（RC-9） |

### 2.10 教例文件清单（读路径走查逐文件对位）

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/query/GetReservationQuery.java` | 单条查询 |
| contract | `dto/query/GetReservationPageQuery.java` | 分页查询 |
| contract | `dto/co/ReservationCO.java` | 契约输出 |
| contract | `enums/ReservationStatus.java` | 契约枚举（查询过滤值域） |
| adapter | `rest/controller/ReservationControllerImpl.java` | 协议适配 |
| application | `service/ReservationAppService.java` | 聚合入口 |
| application | `dto/ReservationViewDTO.java` | 读 DTO（无 version） |
| application | `handler/query/GetReservationHandler.java` | 单条查询编排 |
| application | `handler/query/GetReservationPageHandler.java` | 分页查询编排 |
| application | `repository/ReservationQueryRepository.java` | 读端口（`extends QueryRepository`，返回读 DTO） |
| application | `presenter/ReservationViewPresenter.java` | 读 DTO → CO |
| infrastructure | `mybatis/mapper/ReservationMapper.java` | 分页具名方法（selectPageByCondition / countByCondition） |
| infrastructure | `repository/ReservationQueryRepositoryImpl.java` | 读实现（PO → 读 DTO 投影，与写侧 Impl 同包） |
| resources | `mapper/reservation/ReservationMapper.xml` | 分页双语句 + 动态条件（LIMIT/OFFSET + COUNT） |

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| RC-1~4 既有条款 | ✅ | sample 读路径现行遵循（ArchUnit R1b/R13 守护在册） |
| RC-5~9 归卷新增条款（Query 形态 / 读 DTO 纪律 / 双语句分页 / 读侧无业务判断 / 只读编排，源=原篇 read-path.md 入法） | ✅ | 本卷 §2 形状在册 |
| RC-7 双语句真实例同构 | ✅ | 真实例：sample `OrderMapper.xml` 分页双语句（`pageCondition` 共享片段 + selectPageByCondition / countByCondition）、`OrderQueryRepositoryImpl.findPage`——仅作对照、不逐字镜像（原篇载明） |
| 教例家族 Reservation 读链走查模板 | ⛔ 虚构教例，sample 未落地 | §2 即落地模板 + §2.10 文件清单；设计判断见 docs 设计卡 |
