# ADR-0007 持久化手写 XML SQL 全面接管，移除 MyBatis-Plus

**Status**: Accepted（2026-09）
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0007（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

本仓一等设计目标是「AI 与人共同可理解的全链路上下文」——`ADR-0003-ddd-manual-conversion-no-mapstruct`（旧 common-ddd §ADR-0004）拒绝 MapStruct 的同源论证（AI 辅助下手写模板成本归零，生成器的认知负担仍在）在此同样适用。MyBatis-Plus 的 Wrapper 动态生成与拦截器织入意味着**真正执行的 SQL 不在代码库里**：数据链路从 domain 追到 Repository，再追到 Wrapper / 插件的 SQL 拼装即断。当时 MyBatis-Plus 已被严格圈禁在 infrastructure（domain / application / adapter / contract 四层零命中，ArchUnit 守护在位），但圈禁属「他律」——可剥离性应由架构实际验证而非仅靠纪律。

## Decision Drivers

- 全链路 SQL 可见性（可 grep、可 review、可被 AI 直接引用）
- 可剥离性需由架构实际验证，而非仅靠纪律圈禁
- 依赖树纯净与 PO 零 ORM 注解的边界整洁

## Considered Options

- 保留 MyBatis-Plus（继续圈禁在 infrastructure）
- 切换为纯 MyBatis + 每聚合手写 XML SQL（选定）

## Decision Outcome

切换为纯 MyBatis（mybatis-spring-boot-starter 4.1.0，配套 Boot 4.1.0 / mybatis 3.5.19 / mybatis-spring 4.1.0），每聚合手写 XML SQL 全量接管；MyBatis-Plus 及其 SQL 解析器全部从依赖树移除。执行中落定的五个分支结果：

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

## Consequences

- 正面：每条真正执行的 SQL 都在仓库里（可 grep、可 review、可被 AI 直接引用）；运行时插件栈归零，行为与 SQL 文本一一对应；依赖树纯净（仅 `org.mybatis` 系）；PO 零 ORM 注解，domain / infrastructure 边界更干净
- 成本：每聚合新增约 80–100 行手写 XML（通用 7 条 + 业务查询）；逻辑删除过滤、版本条件由「每语句一条 `AND`」保证，漏写风险由 XML 评审 checklist + 行为等价测试承接（防超卖并发测试为关键证人）
- 守护：ArchUnit R15（`DddArchitectureRules.MYBATIS_PLUS_BANNED`，2026-09 后续版本已删除该规则、编号作废——理由「规则库不为项目选择不用的库立特别法」，论证见 common-test.md 规则集类头变更记录；本 ADR 按冻结教义保留原文）全仓禁入 `com.baomidou..` 代码依赖，防回归；dynamic-datasource 仅存于 common-ddd test scope（兼容验证），永不成为任何层代码依赖

## Confirmation

机械背书（行为等价证人）：
- 真实例（sample）：`OptimisticLockConcurrencyTest.concurrentOrders_optimisticLock_preventsOversell`（原记明示「防超卖并发测试为关键证人」）
- 框架：`MybatisPersistenceTest` 三分通道用例——`updateDomain_staleVersion_throwsOptimisticLockConflict` / `updateDomain_entityGone_throwsNotFoundSemantics_notConflict` / `saveDomain_insertZeroRows_throwsSilentWriteLoss` / `removeDomainById_notExists_throwsSilentWriteLoss`；`MybatisDddAutoConfigurationTest`
- 守护现状：原 R15 `MYBATIS_PLUS_BANNED` 已删除作废（见上「守护」段），现靠依赖树检查（`mvn dependency:tree` 无 MyBatis-Plus 相关构件）+ XML 评审 checklist，非机械规则
- 原记锚点：`MybatisPersistence` / `DddMapper` / `AuditFieldFiller`（§2 仓储支撑）；sample PO 零注解 + `resources/mapper/` 手写 XML；`mybatis.*` 配置命名空间
