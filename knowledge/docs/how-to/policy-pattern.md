# 领域策略 Policy · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该抽、怎么组合"。形状、Before/After 对照、组合形态选择表、职责边界表全在法卷 → [../../specs/current/patterns/building-block/domain-policy.md](../../specs/current/patterns/building-block/domain-policy.md)。

> 设计原理 → [../explanation/domain.md](../explanation/domain.md)（领域策略章节）

## 什么时候需要 Policy

业务规则需要**频繁新增、独立测试、动态启用/禁用**时才抽。典型信号：运营几乎每周都会加一条活动规则，如 VIP 折扣、满减、大促。

不抽的代价就是 Before 反例，已在册法卷 §2.1：所有规则硬编码在一个方法的 if-else 里。新增规则必须改已有方法，违反 OCP。分支膨胀之后无法维护。单条规则的单元测试互相牵连。

场景衔接见 [write-path.md](write-path.md) 业务场景节。

## 决策点

1. **组合形态选哪种**：三种。互斥：严格 `@Order` 排序，命中即返回。叠加：顺序无关，遍历累加。精准路由：按业务标识 Map 精确命中。选择表随形状在册法卷 §2.7。
2. **谁消费策略链**：编排在 Domain Service，由它注入接口集合。Policy 自身纯决策、无副作用，只出结果。职责边界对比表的 canonical 已入法卷 §2.8，与跨聚合卷 CA-6 互指。
3. **归属层定哪**：本卷教例落在领域层的 policy/ 子包。应用层归属是否合法**未落定**，以法卷 DP-3 原条款为准；落地决策走 `../../changes/`。

## 边界与代价

- 每条规则一个无状态单例类，可独立测试、独立启用/禁用。`@Order` 是框架排序机制，禁止承载业务语义（法卷 DP-1）。
- 落位纪律：policy/ 子包专准入可插拔领域规则，也就是无状态、纯计算、无副作用；不放 service/，见法卷 DP-4。
- 示例应用**刻意不演示** Policy。全教例是 {Agg} 通式虚构模板，如会员折扣、满减，真实例映射位没有。业务项目落地时以具体聚合代入。
- 设计原理纵深 → [../explanation/domain.md](../explanation/domain.md)。

## 落地状态

一律以法卷 §3 生效登记为准。形态条款 DP-1/2/4/5 ✅；归属层 DP-3 待 changes/；sample 实现 ⛔ 模板在册。
