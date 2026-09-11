# common-ddd

DDD 战术框架：领域建模基类、CQRS 应用层契约、MyBatis 仓储支撑（手写 XML SQL）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

本模块给业务服务提供 DDD 战术层的通用构建块：聚合根/实体/值对象基类、CQRS Handler 接口、仓储支撑。面向所有采用 DDD 分层架构的业务服务，是框架的核心模块。引入后获得领域建模基类和 MyBatis 持久化支撑：`MybatisPersistence` 基类 + 审计填充自动配置，无运行时插件栈。

> 本模块只提供基类、契约接口、自动装配三类东西，不含任何业务模型。ID 生成、序列化策略、跨服务通信均由业务侧自行决定。

## 2. 核心能力

### 领域建模基类

| 类 | 职责 |
|----|------|
| `Entity<ID>` | 实体基类。`entityEquals()` 基于 ID 判等；不持有 id/version 字段，子类自由声明 |
| `AggregateRoot<ID>` | 聚合根基类。`validate()` 是不变量校验模板，save/update 持久化前由仓储自动调用 |
| `ValueObject` | 值对象标记接口，推荐用 Java record 实现 |
| `Identifiable<ID>` | 标识接口，约束 `getId()` |
| `DomainService` | 领域服务标记接口，用于跨聚合协调的无状态服务 |
| `Factory<T>` | 领域工厂标记接口 |
| `Policy<C>` | 可插拔领域规则接口，`isApplicable(C)` 判断适用性 |
| `Portal` | 外部资源访问标记接口：Domain 定义 XxxPortal，Infrastructure 实现 XxxGateway |
| `DomainEvent` | 领域事件标记接口（`domain/event/`），表达「领域已发生的事实」，仅进程内消费；跨边界用契约层 `IntegrationEvent` |

### 事件角色标记（词汇，非机制）

框架为事件协作定义了「2 种事件 × 2 个方向」的角色词汇，全部是空标记接口。框架不内建任何发布、订阅、投递、去重机制，业务需要时实现对应标记、给身份定型。进程内路线通常是 Spring 的 `ApplicationEventPublisher` + `@EventListener`；跨服务路线是业务自持的消息中间件。

| 标记 | 位置 | 角色定型（实现方承担，框架不执行） |
|------|------|------|
| `DomainEventPublisher` | `application/event/publisher/` | 领域事件进程内发布的身份定型。典型实现是对 Spring `ApplicationEventPublisher` 的薄包装，实现归业务 |
| `IntegrationEventPublisher` | `application/event/publisher/` | 把领域事实翻译为集成事件并出站的身份定型。可靠性策略归业务：直发 / 本地消息表 / 事务消息 |
| `DomainEventSubscriber` | `application/event/subscriber/` | 进程内领域事件接收、域内反应薄编排的身份定型 |
| `IntegrationEventSubscriber` | `adapter/event/subscriber/` | 外部集成事件入站消费的身份定型。与 REST / 定时任务入口同构的 driving adapter，消费端幂等归业务 |

### CQRS Handler 接口

| 标记接口（common-contract） | Handler（本包） | 语义 | 返回值 |
|---|---|---|---|
| `Command` | `CommandHandler<C, R>` | 请做这件事 | **R** |
| `Query` | `QueryHandler<Q, R>` | 请给我这个 | **R** |
| `PageableQuery` | `QueryHandler<Q, PageResult<R>>` | 给我一页 | **PageResult&lt;R&gt;** |

`PageResult<T>` 是框架级分页容器（record），定义在 **contract 层**，与 `PageableQuery` 同居 `dto/query`。它隔离基础设施的分页实现：手写 XML 的 LIMIT/OFFSET 取数 + COUNT 计数，两条语句。`map()` 支持逐层转换。服务端 application 层（读端口 / Handler / AppService）与 infrastructure 层（读实现装填）均使用它。消费方从 common-contract 直接拿到分页元数据：records / total / pageNum / pageSize。

