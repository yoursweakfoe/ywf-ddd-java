---
name: modify-common-module
description: 修改 ywf-ddd-common 公共模块的公开 API 或内部实现。当需要变更框架核心代码时使用。
---

# 修改 Common 模块

## 前置阅读

1. `knowledge/docs/reference/api/common-{module}.md`（目标模块手册，描述镜像）
2. `knowledge/specs/current/modules/{module}.md`（对应法卷，严格件——文件名无 common- 前缀）
3. `ywf-ddd-common/AGENTS.md`（就近宪法：javadoc 是规范法载体、破坏性变更立案）
4. `ywf-ddd-common/README.md`（模块依赖拓扑）
5. `knowledge/specs/current/patterns/discipline/prohibitions.md` §9「Common 模块约束」（构件身份三分法——依赖审查判据的单一事实源）

## 核心原则

- common 模块是**框架核心**，任何公开 API 变更都影响所有消费方
- 修改必须**向后兼容**（新增方法可以，删除/改签名必须慎重）
- 文档与代码**强同步**（不允许代码改了文档没跟上）

## 步骤

### 1. 评估影响范围

- 先查目标模块的**身份登记**（工具库 / 定型装配 → 禁令卷 §9「Common 模块约束」登记表）——按登记身份到该节取对应判据审依赖，判据正文不在本文复述
- 确认修改的是公开 API（`public` / `protected`）还是内部实现（`private`）
- 公开 API 变更需检查所有消费方（sample-application + 其他业务服务）
- 使用 IDE "Find Usages" 或 `grep -r` 确认引用点

### 2. 修改代码

- 遵循模块现有代码风格
- 新增公开类/方法必须有完整 Javadoc（本树 javadoc 是规范法载体 → `ywf-ddd-common/AGENTS.md`）
- 构造器注入区块添加 `// region 依赖注入` 折叠标记

### 3. 更新模块文档

- 更新 `knowledge/docs/reference/api/common-{module}.md`（地图被动跟随）：§2 核心功能类表（新增/修改的类）、§4 依赖关系（如有变化）；§3「使用方式」已降为法卷指针，形状代码不再回填该节
- 规范形状/行为条款同步进法卷 `knowledge/specs/current/modules/{module}.md`（严格件唯一权威）——修订法卷必须走 `knowledge/specs/changes/` 程序，禁止迁就代码偷改（归属法 §4）
- 新设计决策 → 本案 specify §裁决记录落条（无案「修码就法」→ 法卷生效登记行＋theory-map 账本行，→ 归属法 §4）；决策正文不入 api 手册（其 §6「设计决策」已迁出案卷）
- 触及 javadoc 规范表（`GlobalRestExceptionHandler` 异常映射 / `MybatisPersistence` 通道行为）→ 同 PR 改其 javadoc + api 手册对应表（C5 对账 → 归属法 §4 强制同步表）

### 4. 补充/更新测试

- 新增公开 API 必须有对应测试
- 修改行为必须更新现有测试
- 测试规范形状与模板的唯一载体是法卷 `knowledge/specs/current/patterns/discipline/testing-conformance.md`（四类对号取型），入口见 `.agents/skills/new-test/SKILL.md`

### 5. 向后兼容检查

- 新增方法：兼容（无需额外操作）
- 修改方法签名：**不兼容**，需在文档中标注 breaking change
- 删除方法：**不兼容**，确认无消费方引用后方可删除
- 修改默认行为：评估是否影响现有业务逻辑
- 增删 pom 依赖：按禁令卷 §9 登记身份评估——定型装配的命运清单变更直接影响全部使用方（自我宣言 javadoc 与 `knowledge/docs/reference/api/common-{module}.md` §4 依赖同步 + 消费方影响面单列）；工具库的依赖变更走最小化质证

### 6. 关联文档更新

- `knowledge/docs/how-to/` 设计卡零形状代码：只在选型判据/边界变化时核对同题卡片与法卷无矛盾（冲突法卷赢，docs 修——归属法 §4）
- 改了 common-ddd 核心类，检查 `knowledge/docs/explanation/` 与 `knowledge/docs/tutorials/quickstart.md` 中示例是否需同步
- 如修改了标记接口（Command/Query/CO），检查 `knowledge/docs/reference/glossary.md`

## 验证

- [ ] `mvn compile -pl ywf-ddd-common/{module}` 编译通过
- [ ] `mvn test -pl ywf-ddd-common/{module}` 现有测试 + 新测试通过
- [ ] `mvn compile -pl sample-application/sample-service/sample-service-server` 消费方编译通过
- [ ] `knowledge/docs/reference/api/common-{module}.md` 已同步更新
- [ ] 依赖符合模块身份登记判据（禁令卷 §9 诸戒律，按登记身份对号）
- [ ] 子 pom 声明处零 `<exclusions>`（排除只写在策略文件 depMgmt → 禁令卷 §9「exclusions 卫生集中制」）
- [ ] 触及规范形状/行为条款：法卷 `knowledge/specs/current/modules/{module}.md` 已经 `knowledge/specs/changes/` 程序修订；破坏性公开 API 变更已立案并于该案 §裁决记录落条（→ `ywf-ddd-common/AGENTS.md`）
- [ ] `knowledge/scripts/check-docs.ps1` 全绿：C3 符号对账牵连 api 手册与全仓 docs 引用的类/方法名——符号删改报红先修文档；豁免走 `knowledge/scripts/check-docs.whitelist.txt`，只删不增、新增须 PR 评审写理由（归属法 §6）
- [ ] 无业务逻辑泄漏（common 模块纯技术骨架）

## 文档同步（账本速览，判据见步骤与验证清单）

- 必更：`knowledge/docs/reference/api/common-{module}.md`（描述镜像）
- 触及规范形状/行为条款：`knowledge/specs/current/modules/{module}.md`（法卷，走 `knowledge/specs/changes/` 程序）
- 视情况：`knowledge/docs/how-to/`、`knowledge/docs/reference/glossary.md`、`knowledge/docs/explanation/`、`knowledge/docs/tutorials/quickstart.md`、`ywf-ddd-common/README.md`
