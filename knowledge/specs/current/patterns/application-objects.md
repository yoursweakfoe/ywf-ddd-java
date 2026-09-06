# 用法规范法卷：应用层内部对象（框架法 · 严格件）

> **身份**：本卷规范 ViewDTO / Params / Record 三类中间对象的准入；修卷走 `../../changes/`。docs 同题篇（`how-to/application-layer-objects.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| AO-1 | 中间对象三不准入：无契约投影需求、无聚合行为需求、无外部格式需求时，一律走 DTO + CO 标准链路（WC-4/WC-5），不得发明第四种载体 | `how-to/application-layer-objects.md` 结论段入法：「没有契约视图、没有复杂参数、没有外部格式 → 不需要这些中间对象」 | — |
| AO-2 | ViewDTO 仅承载读侧多视图投影，位置 `application/{agg}/dto/`，实现 `ApplicationDTO` 标记（R10b）；ViewPresenter 专职 DTO→CO | R10b；RC-4 互指 | ArchUnit |
| AO-3 | Params 仅在方法签名聚合参数 >3 时使用，不跨 Handler 边界传播 | 原篇适用场景节入法 | — |
| AO-4 | Record 为不可变状态载体：私有构造 + 静态工厂唯一入口（与聚合重建 `reconstitute()` 同型）；禁止裸 setter 构造 | `BasicConverter` 重建契约（`modules/ddd.md` 场景 2）；TC-2 互指 | — |

## 生效登记

AO-2 ✅（sample ViewDTO 现行）；AO-3/AO-4 适用于按需引入件（未引入即真空满足，无 ⛔）。
