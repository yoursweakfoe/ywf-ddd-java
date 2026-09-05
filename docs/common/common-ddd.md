# common-ddd

DDD 战术框架 —— 领域建模基类、CQRS 应用层契约、MyBatis 仓储支撑（手写 XML SQL）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为业务服务提供 DDD 战术层的通用构建块：聚合根/实体/值对象基类、CQRS Handler 接口、仓储支撑。面向所有采用 DDD 分层架构的业务服务，是框架的核心模块。引入后获得领域建模基类 + MyBatis 持久化支撑（`MybatisPersistence` 基类 + 审计填充自动配置，零运行时插件栈）。

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

### 事件角色标记（词汇，非机制）

框架为事件协作定义了「2 种事件 × 2 个方向」的角色词汇，全部是空标记接口——**不内建任何发布、订阅、投递、去重机制**。业务需要时实现对应标记定型身份：进程内路线通常是 Spring `ApplicationEventPublisher` + `@EventListener`，跨服务路线是业务自持的消息中间件。

| 标记 | 位置 | 角色 |
|------|------|------|
| `DomainEventPublisher` | `application/event/publisher/` | 领域事件进程内发布（对 Spring 事件发布的薄包装） |
| `IntegrationEventPublisher` | `application/event/publisher/` | 领域事实翻译为集成事件并出站（可靠性策略归业务：直发 / 本地消息表 / 事务消息） |
| `DomainEventSubscriber` | `application/event/subscriber/` | 进程内领域事件接收，域内反应薄编排 |
| `IntegrationEventSubscriber` | `adapter/event/subscriber/` | 外部集成事件入站消费（与 REST / 定时任务入口同构的 driving adapter，消费端幂等归业务） |

### CQRS Handler 接口

| 标记接口（common-contract） | Handler（本包） | 语义 | 返回值 |
|---|---|---|---|
| `Command` | `CommandHandler<C, R>` | 请做这件事 | **R** |
| `Query` | `QueryHandler<Q, R>` | 请给我这个 | **R** |
| `PageableQuery` | `QueryHandler<Q, PageResult<R>>` | 给我一页 | **PageResult&lt;R&gt;** |

`PageResult<T>` 是框架级分页容器（record），定义在 **contract 层**（与 `PageableQuery` 同居 `dto/query`），隔离基础设施分页实现（手写 XML 的 LIMIT/OFFSET 取数 + COUNT 计数双语句），提供 `map()` 支持逐层转换。服务端 application（读端口 / Handler / AppService）与 infrastructure（读实现装填）均使用它，消费方从 common-contract 直接拿到分页元数据（records / total / pageNum / pageSize）。

`ApplicationService` 是 application 层聚合协调入口的**标记接口**（`common-ddd/application/service/`），业务侧 `XxxAppService` 实现之，与 domain 层 `DomainService` 标记对偶（应用编排 vs 领域协调）。业务类名沿用缩写 `XxxAppService`（`App` = Application 的缩写，仅类名简洁），标记接口保持全名语义。

Adapter 层入口同样以**空标记**定型角色：

- `RestAdapter`（`common-ddd/adapter/rest/controller/`）—— REST 入口适配器标记。业务 `XxxControllerImpl` 在实现 contract 的 `XxxController` 契约接口之外再实现之（contract 接口承载 HTTP 面，标记声明「adapter 层 REST 入口」身份，供 ArchUnit 识别）。不命名 `Controller`：与 contract 契约接口及 Spring `@Controller` 过宽/易混淆。同类标记还有 `ScheduledAdapter`（`adapter/task/scheduler/`，定时任务入口），二者构成「协议伞 / 角色」两级式的对称包结构

application 层读端口同样以空标记定型：`QueryRepository`（`common-ddd/application/repository/application/`）—— 与 domain 层写侧 `Repository`（聚合生命周期五方法契约）对偶，读端口绕过聚合做 PO → 读 DTO 投影、方法签名自由（条件字段业务专属），标记身份供 ArchUnit 识别（R1b 读端口白名单锚点、R13 读写隔离）。

