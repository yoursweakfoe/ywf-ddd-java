# 读路径全链路

> 设计原理 → [module-design/application.md](../module-design/application.md)

## 业务场景

延续示例应用的电商场景（参见 [write-path.md](write-path.md) 业务场景节）。

本文以 **“查询订单详情”** 和 **“分页查询订单列表”** 为案例，展示读操作从 REST 入口到 DTO 投影的完整代码路径。

**业务需求：**

1. 用户点击订单详情页 → 根据订单 ID 查询单条订单（含订单项、状态、金额）
2. 用户打开订单列表页 → 按状态/客户筛选 + 分页浏览
3. 查询是只读操作，不需要加载聚合根（无行为可调用），直接从 PO 投影 DTO 即可
4. 返回给前端的是 CO（契约输出），不暴露 version、deleted 等内部字段

**为什么读侧绕过 domain？（全文教义块）**

CQRS 读写分离：写侧需要聚合根是因为要调用行为方法（`order.pay()`）执行业务规则；读侧不做任何状态变更，只需"把数据查出来给前端看"。因此读侧**完全不经过 domain 层**——不 reconstitute 聚合根、不建领域读模型、不经 Assembler，由基础设施层实现直接 `PO → 读 DTO` 投影。读端口（`OrderQueryRepository`）定义在 application 层、基础设施层实现之，是「写侧 infrastructure → domain」依赖倒置的读侧镜像「infrastructure → application」。读侧也没有业务判断：需要派生值的字段一律在写侧计算并物化——详见下文 §5「读侧业务判断放哪？」专节。

```
REST 请求
  → adapter/rest/controller/OrderControllerImpl（参数包装）
    → application/order/service/OrderAppService（委托 + 呈现）
  → application/order/handler/query/GetOrderHandler（查询编排）
    → application/order/repository/application/OrderQueryRepository（读端口）
      → infrastructure/.../repository/application/OrderQueryRepositoryImpl（PO → 读 DTO 直接投影）
      → application/order/presenter/OrderViewPresenter（DTO → CO）
  ← OrderCO
```

## 1. Contract — Query 定义

### 单条查询

```java
// contract/order/dto/query/GetOrderQuery.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询订单详情")
public class GetOrderQuery implements Query, Serializable {

    /** 订单 ID（非法格式由 Web 层类型转换拦截 → 400） */
    @Schema(description = "订单 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID orderId;
}
```

### 分页查询（record 实现）

```java
// contract/order/dto/query/GetOrderPageQuery.java
public record GetOrderPageQuery(
        String status,       // 订单状态过滤（可选）
        String customerId,   // 客户 ID 过滤（可选）
        int pageNum,         // 页码（从 1 开始）
        int pageSize         // 每页大小
) implements PageableQuery {
}
```

要点：
- 单条查询实现 `Query`，分页查询实现 `PageableQuery`（common-contract）
- `PageableQuery` 继承 `Query`，抽象方法 `pageNum()`/`pageSize()` 与 record 组件**同签名**——零覆写样板
- 校验注解（`@Min`/`@Max`）声明在 record 组件上；读侧仓储统一消费 `safePageNum()`/`safePageSize()` 取防御性钳制值
- record 天然不可变，适合简单 Query

## 2. Adapter — Controller 契约接口 + 实现（纯透传）

```java
// contract/order/adapter/rest/controller/OrderController.java（契约接口，承载 HTTP 映射）
@RequestMapping("/orders")
public interface OrderController {

    @Operation(summary = "查询订单详情", description = "根据 ID 获取订单完整信息")
    @GetMapping("/{orderId}")
    OrderCO getOrder(@PathVariable("orderId") UUID orderId);
}

// adapter/rest/controller/OrderControllerImpl.java（实现，仅标记协议 + 透传；RestAdapter 标记见规则 R8b）
@RestController
public class OrderControllerImpl implements OrderController, RestAdapter {

    private final OrderAppService orderAppService;

    @Override
    public OrderCO getOrder(UUID orderId) {
        // REST 路径参数 → Query 包装 → 透传（非法 UUID 由 Web 层类型转换拦截 → 400）
        return orderAppService.getOrder(new GetOrderQuery(orderId));
    }
}
```

## 3. Application — AppService

