# Adapter 层 — 协议适配

## 职责

将外部协议请求适配到内部应用层：对外 REST 面（Spring MVC Controller，经 Higress 网关入口）；东西向服务间调用复用同一 REST 端点（消费方经 RestClient 直连，一期静态地址）。
**不含业务逻辑，不含转换逻辑**——仅做参数包装和方法转发。

## 设计原则

- rest 入口**纯透传**：协议参数 → Command/Query 包装 → 调用 AppService → 直接返回 CO
- 入口调用 **AppService**（聚合入口），不直接调用 Handler
- Controller 实现 `contract` 模块的接口（REST 契约单一事实源：路径、语义、签名均在接口声明），Controller 仅标记 `@RestController` 并透传
- Scheduler 与 rest 同构——纯入口，透传 AppService；两类入口各自以空标记定型（`RestAdapter` / `ScheduledAdapter`），供 ArchUnit 规则识别与约束
- 东西向端点的运行时访问保障（网关过滤 / 服务间认证 / 一包两部署）见 [contract.md §契约访问边界](contract.md#契约访问边界运行时保障)

## 包结构

→ [directory-structure/server/adapter.md](../directory-structure/server/adapter.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（Controller 在写路径中的位置）

## 核心组件

| 组件 | 命名规范 | 职责 |
|------|---------|------|
| Controller | `XxxControllerImpl` | 实现 contract 的 Controller 契约接口（路径/语义/签名在接口声明），`@RestController` 标记协议，纯透传 AppService |
| Scheduler | `XxxScheduler` | 定时任务入口 → 透传 AppService（实现 `ScheduledAdapter` 标记，规则 R14a/R14b；模板见 [cookbook/scheduled-task.md](../cookbook/scheduled-task.md)） |

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#3-adapter纯透传

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
| 参数包装（协议参数 → Command/Query） | 业务规则判断 |
| 调用 AppService | 直接调用 Handler |
| 直接返回 AppService 结果（CO） | 直接操作 Repository |
| | 调用 Domain 层 |
| | 修改 Command/Query 内容 |
| | 调用 Assembler/Presenter |