### 对象转换

| 接口 | 层 | 方向 |
|------|----|------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） |

三者均为普通 `@Component` 类、逐字段显式赋值（不使用代码生成器）。**最小契约原则**：`BasicAssembler` 仅声明 `toDomain`/`toDTO`、`BasicConverter` 仅声明 `toDomain`/`toPO`（+ List/Set 集合委托 default 方法），**不定义增量更新方法**——需要增量合并的实现类自行声明普通方法（如 `updatePO` 合并业务字段），富领域模型因此无需任何「不支持也要写 throw」样板。富领域模型的 `toDomain` 走 `reconstitute()` 静态工厂。

被转换的 `DTO` 由 `ApplicationDTO` **空标记接口**（`common-ddd/application/dto/`）定型：业务顶层 DTO 类（写侧 `XxxDTO` / 读侧 `XxxViewDTO`）实现之，与 contract 层对外 `CO` 标记对偶（DTO = 内部视图可含 version/审计，CO = 经 Presenter 清洗后对外暴露）。嵌套 DTO（如 `OrderDTO.OrderItemDTO`）随外层定型，不重复标记。

### 仓储支撑（MybatisPersistence）

组合持有业务 Mapper（`DddMapper<PO>` 的扩展接口），直接操作 PO 的底层方法不泄漏为公开 API，封装写侧「load → 行为 → save」链路：

- `saveDomain` / `updateDomain` — 持久化前自动 `validate()`（聚合根不变量校验）+ 经 `AuditFieldFiller` 显式填充审计字段
- `removeDomain` / `removeDomains` — 传实体删除（内部提取 ID）；`removeDomains` 为 **BEST_EFFORT** 批量语义：不存在的 ID 静默跳过，仅当全部不存在时才抛 `IllegalStateException`
- `removeDomainById` / `removeDomainByIds` — 按 ID 删除；`removeDomainById` 为 STRICT（ID 不存在即抛 `IllegalStateException`），`removeDomainByIds` 为 BEST_EFFORT（与 `removeDomains` 一致）
- `findDomainById` / `findDomainsByIds` / `existsDomainById` — 写侧加载聚合（load → 行为 → save 链路）
- 乐观锁版本冲突 → `OptimisticLockConflictException`（继承 `IllegalStateException`，HTTP 409）——UPDATE 影响行数 0 时基类经存在性探测区分「版本冲突（可重试）」与「实体消失（重试无意义）」
- 业务唯一键单条查询由子类以**具名 Mapper 方法**实现（普通 selectOne 语义），基类不设通用条件查询
- **事务边界上收**：本类不声明 `@Transactional`，事务由应用层 Handler 控制（批量原子性由调用方包裹事务保证）

#### DddMapper<PO> —— 通用语句契约（每聚合手写 XML 七条）

每个聚合的业务 Mapper `extends DddMapper<XxxPO>`，配一份**手写 XML**（namespace = 业务 Mapper 全限定名，泛型继承方法按子接口 namespace 解析、无跨 namespace 共享）。全部 ORM 语义由 SQL 文本自身承担——可见、可 grep、可 review：

| 语句 | XML 手写语义 |
|---|---|
| `insert` | 枚举全部业务列（业务铸造 ID 显式传参、`version` 写字面量 0、**不枚举**逻辑删除列——靠 DB 默认值） |
| `updateById` | **全量 UPDATE** + `SET version = version + 1` + `WHERE id = #{id} AND version = #{version} AND is_delete = false`——版本条件由 SQL 文本携带，无运行时拦截器；无版本列的聚合省略该条件即可 |
| `selectById` / `selectByIds` | 查询列 + `AND is_delete = false` 显式过滤（批量为 `foreach` IN） |
| `deleteById` / `deleteByIds` | 逻辑删除聚合 = `UPDATE SET is_delete = true, update_at = #{now}`（操作人列以 `<if test="updatedBy != null">` 守卫）；物理删除聚合 = `DELETE`。审计参数由基类传入，是否消费由聚合 XML 决定 |
| `existsById` | `SELECT EXISTS(SELECT 1 ... AND is_delete = false)`——恒返回一行 boolean，不加载完整行（冲突分类依赖它） |

