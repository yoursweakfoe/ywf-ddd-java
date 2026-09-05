---
name: new-usecase
description: 为已有聚合新增写操作（Command）或读操作（Query）。当需要添加新的业务用例时使用。
---

# 新增用例

## 前置阅读

- 写操作：`docs/application/cookbook/write-path.md`
- 读操作：`docs/application/cookbook/read-path.md`
- `.agents/rules/03-coding-conventions.md`（写侧/读侧固定模式）

## 步骤（写操作）

1. **contract**：创建 `contract/{agg}/dto/{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 字段 + `@Schema` 注解
2. **contract**：在 `{Agg}Service.java` 接口新增方法签名
3. **application**：创建 `application/{agg}/handler/{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<{Action}{Agg}Command, {Agg}DTO>`
   - 固定模式：load → 行为 → save → assembler.toDTO()
   - 标注 `@Transactional(rollbackFor = Exception.class)`
4. **application**：在 `{Agg}AppService` 新增方法
   - `return {agg}Presenter.present({action}{Agg}Handler.handle(command));`
5. **adapter**：在 `{Agg}ServiceImpl` 新增方法
   - 纯透传：`return {agg}AppService.{action}(command);`
6. **domain**（如需新行为）：在聚合根新增行为方法
   - 内含显式 if-throw 校验 + 状态变迁（跨聚合联动由 Handler 同事务直调）

## 步骤（读操作）

1. **contract**：创建 `contract/{agg}/dto/Get{Xxx}Query.java`（或 `PageQuery`）
2. **contract**：在 `{Agg}Service.java` 接口新增方法签名
3. **application**：创建 `application/{agg}/handler/Get{Xxx}Handler.java`
   - 实现 `QueryHandler<Get{Xxx}Query, {Agg}DTO>`（或 `PageResult<{Agg}DTO>`）
   - 调用 Repository 读优化方法（绕过聚合根）
4. **application**：在 `{Agg}AppService` 新增方法
5. **adapter**：在 `{Agg}ServiceImpl` 新增方法
6. **domain**（如需读优化方法）：在 Repository 接口新增方法签名
7. **infrastructure**：在 RepositoryImpl 实现读优化方法（Mapper 投影 DTO）

## 验证

- [ ] 编译通过
- [ ] Handler 返回 DTO（不是 CO）
- [ ] AppService 返回 CO（通过 Presenter）
- [ ] Adapter 纯透传（无业务判断）
- [ ] 写侧 Handler 有 `@Transactional`
- [ ] 读侧不加载完整聚合根

## 变体：跨聚合 Handler

当用例涉及多个聚合协调时（如下单 = Order + Product 库存扣减）：

1. 创建 Domain Service（`domain/shared/service/{Xxx}DomainService.java`）
   - 实现 `DomainService` 标记接口，标注 `@Service` 由组件扫描自动注册（领域层允许 stereotype 注解，见 A2 规则）
   - 协调多个 Repository，修改多个聚合
2. Handler 调用 Domain Service（而非直接操作多个 Repository）
- 详见 `docs/application/cookbook/cross-aggregate.md`

## 变体：批量 Handler

当用例需要一次性处理多条记录时：

1. Command 含 `List<String> ids`（或 items）
2. Handler 返回 `List<DTO>`，标注 `@Transactional`
3. 模式：批量 load → 逐个领域行为 → `updateDomainBatch` → 批量 toDTO
4. AppService 使用 `Presenter.presentList()`
- 详见 `docs/application/cookbook/batch-operations.md`，或使用 `batch-operations` skill

## 文档同步

- 如新增了 Repository 读优化方法，更新 `docs/application/cookbook/read-path.md`
- 如新增了领域行为，更新 `docs/application/cookbook/write-path.md`
