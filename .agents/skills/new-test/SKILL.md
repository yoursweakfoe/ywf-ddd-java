---
name: new-test
description: 为已有聚合、Handler、Domain 模型或基础设施组件编写测试。当需要补充单元测试或集成测试时使用。
---

# 新增测试

## 前置阅读

1. `knowledge/docs/how-to/testing.md`（**四类测试模板的唯一载体**——本文零模板，只给顺序与检查项，D6 裁决）
2. `knowledge/docs/reference/api/common-test.md`（ArchUnit 规则集 + 测试基础设施）
3. `knowledge/specs/capabilities/<agg>.md`（被测行为的现行契约——断言从 Scenario 来，不即兴）

## 步骤

1. 定性：本次要测的是 Handler（A 形）/ 领域模型（B 形）/ Converter（C 形）/ 全链路（D 形）——到 testing.md 对号取模板
2. 造数：一律经 Factory / `reconstitute()` 两条合法路径（禁反射绕过）；重复数据放 `fixtures/` 或 `support/`
3. 断言：AssertJ；正常路径 + ≥2 异常路径；HTTP 面错误按 400（绑定层）/ 422（领域 BusinessException）/ 409（乐观锁/实体消失）/ 500（SilentWriteLoss，勿重试）四通道对号
4. 位置镜像被测类包路径（Handler 测试在 `handler/command|query/` 同名侧）

## 验证

- [ ] `mvn test -pl {module}` 全绿（H2 test profile，零外部基础设施）
- [ ] 单元测试零容器（`@Mock`+`@InjectMocks`）；容器测试仅 `@ActiveProfiles("test")`
- [ ] 新行为/新通道：对应 `knowledge/specs/changes/<slug>/` delta 已立，测试断言与 Scenario 一一对应

## 文档同步

- 新增测试**形态**（非用例）：更新 `knowledge/docs/how-to/testing.md`；触及测试基础设施：更新 `knowledge/docs/reference/api/common-test.md`