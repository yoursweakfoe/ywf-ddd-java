# common-ddd

DDD 战术框架 —— 领域建模基类、CQRS 应用层契约、MyBatis-Plus 仓储支撑。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为业务服务提供 DDD 战术层的通用构建块：聚合根/实体/值对象基类、CQRS Handler 接口、仓储支撑。面向所有采用 DDD 分层架构的业务服务，是框架的核心模块。引入后获得领域建模基类 + MyBatis-Plus 插件自动配置。

> 只提供「基类 + 契约接口 + 自动装配」，不包含任何业务模型。ID 生成、序列化策略、跨服务通信均由业务侧自行决定。

## 2. 核心能力

### 领域建模基类

| 类 | 职责 |
|----|------|
| `Entity<ID>` | 实体基类，`entityEquals()` 基于 ID 判等；不持有 id/version 字段，子类自由声明 |
| `AggregateRoot<ID>` | 聚合根基类，`validate()` 不变量校验模板（save/update 持久化前由仓储自动调用） |
| `ValueObject` | 值对象标记接口，推荐 Java record 实现 |
| `Identifiable<ID>` | 标识接口，约束 `getId()` |
| `DomainService` | 领域服务标记接口（跨聚合协调的无状态服务） |
| `Factory<T>` | 领域工厂标记接口 |
| `Policy<C>` | 可插拔领域规则接口，`isApplicable(C)` 适用性判断 |
| `Portal` | 外部资源访问标记接口（Domain 定义 XxxPortal，Infrastructure 实现 XxxGateway） |
| `DomainEvent` | 领域事件标记接口（`domain/event/`）—— 表达「领域已发生的事实」，仅进程内消费；跨边界用契约层 `IntegrationEvent` |

### CQRS Handler 接口

| 标记接口（common-contract） | Handler（本包） | 语义 | 返回值 |
|---|---|---|---|
| `Command` | `CommandHandler<C, R>` | 请做这件事 | **R** |
| `Query` | `QueryHandler<Q, R>` | 请给我这个 | **R** |
| `PageableQuery` | `QueryHandler<Q, PageResult<R>>` | 给我一页 | **PageResult&lt;R&gt;** |

`PageResult<T>` 是框架级分页容器（record），定义在 **contract 层**（与 `PageableQuery` 同居 `dto/query`），隔离 MyBatis-Plus `Page<PO>`，提供 `map()` 支持逐层转换。服务端 application（读端口 / Handler / AppService）与 infrastructure（读实现装填）均使用它，消费方从 common-contract 直接拿到分页元数据（records / total / pageNum / pageSize）。

`ApplicationService` 是 application 层聚合协调入口的**标记接口**（`common-ddd/application/service/`），业务侧 `XxxAppService` 实现之，与 domain 层 `DomainService` 标记对偶（应用编排 vs 领域协调）。业务类名沿用缩写 `XxxAppService`（`App` = Application 的缩写，仅类名简洁），标记接口保持全名语义。

Adapter 层入口同样以**空标记**定型角色：

- `RestAdapter`（`common-ddd/adapter/rest/controller/`）—— REST 入口适配器标记。业务 `XxxControllerImpl` 在实现 contract 的 `XxxController` 契约接口之外再实现之（contract 接口承载 HTTP 面，标记声明「adapter 层 REST 入口」身份，供 ArchUnit 识别）。不命名 `Controller`：与 contract 契约接口及 Spring `@Controller` 过宽/易混淆。同类标记还有 `ScheduledAdapter`（`adapter/task/scheduler/`，定时任务入口），二者构成「协议伞 / 角色」两级式的对称包结构

### 对象转换

| 接口 | 层 | 方向 |
|------|----|------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） |

三者均为普通 `@Component` 类、逐字段显式赋值（不使用代码生成器）。**最小契约原则**：`BasicAssembler` 仅声明 `toDomain`/`toDTO`、`BasicConverter` 仅声明 `toDomain`/`toPO`（+ List/Set 集合委托 default 方法），**不定义增量更新方法**——需要增量合并的实现类自行声明普通方法（如 `updatePO` 合并业务字段），富领域模型因此无需任何「不支持也要写 throw」样板。富领域模型的 `toDomain` 走 `reconstitute()` 静态工厂。

被转换的 `DTO` 由 `ApplicationDTO` **空标记接口**（`common-ddd/application/dto/`）定型：业务顶层 DTO 类（写侧 `XxxDTO` / 读侧 `XxxViewDTO`）实现之，与 contract 层对外 `CO` 标记对偶（DTO = 内部视图可含 version/审计，CO = 经 Presenter 清洗后对外暴露）。嵌套 DTO（如 `OrderDTO.OrderItemDTO`）随外层定型，不重复标记。

