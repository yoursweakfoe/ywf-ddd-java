---
name: batch-operations
description: 为已有聚合新增批量写操作（批量 Command + 批量 Handler，三种失败策略选档），照 batch-write 法卷形状施工。当需要一次性处理多条记录时使用。
---

# 新增批量操作

## 前置阅读

- `knowledge/specs/current/patterns/batch-write.md`（批量写法卷：BW-1~BW-7 条款 + §2 规范形状，施工唯一权威——本技能只载工序，形状不复述）
- `knowledge/docs/how-to/batch-operations.md`（设计卡：该不该用、原子性三档位选型、批大小归谁管、边界代价）
- `knowledge/specs/current/patterns/write-chain.md`（写侧四拍链 WC-2/WC-3/WC-4/WC-9——批量形态即其一扩多的展开）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立三件套（模板在 `knowledge/specs/changes/_template/`，首案时开册、目录未立属正常）：proposal（why/what/不做）→ spec-delta（对 current 的 ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（归属法卷 §4）。

## 步骤

1. **选型**：按法卷 §2.4 选择表定原子性档位——全批回滚（§2.2 形态）/ 跳过失败项返回结果（§2.3 形态）/ 逐条独立（无事务）；档位决定 Handler 是否标 `@Transactional`（见第 4 步）
2. **contract**：创建 `contract/{agg}/dto/command/Batch{Action}{Agg}Command.java`，形状照法卷 §2.1——`Command` 标记接口 + ID 集合契约边界一次定型（`List<UUID>`，禁降级 String 后手工 `fromString`，BW-2）
3. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名（REST 映射注解同处声明，WC-9）
4. **application**：创建 `application/{agg}/handler/command/Batch{Action}{Agg}Handler.java`，照法卷 §2.2（全批原子）或 §2.3（部分失败）形状施工，两形态互斥选用（BW-6）
   - 实现 `CommandHandler<Batch{Action}{Agg}Command, List<{Agg}DTO>>`
   - 全批原子：标 `@Transactional(rollbackFor = Exception.class)`——事务边界在本层、框架批量通道刻意不标（BW-3，R11 机器强制）；部分失败/逐条独立：不标，逐条 try-catch 收集双列表返回
   - 四拍批量形态：批量 load → 逐条聚合行为（规则在聚合根内，禁绕聚合直改表，WC-3/BW-7）→ 基类批量通道 `updateDomainBatch` 落库（内部逐条 validate，BW-4）→ 批量 toDTO
   - 消费契约：批量通道 = 单事务逐条循环（非多值 SQL），建议 ≤500 条/批由调用方自行分片、框架不设护栏（BW-5，细则见 `MybatisPersistence` javadoc「消费契约」节）
5. **application**：在 `application/{agg}/service/{Agg}AppService.java` 新增方法：`return {agg}Presenter.presentList(handler.handle(command));`（`presentList` 为 `BasicPresenter` default 实现；部分失败形态呈现双列表结果 CO）
6. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增透传方法（零逻辑，WC-4）

## 验证

- [ ] Handler 返回 `List<DTO>`（不是 CO）
- [ ] AppService 经 `Presenter.presentList()` 返回 `List<CO>`（部分失败形态为双列表结果 CO）
- [ ] 全批原子形态：Handler 标 `@Transactional(rollbackFor = Exception.class)`（BW-3/R11）
- [ ] 部分失败/逐条独立形态：Handler **无** `@Transactional`（BW-6：catch 后事务语义混乱）
- [ ] ID 集合 `List<UUID>` 全程不降级 String（BW-2）
- [ ] 领域行为在聚合根内，Handler 不写 if-else 分支（WC-3）
- [ ] 落库走基类批量通道 `updateDomainBatch`/`saveDomainBatch`（BW-4：非循环单条 update；BW-1：未另发明新层）
- [ ] 预计超 500 条/批的入口有调用方分片（BW-5）
- [ ] 编译通过
