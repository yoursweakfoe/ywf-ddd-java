# 用法规范法卷：外部集成（框架法 · 严格件）

> **身份**：本卷规范 Portal/Gateway（外部系统 ACL）形态；修卷走 `../../changes/`。docs 同题篇（`how-to/gateway.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| GW-1 | 外部能力在 domain 侧以 Portal 接口表达：出入参一律领域对象/值对象，禁止出现外部 SDK 类型或传输格式 | 框架 `Portal` 标记接口（common-ddd 已备，见下）；依赖倒置原则（AGENTS 九条 5） | C3 |
| GW-2 | Gateway 实现 Portal，置于 `infrastructure/gateway/{feature}/`：外部 DTO ↔ 领域对象的 ACL 翻译只发生在本层，外部格式不得越过 Gateway 边界 | AGENTS 九条 5（Infrastructure 实现 Domain 接口） | — |
| GW-3 | 超时、重试、幂等键属 Gateway 内部事务，对 domain 透明；策略参数经配置注入，禁止硬编码 | `modules/cloud.md` 容错条款互指 | — |
| GW-4 | 一个 Portal 只表达一类外部能力；禁止「上帝 Gateway」聚合不相关外部系统 | 原篇职责边界结论入法 | — |

## 生效登记

| 条款 | 状态 | 说明 |
|---|---|---|
| GW-1 框架侧 | ✅ | `Portal` 标记接口在库 |
| 业务侧实现 | ⛔ sample 未实现 | 示例应用保持最小闭环，无任何 Gateway 业务实现（真实例登记于原篇状态注）——本卷条款对将来落地件生效 |