`ApplicationService` 是 application 层聚合协调入口的**标记接口**，位于 `common-ddd/application/service/`。业务侧 `XxxAppService` 实现它，与 domain 层 `DomainService` 标记对偶：一个做应用编排，一个做领域协调。业务类名沿用缩写 `XxxAppService`，`App` 即 Application 的缩写，只为类名简洁；标记接口保持全名语义。

Adapter 层入口同样以空标记定型角色：

- `RestAdapter`（`common-ddd/adapter/rest/controller/`）：REST 入口适配器标记。业务 `XxxControllerImpl` 在实现 contract 的 `XxxController` 契约接口之外再实现它。分工是：contract 接口承载 HTTP 面，标记声明「adapter 层 REST 入口」身份，供 ArchUnit 识别。标记刻意不命名 `Controller`，避免与 contract 契约接口、Spring `@Controller` 混淆。同类标记还有 `ScheduledAdapter`（`adapter/task/scheduler/`），定时任务入口。二者构成对称的两级包结构：一级按协议分，二级按角色分

application 层读端口同样以空标记定型。`QueryRepository`（`common-ddd/application/repository/`）与 domain 层写侧 `Repository`（聚合生命周期五方法契约）对偶：读端口绕过聚合，做 PO → 读 DTO 投影，方法签名自由，条件字段业务专属。标记身份供 ArchUnit 识别，是 R1b 读端口白名单锚点和 R13 读写隔离的落点。业务读端口接口按聚合放在自己的 `application/{agg}/repository/` 下。

### 对象转换

| 接口 | 层 | 方向 |
|------|----|------|
| `BasicAssembler<Domain, DTO>` | 应用层 | DTO ↔ Domain |
| `BasicConverter<Domain, PO>` | 基础设施层 | Domain ↔ PO |
| `BasicPresenter<DTO, CO>` | 应用层 | DTO → CO（单向） |

三者均为普通 `@Component` 类，逐字段显式赋值，不使用代码生成器。**最小契约原则**：`BasicAssembler` 仅声明 `toDomain`/`toDTO`，`BasicConverter` 仅声明 `toDomain`/`toPO`，两者另带 List/Set 集合委托的 default 方法；都不定义增量更新方法。需要增量合并的实现类自行声明普通方法，如 `updatePO` 合并业务字段。富领域模型因此没有「不支持也要写 throw」的样板，其 `toDomain` 走 `reconstitute()` 静态工厂。

被转换的 `DTO` 由 `ApplicationDTO` **空标记接口**定型，位于 `common-ddd/application/dto/`。业务顶层 DTO 类实现它：写侧 `XxxDTO`，读侧 `XxxViewDTO`。它与 contract 层对外 `CO` 标记对偶：DTO 是内部视图，可含 version/审计字段；CO 是经 Presenter 清洗后对外暴露的形态。嵌套 DTO（如 `OrderDTO.OrderItemDTO`）随外层定型，不重复标记。

### 仓储支撑（MybatisPersistence）

`MybatisPersistence` 组合持有业务 Mapper，即 `DddMapper<PO>` 的扩展接口。直接操作 PO 的底层方法不泄漏为公开 API，封装写侧「load → 行为 → save」链路：

- `saveDomain` / `updateDomain`：持久化前自动 `validate()` 校验聚合根不变量，再经 `AuditFieldFiller` 显式填充审计字段
- `removeDomain` / `removeDomains`：传实体删除，内部提取 ID，走 `removeDomainById` / `removeDomainByIds`，同一严格/宽松语义
- `removeDomainById` / `removeDomainByIds`：按 ID 删除。`removeDomainById` 为 STRICT：按存在的 ID 删除却 0 命中即抛 `SilentWriteLossException`。`removeDomainByIds` 为 BEST_EFFORT：部分未命中静默跳过，整批 0 命中才抛 `SilentWriteLossException`，与 `removeDomains` 一致
- `findDomainById` / `findDomainsByIds` / `existsDomainById`：写侧加载聚合，供 load → 行为 → save 链路使用
- **0 影响行三分通道**：UPDATE 影响 0 行时，基类先做存在性探测再分类。实体仍在：抛 `OptimisticLockConflictException`，版本被并发推进，**可重试**；该类继承 `IllegalStateException`，HTTP 409。实体已消失：抛普通 `IllegalStateException`，并发删除属业务竞态，重试无意义，HTTP 409。INSERT / DELETE 影响 0 行：抛 `SilentWriteLossException`，即静默写丢失。合法语句 0 行属基础设施级事故信号而非业务冲突：**勿重试**、需人工介入。它刻意不继承 ISE，避免混入 409/WARN 通道躲过告警；HTTP 500 + ERROR。三异常均在 common-exception 的 `exception/type` 包。完整异常 → HTTP 映射表见 `common-exception.md` §2
- 业务唯一键单条查询由子类以具名 Mapper 方法实现，语义是普通 selectOne；基类不设通用条件查询
- **事务边界上收**：本类不声明 `@Transactional`，事务由应用层 Handler 控制，批量原子性由调用方包裹事务保证

