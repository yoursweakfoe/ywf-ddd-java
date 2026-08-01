# Adapter 层 — 协议适配

## 职责

将外部协议请求适配到内部应用层，对外暴露 Dubbo Triple REST + gRPC 服务。
**不含业务逻辑，不含转换逻辑**——仅做参数包装和方法转发。

## 设计原则

- Facade 是**纯透传**：REST 参数 → Command/Query 包装 → 调用 AppService → 直接返回 CO
- Facade 调用 **AppService**（聚合入口），不直接调用 Handler
- Facade 实现标注 `@DubboService`，接口定义在 `contract` 模块
- Consumer / Scheduler 与 Facade 同构——纯入口，透传 AppService

## 包结构

→ [directory-structure/server/adapter.md](../directory-structure/server/adapter.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（Facade 在写路径中的位置）

## 核心组件

| 组件 | 命名规范 | 职责 |
|------|---------|------|
| Facade | `XxxServiceImpl` | 实现 contract 接口，`@DubboService`，纯透传 AppService |
| Consumer | `XxxEventConsumer` | 接收外部 MQ Integration Event → 反序列化 → 透传 AppService |
| Scheduler | `XxxScheduler` | 定时任务入口 → 透传 AppService |

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#3-adapter--facade纯透传

## 协作关系

```
调用方 ──RPC/REST──→ adapter/facade ──→ AppService ──→ Handler
                         │                   │              │
                         │                   │  ←── DTO ──┘
                         │    ←── CO ──────┘
                         │
                    调用方 ←── CO
```

- **contract** 定义接口 + CQE + CO，adapter 负责实现接口
- **application** 接收 adapter 的透传调用，返回 CO

## 规则

| 允许 | 禁止 |
|------|------|
| 参数包装（REST 参数 → Command/Query） | 业务规则判断 |
| 调用 AppService | 直接调用 Handler |
| 直接返回 AppService 结果（CO） | 直接操作 Repository |
| | 调用 Domain 层 |
| | 修改 Command/Query 内容 |
| | 调用 Assembler/Presenter |
