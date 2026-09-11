# 应用层内部对象 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"用哪种对象、准不准入"。代码模板、承载对比、选择表全在法卷 → [../../specs/current/patterns/building-block/application-objects.md](../../specs/current/patterns/building-block/application-objects.md)。

> 设计原理 → [../explanation/application.md](../explanation/application.md)

## 什么时候需要内部对象

Application 层夹在两类对象之间：Handler 管领域与内部数据的来回，Presenter 管内部数据到契约 CO 的出口。这一层需要按用途区分数据对象，用后缀把语义写在名字上，而不是一律叫 DTO。

写侧投影 DTO 是基线。读侧多视图、入路径富化、外部报文防腐是三个扩展场景，分别对应 ViewDTO / ParamsDTO / RecordDTO，这些都是虚构教例名。四对象角色总表、写/读承载对比、准入选择表都在法卷 §2.4~§2.6。DTO 与 CO 的职责分工 canonical 在 `knowledge/specs/current/patterns/discipline/coding-conventions.md`，本卡不复述。

## 决策点

1. **先问要不要**：没有契约投影需求、没有聚合行为需求、也没有外部格式需求 → 一律走标准链路。禁止发明第四种载体，这就是法卷 AO-1 的三不准入。
2. **写还是读**：version 只属于写侧关注点。读侧无 version，且不与写侧复用，两边各自演进，见法卷 AO-5。
3. **要不要多视图 / 富化**：同一查询要出详情和列表多个 CO → ViewDTO 加 ViewPresenter 做裁剪，见法卷 AO-6。入参要查库、查配置、拼安全上下文 → 用 Params 把契约参数和内部参数隔开，见法卷 AO-7，准入线是 AO-3。
4. **外部格式差异大不大**：报文与领域模型截然不同 → Record 在防腐入口一次解析定型，领域模型不接触外部格式，见法卷 AO-8。这就是防腐层（Anti-Corruption Layer）的落地。

## 边界与代价

- Params 不跨 Handler 边界传播，见法卷 AO-3。Record 的法条要求不可变：私有构造加静态工厂，见 AO-4。教学模板里可 set 的形态与法条相抵，落地以法卷为准，冲突点的裁决待 changes/。
- 没有多视图、没有富化、没有外部格式差异 → 这些中间对象一概不需要，标准链路最简。Assembler 不得跨层直产 CO。
- 设计原理纵深 → [../explanation/application.md](../explanation/application.md)

## 落地状态

一律以法卷 §3 生效登记为准。写/读投影真实例 ✅；Params、Record 教例件 ⛔ 未实现，模板在册。
