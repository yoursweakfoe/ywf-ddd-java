---
name: ddd-review
description: DDD 架构合规审查。完成编码后必跑自查、人工要求 review、或 PR 提交前使用；末步跑 check-docs 防腐闸。
---

# DDD 架构合规审查

## 前置阅读

> 本技能只载流程不载法：每个检查项 = 一个检查动作 + 法卷条款编号指针，条款原文一律到卷取用（归属法 §2「skill 内零法条零模板」）。

| 读什么 | 文件 | 取什么 |
|---|---|---|
| 禁令卷 | `knowledge/specs/current/patterns/discipline/prohibitions.md` | §1~§4 四层禁止面、§5 契约、§6 持久化铁律、§7 时间与线程、§8 通用、§9 Common 登记表、§10 Git 工作法 |
| 编码公约卷 | `knowledge/specs/current/patterns/discipline/coding-conventions.md` | CC-1~CC-9；§2.1 类型后缀表；§2.2 结构映射表 |
| blueprint 卷（聚合构建宪） | `knowledge/specs/current/patterns/building-block/aggregate-blueprint.md` | §1 槽位表（①-㉓）、§2 BP 条款、§3 验收单、§5 服务骨架通式 |
| ArchUnit 编号表 | `knowledge/docs/reference/api/common-test.md` §2 | R 系 / C1 规则编号表（规则 `as()` 前缀与 `DddArchitectureRules` 源码自对账；检查项只引编号不复述） |
| 归属法卷 | `knowledge/specs/current/patterns/meta/attribution-law.md` | §2 事实归属表、§4 强制同步规则（「文档与契约」维度用） |

## 审查清单

### 分层依赖

- [ ] 依赖方向 adapter → application → domain ← infrastructure，无跨界反向边（法条 WC-1；违反 = R1/R2/R3；infra 访问 application 仅限读端口实现 / ApplicationDTO 锚点 = R1b）
- [ ] Domain 零框架**运行时**依赖（违反 = R3/R4）。唯一豁免 `org.springframework.stereotype` 装配注解（法条 禁令卷 §1；R4 白名单 `DOMAIN_IS_FRAMEWORK_NEUTRAL_EXCEPT_STEREOTYPE`；DomainService 标 `@Service` = CA-5 允许）
- [ ] Domain 不依赖 common-security、不感知认证上下文（法条 CC-8；违反 = R6）
- [ ] Handler 不使用 Mapper / PO（法条 禁令卷 §2——禁止面收在 Handler 粒度）

### 职责边界

- [ ] Handler 不写业务规则、无 if-else 业务分支（决策在聚合根内——法条 禁令卷 §2、WC-3）
- [ ] Handler 返回 DTO 而非 CO；AppService 经 Presenter 返回 CO（法条 禁令卷 §2、WC-4；Assembler/Presenter 强制分离 = WC-5/BP-9）
- [ ] `CommandHandler.handle` 标注 `@Transactional(rollbackFor = Exception.class)`；RepositoryImpl / MybatisPersistence 侧**不**标注（违反 = R11；法条 BP-10、WC-10——事务边界上收 Handler）
- [ ] QueryHandler 只注入 QueryRepository 读端口，不触碰写侧 Repository、不加载聚合根、无 `@Transactional`（违反 = R13；法条 RC-2/RC-9、BP-11）
- [ ] Adapter 纯透传：无业务判断、不越过 AppService 直调 Handler、不碰 Repository/Domain、不调 Assembler/Presenter（法条 禁令卷 §3、WC-4）

### 持久化

- [ ] 写端口接口在 `domain/{agg}/repository/`、读端口在 `application/{agg}/repository/`；两侧实现合并同包 `infrastructure/persistence/{ds}/{agg}/repository/`（RepositoryImpl / QueryRepositoryImpl 后缀区分）（违反 = R5a/R5b；槽位法条 blueprint 卷 §1 ⑮㉑⑲㉒、BP-X3）
- [ ] PO 零 ORM 注解 + XML 七语句契约——逐条对照 BP-X1/BP-X2（禁止面 禁令卷 §6；详表镜像 `knowledge/docs/reference/api/common-ddd.md` §2；XML 槽位 = blueprint 卷 §1 ⑳）
- [ ] Converter.toDomain() 使用 `reconstitute()`（不走业务构造器——法条 CC-4/BP-X2；聚合构造两扇门 = BP-8）
- [ ] 无跨聚合共享 PO / Mapper（法条 禁令卷 §4；聚合自包含 → blueprint 卷 §5）
- [ ] `application/{agg}/dto/` 下 DTO 实现 `ApplicationDTO` 标记（违反 = R10a/R10b；法条 blueprint 卷 §3 验收单）

### 跨聚合协调

- [ ] 跨聚合联动 = DomainService / Handler 同事务直调、补偿与业务原子提交，禁异步化「尽力而为」（法条 CA-3；载体位置 `domain/shared/service/` = CA-1/CA-2；批量加载防 N+1 = CA-7；禁止面 禁令卷 §1「跨聚合直接修改对方内部状态」）

### 命名与包结构

- [ ] 新增文件位于正确的聚合子包内（必含子段：`handler/command|query/`、`repository/`、`adapter/rest/controller/`——槽位法条 blueprint 卷 §1，Handler 定位 = CC-1）
- [ ] 命名符合 CC-2 后缀制（法条：编码公约卷 §2.1 类型后缀表——Command/Query/CO/DTO/PO/Portal/Gateway，禁自创第六种载体）
- [ ] 聚合根身份槽为专属终类型 `{Agg}Id`（record implements `Identifier`，落位 `domain/{agg}/id/`），且 `Repository` 端口 ID 槽与根槽一致；契约 CQE/CO、PO、读侧端口与读 DTO、子实体 PK 维持原生值（违反 = R15；法条 blueprint 卷 §1 ⑫ / §2 BP-13、BP-15；决策快照 → 案卷 2026-09-typed-identifier §裁决记录）

