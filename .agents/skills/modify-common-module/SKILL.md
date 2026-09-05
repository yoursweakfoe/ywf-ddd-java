---
name: modify-common-module
description: 修改 ywf-ddd-common 公共模块的公开 API 或内部实现。当需要变更框架核心代码时使用。
---

# 修改 Common 模块

## 前置阅读

1. `docs/common/common-{module}.md`（目标模块文档）
2. `ywf-ddd-common/README.md`（模块依赖拓扑）
3. `.agents/rules/04-forbidden-patterns.md`（Common 模块约束节）

## 核心原则

- common 模块是**框架核心**，任何公开 API 变更都影响所有消费方
- 修改必须**向后兼容**（新增方法可以，删除/改签名必须慎重）
- 文档与代码**强同步**（不允许代码改了文档没跟上）

## 步骤

### 1. 评估影响范围

- 确认修改的是公开 API（`public` / `protected`）还是内部实现（`private`）
- 公开 API 变更需检查所有消费方（sample-application + 其他业务服务）
- 使用 IDE "Find Usages" 或 `grep -r` 确认引用点

### 2. 修改代码

- 遵循模块现有代码风格
- 新增公开类/方法必须有完整 Javadoc
- 构造器注入区块添加 `// region 依赖注入` 折叠标记

### 3. 更新模块文档

- 更新 `docs/common/common-{module}.md`：
  - 核心功能表（新增/修改的类）
  - 使用方式（场景代码）
  - 设计决策表（如有新决策）
  - 依赖关系（如有变化）

### 4. 补充/更新测试

- 新增公开 API 必须有对应测试
- 修改行为必须更新现有测试
- 参照 `.agents/skills/new-test/SKILL.md` 的模板

### 5. 向后兼容检查

- 新增方法：兼容（无需额外操作）
- 修改方法签名：**不兼容**，需在文档中标注 breaking change
- 删除方法：**不兼容**，确认无消费方引用后方可删除
- 修改默认行为：评估是否影响现有业务逻辑

### 6. 关联文档更新

- 如修改了 common-ddd 的核心类，检查 `docs/application/cookbook/` 中的代码示例是否需同步
- 如修改了标记接口（Command/Query/CO），检查 `docs/glossary.md`

## 验证

- [ ] `mvn compile -pl ywf-ddd-common/{module}` 编译通过
- [ ] `mvn test -pl ywf-ddd-common/{module}` 现有测试 + 新测试通过
- [ ] `mvn compile -pl sample-application/sample-service/sample-service-server` 消费方编译通过
- [ ] `docs/common/common-{module}.md` 已同步更新
- [ ] 无新增超出编译需要的依赖（依赖最小化）
- [ ] 无业务逻辑泄漏（common 模块纯技术骨架）

## 文档同步

- 必须更新：`docs/common/common-{module}.md`
- 视情况更新：`docs/application/cookbook/`、`docs/glossary.md`、`ywf-ddd-common/README.md`
