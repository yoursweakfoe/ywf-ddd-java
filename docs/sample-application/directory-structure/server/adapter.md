# Adapter 层目录结构

```
adapter/
├── web/                     # REST 面（对外，经网关入口）
│   └── {Aggregate}Controller.java    # @RestController 实现 contract 接口，纯透传 AppService
├── grpc/                    # gRPC 面（东西向内部通道）
│   └── {Aggregate}InternalGrpcService.java  # @GrpcService 实现 proto stub，纯透传 AppService
├── consumer/                # MQ 消息消费入口（Integration Event 入站）【按需】
│   └── {Xxx}EventConsumer.java       # 接收 MQ → 反序列化 → 透传 AppService
├── scheduler/               # 定时任务入口【按需】
└── shared/                  # 跨聚合/系统级入口（如有）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `web/` | @RestController 实现 contract 接口（spring-web 注解声明路径，纯透传至 AppService） |
| `grpc/` | @GrpcService 实现 proto stub（CO → proto 翻译，纯透传至 AppService） |
| `consumer/` | 接收外部服务 MQ Integration Event → 反序列化 → 透传 AppService 用例方法（与 web/grpc 同构，纯入口） |
| `scheduler/` | 定时任务入口 |
| `shared/` | 跨聚合/系统级入口（如有） |
