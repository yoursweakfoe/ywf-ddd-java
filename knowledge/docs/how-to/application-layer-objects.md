# 应用层内部对象 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"用哪种对象、准不准入"。一切代码模板、承载对比、选择表见法卷 → [../../specs/current/patterns/application-objects.md](../../specs/current/patterns/application-objects.md)。

> 设计原理 → [../explanation/application.md](../explanation/application.md)

## 什么时候需要内部对象

Application 层在 Handler（领域 ↔ 内部数据）和 Presenter（内部数据 ↔ 契约 CO）之间，需要语义明确的后缀区分不同用途的数据对象，而不是泛化的 DTO。写侧投影 DTO 是基线；读侧多视图、入路径富化、外部报文防腐是三个扩展场景，分别对应 ViewDTO / ParamsDTO / RecordDTO（虚构教例名）——四对象角色总表、写/读承载对比、准入选择表均在法卷 §2.4~§2.6。DTO 与 CO 的职责分工 canonical 在 `.agents/rules/03-coding-conventions.md`（本卡不复述）。

## 决策点

1. **先问要不要**：无契约投影需求、无聚合行为需求、无外部格式需求 → 一律标准链路，禁止发明第四种载体（法卷 AO-1 三不准入）。
2. **写还是读**：version 只属于写侧关注点，读侧无 version 且不与写侧复用、各自演进（AO-5）。
3. **要不要多视图 / 富化**：同一查询要出详情 + 列表多个 CO → ViewDTO + ViewPresenter 裁剪（AO-6）；入参要查库、查配置、拼安全上下文 → Params 隔离契约与内部参数（AO-7，AO-3 准入线）。
4. **外部格式差异大不大**：报文与领域模型截然不同 → Record 在防腐入口一次解析定型，领域模型不接触外部格式（AO-8）——这就是防腐层（Anti-Corruption Layer）的落地。

## 边界与代价

- Params 不跨 Handler 边界传播（AO-3）；Record 法条要求不可变（私有构造 + 静态工厂，AO-4）——教学模板的可 set 形态与法条相抵，落地以法卷为准，冲突点裁决待 changes/
- 没有多视图、没有富化、没有外部格式差异 → 这些中间对象一概不需要，标准链路最简（Assembler 不得跨层直产 CO）
- 设计原理纵深 → [../explanation/application.md](../explanation/application.md)

## 落地状态

一律以法卷 §3 生效登记为准（写/读投影真实例 ✅ / Params、Record 教例件 ⛔ 未实现、模板在册）。
