---
name: new-usecase
description: 为已有聚合新增写操作（Command）或读操作（Query）。当需要添加新的业务用例时使用。
---

# 新增用例

## 前置阅读

- 写操作 → `knowledge/specs/current/patterns/chain/write-chain.md`（WC 系条款 + §2 形状唯一样本；同题设计卡 `knowledge/docs/how-to/write-path.md` 仅载选型叙事）
- 读操作 → `knowledge/specs/current/patterns/chain/read-chain.md`（RC 系条款 + §2 形状；同题设计卡 `knowledge/docs/how-to/read-path.md`）
- 读侧中间对象取舍 → `knowledge/specs/current/patterns/building-block/application-objects.md`（AO 系）
- 命名后缀/分页细则 → `knowledge/specs/current/patterns/discipline/coding-conventions.md`（CC 系；写/读固定模式已归两条链路法卷，本卷 §3 仅互指）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立四件套（模板在 `knowledge/specs/changes/_template/`，首案时开册、目录未立属正常）：specify（问题/验收 AC 账/不做/裁决问句）→ plan（技术裁量 + 对 current 的修卷 delta：ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks（纯清单）→ implement（执行账）。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（归属法卷 §4）。

## 步骤（写操作）

1. **contract**：创建 `contract/{agg}/dto/command/{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 字段 + `@Schema` / 校验注解（声明于契约数据类，`@Valid` 在契约接口触发）
   - 文本/数值字段带对齐 schema 列宽的输入上界（`@Size` / `@Digits`，细则 → WC-7，唯一样本在写链卷 §2）
2. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名（映射与文档注解全住契约接口 → WC-9）
3. **application**：创建 `application/{agg}/handler/command/{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<{Action}{Agg}Command, {Agg}DTO>`
   - 入口一点定型：调用任何 domain 接口前，把 Command 携入的裸 ID 经 `{Agg}Id.of(...)` 换型，其后链路全类型化；禁隐式自动转换代劳（法条写链 WC-13；决策快照 → 案卷 2026-09-typed-identifier §裁决记录）
   - 四拍链：load → 聚合行为 → save → assembler.toDTO()（→ WC-2，形状唯一样本写链卷 §2.5）
   - 标注 `@Transactional(rollbackFor = Exception.class)`（R11 机器强制：事务边界在 CommandHandler.handle）
   - 影响 0 行处置：基类 `MybatisPersistence` 已按写失败语义三分通道分类抛送，Handler 不加判 0 分支、绝不吞错（三分细则 → WC-12，取证 `MybatisPersistence.throwUpdateFailed` javadoc）
4. **application**：在 `application/{agg}/service/{Agg}AppService.java` 新增方法
   - 委托 Handler 再经 Presenter 呈现，返回 CO（→ WC-4，形状写链卷 §2.4）
5. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增方法
   - 纯透传零逻辑（→ WC-4/WC-9）
6. **domain**（如需新行为）：在聚合根新增行为方法
   - 显式 if-throw 校验 + 状态变迁封在聚合根内（→ WC-3/WC-6）；跨聚合协调见下「变体：跨聚合」

## 步骤（读操作 — 读端口绕过聚合根，R13 / RC 系）

1. **contract**：创建 `contract/{agg}/dto/query/Get{X}Query.java`（单条 = 实现 `Query`；分页 = record 实现 `PageableQuery`，命名 `{X}PageQuery` → RC-5，分页参数须显式传入无默认 → RC-3/CC-9；输出形状变化时补 `dto/co/`）
2. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名（→ WC-9）
3. **application**：读 DTO `application/{agg}/dto/{X}ViewDTO.java`（实现 `ApplicationDTO` 标记，R10a/R10b；读侧无 version → AO-5；已有可复用则跳过）
4. **application**：在 `application/{agg}/repository/{Agg}QueryRepository.java` 新增读方法签名
   - 该接口 `extends QueryRepository` 标记（读端口，方法签名自由 → RC-6）；不存在则新建此文件
   - 读端口签名维持原生值承载（契约层裸值，`{Agg}Id` 不入读侧——案卷 2026-09-typed-identifier 裁 Q1/Q6）
5. **infrastructure**：在 `infrastructure/persistence/master/{agg}/repository/{Agg}QueryRepositoryImpl.java` 实现
   - Mapper 取 PO → 实现侧 `toViewDTO` **直投读 DTO**（→ RC-1，形状唯一样本读链卷 §2.8），不 reconstitute 聚合根、不经 domain Repository
   - 分页 = 具名双语句（取数 + 计数共享 XML `<sql>` 条件片段），实现侧消费 `safePageNum()`/`safePageSize()` 钳制（→ RC-7）
6. **application**：创建 `application/{agg}/handler/query/Get{X}Handler.java`
   - 实现 `QueryHandler<Get{X}Query, {X}ViewDTO>`（或 `PageResult<{X}ViewDTO>`）；无 `@Transactional`（→ RC-9）
   - 只注入 `{Agg}QueryRepository`（R13：QueryHandler 禁止触碰写侧 Repository → RC-2）
7. **application**：在 `{Agg}AppService` 新增方法（读侧经 `{Agg}ViewPresenter` 转 `CO` / `PageResult<CO>` → RC-9/AO-6）
8. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增透传方法

> **R13（类型锚点）**：读方法只进读端口 `application/{agg}/repository/{Agg}QueryRepository`；写端口 `domain/{agg}/repository/{Agg}Repository` 仅承载聚合生命周期。ArchUnit 以 `Repository` 类型为锚监控 QueryHandler 的依赖（读侧完全绕过 domain，条款 → 读链卷 RC-1/RC-2）。

## 验证

- [ ] 编译通过
- [ ] Handler 返回 DTO（不是 CO）
- [ ] AppService 返回 CO（通过 Presenter）
- [ ] Adapter 纯透传（无业务判断）
- [ ] 写侧 Handler 有 `@Transactional`（R11）
- [ ] 读侧 Handler 不注入写侧 Repository、不加载聚合根（R13）
- [ ] 编码完成跑 `ddd-review` 技能自查（末步内置 check-docs 七校验）

## 变体：跨聚合 Handler

当用例涉及多个聚合协调时（如下单同时涉及订单聚合与库存聚合）：

1. 创建 Domain Service（`domain/shared/service/{Xxx}DomainService.java`）
   - 实现 `DomainService` 标记、标 `@Service` 组件扫描注册（→ CA-1/CA-5；R4 stereotype 豁免，本体仍纯 Java）
   - 协调多个 Repository，修改多个聚合；事务边界留在 Handler，Service 不吞事务（→ CA-2）
2. Handler 调用 Domain Service（而非直接操作多个 Repository；同事务补偿动作同步直调 → CA-3）
- 条款与形状唯一权威 → `knowledge/specs/current/patterns/collaboration/cross-aggregate.md`（CA 系）；选型叙事见同题设计卡 `knowledge/docs/how-to/cross-aggregate.md`

## 变体：批量 Handler

当用例需要一次性处理多条记录时：

1. Command 携带 `List<UUID>` ID 集合（或 items，禁降级 String → BW-2），置于 `contract/{agg}/dto/command/`
2. Handler 返回 `List<DTO>`，标注 `@Transactional`（整批原子 = 默认形态 → BW-3；部分失败为互斥独立形态 → BW-6）
3. 批量落库一律走基类通道 `updateDomainBatch`（逐条 validate、不绕聚合行为 → BW-4；大批自行分片建议 ≤500 条/批 → BW-5）
4. AppService 使用 `presentList()` 呈现
- 条款与形状唯一权威 → `knowledge/specs/current/patterns/chain/batch-write.md`（BW 系）；完整 SOP 直接用 `batch-operations` skill

## 文档同步

- 同步义务只在归档折叠那一刻发生（第 0 步）：写回 `sample-application/specs/current/<agg>.md`。how-to 设计卡与链路法卷不随业务增量跟改——若本用例引入了新的通用形状，那属框架行为变更，先在 `knowledge/specs/changes/` 立法
- 新增错误码位点（`{agg}:err.{scene}`）→ 登记唯一账本 `knowledge/docs/how-to/error-handling.md`（归属法卷 §5）