逻辑删除列名、版本列有无、物理还是逻辑删除——都是**聚合级选择**，逐篇 XML 自行表达，不存在全局隐式约定。

#### AuditFieldFiller —— 审计字段显式填充

基于 MyBatis 核心反射 `MetaObject`（按字段名读写，PO 无需任何 ORM 注解），由 `MybatisPersistence` 在 `mapper.insert` / `mapper.updateById` 前**显式调用**——触发链透明，无拦截器魔法：

- `fillInsert`：createAt / updateAt（已有值不覆盖）+ createdBy / updatedBy（四道宽松守卫：字段名已配置、容器存在 `CurrentUserProvider` Bean、provider 返回非 null、PO 声明该字段）
- `fillUpdate`：无条件刷新 updateAt +（守卫满足时）updatedBy
- 时间源 = 注入 `Clock`（`ClockAutoConfiguration` 缺省 UTC，业务 Bean 退位，见 ADR-0006）；字段名经 `AuditProperties`（`ywf.ddd.audit.*`）可配
- 逻辑删除的审计刷新不走本组件——由基类把 `now` / `updatedBy` 作为 delete 语句的 SQL 参数传入

### MyBatis 持久化自动配置

`MybatisDddAutoConfiguration`：`@ConditionalOnClass(SqlSessionFactory.class)` 门控（纯领域消费方不被强制 MyBatis 运行时）、after mybatis-spring-boot-starter 的 `MybatisAutoConfiguration` 排序、`@Import(AuditFieldFiller)` + `@EnableConfigurationProperties(AuditProperties)`；`Clock` 由独立的 `ClockAutoConfiguration` 提供。

**零运行时插件**——框架不注册任何 MyBatis `Interceptor`：分页（XML LIMIT/OFFSET 双语句）、乐观锁（UPDATE 文本的版本条件）均由手写 SQL 承担；防全表 UPDATE/DELETE 不设运行时拦截器，手写 XML 使每条语句可见、可 review，「无 WHERE 全表操作」是评审可见项而非运行时黑盒（论证见 ADR-0007）。业务侧经标准 `mybatis.*` 配置命名空间自定义（`configuration.*` / `type-aliases-package` / `mapper-locations`）。

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

### 场景 2：PO + Mapper + XML + RepositoryImpl（仓储实现）

PO 是**零 ORM 注解**的纯 `@Data` POJO——表名、主键、版本条件、逻辑删除过滤全部写在 XML 的 SQL 文本里：

```java
@Data
public class OrderPO {
    private String id;                 // 业务铸造（UUIDv7 文本），INSERT 显式传参
    private String status;
    private BigDecimal totalAmount;
    private Integer version;           // 版本条件由 UPDATE 语句文本携带
    private OffsetDateTime createAt;   // AuditFieldFiller 填充
    private OffsetDateTime updateAt;
}
```

Mapper 扩展框架契约接口，通用七条语句 + 业务查询同一篇 XML 承载：

```java
@Mapper
public interface OrderMapper extends DddMapper<OrderPO> {
    // 分页双语句 + 业务专有查询，实现在 resources/mapper/order/OrderMapper.xml
    List<OrderPO> selectPageByCondition(@Param("status") String status, @Param("offset") long offset, @Param("limit") long limit);
    long countByCondition(@Param("status") String status);
}
```

RepositoryImpl 继承 `MybatisPersistence`，构造器注入四件依赖（Mapper / Clock / AuditProperties / CurrentUserProvider）：

