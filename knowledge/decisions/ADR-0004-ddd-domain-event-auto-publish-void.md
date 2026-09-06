# ADR-0004 领域事件自动发布（作废登记）

**Status**: Void（编号作废登记，无正文）
**迁移来源**: docs/common/common-ddd.md §6 编号注记（旧 ADR-0003 位）+ docs/references.md §ADR 总索引（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

本编号位对应的旧决策「领域事件：进程内 Spring Event + 仅聚合根注册 + 仓储自动发布」已于 2026-09 随事件留白决策（commit 5bc4b4b）整体废弃移除，无正文保留。

## 作废原因（一句话）

尝试让 AI 自我迭代事件设计后复杂度不断膨胀（代码量占整库抽取的一半）导致脚手架重心偏移，最终决定对 event 进行大范围留白（只保留领域事件和集成事件两个标记接口，其余一律移除）；编号空置、不重排（common-ddd.md §6 编号注记原文）。

## Confirmation

不适用（编号空置，无正文，无背书对象）。
