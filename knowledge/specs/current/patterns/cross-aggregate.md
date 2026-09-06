# 用法规范法卷：跨聚合协作（框架法 · 严格件）

> **身份**：本卷是跨聚合编排规范的**唯一权威**；修卷走 `../../changes/`。docs 同题篇（`how-to/cross-aggregate.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| CA-1 | 涉及多聚合的业务逻辑落在 Domain Service：位置 `domain/shared/service/`，实现 `DomainService` 标记接口（stereotype 豁免见 R4，纯 Java 本体） | R4；AGENTS 九条 4 的延伸（规则仍归聚合，Service 只编排） | C3 |
| CA-2 | Domain Service 只注入各聚合的写侧 Repository；不吞事务——事务边界在调用它的 Handler（WC-2） | WC-2 互指 | — |
| CA-3 | 同事务内的跨聚合补偿动作（如回补类操作）必须同步直调、处于同一 Handler 事务边界；禁止异步化「尽力而为」 | 示例聚合行为守恒测试（补偿原子化） | 测试名 |
| CA-4 | 跨聚合的读需求各走各的读端口（RC-1），禁止经他人聚合根取数 | R13 | ArchUnit |

## 生效登记

CA-1~4 ✅。