#### DddMapper<PO> ：通用语句契约（每聚合手写 XML 七条）

每个聚合的业务 Mapper `extends DddMapper<XxxPO>`，配一份**手写 XML**。namespace 为业务 Mapper 全限定名；泛型继承来的方法按子接口 namespace 解析，无跨 namespace 共享。全部 ORM 语义由 SQL 文本自身承担，可见、可 grep、可 review：

| 语句 | XML 手写语义 |
|---|---|
| `insert` | 枚举全部业务列：业务铸造 ID 显式传参、`version` 写字面量 0、**不枚举**逻辑删除列，靠 DB 默认值 |
| `updateById` | **全量 UPDATE** + `SET version = version + 1` + `WHERE id = #{id} AND version = #{version} AND is_deleted = false`。版本条件由 SQL 文本携带，无运行时拦截器；无版本列的聚合省略该条件即可 |
| `selectById` / `selectByIds` | 查询列 + `AND is_deleted = false` 显式过滤；批量为 `foreach` IN |
| `deleteById` / `deleteByIds` | 逻辑删除聚合 = `UPDATE SET is_deleted = true, updated_at = #{now}`，操作人列以 `<if test="updatedBy != null">` 守卫；物理删除聚合 = `DELETE`。审计参数由基类传入，是否消费由该聚合的 XML 决定 |
| `existsById` | `SELECT EXISTS(SELECT 1 ... AND is_deleted = false)`。恒返回一行 boolean，不加载完整行；冲突分类依赖它 |

逻辑删除列名、版本列有无、物理还是逻辑删除，都是**聚合级选择**，逐篇 XML 自行表达，不存在全局隐式约定。

#### AuditFieldFiller ：审计字段显式填充

基于 MyBatis 核心反射 `MetaObject` 按字段名读写，PO 无需任何 ORM 注解。由 `MybatisPersistence` 在 `mapper.insert` / `mapper.updateById` 前**显式调用**，触发链透明，无拦截器魔法：

- `fillInsert`：填 createdAt / updatedAt，已有值不覆盖；再填 createdBy / updatedBy，需同时满足四道宽松守卫：字段名已配置、容器存在 `CurrentUserProvider` Bean、provider 返回非 null、PO 声明该字段
- `fillUpdate`：无条件刷新 updatedAt；守卫满足时同时刷新 updatedBy
- 时间源是注入的 `Clock`。`ClockAutoConfiguration` 缺省 UTC；业务项目定义自己的 `Clock` Bean 时，框架 Bean 退位。字段名经 `AuditProperties` 可配，配置前缀 `ywf.ddd.audit.*`
- 逻辑删除的审计刷新不走本组件：基类把 `now` / `updatedBy` 作为 delete 语句的 SQL 参数传入

### MyBatis 持久化自动配置

`MybatisDddAutoConfiguration` 做装配自检，条件两道：`@ConditionalOnClass(SqlSessionFactory.class)` 挡住 classpath 剔除 starter 的情况；`@ConditionalOnBean(SqlSessionFactory.class)` 挡住容器级缺席，如排除了 `MybatisAutoConfiguration` 或自备 ORM。两路都优雅退化，不产半残 Bean。注意其语义是自检，而非「保护纯领域消费方」：common-ddd 是定型装配（opinionated starter），所有采用服务都是单 jar 全套四层，不存在纯领域消费方。装配排序在 mybatis-spring-boot-starter 的 `MybatisAutoConfiguration` 之后；`@Import(AuditFieldFiller)` + `@EnableConfigurationProperties(AuditProperties)`；`Clock` 由独立的 `ClockAutoConfiguration` 提供。

