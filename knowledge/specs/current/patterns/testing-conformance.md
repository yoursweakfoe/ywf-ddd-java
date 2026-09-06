# 用法规范法卷：测试符合性（框架法 · 严格件）

> **身份**：本卷规范「怎么写测试才算合格」；修卷走 `../../changes/`。docs 同题篇（`how-to/testing.md`）为宽松件（四型模板全套），冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| TC-1 | 测试四分型强制：Handler 单测（Mockito，零 Spring 容器）/ Domain 纯 JUnit / Converter 往返（PO↔domain 等价）/ 集成（test profile + H2）；容器测试只准走 test profile | `modules/test.md`（common-test 场景 2/3）；原篇四型节入法 | mvn 全绿 |
| TC-2 | 聚合合法实例**只能**经 Factory 或 `reconstitute()` 构造；测试不得裸构造绕过不变式（绕过=教坏后来者，合法路径的守门人正是这两处） | 原篇入法：「测试禁止绕过……不得手动 new」；`modules/ddd.md` 场景 1；AO-4 互指 | 聚合根测试 |
| TC-3 | 涉及新行为/新通道的测试，断言与 `changes/<slug>/` delta 的 Scenario 一一对应（spec-first：先法案后断言） | rules/05 §2「skill 首步产出 delta」；本区 README 守则 2 | bill 对账 |
| TC-4 | 单测类禁止 `@Autowired` 字段注入；断言统一 AssertJ | 原篇检查单入法 | 评审项 |
| TC-5 | 「clone 即全绿」演示契约：全仓测试离线运行、零外部基础设施依赖 | 示例树宪法（`sample-application/AGENTS.md`）在册承诺 | CI 复跑 |

## 生效登记

TC-1~5 ✅（119 测试基线在册对账）。
