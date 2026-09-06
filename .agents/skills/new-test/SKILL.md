---
name: new-test
description: 为已有聚合、Handler、Domain 模型或基础设施组件编写测试。当需要补充单元测试或集成测试、或在 new-aggregate / new-usecase 产码后补齐测试覆盖时使用。
---

# 新增测试

> 本技能只载流程不载法：定性 → 立约 → 造数 → 分层施工 → 验证。四类测试条款（TC-1~8）、规范形状模板、Fixture 模式与命名表全住法卷 `knowledge/specs/current/patterns/testing-conformance.md`（唯一载体），每步按指针取用，此不复述。

## 前置阅读

| 读什么 | 文件 | 内容 |
|---|---|---|
| 测试法卷（唯一权威） | `knowledge/specs/current/patterns/testing-conformance.md` | §1 条款 TC-1~8；§2 规范形状（§2.5 Fixture、§2.6 命名、§2.7 验收终板、§2.8 真实例指针位） |
| common-test 模块法卷 | `knowledge/specs/current/modules/test.md` | 场景 1 ArchUnit 守护 / 场景 2 容器集成 / 场景 3 Mockito 单元 |
| common-test 字典 | `knowledge/docs/reference/api/common-test.md` | ArchUnit 规则集与测试基础设施 API（描述镜像） |
| 设计卡（宽松件） | `knowledge/docs/how-to/testing.md` | 选型判据存疑才读；冲突法卷赢 |
| 被测行为契约 | `sample-application/specs/current/<agg>.md` | 示例业务行为现行契约，断言来源 |

## 步骤

1. **定性**（TC-1）：按被测层对号取型——application Handler → A 型，domain 聚合根 → B 型，infrastructure Converter → C 型，聚合全链路行为 → D 型；四型不互斥。做完一个聚合的新行为，A/B/C 各先就位一份，D 型按契约覆盖需要补。形状 → 法卷 §2.1~§2.4
2. **立约**（TC-3）：涉新行为/新通道，先确认行为所属区已立 `changes/<slug>/` delta（框架 → `knowledge/specs/changes/`、示例业务 → `sample-application/specs/changes/`），断言与 delta 的 Scenario 一一对应——先有法案后有断言。补测既有行为则跳过本步，断言直接取自现行契约
3. **造数**（TC-2）：聚合合法实例只经 Factory（「创建即合法」新建路径）或 `reconstitute()`（「惰性重建」任意状态路径）两条通道，禁裸构造器与反射绕过；共享造数住 `support/` 或 `fixtures/` 包 → 法卷 §2.5
4. **A 型 · Handler 级**（法卷 §2.1；`knowledge/specs/current/modules/test.md` 场景 3）：零容器 Mockito 单测；测什么——仅委托链 load → 聚合行为 → save → toDTO 与异常路径（覆盖下限归 TC-6），领域规则归 B 型管、不重复断言；位置镜像产码：写侧 `application/{agg}/handler/command/`、读侧 `handler/query/`（TC-7）
5. **B 型 · Domain 级**（法卷 §2.2；TC-1/TC-2）：纯 JUnit 零 Mock；测什么——聚合状态机全部合法迁移、非法起点异常、validate 不变量、Factory 创建即合法路径，一迁一测；状态与迁移名以目标聚合真实状态机为准，勿照抄模板进产码；验收 `mvn test -pl {module} -Dtest={Agg}Test`
6. **C 型 · Infrastructure 级**（法卷 §2.3；TC-7）：Converter 测什么——往返等价（PO↔domain）加脏数据快速失败（复杂列非法文本抛异常，不静默兜底）；位置镜像 `infrastructure/persistence/{ds}/{agg}/converter/`。Repository/Gateway 实现不单独成型：真实性由 D 型真 H2 语义穿透验证
7. **D 型 · 聚合级全链路**（法卷 §2.4；`knowledge/specs/current/modules/test.md` 场景 2；TC-1/TC-5）：容器测试只走 test profile + H2 内存库、零外部基础设施——「clone 即全绿」是演示契约；测什么——完整业务流经真 SQL 的语义与 HTTP 面错误通道分界，状态码以 exception 法卷 `knowledge/specs/current/modules/exception.md`（EV 系）为准不即兴；集成测试统一住 `integration/`（TC-7）
8. **命名对号**（TC-4/TC-8）：类名/方法名/Fixture 名 → 法卷 §2.6；单元类禁 `@Autowired` 字段注入、断言统一 AssertJ。架构守护既有测试天然覆盖新包（真实例：`DddArchitectureTest`），无需为本步新写规则
9. **验证**：交付前 `mvn test -pl {module}` 全模块通过（test profile 零外部基础设施，TC-5），涉跨模块影响再 `mvn -B test` 全树复跑；最后对法卷 §2.7 验收终板逐条勾满五项（canonical=§2.7，此不复列清单）

## 文档同步

- 本技能与法卷 §2.7 同源、canonical=法卷：检查项有出入时修法卷（走 `knowledge/specs/changes/` 程序，严格件不得偷改），不在本文件偷改或添裸法条
- 新增测试**形态**或选型判据变化：修法卷同 PR 连带设计卡 `knowledge/docs/how-to/testing.md`（冲突法卷赢）
- 触及测试基础设施（common-test）：同 PR 随码更新 `knowledge/docs/reference/api/common-test.md`（地图区守则）