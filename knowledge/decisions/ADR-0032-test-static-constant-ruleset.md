# ADR-0032 规则集为静态常量

**Status**: Accepted
**迁移来源**: docs/common/common-test.md §6 · 旧 ADR-0002（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

ArchUnit 规则如何暴露给业务服务。

## Decision Drivers

- 消费方零重复定义（规则单源在框架）
- 引用即挂载（静态字段是最小消费面）

## Considered Options

- 基类 / 组合工具（消费方继承或调用装配方法）
- 公开静态常量（选定）

## Decision Outcome

公开静态常量。业务服务直接引用 `DddArchitectureRules.XXX`，无需重复定义。

## Consequences

- 消费方测试类以 `@ArchTest static final ArchRule r = DddArchitectureRules.XXX` 形态挂载
- 业务扫描承诺「零本地谓词覆写」（2026-09-05 包命名税迁移后收回，见 DddArchitectureRules 类头）

## Confirmation

机械背书：
- 双端 `@ArchTest` 挂载点即证明：`ApplicationArchitectureTest`（全部共享常量直引）/ `DddArchitectureTest`（框架扫描覆写版除外部分直引）
- 原记锚点：`DddArchitectureRules` 类公开 `ArchRule` 静态字段