```java
// application/order/service/OrderAppService.java（节选）
@Service
public class OrderAppService implements ApplicationService {

    private final OrderViewPresenter orderViewPresenter;  // 读侧 Presenter（写侧用 OrderPresenter）
    private final GetOrderHandler getOrderHandler;

    public OrderCO getOrder(GetOrderQuery query) {
        return orderViewPresenter.present(getOrderHandler.handle(query));
    }
}
```

## 4. Application — QueryHandler

### 单条查询

```java
// application/order/handler/query/GetOrderHandler.java
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderViewDTO> {

    private final OrderQueryRepository orderQueryRepository;

    public GetOrderHandler(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
    }

    @Override
    public OrderViewDTO handle(GetOrderQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 投影，不 reconstitute 聚合根；
        // 非法 UUID 已由 Web 层类型转换拦截（400），此处必为合法值
        return orderQueryRepository.findById(query.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
    }
}
```

### 分页查询

```java
// application/order/handler/query/GetOrderPageHandler.java
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {

    private final OrderQueryRepository orderQueryRepository;

    public GetOrderPageHandler(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
    }

    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 分页投影；
        // 分页参数经 Query 双通道 safe*() 在实现侧统一钳制，Handler 不重复处理
        return orderQueryRepository.findPage(query);
    }
}
```

要点：
- 实现 `QueryHandler<Q, R>`（common-ddd）
- 无需 `@Transactional`（只读操作）
- 分页返回 `PageResult<DTO>`，不返回 CO
- Handler 只依赖读端口 `OrderQueryRepository`，不经 Assembler、不经聚合根（教义见顶部教义块）

## 5. Application — 读端口（Query Port）

```java
// application/order/repository/application/OrderQueryRepository.java
public interface OrderQueryRepository extends QueryRepository {   // 空标记（common-ddd）：读端口身份

    /** 按 ID 投影订单读 DTO（不存在返回 empty）。 */
    Optional<OrderViewDTO> findById(UUID id);

    /**
     * 分页投影订单读 DTO。
     * 实现侧统一消费 {@code query.safePageNum()} / {@code query.safePageSize()}
     * 双通道防御钳制（1..MAX_PAGE_SIZE）。
     */
    PageResult<OrderViewDTO> findPage(GetOrderPageQuery query);
}
```

要点：
- `extends QueryRepository`（common-ddd 空标记）：标记「读端口」身份，供 R1b / R13 架构规则按类型识别；方法签名自由
- 端口返回**读 DTO**（application 层类型），不返回领域类型
- `PageResult<T>` 是框架级分页容器（common-contract，与 `PageableQuery` 同居契约层），业务在 application/infrastructure 用它，消费方从契约直接拿到分页元数据
- 写侧 `OrderRepository`（domain 层）只保留聚合生命周期，读侧不经过它

### 读侧业务判断放哪？

**读侧没有业务判断。** 业务规则只在写侧（领域聚合根）计算并物化：

| 场景 | 落点 | 说明 |
|------|------|------|
| 纯投影读（列表/字段展示） | infra 层 PO → 读 DTO 直投 | 读侧唯一形态 |
| 需要派生值（如「是否可取消」） | **写侧聚合根计算 → 物化到 PO 列** | 读侧只投影物化后的值 |

若某"读"需要现算业务逻辑，那是建模信号——该计算应下沉到写侧物化（如订单创建时算出 `canCancel` 布尔列），而不是在读路径里引入领域判断。

## 6. Infrastructure — 读实现（PO → DTO 直接投影）

```java
// infrastructure/persistence/master/order/repository/application/OrderQueryRepositoryImpl.java
@Component
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    private static final JsonMapper MAPPER = new JsonMapper();
    private final OrderMapper orderMapper;

    public OrderQueryRepositoryImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public Optional<OrderViewDTO> findById(UUID id) {
        OrderPO po = orderMapper.selectById(id.toString());
        if (po == null) return Optional.empty();
        return Optional.of(toViewDTO(po));
    }

    @Override
    public PageResult<OrderViewDTO> findPage(GetOrderPageQuery query) {
        // 双通道防御钳制（1..MAX_PAGE_SIZE）：即使调用点未经 Bean Validation 也安全
        int safePageNum = query.safePageNum();
        int safePageSize = query.safePageSize();
        // 手写分页：offset 由钳制后的页码换算（long 乘法防大页码溢出）；
        // 取数 / 计数两条具名方法共享 XML 内同一 <sql> 条件片段，无运行时分页插件
        long offset = (safePageNum - 1) * (long) safePageSize;
        List<OrderPO> rows = orderMapper.selectPageByCondition(
                query.status(), query.customerId(), offset, safePageSize);
        long total = orderMapper.countByCondition(query.status(), query.customerId());
        return new PageResult<>(
                rows.stream().map(this::toViewDTO).toList(),
                total, safePageNum, safePageSize);
    }

    /** PO → 读 DTO 直接投影（不经过 domain，不 reconstitute 聚合根）。 */
    private OrderViewDTO toViewDTO(OrderPO po) {
        OrderViewDTO dto = new OrderViewDTO();
        dto.setId(po.getId());
        dto.setStatus(po.getStatus());
        dto.setItems(deserializeItems(po.getItems()));
        dto.setTotalAmount(po.getTotalAmount());
        dto.setCustomerId(po.getCustomerId());
        dto.setTrackingNumber(po.getTrackingNumber());
        dto.setCancelReason(po.getCancelReason());
        dto.setCreateAt(po.getCreateAt());
        dto.setUpdateAt(po.getUpdateAt());
        return dto;
    }
}
```