```java
@Component
public class OrderRepositoryImpl
        extends MybatisPersistence<OrderMapper, OrderPO, Order, UUID>
        implements OrderRepository {

    private final OrderConverter converter;

    public OrderRepositoryImpl(OrderMapper mapper,
                               OrderConverter converter,
                               Clock clock,
                               AuditProperties auditProperties,
                               ObjectProvider<CurrentUserProvider> currentUserProvider) {
        super(mapper, clock, auditProperties, currentUserProvider);
        this.converter = converter;
    }

    @Override protected BasicConverter<Order, OrderPO> getConverter() { return converter; }

    /** 领域 ID（UUID）→ PO 主键（String）；类型一致时无需覆写 */
    @Override protected Serializable toPersistenceId(UUID id) { return id.toString(); }

    @Override public Optional<Order> findById(UUID id) { return findDomainById(id); }

    @Override public void save(Order domain) { saveDomain(domain); }

    @Override public void update(Order domain) { updateDomain(domain); }

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
// application 层：读端口 extends QueryRepository 空标记，返回读 DTO（PO → DTO 直接投影，不经过 domain）
public interface OrderQueryRepository extends QueryRepository {
    PageResult<OrderViewDTO> findPage(GetOrderPageQuery query);
}

// application 层：Handler 整体传入 Query 对象——分页参数由实现侧经 safePageNum()/safePageSize() 统一钳制
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {
    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        return orderQueryRepository.findPage(query);
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
           → mybatis-spring-boot-starter 4.1.0（Boot 4.1.0 / mybatis 3.5.19 / mybatis-spring 4.1.0）
           → dynamic-datasource-spring-boot4-starter（test scope，多数据源路由兼容性验证；独立模块，非 ORM 增强的一部分）
           → h2（test scope，持久化测试的内嵌库）
```

依赖树纯净：仅 `org.mybatis` 系，无任何 ORM 增强框架或其 SQL 解析器传递依赖。

## 5. 设计原则

- **对偶原则（包结构镜像）**：框架支撑类的包层级与业务使用它的层级对齐——业务在 domain 层用（`AggregateRoot`、`Repository`、`DomainService`）→ 放 `common-ddd/domain`；业务在 application 层用（`QueryHandler`、`BasicAssembler`、`ApplicationService`、`ApplicationDTO`）→ 放 `common-ddd/application`；业务在 adapter 层用（`RestAdapter`、`ScheduledAdapter`）→ 放 `common-ddd/adapter`；业务在 infrastructure 层用（`MybatisPersistence`、`BasicConverter`）→ 放 `common-ddd/infrastructure`。`PageResult`/`PageableQuery` → 放 `common-contract/dto/query`（契约层定位论证见 §2）。
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **SQL 文本即契约**：每条执行的语句都在仓库里（手写 XML），无动态生成、无运行时织入（ADR-0007）
- **`@ConditionalOnMissingBean`**：`Clock` 等平台级 Bean 允许业务项目定义自己的 Bean 覆盖，框架配置整体退位

## 6. 设计决策

> 编号注记：ADR-0003（领域事件自动发布）已废弃移除（2026-09 事件留白决策），编号空置、不重排。

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

**决策**：选全量 UPDATE。本框架场景下脏检查收益极低且增加复杂度（需要变更追踪设施）；全量更新保证审计字段刷新——XML 的 `updateById` 语句逐列枚举，PO 由 Converter 完整装配，null 字段真实写为 NULL。

**确认**：`updateDomain` 走 `mapper.updateById` 全量 UPDATE（见各聚合 `resources/mapper/**/XxxMapper.xml`）。

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

- 状态：accepted（2026-09 补录，论证经 pgjdbc / MyBatis / 业界 ORM 一手对照调研）

**决策**：全框架统一 `java.time.OffsetDateTime`，唯一时间源 = 框架级 `Clock` Bean（`ClockAutoConfiguration` 缺省 `Clock.systemUTC()`，`@ConditionalOnMissingBean` 类级退位，业务测试以 `Clock.fixed(instant, ZoneOffset.UTC)` 覆盖）。时间类型贯穿 domain / 持久化 / 契约 / 序列化四层，时区错误是系统性风险，故收敛为一型一源。

**关键事实（三条）**：

