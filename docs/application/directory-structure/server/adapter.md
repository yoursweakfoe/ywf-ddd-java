# Adapter 层目录结构

```
adapter/
├── rest/                     # REST 面（对外经网关入口；东西向复用同一端点）
│   └── {Aggregate}ControllerImpl.java   # @RestController 实现 contract 的 Controller 契约接口，纯透传 AppService
├── consumer/                # MQ 消息消费入口（Integration Event 入站）【按需】
│   └── {Xxx}EventConsumer.java       # 接收 MQ → 反序列化 → 透传 AppService
├── scheduler/               # 定时任务入口【按需】
└── shared/                  # 跨聚合/系统级入口（如有）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `rest/` | @RestController 实现 contract 的 Controller 契约接口（路径/语义/签名在接口声明，纯透传至 AppService） |
| `consumer/` | 接收外部服务 MQ Integration Event → 反序列化 → 透传 AppService 用例方法（与 rest 同构，纯入口） |
| `scheduler/` | 定时任务入口 |
| `shared/` | 跨聚合/系统级入口（如有） |
