---
name: batch-operations
description: 为已有聚合新增批量写操作（批量 Command + 批量 Handler）。当需要一次性处理多条记录时使用。
---

# 新增批量操作

## 前置阅读

- `docs/application/cookbook/batch-operations.md`（批量 Handler + 事务边界 + 三种失败策略）
- `.agents/rules/03-coding-conventions.md`（写侧固定模式）

## 步骤

1. **contract**：创建 `contract/{agg}/dto/command/Batch{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 核心字段：`List<String> ids`（或 `List<{Xxx}Item> items`）
   - `@Schema` 注解
2. **contract**：在 `contract/{agg}/adapter/rest/{Agg}Controller.java` 契约接口新增方法签名
3. **application**：创建 `application/{agg}/handler/command/Batch{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<Batch{Action}{Agg}Command, List<{Agg}DTO>>`
   - 全批原子策略时标注 `@Transactional(rollbackFor = Exception.class)`（R11）
   - 固定模式：批量 load → 逐个领域行为 → updateDomainBatch → 批量 toDTO
4. **application**：在 `application/{agg}/service/{Agg}AppService.java` 新增方法
   - `return {agg}Presenter.presentList(handler.handle(command));`
5. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增方法（纯透传）

## 失败策略选择

三种策略（全部回滚 / 跳过失败项 / 每条独立）的实现方式与适用场景表 → 见 `docs/application/cookbook/batch-operations.md` §3（canonical，含部分失败模式代码）。核心约束：只有全批原子策略标注 `@Transactional`；部分失败/逐条独立策略**不加**（catch 后事务语义混乱）。

## 验证

- [ ] Handler 返回 `List<DTO>`（不是 CO）
- [ ] AppService 通过 Presenter.presentList() 返回 `List<CO>`
- [ ] 全部回滚策略时 Handler 有 `@Transactional`
- [ ] 部分失败策略时 Handler **无** `@Transactional`
- [ ] 领域行为在聚合根内（不在 Handler 内写 if-else）
- [ ] 使用 `updateDomainBatch`（非循环 updateDomain）
- [ ] 编译通过