### 仓储支撑（MybatisPlusPersistence）

组合持有 `BaseMapper`（不继承 ServiceImpl，避免 `save(PO)`/`updateById(PO)` 等底层 PO 直操方法泄漏为公开 API），封装：

- `saveDomain` / `updateDomain` — 持久化前自动 `validate()`（聚合根不变量校验）
- `removeDomain` / `removeDomains` — 传实体删除（内部提取 ID）；`removeDomains` 为 **BEST_EFFORT** 批量语义：不存在的 ID 静默跳过，仅当全部不存在时才抛 `IllegalStateException`
- `removeDomainById` / `removeDomainByIds` — 按 ID 删除；`removeDomainById` 为 STRICT（ID 不存在即抛 `IllegalStateException`），`removeDomainByIds` 为 BEST_EFFORT（与 `removeDomains` 一致）
- `findDomainById` / `findDomainsByIds` / `findDomainOneByCondition` — 写侧加载聚合（load → 行为 → save 链路）
- 乐观锁冲突 → `IllegalStateException`（HTTP 409）
- **事务边界上收**：本类不声明 `@Transactional`，事务由应用层 Handler 控制（批量原子性由调用方包裹事务保证）

### MyBatis-Plus 自动配置

| 拦截器 | 说明 |
|--------|------|
| PaginationInnerInterceptor | 物理分页（AUTO 方言） |
| OptimisticLockerInnerInterceptor | 乐观锁（仅 @Version 实体生效） |
| BlockAttackInnerInterceptor | 防全表 UPDATE/DELETE（始终开启） |

`@ConditionalOnMissingBean` 允许业务项目完全自定义覆盖。

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-ddd</artifactId>
</dependency>
```

### 场景 1：聚合根（状态机 + 不变量校验）

```java
public class Order extends AggregateRoot<UUID> {
    private UUID id;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private Integer version;

    /** 业务构造器（创建新订单） */
    public Order(UUID id, List<OrderItem> items) {
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
        this.totalAmount = calculateTotal();
    }

    /** 重建构造器（Converter 使用） */
    public static Order reconstitute(UUID id, OrderStatus status, List<OrderItem> items,
                                     BigDecimal totalAmount, Integer version) {
        Order order = new Order(id, items);
        order.status = status;
        order.totalAmount = totalAmount;
        order.version = version;
        return order;
    }

    @Override public UUID getId() { return id; }

    /** 支付：状态机校验 + 状态变迁 */
    public void pay() {
        if (status != OrderStatus.PENDING) {
            throw new BusinessException("order:err.invalidTransition");
        }
        this.status = OrderStatus.PAID;
    }

    /** 不变量校验（仓储 save/update 前自动调用） */
    @Override public void validate() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order:err.itemsEmpty");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("order:err.totalMustBePositive");
        }
    }
}
```

### 场景 2：RepositoryImpl（仓储实现）

```java
@Component
public class OrderRepositoryImpl
        extends MybatisPlusPersistence<OrderMapper, OrderPO, Order, UUID>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper, OrderConverter converter) {
        super(mapper);
        this.converter = converter;
    }

    @Override protected BasicConverter<Order, OrderPO> getConverter() { return converter; }

    @Override public Optional<Order> findById(UUID id) { return findDomainById(id); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void save(Order domain) { saveDomain(domain); }

    @Override @Transactional(rollbackFor = Exception.class)
    public void update(Order domain) { updateDomain(domain); }

    @Override public void deleteById(UUID id) { removeDomainById(id); }
}
```

### 场景 3：批量操作

`saveDomainBatch` / `updateDomainBatch` 语义为**单事务循环**（逐条 insert/update），非多行 VALUES SQL——每条聚合须独立 `validate()`，多行 UPDATE/INSERT 无法触发逐聚合行为；批量原子性由调用方（Handler 标 `@Transactional`）保证。

```java
repository.saveDomainBatch(List.of(order1, order2, order3));           // 批量保存
repository.updateDomainBatch(List.of(order1, order2));                // 批量更新
List<Order> orders = repository.findDomainsByIds(List.of(id1, id2));  // 批量写侧加载

