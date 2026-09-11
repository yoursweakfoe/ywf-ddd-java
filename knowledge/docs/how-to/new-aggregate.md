# 新建聚合 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该拆新聚合、怎么定案"。全套规范形状与逐件教学走查已整体归卷 → [aggregate-blueprint.md](../../specs/current/patterns/building-block/aggregate-blueprint.md)：§1 槽位清单、§2 条款（BP-1~BP-12 / BP-X1~X3）、§3 验收单、§4 逐件走查。冲突以法卷为准，见归属法卷 §2。本篇零形状代码。
> 包结构参考 → [aggregate-blueprint 法卷 §5](../../specs/current/patterns/building-block/aggregate-blueprint.md)；写/读全链路走查 → [write-path.md](write-path.md) / [read-path.md](read-path.md)

## 什么时候需要新聚合

要把一块实体从现有聚合里拆出去时，对照四条拆分信号。命中任一条，就值得独立建一个聚合：

1. **独立生命周期**：自带状态机，独立演进，不跟随宿主聚合的操作。
2. **独立数据归属**：掌管外部交易号、渠道参数、退款信息这类独立数据块，不落在任何宿主不变式内。
3. **独立事务边界**：与宿主是一对多或并列关系，宿主存活期内可以追加子实例。
4. **可能拆分为独立微服务**：先拆聚合，日后服务化只是移包，不用重构。

四条全不中 → 在现有聚合加字段或实体就行，别付新建聚合的成本。

## 三个决策点

1. **教例家族选哪个**：聚合构建走查用 Payment 家族，它是虚构教例。写/读路径走查用 Reservation 家族。照自己任务对应的同家族篇目抄，形状就不走样。
2. **读端口配对是否同期建**：法卷 BP-1 允许这两个文件随首个读用例补齐。建聚合时已有读场景，就同 PR 建满，否则读侧只能借写端口，那是法卷 BP-11 的违规。
3. **契约枚举奇偶**：CO 一旦暴露状态值域，契约枚举和 domain 枚举就是同一值域的两个化身。分层互禁引用，所以这份重复不可消除。收口办法是奇偶守卫测试，见法卷 BP-2 / BP-7，不是把两个类合并。

## 边界与代价

- **22 个文件不是 22 层抽象**。它是同一套分层的槽位占位，各件职责与归属看法卷 §1 槽位注。真实代价是：每个新用例要横跨契约、应用、领域、基础设施、适配五端同步落件。
- 聚合间协作不归本模式。走 Domain Service 同事务直调，见 [cross-aggregate.md](cross-aggregate.md)。
- 设计原理纵深 → [../explanation/domain.md](../explanation/domain.md)、[../explanation/application.md](../explanation/application.md)。

## 落地顺序与验收

- 顺序：法卷 BP-3，contract → domain → infrastructure → application → adapter，接口先行、依赖倒序。
- 验收：法卷 §3 验收单逐条勾验；逐件形状对照法卷 §4 走查。

## 创建工具

- 技能 [../../../.agents/skills/new-aggregate/SKILL.md](../../../.agents/skills/new-aggregate/SKILL.md)：按法卷分阶段生成全套文件。
