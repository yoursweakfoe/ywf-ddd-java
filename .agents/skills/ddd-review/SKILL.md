---
name: ddd-review
description: DDD 架构合规审查。完成编码后自查、人工要求 review、或 PR 提交前使用。
---

# DDD 架构合规审查

## 前置阅读

- `.agents/rules/04-forbidden-patterns.md`（禁止清单）
- `.agents/rules/02-architecture.md`（依赖方向）

## 审查清单

### 分层依赖

- [ ] Domain 层零框架注解（无 @Component / @Service / @Autowired / @TableName）
- [ ] Domain 层不 import 任何 infrastructure / application / adapter 类
- [ ] Application 层不 import Mapper / PO 类
- [ ] Adapter 层不 import Domain / Repository / Mapper

### 职责边界

- [ ] Handler 不含业务规则（if-else 判断应在聚合根方法内）
- [ ] Handler 返回 DTO（不是 CO）
- [ ] AppService 返回 CO（通过 Presenter）
- [ ] AppService 不含编排逻辑（仅委托 + 呈现）
- [ ] Adapter 纯透传（无业务判断、无 Assembler/Presenter 调用）

### 持久化

- [ ] Repository 接口在 `domain/{agg}/repository/`
- [ ] Repository 实现在 `infrastructure/persistence/{ds}/{agg}/repository/`
- [ ] PO 有 `@Version` + `@TableLogic` + `@TableName` 含 schema 前缀
- [ ] Converter.toDomain() 使用 `reconstitute()`（不走业务构造器）
- [ ] 无跨聚合共享 PO / Mapper

### 事件

- [ ] DomainEvent 所有字段 final
- [ ] registerEvent 在状态变迁之后
- [ ] EventHandler 事务注解选择正确
- [ ] 集成事件定义在 contract 模块

### 命名与包结构

- [ ] 新增文件位于正确的聚合子包内
- [ ] 命名符合规范表（Command/Query/CO/DTO/PO/Portal/Gateway）
- [ ] 无具名领域异常类

### 时间与注入

- [ ] 时间字段使用 `OffsetDateTime`（禁止 `LocalDateTime`）
- [ ] 依赖注入使用构造器（禁止 `@Autowired` 字段注入）
- [ ] Domain Service 零框架注解（Bean 注册在 `infrastructure/config/`）
- [ ] Repository 实现类的 `save()`/`update()` 标注 `@Transactional(rollbackFor = Exception.class)`（基类自调用不生效）

### 适配器

- [ ] web 入口为 `@RestController`（实现 contract 接口，spring-web 注解声明路径）
- [ ] web 入口纯透传（无 Assembler/Presenter 调用、无业务判断）

### 虚拟线程兼容性

- [ ] 无 `synchronized` 块/方法（pinning 风险）
- [ ] ThreadLocal 在 finally 中清理
- [ ] 无 Thread.sleep 用于业务等待（应使用 ScheduledExecutor / 延迟队列）

### 代码组织

- [ ] 长类使用 `// region` / `// endregion` 折叠标记按职责分组
- [ ] 领域层 version 字段为只读透传（不参与业务决策，仅供持久化层乐观锁）
- [ ] 状态转换守卫使用模式匹配 switch（穷尽性检查）

### 文档

- [ ] 相关 cookbook 文档已同步
- [ ] 如新增公开 API，common 模块文档已更新

## 输出格式

审查结果按以下格式输出：

```
PASS: N items
WARN: (list with fix suggestions)
FAIL: (list with rule citation from .agents/rules/04-forbidden-patterns.md)
```
