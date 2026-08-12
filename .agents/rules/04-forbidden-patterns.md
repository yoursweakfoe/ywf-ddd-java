# 04 — 禁止清单

合并各层设计文档中的"禁止"规则为统一清单。违反任何一条即为架构违规。

## Domain 层禁止

- 禁止引入 Spring / MyBatis / 任何框架注解（零框架依赖）
- 禁止暴露 setter 或 public 字段（状态变迁只通过行为方法）
- 禁止在 Domain 层实现 Repository（实现必须在 Infrastructure）
- 禁止在领域事件中引用 Infrastructure 类
- 禁止跨聚合直接修改对方内部状态（通过 Repository 读取）
- 禁止定义具名领域异常类（统一 BusinessException + 错误码）
- 禁止使用 Lombok `@Data`（聚合根/实体/值对象手写 equals/toString）

## Application 层禁止

- 禁止在 Handler 内写业务规则（决策在聚合根方法内）
- 禁止 Handler 直接使用 Mapper / PO（破坏依赖方向）
- 禁止 AppService 包含编排逻辑（委托 Handler）
- 禁止 AppService / Handler 包含 if-else 业务判断
- 禁止 Handler 返回 CO（应返回 DTO，由 Presenter 转 CO）
- 禁止 CO 暴露内部实现细节（version / deleted / 内部评分）

## Adapter 层禁止

- 禁止业务规则判断
- 禁止直接调用 Handler（必须经 AppService）
- 禁止直接操作 Repository
- 禁止调用 Domain 层
- 禁止修改 Command/Query 内容
- 禁止调用 Assembler / Presenter

## Infrastructure 层禁止

- 禁止在 PO / Repository 中写业务逻辑
- 禁止被 Domain 层引用（方向反了）
- 禁止将外部 SDK 类型泄漏到 Domain 层（必须 ACL 翻译）
- 禁止跨聚合共享 PO / Mapper
- 禁止在 Repository 中直接拼接 SQL 字符串（复杂 SQL 写 XML）

## Contract 模块禁止

- 禁止任何实现类（纯接口 + 数据载体）
- 禁止业务逻辑
- 禁止依赖 Spring / gRPC / MyBatis
- 禁止依赖 server 模块

## 通用禁止

- 禁止 Specification 模式（用显式 if-throw + Policy 替代）
- 禁止 Mediator 模式（Handler 1:1 对应 CQE，无需中间路由）
- 禁止在核心代码（src/ / sample-application/）中嵌入 AI 工具专属指令
- 禁止使用 `LocalDateTime`（统一 `OffsetDateTime`，带时区无歧义）

## Infrastructure 层最小化原则

- 禁止引入当前不使用的组件（“以后可能用到”不是理由）
- 禁止保留死代码（注释掉的代码块、TODO-restore、空实现）
- 禁止手动记录日志替代框架机制（如用 System.out 替代 SLF4J）
- 禁止在 infrastructure 层定义业务规则（仅做技术实现 + ACL 翻译）

## PO 强制注解

- PO **必须**声明 `@Version` 字段（乐观锁，无此注解视为架构违规）
- PO **必须**声明 `@TableLogic` 字段（逻辑删除，字段名 `isDelete`）
- 建表 DDL 必须包含 `version INT NOT NULL DEFAULT 0` 和 `is_delete BOOLEAN NOT NULL DEFAULT FALSE`

## 虚拟线程兼容

- 禁止在生产代码中使用 `synchronized` 块/方法（导致虚拟线程 pinning）
- 需要互斥时使用 `ReentrantLock`（虚拟线程友好，不会钉住载体线程）
- 禁止在 Domain 层调用 SecurityUtil（领域模型不感知认证上下文）

## Common 模块约束

- 禁止 common 模块包含任何业务逻辑（纯技术骨架）
- 禁止 common 模块声明超出自身编译需要的依赖（依赖最小化）
- 禁止 common 模块的 test scope 依赖泄漏给消费方（Maven test scope 不传递）
- 禁止新增 common 模块时不附带 `ywf-ddd-common/docs/common-{module}.md` 文档
- 禁止在 common 模块中硬编码业务包名（通过泛型 / SPI / 配置注入）

## Git 工作流（绝对约束）

- 禁止 Agent 执行任何 git 写操作（commit / push / reset / rebase / merge / cherry-pick / tag / branch -d 等）
- 禁止 Agent 修改 .git/ 目录下的任何文件
- 仅允许 git 只读操作（status / log / diff / show / blame）用于上下文理解
- 所有提交、分支管理、合并操作由人工手动完成
- .gitignore / .gitattributes 允许 Agent 修改（属于项目配置，非提交树）
