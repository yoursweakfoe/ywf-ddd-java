---
name: ddd-review
description: DDD 架构合规审查。完成编码后自查、人工要求 review、或 PR 提交前使用。
---

# DDD 架构合规审查

## 前置阅读

- `.agents/rules/04-forbidden-patterns.md`（禁止清单法条）
- `.agents/rules/02-architecture.md`（依赖方向）
- `docs/common/common-test.md` §2（ArchUnit 规则编号表 R1-R14 / C1——教义单一事实源，检查项只引编号不复述）

## 审查清单

### 分层依赖

- [ ] 依赖方向 adapter → application → domain ← infrastructure，无跨界反向边（违反 = R1/R2/R3；infra 访问 application 仅限读端口实现 / ApplicationDTO 锚点，R1b）
- [ ] Domain 零框架**运行时**依赖（违反 = R3/R4）。唯一例外：`org.springframework.stereotype` 装配注解——DomainService 标注 `@Service` 为教义允许（R4 白名单，common-test 共享规则）
- [ ] Domain 不依赖 common-security（违反 = R6）
- [ ] Application 层不 import Mapper / PO 类（rules 04「Application 层禁止」）

### 职责边界

- [ ] Handler 不含业务规则（if-else 判断应在聚合根方法内，rules 04）
- [ ] Handler 返回 DTO 而非 CO；AppService 经 Presenter 返回 CO（rules 04「禁止 Handler 返回 CO」）
- [ ] CommandHandler.handle 标注 `@Transactional(rollbackFor = Exception.class)`；RepositoryImpl / MybatisPersistence **不**标注（违反 = R11，事务边界上收应用层 Handler）
- [ ] QueryHandler 只注入 QueryRepository 读端口，不触碰写侧 Repository、不加载聚合根（违反 = R13）
- [ ] Adapter 纯透传（无业务判断、无 Assembler/Presenter 调用，rules 04「Adapter 层禁止」）

### 持久化

- [ ] 写端口接口在 `domain/{agg}/repository/`、读端口在 `application/{agg}/repository/`；两侧实现合并同包 `infrastructure/persistence/{ds}/{agg}/repository/`（类名后缀 RepositoryImpl / QueryRepositoryImpl 区分）（违反 = R5a/R5b）
- [ ] PO 零 ORM 注解 + XML 七语句契约（schema 前缀 / version 条件 / is_delete 过滤 / existsById 恒返回 boolean）——法条见 rules 04「持久化与 SQL」，详表见 `docs/common/common-ddd.md` §2，模板见 cookbook/new-aggregate.md ⑲
- [ ] Converter.toDomain() 使用 `reconstitute()`（不走业务构造器）
- [ ] 无跨聚合共享 PO / Mapper（rules 04「Infrastructure 层禁止」）
- [ ] application/{agg}/dto/ 下 DTO 实现 `ApplicationDTO` 标记（违反 = R10a/R10b）

### 跨聚合协调

- [ ] 跨聚合联动 = DomainService / Handler 同事务直调（补偿与业务原子提交）

### 命名与包结构

- [ ] 新增文件位于正确的聚合子包内（必含子段：`handler/command|query/`、`repository/`、`adapter/rest/controller/`）
- [ ] 命名符合 rules 03「命名规范」表（Command/Query/CO/DTO/PO/Portal/Gateway）

### 异常

- [ ] 无具名领域异常类，统一 BusinessException + `{aggregate}:err.{scene}` 错误码 + 显式 if-throw（rules 03「异常策略」、rules 04「Domain 层禁止」）

### 时间与注入

- [ ] 时间字段使用 `OffsetDateTime`，经注入 `Clock` 取当前时间（禁止 `LocalDateTime`/`ZonedDateTime` 持久化，rules 04「通用禁止」）
- [ ] 依赖注入使用构造器（禁止 `@Autowired` 字段注入）
- [ ] Domain 层无 public setter（违反 = R12）

### 适配器

- [ ] web 入口为 `adapter/rest/controller/{Agg}ControllerImpl`：`@RestController` 实现契约接口 + `RestAdapter` 标记（违反 = R8a/R8b），纯透传

### 虚拟线程兼容性

- [ ] 无 `synchronized` 块/方法（pinning 风险，rules 04「虚拟线程兼容」，互斥用 `ReentrantLock`）
- [ ] 身份上下文 ThreadLocal 由框架托管（`SecurityContextHolderFilter` 统一管理），业务代码**不做**手工 finally 清理（rules 03「虚拟线程」）
- [ ] 无 Thread.sleep 用于业务等待（应使用 ScheduledExecutor / 延迟队列）

### 代码组织

- [ ] 长类使用 `// region` / `// endregion` 折叠标记按职责分组
- [ ] 领域层 version 字段为只读透传（不参与业务决策，仅供持久化层乐观锁）
- [ ] 状态转换守卫使用模式匹配 switch（穷尽性检查）

### 基础设施最小化

- [ ] 未引入当前不使用的组件、无死代码（rules 04「Infrastructure 层最小化原则」）
- [ ] common 模块依赖符合身份登记判据（rules 04「Common 模块约束」构件身份二分法：定型装配审「宣言在位 + 命运依赖被本包使用或封装」，工具库审「最小化」）

### 文档

- [ ] 相关 cookbook 文档已同步
- [ ] 如新增公开 API，common 模块文档已更新

## 审查范围指引

| 变更类型 | 重点审查维度 |
|---------|-------------|
| 新增聚合 | 分层依赖 + 职责边界 + 持久化 + 命名包结构 + 文档（全量） |
| 新增用例 | 职责边界 + 异常 + 命名包结构 |
| 新增 Portal/Gateway | 分层依赖 + 持久化 + 基础设施最小化 |
| 修改 common 模块 | 基础设施最小化 + 文档 |
| 修改配置/部署 | 基础设施最小化 |

## 输出格式

审查结果按严重度分级输出：FAIL（必须修复）/ WARN（建议修复）/ PASS。

```
PASS: N items
WARN: (list with fix suggestions)
FAIL: (list with citation：ArchUnit 编号见 docs/common/common-test.md §2，法条见 .agents/rules/04-forbidden-patterns.md)
```
