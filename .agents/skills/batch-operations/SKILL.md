---
name: batch-operations
description: 为已有聚合新增批量写操作（批量 Command + 批量 Handler）。当需要一次性处理多条记录时使用。
---

# 新增批量操作

## 前置阅读

- `knowledge/docs/how-to/batch-operations.md`（批量 Handler + 事务边界 + 三种失败策略）
- `.agents/rules/03-coding-conventions.md`（写侧固定模式）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立三件套（模板在 `knowledge/specs/changes/_template/`）：proposal（why/what/不做）→ spec-delta（对 current 的 ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（rules/05 §4）。
## 步骤

1. **contract**：创建 `contract/{agg}/dto/command/Batch{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 核心字段：`List<UUID> ids`（或 `List<{Xxx}Item> items`）
   - `@Schema` 注解
2. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名
3. **application**：创建 `application/{agg}/handler/command/Batch{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<Batch{Action}{Agg}Command, List<{Agg}DTO>>`
   - 全批原子策略时标注 `@Transactional(rollbackFor = Exception.class)`（R11）
   - 固定模式：批量 load → 逐个领域行为 → 经 RepositoryImpl 基类批量通道落库（`updateDomainBatch`，`MybatisPersistence` 继承的基行为，非 domain Repository 接口方法）→ 批量 toDTO
   - 消费契约（源 MybatisPersistence javadoc）：batch = 单事务逐条循环，非多行 SQL；调用方必须按 ≤500 条/批自行分片，同一条 Handler `@Transactional` 事务内完成（框架不设行数护栏）
4. **application**：在 `application/{agg}/service/{Agg}AppService.java` 新增方法
   - `return {agg}Presenter.presentList(handler.handle(command));`
5. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增方法（纯透传）

## 失败策略选择

三种策略（全部回滚 / 跳过失败项 / 每条独立）的实现方式与适用场景表 → 见 `knowledge/docs/how-to/batch-operations.md` §3（canonical，含部分失败模式代码）。核心约束：只有全批原子策略标注 `@Transactional`；部分失败/逐条独立策略**不加**（catch 后事务语义混乱）。

## 验证

- [ ] Handler 返回 `List<DTO>`（不是 CO）
- [ ] AppService 通过 Presenter.presentList() 返回 `List<CO>`
- [ ] 全部回滚策略时 Handler 有 `@Transactional`
- [ ] 部分失败策略时 Handler **无** `@Transactional`
- [ ] 领域行为在聚合根内（不在 Handler 内写 if-else）
- [ ] 使用 `updateDomainBatch`（非循环 updateDomain）
- [ ] 编译通过
