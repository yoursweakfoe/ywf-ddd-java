# Adapter 层目录结构

```
adapter/
├── rest/                     # REST 面（对外经网关入口；东西向复用同一端点）
│   └── {Aggregate}ControllerImpl.java   # @RestController 实现 contract 的 Controller 契约接口 + RestAdapter 标记，纯透传 AppService
├── event/                    # 集成事件入站【按需】
│   └── consumer/             # MQ 消息消费入口（Integration Event 入站）
│       └── {Xxx}EventConsumer.java   # 实现 IntegrationEventConsumer 标记，接收 MQ → 反序列化 → 透传 AppService
├── scheduler/               # 定时任务入口【按需】
└── shared/                  # 跨聚合/系统级入口（如有）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `rest/` | @RestController 实现 contract 的 Controller 契约接口 + `RestAdapter` 标记（common-ddd/adapter/rest/）（路径/语义/签名在接口声明，纯透传至 AppService） |
| `event/consumer/` | 接收外部服务 MQ Integration Event → 反序列化 → 透传 AppService 用例方法（与 rest 同构，纯入口）。实现 `IntegrationEventConsumer` 标记（common-ddd/adapter/event/consumer/），与 application 层出站 `IntegrationEventPublisher` 对偶 |
| `scheduler/` | 定时任务入口 |
| `shared/` | 跨聚合/系统级入口（如有） |