order.markCancelled();
repository.removeDomain(order);                        // 实体删除（不存在则抛 IllegalStateException）
repository.removeDomainByIds(List.of(id1, id2));       // 按 ID 批量删除（BEST_EFFORT：不存在的静默跳过）
```

### 场景 4：PageResult 分页链路（读侧，绕过 domain）

```java
// application 层：读端口返回读 DTO（PO → DTO 直接投影，不经过 domain）
public interface OrderQueryRepository {
    PageResult<OrderViewDTO> findPage(String status, String customerId, int pageNum, int pageSize);
}

// application 层：Handler 直接取读 DTO
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {
    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        return orderQueryRepository.findPage(
                query.status(), query.customerId(), query.pageNum(), query.pageSize());
    }
}
```

> 读侧完全绕过 domain 层（不 reconstitute 聚合根、不建领域读模型），基础设施层实现读端口
> （infrastructure → application，写侧依赖倒置的读侧镜像），直接从 PO 投影读 DTO。
> 读侧无业务判断，派生值在写侧计算并物化到 PO 列。详见 `docs/application/cookbook/read-path.md`。

写侧完整示例见 `docs/application/cookbook/write-path.md`。

## 4. 依赖关系

```
common-ddd → common-contract（Command / Query / CO / IntegrationEvent 标记接口）
           → common-exception（BusinessException）
           → mybatis-plus-spring-boot4-starter
           → mybatis-plus-jsqlparser
           → dynamic-datasource-spring-boot4-starter（test scope，多数据源兼容性验证）
           → h2（test scope，持久化测试的内嵌库）
