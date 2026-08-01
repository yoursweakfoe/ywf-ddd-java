# Adapter 层目录结构

```
adapter/
├── {aggregate}/
│   ├── facade/                # @DubboService 实现（纯透传）
│   │   └── {xxx}ServiceImpl.java
│   ├── consumer/              # MQ 消息消费入口（Integration Event 入站）
│   │   └── {Xxx}EventConsumer.java   # 接收 MQ → 反序列化 → 透传 AppService
│   └── scheduler/             # 定时任务入口
└── shared/                    # 跨聚合/系统级入口（如有）
```

## 目录职责

| 目录 | 职责 |
|------|------|
| `{aggregate}/facade/` | @DubboService 实现（纯透传至 AppService） |
| `{aggregate}/consumer/` | 接收外部服务 MQ Integration Event → 反序列化 → 透传 AppService 用例方法（与 Facade 同构，纯入口） |
| `{aggregate}/scheduler/` | 定时任务入口 |
| `shared/` | 跨聚合/系统级入口（如有） |
