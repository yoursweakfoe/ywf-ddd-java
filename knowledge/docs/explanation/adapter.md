# Adapter 层 — 协议适配

## 职责

adapter 层把外部协议请求适配成对内部应用层的调用。**不含业务逻辑，也不含转换逻辑**，只做两件事：包装参数、转发方法。

入口有两类。对外 REST 面是 Spring MVC Controller，流量经 Higress 网关入口；东西向服务间调用复用同一组 REST 端点，消费方经 RestClient 直连，一期静态地址。

## 设计原则

- rest 入口**纯透传**：协议参数包装成 Command/Query → 调用 AppService → 直接返回 CO。
- 入口一律调用 **AppService**，即聚合入口，不直接调用 Handler。
- Controller 实现 `contract` 模块的接口。路径、语义、签名全部声明在接口上，接口就是 REST 契约的单一事实源；Controller 只标记 `@RestController` 并透传。「为何映射上契约接口」的完整论证，canonical 见 [contract.md「设计原则」「文档注解归属」](contract.md)，本文不复述。
- Scheduler 与 rest 同构：都是纯入口，都透传 AppService。两类入口各自以空标记接口定型——`RestAdapter` / `ScheduledAdapter`——供 ArchUnit 规则识别与约束。
- 东西向端点的运行时访问保障——网关过滤、服务间认证、一包两部署——见 [contract.md §契约访问边界](contract.md#契约访问边界运行时保障)。

## 包结构

→ [aggregate-blueprint §5](../../specs/current/patterns/building-block/aggregate-blueprint.md)

> 完整代码示例 → [cookbook/write-path.md](../how-to/write-path.md)，看 Controller 在写路径中的位置。

## 核心组件

| 组件 | 命名规范 | 职责 |
|------|---------|------|
| Controller | `XxxControllerImpl` | 实现 contract 的 Controller 契约接口。路径、语义、签名都在接口上声明，实现类以 `@RestController` 标记协议，纯透传 AppService。 |
| Scheduler | `XxxScheduler` | 定时任务入口，透传 AppService。实现 `ScheduledAdapter` 标记，对应规则 R14a/R14b；模板见 [cookbook/scheduled-task.md](../how-to/scheduled-task.md)。 |

→ 完整代码见 [cookbook/write-path.md §2 Adapter：Controller 契约接口 + 实现，纯透传](../how-to/write-path.md)

## 协作关系

```
调用方 ──REST──→ adapter/rest ──→ AppService ──→ Handler
                     │                │            │
                     │                │  ←── DTO ──┘
                     │        ←── CO ─┘
                     │
                调用方 ←── CO
```

- **contract** 定义接口 + CQE + CO，adapter 负责实现接口
- **application** 接收 adapter 的透传调用，返回 CO

## 规则

| 允许 | 禁止 |
|------|------|
| 参数包装：协议参数 → Command/Query | 业务规则判断 |
| 调用 AppService | 直接调用 Handler |
| 直接返回 AppService 结果，即 CO | 直接操作 Repository |
| | 调用 Domain 层 |
| | 修改 Command/Query 内容 |
| | 调用 Assembler/Presenter |