```

## 5. 设计原则

- **对偶原则（包结构镜像）**：框架支撑类的包层级与业务使用它的层级对齐——业务在 domain 层用（`AggregateRoot`、`Repository`、`DomainService`）→ 放 `common-ddd/domain`；业务在 application 层用（`QueryHandler`、`BasicAssembler`、`ApplicationService`、`ApplicationDTO`）→ 放 `common-ddd/application`；业务在 adapter 层用（`RestAdapter`、`ScheduledAdapter`）→ 放 `common-ddd/adapter`；业务在 infrastructure 层用（`MybatisPlusPersistence`、`BasicConverter`）→ 放 `common-ddd/infrastructure`。`PageResult`/`PageableQuery` 属契约层（分页信封是消费方可见的契约类型）→ 放 `common-contract/dto/query`。
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **`@ConditionalOnMissingBean`**：MyBatis-Plus 插件允许业务项目完全自定义覆盖

## 6. 设计决策

### ADR-0001 基类不持有 id/version 字段

- 状态：accepted

**背景**：聚合根基类是否内置 id/version 字段。

**选项**：
- 内置字段：子类少写样板，但 ID 类型（UUID/Long/业务编码）被强制统一
- 泛型化 + 不持有：子类自由声明

**决策**：选泛型化 + 不持有。ID 生成与业务强相关，由子类构造器自行决定。

**确认**：`Entity<ID>` / `AggregateRoot<ID>` 无 id/version 字段。

### ADR-0002 全量 UPDATE 而非脏检查

- 状态：accepted

**背景**：持久化采用脏检查还是全量更新。

**决策**：选全量 UPDATE。MyBatis-Plus 场景下脏检查收益极低且增加复杂度；全量更新保证审计字段刷新。

**确认**：`updateDomain` 走 MyBatis-Plus 全量 update。

### ADR-0004 对象转换纯手写，不用 MapStruct

- 状态：accepted

**背景**：Converter/Assembler/Presenter 用代码生成器还是手写。

**决策**：选手写显式映射。AI 辅助开发下手写模板成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在。聚合根重建走 reconstitute，完整性由往返测试守护。

**确认**：`BasicConverter` / `BasicAssembler` / `BasicPresenter` 无生成器依赖。

### ADR-0005 CQRS 契约：Query 纯标记

- 状态：accepted（2026-08 修订）

**背景**：Handler 接口的契约设计。

**决策**：Query 为纯标记（无泛型，避免 contract 与 internal 类型耦合，返回类型由 Service 方法签名定义）。

**确认**：`QueryHandler` / `CommandHandler` 接口签名。

### ADR-0006 时间统一 OffsetDateTime + 统一注入 Clock

- 状态：accepted（2026-09 补录，经一手源码调研论证）

**背景**：时间类型贯穿 domain / 持久化 / 契约 / 序列化四层，时区错误是系统性风险（数据漂移、排序错乱、去重漏判）。技术栈为 PostgreSQL `timestamptz` + pgjdbc + MyBatis(-Plus) + Jackson。需以一手证据锁定唯一时间类型与唯一时间源。

**决策**：全框架统一 `java.time.OffsetDateTime`；框架级 `Clock` Bean 统一注入（`ClockAutoConfiguration` 缺省 `Clock.systemUTC()`，类级 `@ConditionalOnMissingBean` 退位，业务测试以 `Clock.fixed(instant, ZoneOffset.UTC)` 覆盖）。

**论证（类型对照，证据链见各来源）**：

| Java 类型 | pgjdbc 原生绑定 | MyBatis 路径 | timestamptz 往返 | 判定 |
|---|---|---|---|---|
| **OffsetDateTime** | ✔ 官方矩阵中 timestamptz 的**唯一**双向原生类型 | ✔ 原生 `setObject`/`getObject`（MyBatis 3.5.0+） | 瞬时恒对；读回偏移恒 +00:00 | **采纳** |
| Instant | ✘ 驱动从未支持（2023 年 PR #2943 被维护者关闭未合并） | ⚠ 恒走 `java.sql.Timestamp` 桥 | 瞬时碰巧正确，非原生、跨库语义漂移 | 不作迁移目标 |
| ZonedDateTime | ✘ 双向抛 `PSQLException` | ✘ 原生调用必炸 | — | **禁用** |
| LocalDateTime | 仅 `timestamp`（无时区）列 | ✔ 原生 | 写依赖会话时区（值漂移）；读 timestamptz 抛异常 | **禁止映射 timestamptz** |

**关键事实**：

1. **偏移往返语义**：写入时偏移被丢弃（PG 归一化为绝对瞬时、以 UTC 存储，原始偏移不保留——PG 官方文档 §8.5.3）；读回时驱动**恒定返回 +00:00**（二进制路径硬编码 `OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)`，文本路径同样归一）——与会话时区、JVM 时区、传输模式全部无关。`timestamptz` + `OffsetDateTime` 的实质是「带类型纪律的绝对瞬时」
2. **MyBatis-Plus 委托链**：`MybatisConfiguration extends Configuration`，MP 零自带时间 handler，`OffsetDateTime` 字段实际走 MyBatis 原生 handler 路径
3. **业界收敛**：Hibernate 6 / jOOQ 默认 / Spring Data JDBC 均把 timestamptz 映射为 OffsetDateTime；jOOQ 虽增设 `SQLDataType.INSTANT`，作者原话 "For all practical purposes, Instant and OffsetDateTime are the same data type"
4. **现代性核查**：Java 8→25 无新时间类型（Java 23 仅增 `Instant.until`）；ThreeTen-Extra 定位是 complement（补充）非替代；Joda-Time 官方宣告 finished 并建议迁移 java.time；JDK 25 javadoc 对 OffsetDateTime 的定位即 "communicating to a database"
5. **Clock 同构论证**：`OffsetDateTime.now(systemUTC())` 产出偏移 `Z`，与 pgjdbc 读回的 `+00:00` **equals 相等**——写读往返断言稳定；`systemDefaultZone()` 则使每个非 UTC 主机制造「同瞬时不等值」地雷。JDK `Clock` javadoc 原文背书：`systemDefaultZone()` "hard codes a dependency to the default time-zone... recommended to avoid"；DI 注入 Clock 即官方推荐实践

**配套规则**：

1. **比较语义**：表达「同一瞬时」一律 `isEqual()` / `OffsetDateTime.timeLineOrder()`；`equals()` 仅作同 UTC 源值的往返断言（`equals` 要求偏移亦相等——JSR-310 著名陷阱，本框架 UTC 统一后在写读回路上被结构性消除）
2. **精度**：PG 分辨率 1µs，pgjdbc 对 >499ns 执行 `+1µs` 舍入——Java 纳秒精度必然丢失；内存值与 DB 回显值比较时注意
3. **展示层禁用 `getString()` 取时间**：`prepareThreshold`（默认 5）后结果集从文本切二进制，显示格式前后不一致，且按 JVM 客户端时区渲染
4. **纵深防御**：容器统一 `TZ=UTC`，消除 `java.sql.Timestamp` 桥路径与日志格式的宿主时区残余泄漏
5. **value-based class**：禁止对 `OffsetDateTime` 实例加锁（与虚拟线程规则同向）

**确认**：`ClockAutoConfiguration`（`systemUTC` 缺省）、`BasicAutoFillHandler`（`OffsetDateTime.now(clock)` 填充审计字段）、PO 审计列 `createAt`/`updateAt` 均为 `OffsetDateTime`。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：Specification 模式 | 采纳为纯接口（可选工具）：领域规则 and/or/not 组合表达，供复杂校验场景；查询过滤仍用 MyBatis-Plus `LambdaQueryWrapper`，简单校验仍用聚合根 if-throw |
