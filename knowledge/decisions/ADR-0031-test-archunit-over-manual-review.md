# ADR-0031 ArchUnit 而非人工 Code Review

**Status**: Accepted
**迁移来源**: docs/common/common-test.md §6 · 旧 ADR-0001（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

DDD 分层约束靠人工 review 还是自动化守护。

## Decision Drivers

- 纪律约束不可依赖（人的遗忘与豁免）
- CI 是红线的自然执行点

## Considered Options

- 人工 Code Review
- ArchUnit 规则编码为测试（选定）

## Decision Outcome

选 ArchUnit 自动化。规则编码为测试，CI 自动执行，不依赖人的纪律性。

## Consequences

- 分层教义获得机器红线；规则编号（R##）成为跨文档引用锚点（教义锚点、永不重排永不复用——见 DddArchitectureRules 类头）
- 已知边界：规则覆盖不到的教义（时间类型、CO 返回等）仍在类头「已知缺口」诚实登记，不假装全图

## Confirmation

机械背书（本决策即规则库的存在本身）：
- `DddArchitectureRules` 全部 18 常量（R1/R1b/R2/R3/R4/R5a/R5b/R6/R8a/R8b/R10a/R10b/R11/R12/R13/R14a/R14b/C1）
- 双端扫描挂载：框架扫描 `DddArchitectureTest`（packages = `com.yoursweakfoe.common.ddd`）+ 业务扫描 `ApplicationArchitectureTest`（packages = `com.yoursweakfoe.sampleapplication.sampleservice`）
- 防死规则负证明：`DomainPurityRuleProofTest`（R4 违例必失败实证）
