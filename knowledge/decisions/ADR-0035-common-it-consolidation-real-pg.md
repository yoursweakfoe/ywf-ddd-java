# ADR-0035 common 测试集中试验场与真 PG 全仓化（推翻 ADR-0034 豁免句）

**Status**: Accepted（2026-09-08，用户裁决：Q1 全称 schema `integration_test`/`integration_test_vacant`「我们不缺这点长度」/ Q2 模块名拼写修正 / Q3 保原包 / Q4 三段生命周期照推荐）
**关联法案**: `2026-09-common-it-consolidation`（TC-1/TC-5/TC-7/TC-9 改卷、TC-10 开卷、禁令卷 §9 身份三分）
**部分推翻**: ADR-0034（其末句「common 模块自身测试树不受辖域，框架测自己继续 H2 自洽」失效；sample 真 PG 诸裁决仍有效）

## Context and Problem Statement

ADR-0034 折叠后法卷自相矛盾：TC-1/TC-9 普适措辞（容器测试只走真 PG）与 common-ddd 实测 H2 基座互相打脸，靠辖域豁免句缝补。用户裁决「h2 问题太多、要完全真实的 infra 环境测」，并指定全部 common 测试（含互引与独立单测）迁出 library 模块、集中到专用集成测试模块——测试栈依赖不再散居八个发布件 POM。

## Decision Outcome

1. 新建第 9 模块 `common-packages-integration-test`（试验场，禁令卷 §9 第三身份：`src/main` 永空置、不发布、测试栈无最小化质证义务），55 卷测试资产 git mv 迁入并**保持原包名**（classpath split package 合法，白盒访问零扩大）。
2. H2 全仓退役：common 测试基座 = 真 PG 专属库 `ddd_framework_test`；`PgTestSupport` 三段生命周期（maintenance 库自建 → 每 JVM DROP/CREATE `integration_test`/`integration_test_vacant` 复位+执行 fixture DDL → shutdown hook 清空），零人工前置（TC-9 框架轨）。
3. 「空库」教例真实化：dynamic-datasource 双源改**同库异 schema**（currentSchema 裁决表可见性，fixture 表名去 schema 限定），替代 H2 时代「同 URL 不跑 INIT」戏法。
4. 各 library 模块测试栈清零（h2/dynamic-datasource/junit/assertj/common-test test 依赖全摘）。

## Consequences

- 正向：全仓 312 测试（试验场 193 + sample 119）同一真 PG 基座；library POM 净化；试验场实测当日即暴露跨模块组合缺口（`GlobalRestExceptionHandler` 在场时方法访问拒绝异常被兜底成 500 而非 403——security×exception 组合行为待另案裁决，ResourceServer 卷施工期以 `exclude` 保持类型分界）。actuator 类自动装配的横向污染（observability 依赖被试验场摘除）同为其证。
- 代价：common 测试运行需 PG 容器在跑（与 sample 同前置，TC-5 辖域）；跨 artifact 同包属刻意技术，JPMS 化时须重审。
- 验证：`mvn clean install`（common 193 绿 ×2 轮、轮间 schema 零残留实证）+ sample 119 回归绿。
