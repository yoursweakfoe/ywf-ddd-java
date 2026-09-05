---
name: batch-operations
description: 为已有聚合新增批量写操作（批量 Command + 批量 Handler）。当需要一次性处理多条记录时使用。
---

# 新增批量操作

## 前置阅读

- `docs/application/cookbook/batch-operations.md`（批量 Handler + 事务边界 + 三种失败策略）
- `.agents/rules/03-coding-conventions.md`（写侧固定模式）

## 步骤

1. **contract**：创建 `contract/{agg}/dto/Batch{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 核心字段：`List<String> ids`（或 `List<{Xxx}Item> items`）
   - `@Schema` 注解
2. **contract**：在 `{Agg}Service.java` 接口新增方法签名
3. **application**：创建 `application/{agg}/handler/Batch{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<Batch{Action}{Agg}Command, List<{Agg}DTO>>`
   - 标注 `@Transactional(rollbackFor = Exception.class)`
   - 固定模式：批量 load → 逐个领域行为 → updateDomainBatch → 批量 toDTO
4. **application**：在 `{Agg}AppService` 新增方法
   - `return {agg}Presenter.presentList(handler.handle(command));`
5. **adapter**：在 `{Agg}ControllerImpl` 新增方法（纯透传）

## 失败策略选择

| 策略 | 实现 | 适用 |
|------|------|------|
| 全部成功或全部回滚（默认） | `@Transactional` + 异常传播 | 批量确认/取消 |
| 跳过失败项 | Handler 内 try-catch + 收集错误，**不加** @Transactional | 批量导入 |
| 每条独立 | 去掉 @Transactional，循环内各自 save | 批量通知/日志 |

## 验证

- [ ] Handler 返回 `List<DTO>`（不是 CO）
- [ ] AppService 通过 Presenter.presentList() 返回 `List<CO>`
- [ ] 全部回滚策略时 Handler 有 `@Transactional`
- [ ] 部分失败策略时 Handler **无** `@Transactional`
- [ ] 领域行为在聚合根内（不在 Handler 内写 if-else）
- [ ] 使用 `updateDomainBatch`（非循环 updateDomain）
- [ ] 编译通过
