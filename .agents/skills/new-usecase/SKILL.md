---
name: new-usecase
description: 为已有聚合新增写操作（Command）或读操作（Query）。当需要添加新的业务用例时使用。
---

# 新增用例

## 前置阅读

- 写操作：`docs/application/cookbook/write-path.md`
- 读操作：`docs/application/cookbook/read-path.md`（读端口流程）
- `.agents/rules/03-coding-conventions.md`（写侧/读侧固定模式）

## 步骤（写操作）

1. **contract**：创建 `contract/{agg}/dto/command/{Action}{Agg}Command.java`
   - 实现 `Command` 标记接口
   - 字段 + `@Schema` / 校验注解（声明于契约数据类，`@Valid` 在契约接口触发）
2. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名（HTTP 映射注解同处声明）
3. **application**：创建 `application/{agg}/handler/command/{Action}{Agg}Handler.java`
   - 实现 `CommandHandler<{Action}{Agg}Command, {Agg}DTO>`
   - 固定模式：load → 行为 → save → assembler.toDTO()
   - 标注 `@Transactional(rollbackFor = Exception.class)`（R11 强制：事务边界在 CommandHandler.handle）
4. **application**：在 `application/{agg}/service/{Agg}AppService.java` 新增方法
   - `return {agg}Presenter.present({action}{Agg}Handler.handle(command));`
5. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增方法
   - 纯透传：`return {agg}AppService.{action}(command);`
6. **domain**（如需新行为）：在聚合根新增行为方法
   - 内含显式 if-throw 校验 + 状态变迁（跨聚合联动由 Handler 同事务直调）

## 步骤（读操作 — QueryRepository 读端口，R13）

1. **contract**：创建 `contract/{agg}/dto/query/Get{X}Query.java`（分页用 `{X}PageQuery implements PageableQuery`；输出形状变化时补 `dto/co/`）
2. **contract**：在 `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` 契约接口新增方法签名
3. **application**：读 DTO `application/{agg}/dto/{X}ViewDTO.java`（实现 `ApplicationDTO` 标记，R10a/R10b；已有可复用则跳过）
4. **application**：在 `application/{agg}/repository/{Agg}QueryRepository.java` 新增读方法签名
   - 该接口 `extends QueryRepository` 标记（读端口，方法签名自由）；不存在则新建此文件
5. **infrastructure**：在 `infrastructure/persistence/master/{agg}/repository/{Agg}QueryRepositoryImpl.java` 实现
   - Mapper 查询 → **PO 直接投影读 DTO**（`resultType` 指读 DTO 或 XML `<resultMap>`），不 reconstitute 聚合根、不经 domain Repository
6. **application**：创建 `application/{agg}/handler/query/Get{X}Handler.java`
   - 实现 `QueryHandler<Get{X}Query, {X}ViewDTO>`（或 `PageResult<{X}ViewDTO>`）
   - 只注入 `{Agg}QueryRepository`（R13：QueryHandler 禁止触碰写侧 Repository）
7. **application**：在 `{Agg}AppService` 新增方法（经 Presenter 转 `CO` / `PageResult<CO>`）
8. **adapter**：在 `adapter/rest/controller/{Agg}ControllerImpl.java` 新增透传方法

> **R13（类型锚点）**：读方法只进读端口 `application/{agg}/repository/{Agg}QueryRepository`；写端口 `domain/{agg}/repository/{Agg}Repository` 仅承载聚合生命周期。ArchUnit 以 `Repository` 类型为锚监控 QueryHandler 的依赖（读侧完全绕过 domain，见 rules 03「读侧固定模式」）。

## 验证

- [ ] 编译通过
- [ ] Handler 返回 DTO（不是 CO）
- [ ] AppService 返回 CO（通过 Presenter）
- [ ] Adapter 纯透传（无业务判断）
- [ ] 写侧 Handler 有 `@Transactional`（R11）
- [ ] 读侧 Handler 不注入写侧 Repository、不加载聚合根（R13）

## 变体：跨聚合 Handler

当用例涉及多个聚合协调时（如下单 = Order + Product 库存扣减）：

1. 创建 Domain Service（`domain/shared/service/{Xxx}DomainService.java`）
   - 实现 `DomainService` 标记接口，标注 `@Service` 由组件扫描自动注册（领域层允许 stereotype 注解，见 R4 规则）
   - 协调多个 Repository，修改多个聚合
2. Handler 调用 Domain Service（而非直接操作多个 Repository）
- 详见 `docs/application/cookbook/cross-aggregate.md`

## 变体：批量 Handler

当用例需要一次性处理多条记录时：

1. Command 含 `List<String> ids`（或 items），置于 `contract/{agg}/dto/command/`
2. Handler 返回 `List<DTO>`，标注 `@Transactional`（默认全批原子策略；部分失败策略不加，见下）
3. 模式：批量 load → 逐个领域行为 → `updateDomainBatch` → 批量 toDTO
4. AppService 使用 `Presenter.presentList()`
- 详见 `docs/application/cookbook/batch-operations.md`，或使用 `batch-operations` skill

## 文档同步

- 如新增了读端口方法，更新 `docs/application/cookbook/read-path.md`
- 如新增了领域行为，更新 `docs/application/cookbook/write-path.md`
