# ADR-0003 对象转换纯手写，不用 MapStruct

**Status**: Accepted
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0004（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

Converter / Assembler / Presenter 三类对象映射用代码生成器（MapStruct 等）还是手写显式映射。

## Decision Drivers

- AI 辅助开发下手写模板成本归零
- 生成器的认知负担仍在：注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫
- 聚合根重建走 reconstitute，完整性需要测试守护

## Considered Options

- **代码生成器（MapStruct）**：省样板，但生成代码不可见、注解处理链有认知负担
- **手写显式映射**：全部映射逻辑在仓库里

## Decision Outcome

选手写显式映射。AI 辅助开发下手写模板成本归零，而生成器的认知负担（注解处理链、生成代码不可见、Lombok 桥接、@MapperScan 误扫）仍在。聚合根重建走 reconstitute，完整性由往返测试守护。

## Consequences

- 映射代码可见、可 grep、可评审
- 完整性责任转移到往返测试（round-trip test 是守护证人）
- 本论证与 ADR-0007-ddd-remove-mybatis-plus 拒绝 SQL 生成的论证同源

## Confirmation

机械背书：
- 真实例（sample）：`OrderConverterTest.roundTrip_shouldPreserveData` / `toDomain_shouldReconstituteOrder`（往返测试即本决策的完整性守护）
- 框架：`BasicConverterTest`、`BasicAssemblerPresenterTest`
- 原记锚点：`BasicConverter` / `BasicAssembler` / `BasicPresenter` 无生成器依赖
