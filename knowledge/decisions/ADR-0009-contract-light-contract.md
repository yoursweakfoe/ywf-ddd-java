# ADR-0009 轻契约（不含 REST/RPC 注解）

**Status**: Superseded by [ADR-0010](ADR-0010-contract-heavy-contract-http-mapping.md)（旧 common-contract §ADR-0003 重契约）
**迁移来源**: docs/common/common-contract.md §6 · 旧 ADR-0002 废弃墓碑（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

旧决策：contract 模块的契约接口不含 REST / RPC 注解（轻契约），HTTP 映射留在 Controller。

## Decision Outcome

已废弃，由重契约（ADR-0010，旧 common-contract §ADR-0003）取代——重契约论证「协议无关的轻契约」是伪命题，见 ADR-0010。本文件按 references.md §ADR 总索引注记保留为废弃记录：**旧模型勿再引用**。

## Consequences

- 「零框架依赖」约束下契约曾保持纯类型；该立场已由 ADR-0010 反转（contract 依赖 spring-web 承载 HTTP 映射注解）

## Confirmation

人工评审（迁移自 contract 旧 ADR-0002）——历史墓碑记录，现行有效决策及其背书见 ADR-0010。
