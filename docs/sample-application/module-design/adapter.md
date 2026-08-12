# Adapter 层 — 协议适配

## 职责

将外部协议请求适配到内部应用层：对外 REST 面（Spring MVC Controller，经网关入口）+ 东西向 gRPC 面（proto stub 实现）。
**不含业务逻辑，不含转换逻辑**——仅做参数包装和方法转发。

## 设计原则

- web / grpc 入口都是**纯透传**：协议参数 → Command/Query 包装 → 调用 AppService → 直接返回 CO（gRPC 面做 CO → proto message 翻译）
- 入口调用 **AppService**（聚合入口），不直接调用 Handler
- Controller 实现 `contract` 模块的接口（方法签名单一事实源），REST 路径以 spring-web 注解在 Controller 上显式声明
- gRPC service 实现 contract 模块 proto 生成的 stub（`@GrpcService`）
- Consumer / Scheduler 与 web/grpc 同构——纯入口，透传 AppService

## 包结构

→ [directory-structure/server/adapter.md](../directory-structure/server/adapter.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（Controller 在写路径中的位置）

## 核心组件

| 组件 | 命名规范 | 职责 |
|------|---------|------|
| Controller | `XxxController` | 实现 contract 接口，`@RestController` + spring-web 注解声明路径，纯透传 AppService |
| GrpcService | `XxxInternalGrpcService` | 实现 proto stub，`@GrpcService`，纯透传 AppService（CO → proto 翻译） |
| Consumer | `XxxEventConsumer` | 接收外部 MQ Integration Event → 反序列化 → 透传 AppService |
| Scheduler | `XxxScheduler` | 定时任务入口 → 透传 AppService |

→ 完整代码见 [cookbook/write-path.md](../cookbook/write-path.md)#3-adapter纯透传

## 协作关系

```
调用方 ──REST/gRPC──→ adapter/web | adapter/grpc ──→ AppService ──→ Handler
                          │                              │              │
                          │                              │  ←── DTO ──┘
                          │              ←── CO ──────┘
                          │
                     调用方 ←── CO（REST）/ proto message（gRPC）
```

- **contract** 定义接口 + CQE + CO + proto，adapter 负责实现接口 / stub
- **application** 接收 adapter 的透传调用，返回 CO

## 规则

| 允许 | 禁止 |
|------|------|
| 参数包装（协议参数 → Command/Query） | 业务规则判断 |
| 调用 AppService | 直接调用 Handler |
| 直接返回 AppService 结果（CO / proto 翻译） | 直接操作 Repository |
| | 调用 Domain 层 |
| | 修改 Command/Query 内容 |
| | 调用 Assembler/Presenter |