**零运行时插件**：框架不注册任何 MyBatis `Interceptor`。分页靠 XML 的 LIMIT/OFFSET 双语句，乐观锁靠 UPDATE 文本里的版本条件，均由手写 SQL 承担。防全表 UPDATE/DELETE 不设运行时拦截器：手写 XML 使每条语句可见、可 review，「无 WHERE 全表操作」是评审可见项，不是运行时黑盒。业务侧经标准 `mybatis.*` 配置命名空间自定义：`configuration.*` / `type-aliases-package` / `mapper-locations`。

## 3. 使用方式

> **严格规范在法卷**：本节正文已升入法卷 → [../../../specs/current/modules/ddd.md](../../../specs/current/modules/ddd.md)。条款、代码形状、禁令以法卷为准，本字典条目只留面向人的宽松指引。

## 4. 依赖关系

```
common-ddd → common-contract（Command / Query / CO / IntegrationEvent 标记接口）
           → common-exception（BusinessException / OptimisticLockConflictException / SilentWriteLossException）
           → mybatis-spring-boot-starter 4.1.0（Boot 4.1.0 / mybatis 3.5.19 / mybatis-spring 4.1.0）

（测试栈已全数迁出：一切 common 测试住试验场 common-packages-integration-test，法卷 TC-10；
 dynamic-datasource / PG 驱动 / fixture 均随迁，本模块 POM 零测试依赖。）
```

依赖树纯净：仅 `org.mybatis` 系，无任何 ORM 增强框架或其 SQL 解析器的传递依赖。

## 5. 设计原则

- **对偶原则（包结构镜像）**：框架支撑类的包层级，与业务使用它的层级对齐。业务在 domain 层用的放 `common-ddd/domain`：`AggregateRoot`、`Repository`、`DomainService`。业务在 application 层用的放 `common-ddd/application`：`QueryHandler`、`BasicAssembler`、`ApplicationService`、`ApplicationDTO`。业务在 adapter 层用的放 `common-ddd/adapter`：`RestAdapter`、`ScheduledAdapter`。业务在 infrastructure 层用的放 `common-ddd/infrastructure`：`MybatisPersistence`、`BasicConverter`。`PageResult`/`PageableQuery` 放 `common-contract/dto/query`，契约层定位论证见 §2。
- **基类不绑定 ID 类型**：`Entity<ID>` / `AggregateRoot<ID>` 泛型化，子类自由声明 UUID / Long / String
- **基类不持有 id/version 字段**：子类按业务需要自行声明，避免继承污染
- **全量 UPDATE**：不做脏检查，保证 `update_time` 审计字段始终刷新
- **SQL 文本即契约**：每条执行的语句都在仓库里（手写 XML），无动态生成、无运行时织入
- **`@ConditionalOnMissingBean`**：`Clock` 等平台级 Bean 允许业务项目定义自己的 Bean 覆盖，框架 Bean 退位。条件是 `@Bean` 方法级的，不是类级整体退位

## 6. 设计决策（已迁出）

> 本模块的历史决策日志已随案卷清理归零，旧编号制已废。当时的裁决快照在封存案卷 §裁决记录，今天仍成立的论证在 `docs/explanation/`。地图只留指针，本区不维护决策正文。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：聚合根 ID 自动生成策略 | ID 生成与业务强相关，由子类构造器自行决定 |
| 边界：脏检查 / 变更追踪 | 已由全量 UPDATE 策略覆盖 |
| 边界：Specification 模式 | 采纳为纯接口，是可选工具：领域规则用 and/or/not 组合表达，供复杂校验场景。查询过滤用业务 Mapper 具名方法 + 手写 XML 动态条件 `<if>`；简单校验仍用聚合根 if-throw |