1. **写入丢弃偏移**：`timestamptz` 被 PG 归一化为绝对瞬时、以 UTC 存储，原始偏移不保留（PG 官方文档 §8.5.3）
2. **读回恒 +00:00**：pgjdbc 二进制 / 文本路径均恒以 UTC 偏移返回——与会话时区、JVM 时区、传输模式无关；`OffsetDateTime` 亦是 pgjdbc 映射矩阵中 timestamptz 唯一双向原生类型、MyBatis 3.5.0+ 内置原生 TypeHandler、Hibernate 6 / jOOQ / Spring Data JDBC 的同一收敛选择
3. **禁用 LocalDateTime / ZonedDateTime 的原因**：ZonedDateTime 双向 `PSQLException`（驱动不支持）；LocalDateTime 写入依赖会话时区（值漂移）、读 timestamptz 抛异常；Instant 非原生（仅 `Timestamp` 桥，跨库语义漂移），不作迁移目标

**配套规则**：表达「同一瞬时」一律 `isEqual()`（`equals` 要求偏移亦相等，写读恒 UTC 后被结构性消除）；PG 分辨率 1µs，Java 纳秒精度落库必丢失，内存值与 DB 回显比较时注意；展示层禁用 `getString()` 取时间（`prepareThreshold` 后文本 / 二进制切换致显示格式不一致）；容器统一 `TZ=UTC` 纵深防御；禁止对 `OffsetDateTime` 实例加锁（value-based，与虚拟线程规则同向）。

**确认**：`ClockAutoConfiguration`（`systemUTC` 缺省）、`AuditFieldFiller`（`OffsetDateTime.now(clock)` 填充审计字段）、PO 审计列 `createAt`/`updateAt` 均为 `OffsetDateTime`。

### ADR-0007 持久化手写 XML SQL 全面接管，移除 MyBatis-Plus

- 状态：accepted（2026-09）

**背景**：本仓一等设计目标是「AI 与人共同可理解的全链路上下文」——ADR-0004 拒绝 MapStruct 的同源论证（AI 辅助下手写模板成本归零，生成器的认知负担仍在）在此同样适用。MyBatis-Plus 的 Wrapper 动态生成与拦截器织入意味着**真正执行的 SQL 不在代码库里**：数据链路从 domain 追到 Repository，再追到 Wrapper / 插件的 SQL 拼装即断。当时 MyBatis-Plus 已被严格圈禁在 infrastructure（domain / application / adapter / contract 四层零命中，ArchUnit 守护在位），但圈禁属「他律」——可剥离性应由架构实际验证而非仅靠纪律。

**决策**：切换为纯 MyBatis（mybatis-spring-boot-starter 4.1.0，配套 Boot 4.1.0 / mybatis 3.5.19 / mybatis-spring 4.1.0），每聚合手写 XML SQL 全量接管；MyBatis-Plus 及其 SQL 解析器全部从依赖树移除。执行中落定的五个分支结果：

1. **逻辑删除——保留语义，降级为聚合级选择**：`UPDATE SET is_delete = true` 置位与 `AND is_delete = false` 过滤写进每篇 XML 文本；不需要逻辑删除的聚合直接写物理 `DELETE`，基类语义不变
2. **防全表 UPDATE/DELETE 拦截器——裁撤**：手写 XML 使每条 UPDATE / DELETE 语句可见、可 grep、可 review，「无 WHERE 全表操作」从运行时黑盒风险降级为代码评审可见项；不自研替代拦截器
3. **审计填充——基类显式调用**：`AuditFieldFiller`（基于 MyBatis 核心 `MetaObject` 按字段名反射）由 `MybatisPersistence` 在写库前显式调用，替代隐式触发链；`AuditProperties` / `Clock` / `CurrentUserProvider` 四道宽松守卫语义逐条保留
4. **dynamic-datasource——保留（test scope）**：2026-09 一手调研（POM / 源码）证实它是与 MyBatis-Plus 无关的独立多数据源路由模块（对 MyBatis-Plus 仅有 dependencyManagement 条目 + 一处 `Class.forName` 反射带优雅降级；`DynamicRoutingDataSource` 直接构建于 Spring `AbstractRoutingDataSource`）。框架测试继续在其 `DynamicRoutingDataSource` 包裹下运行，作为 `MybatisPersistence` 多数据源兼容性的真实库实证；消费方按需 opt-in（用法与注意事项见 `docs/application/module-design/infrastructure.md`）。跟进项：SpEL 数据源表达式注入加固（PR #767）已合入 master 但不在 4.5.0 发布内，使用 SpEL 表达式的消费方待 4.5.1+ 发布后升级
5. **基类通用条件查询 `findDomainOneByCondition`——删除**：业务唯一键单查 / 计数由子类以**具名 Mapper 方法 + 具名 XML 语句**实现，SQL 按业务命名，基类不设条件查询通道