要点：
- 读实现**不经 Converter、不 reconstitute 聚合根**（教义见顶部教义块）：直接 PO → **读 DTO** 轻量投影，订单项 JSON 直接反序列化为应用层 DTO（不经过领域值对象）
- 分页 = Mapper 具名方法 + 手写 XML 双语句（LIMIT/OFFSET 取数 + 同条件 COUNT），`PageResult<T>` 隔离底层分页形态，`map()` 支持链式转换

分页双语句的声明与 SQL 文本（节选自 sample 实际实现）：

```java
// infrastructure/persistence/master/order/mybatis/mapper/OrderMapper.java（具名分页方法）
List<OrderPO> selectPageByCondition(@Param("status") String status,
                                    @Param("customerId") String customerId,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

long countByCondition(@Param("status") String status,
                      @Param("customerId") String customerId);
```

```xml
<!-- resources/mapper/order/OrderMapper.xml —— WHERE 片段共享，杜绝两条语句条件漂移 -->
<sql id="pageCondition">
    WHERE is_delete = false
    <if test="status != null">AND status = #{status}</if>
    <if test="customerId != null">AND customer_id = #{customerId}</if>
</sql>

<select id="selectPageByCondition" resultType="...mybatis.po.OrderPO">
    SELECT <include refid="columns"/>
    FROM orders.orders
    <include refid="pageCondition"/>
    ORDER BY create_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>

<select id="countByCondition" resultType="long">
    SELECT COUNT(*)
    FROM orders.orders
    <include refid="pageCondition"/>
</select>
```

## 写路径 vs 读路径对比

| 维度 | 写路径（CommandHandler） | 读路径（QueryHandler） |
|------|------------------------|----------------------|
| 事务 | `@Transactional(rollbackFor = Exception.class)` | 可省略（只读） |
| 聚合根 | 必须加载（load → 行为 → save） | 完全绕过（PO → DTO 直投） |
| 经过 domain 层 | 是（聚合根 + Repository + Assembler） | **否**（读端口直连 PO） |
| 返回类型 | 写侧 DTO（含 version） | 读侧 DTO（不含 version） |
| 依赖 | Repository + Assembler + DomainService | OrderQueryRepository（读端口） |
| 固定模式 | load → 调用行为 → save → toDTO | findById/findPage → 返回读 DTO |

## 完整文件清单（读路径涉及）

| 层 | 文件 | 职责 |
|----|------|------|
| contract | `dto/query/GetOrderQuery.java` | 单条查询 |
| contract | `dto/query/GetOrderPageQuery.java` | 分页查询 |
| contract | `dto/co/OrderCO.java` | 契约输出 |
| adapter | `rest/controller/OrderControllerImpl.java` | 协议适配 |
| application | `service/OrderAppService.java` | 聚合入口 |
| application | `handler/query/GetOrderHandler.java` | 单条查询编排 |
| application | `handler/query/GetOrderPageHandler.java` | 分页查询编排 |
| application | `repository/application/OrderQueryRepository.java` | 读端口（返回读 DTO） |
| application | `presenter/OrderViewPresenter.java` | 读 DTO → CO |
| infrastructure | `repository/application/OrderQueryRepositoryImpl.java` | 读实现（PO → 读 DTO 投影） |
| resources | `mapper/order/OrderMapper.xml` | 分页双语句 + 动态条件（LIMIT/OFFSET + COUNT） |