### 异常

- [ ] 无具名领域异常类、domain 层不设 `exception/` 包；统一 `BusinessException` + `{aggregate}:err.{scene}` 位点 + 聚合行为方法内显式 if-throw（法条 EV-1/EV-2/EV-6、WC-6；禁止面 禁令卷 §1；全仓 key 登记账本 `knowledge/docs/how-to/error-handling.md`）

### 时间与注入

- [ ] 持久化与领域时间一律 `OffsetDateTime`，当前时间经注入 `Clock` 取、禁无参 `now()`（法条 CC-5；禁止面 禁令卷 §7——`LocalDateTime`/`ZonedDateTime` 系持久化禁入）
- [ ] Bean 依赖用构造器注入（各法卷规范形状统一样本姿势；单测类禁 `@Autowired` 字段注入 = TC-4）
- [ ] Domain 层无 public setter、状态变迁只经行为方法（违反 = R12；法条 禁令卷 §1）

### 适配器

- [ ] web 入口为 `adapter/rest/controller/{Agg}ControllerImpl`：`@RestController` 实现契约接口 + `RestAdapter` 标记（违反 = R8a/R8b），纯透传零逻辑；HTTP 映射与文档注解全住契约接口（法条 BP-4、WC-9）

### 虚拟线程兼容性

- [ ] 生产代码无 `synchronized` 块/方法（pinning——法条 禁令卷 §7；互斥用 `ReentrantLock`）
- [ ] 身份上下文由 Spring Security 链托管（`SecurityContextHolder` 读取，业务侧经 `SecurityUtil` 且只许 Application/Adapter 层用 = CC-8），业务代码**不做**手工 finally 清理（法条 禁令卷 §7）
- [ ] 乐观锁冲突重试 = Handler 包装器 + 指数退避、每次重试重新 load，禁零退避热重试（法条 OL-3/OL-5；`Thread.sleep` 作退避等待系虚拟线程下明示合法——旧「业务等待禁 sleep」一刀切已废）

### 代码组织

- [ ] 长类使用 `// region` / `// endregion` 折叠标记按职责分组（现行源码实践、未入法典 → 至多 WARN）
- [ ] `version` 由框架 SQL 维护，业务层禁手工读写版本号、不参与业务决策（法条 OL-4；CO 不暴露 = BP-9）
- [ ] 状态转换守卫用 JDK 21 穷尽性模式匹配 switch，新增枚举值编译器强制处理（框架不强制、留业务侧用——论证账本 `knowledge/docs/explanation/theory-map.md`「模式匹配 switch」行；规则收口 = BP-12）

### 基础设施最小化

- [ ] 未引入当前不使用的组件、无死代码（注释块/TODO-restore/空实现）、无 `System.out` 替代 SLF4J（法条 禁令卷 §4「最小充分原则」）
- [ ] common 模块依赖符合身份登记判据（法条 禁令卷 §9：先查登记表——定型装配审「自我宣言在位 + 命运依赖被本包使用或封装 + 消费方经公开 API」，工具库审「最小化」；exclusions 卫生集中制：排除只写两个策略 pom，子 pom 声明处零 exclusions）

### 文档与契约

- [ ] 新行为已走所属契约区 `changes/<slug>/` 四件套并归档折叠（框架 → `knowledge/specs/`、示例业务 → `sample-application/specs/`），相关 how-to / explanation 已随动、折叠之外无孤儿债（法条 归属法卷 §4——归档折叠是同步义务唯一发生时点）
- [ ] 锚点抽查（实施内容基准律 → 归属法卷 §2 表注，本条不复述）：本次涉及的 skill/docs 中每处形状性表述（包位/注解/签名/文件槽位）逐一验证**已挂法卷节号且翻卷可解析**；锚不到 = 法卷覆盖缺口，走 `changes/` 立案补法、不得就地自造；纯工序句免检
- [ ] 新增/变更公开 API：`knowledge/docs/reference/api/` 对应模块文档已同 PR 随更（法条 禁令卷 §9「新增 common 模块必附文档」行；异常→HTTP 映射表另有 C5 对账 = EV-5）
- [ ] 新卷归摊：本次若新增 `specs/current/patterns/` 法卷，逐卷以摊卡验证问句裁决归摊（法条 → `knowledge/specs/current/patterns/meta/pattern-taxonomy.md` meta-1/2/5：恰落一摊、路径即名册、编号取摊名式）；任何摊都裁不进 = 摊地图缺口，回 `changes/` 先改摊地图
- [ ] `powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-docs.ps1` 退出码 0（非零即 FAIL；校验清单以工具输出为准不在本复述，变红裁决纪律——文档错修文档、工具误伤修工具——见 `knowledge/docs/reference/doc-guards.md`；新增 C3 豁免须 PR 评审写理由进 `knowledge/scripts/check-docs.whitelist.txt`，只删不增）
- [ ] `powershell -NoProfile -ExecutionPolicy Bypass -File knowledge/scripts/check-diagrams.ps1` 退出码 0（触及 diagrams/ 图源或产物时必跑；三方哈希对账语义 → doc-guards 图管线节）

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
FAIL: (list with citation：ArchUnit 编号见 knowledge/docs/reference/api/common-test.md §2 规则清单表，法条锚点 = knowledge/specs/current/ 对应法卷条款编号（禁令卷 §n / CC-n / BP-n / WC-n / RC-n / OL-n / CA-n / EV-n / TC-n）)
```