**论据（每项能力的接管落点，逐条对齐行为语义）**：

| 原能力 | 手写接管落点 |
|---|---|
| 通用 `insert`（非空列动态拼） | XML 枚举全部业务列；审计列由 `AuditFieldFiller` 保证非空；逻辑删除列不入 INSERT，靠 DB 默认值 |
| 按主键全列更新 + 乐观锁（注解 + 拦截器织入版本条件） | XML `SET version = version + 1 ... WHERE id = #{id} AND version = #{version} AND is_delete = false`；影响行数 0 → 基类存在性探测分类（`OptimisticLockConflictException` / `IllegalStateException`），分类链不变 |
| 逻辑删除翻译（DELETE → UPDATE 置位 + 审计刷新） | XML `UPDATE SET is_delete = true, update_at = #{now}`（`updated_by` 以 `<if>` 守卫），审计参数由基类经 Clock / `CurrentUserProvider` 传入 |
| 隐式 `is_delete = false` 过滤 | 每条 select / update / delete 语句显式携带——比隐式更可见，漏写属评审可查缺陷 |
| 类型安全条件构造器 | 具名 Mapper 方法 + XML `<if>` 动态条件 |
| 分页（运行时物理分页插件） | `selectPageByCondition` + `countByCondition` 双语句共享 `<sql>` 条件片段，`ORDER BY create_at DESC` + 数据库原生 `LIMIT / OFFSET`；`PageResult` / `PageableQuery` 契约零改动，单页上限仍由 `PageableQuery.MAX_PAGE_SIZE` 钳制 |
| 审计字段自动填充回调 | `AuditFieldFiller.fillInsert` / `fillUpdate` 显式调用（同配置 / 同时间源 / 同 SPI） |
| 表名 / 主键 / 版本 / 逻辑删除注解模型 | PO 回归纯 `@Data` POJO，全部语义入 SQL 文本 |
| 防全表攻击拦截器 + SQL 解析器 | 裁撤（决策 2），SQL 可见性 + 评审接管 |

**后果**：

- 正面：每条真正执行的 SQL 都在仓库里（可 grep、可 review、可被 AI 直接引用）；运行时插件栈归零，行为与 SQL 文本一一对应；依赖树纯净（仅 `org.mybatis` 系）；PO 零 ORM 注解，domain / infrastructure 边界更干净
- 成本：每聚合新增约 80–100 行手写 XML（通用 7 条 + 业务查询）；逻辑删除过滤、版本条件由「每语句一条 `AND`」保证，漏写风险由 XML 评审 checklist + 行为等价测试承接（防超卖并发测试为关键证人）
- 守护：ArchUnit R15（`DDDArchitectureRules.MYBATIS_PLUS_BANNED`）全仓禁入 `com.baomidou..` 代码依赖，防回归；dynamic-datasource 仅存于 common-ddd test scope（兼容验证），永不成为任何层代码依赖

**确认**：`MybatisPersistence` / `DddMapper` / `AuditFieldFiller`（§2 仓储支撑）；sample PO 零注解 + `resources/mapper/` 手写 XML；`mybatis.*` 配置命名空间；`mvn dependency:tree` 无 MyBatis-Plus 相关构件。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 全量 UPDATE 策略已覆盖 |
| 边界：Specification 模式 | 采纳为纯接口（可选工具）：领域规则 and/or/not 组合表达，供复杂校验场景；查询过滤用业务 Mapper 具名方法 + 手写 XML 动态条件（`<if>`），简单校验仍用聚合根 if-throw |
