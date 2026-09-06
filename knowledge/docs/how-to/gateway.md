# Gateway（Portal 实现）· 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该用、怎么选"。一切形状、代码、命名表与落地模板见法卷 → [../../specs/current/patterns/external-gateway.md](../../specs/current/patterns/external-gateway.md)。

> 设计原理 → [../explanation/infrastructure.md](../explanation/infrastructure.md)（gateway 章节）

## 什么时候需要外部集成

业务动作要借用外部能力（支付扣款、文件存储、短信、第三方 RPC），而领域层不应知道"谁提供、怎么调"。案例教例：支付单执行扣款时调用第三方支付平台——领域只关心"支付成功/失败"这个业务事实，不关心底层是支付宝还是微信还是 Stripe，且未来可能换渠道或多渠道并存。解法是 ACL（防腐层）翻译：Domain 定 Portal（我需要什么），Infra 落 Gateway（谁提供、怎么调）。第三方 SDK 类型一旦泄漏进领域层，换支付渠道就要改 Domain——这是本模式要付的代价的反面。

## 四个设计决策点

1. **要不要 Portal**：外部格式需要被翻译成领域语言时才要（法卷 GW-1：出入参禁外部 SDK 类型与传输格式）。数据库读写走 Repository 卷，不算外部能力。
2. **返回值对象住哪**：它是值对象，落 model/，不放 portal/——portal/ 只准入接口（法卷 GW-5/GW-6，命名表在册）。
3. **能力粒度**：一个 Portal 一类能力，禁止「上帝 Gateway」聚合不相关外部系统（法卷 GW-4）。
4. **在哪个边界调用**：使用方注入 Portal 接口而非 Gateway 实现，事务内调用时事务边界仍在 Handler（法卷 GW-8 变体形状）。

## 边界与代价

- 超时、重试、幂等键属 Gateway 内部事务，对 domain 透明，策略参数经配置注入禁止硬编码（法卷 GW-3，容错互指 cloud 卷）
- Repository → persistence 与 Portal → gateway 是「Domain 定义接口、Infra 实现」的对偶结构，两者对照表 canonical 在 [../explanation/infrastructure.md](../explanation/infrastructure.md)（persistence / gateway 章节），法卷与本卡均不复制
- 虚构教例（payment 聚合 + 支付宝渠道）的教学世界定位 → 法卷 §2 引言注记

## 落地状态

一律以法卷 §3 生效登记为准（框架 `Portal` 标记接口 ✅ / 业务 Portal + Gateway 件 ⛔ 虚构教例，落地模板在册）。
