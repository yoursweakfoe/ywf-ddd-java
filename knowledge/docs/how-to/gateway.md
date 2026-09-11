# Gateway（Portal 实现）· 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该用、怎么选"。形状、代码、命名表与落地模板全在法卷 → [../../specs/current/patterns/external-gateway.md](../../specs/current/patterns/external-gateway.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md)（gateway 章节）

## 什么时候需要外部集成

业务动作要借用外部能力时用，比如支付扣款、文件存储、短信、第三方 RPC。前提是一条：领域层不应该知道"谁提供、怎么调"。

案例教例是支付单执行扣款时调用第三方支付平台。领域只关心"支付成功/失败"这个业务事实，不关心底层是支付宝、微信还是 Stripe，而且未来可能换渠道、多渠道并存。

解法是 ACL 防腐层翻译。Domain 定 Portal，声明我需要什么。Infra 落 Gateway，实现谁提供、怎么调。

这个模式省下的是什么，看反面就知道：第三方 SDK 类型一旦泄漏进领域层，换支付渠道就得改 Domain。

## 四个设计决策点

1. **要不要 Portal**：外部格式需要被翻译成领域语言时才要，见法卷 GW-1，该条禁止出入参出现外部 SDK 类型与传输格式。数据库读写不算外部能力，走 Repository 卷。
2. **返回值对象住哪**：它是值对象，落 model/，不放 portal/。portal/ 只准入接口。命名表在册，见法卷 GW-5/GW-6。
3. **能力粒度**：一个 Portal 一类能力。禁止把不相关的外部系统塞进同一个 Gateway，法卷 GW-4 管这种东西叫「上帝 Gateway」。
4. **在哪个边界调用**：使用方注入 Portal 接口，而不是 Gateway 实现。在事务内调用时，事务边界仍在 Handler，变体形状见法卷 GW-8。

## 边界与代价

- 超时、重试、幂等键属 Gateway 内部事务，对 domain 透明。策略参数经配置注入，禁止硬编码（法卷 GW-3），容错细节与 cloud 卷互指。
- Repository → persistence 与 Portal → gateway 是对偶结构：都由 Domain 定义接口、Infra 实现。两者对照表的 canonical 在 [../explanation/infrastructure.md](../explanation/infrastructure.md) 的 persistence / gateway 章节，法卷与本卡都不复制。
- 虚构教例（payment 聚合 + 支付宝渠道）在教学世界的定位，见法卷 §2 引言注记。

## 落地状态

一律以法卷 §3 生效登记为准。框架 `Portal` 标记接口 ✅；业务 Portal + Gateway 件 ⛔ 虚构教例，落地模板在册。
