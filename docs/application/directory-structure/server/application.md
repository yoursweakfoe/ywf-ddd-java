# Application 层目录结构

```
application/
└── {aggregate}/                        # 按聚合自包含
    ├── {Aggregate}AppService.java      # 聚合入口（全部用例方法）
    ├── handler/                        # CQRS Handler【按需】
    │   ├── command/                    # CommandHandler（写用例，与 CQE 1:1）
    │   └── query/                      # QueryHandler（读用例，与 CQE 1:1）
    ├── event/                          # 事件处理【按需】
    │   ├── listener/                   # DomainEventListener（域内反应）
    │   │   └── {Xxx}DomainEventListener.java   # 监听领域事件 → 委托 DomainService / Publisher
    │   └── publisher/                  # Publisher（出站投递）
    │       └── {Xxx}EventPublisher.java    # 被 CommandHandler/DomainEventListener 调用 → 翻译为集成事件 → 投递
    ├── assembler/                      # Domain → DTO（手写显式映射）
    ├── presenter/                      # DTO → CO（手写显式映射）
    └── dto/                            # 内部视图

```

## 目录职责

| 目录 | 职责 |
|------|------|
| `{aggregate}/{xxx}AppService.java` | 聚合入口，全部用例方法（简单直接实现，复杂委托 Handler） |
| `{aggregate}/handler/` | Command/Query Handler（与 CQE 1:1）；用例完成后如需通知外部则调用 publisher |
| `{aggregate}/event/listener/` | DomainEventListener，监听 Spring ApplicationEvent 执行域内反应（如取消→回补库存）；如需通知外部则调用 publisher |
| `{aggregate}/event/publisher/` | 被 CommandHandler/DomainEventListener 显式调用 → 翻译为契约 IntegrationEvent → 调 Infrastructure MQ Producer 投递。AppService 不直接依赖 publisher |
| `{aggregate}/assembler/` | Domain → DTO 转换（手写显式映射） |
| `{aggregate}/presenter/` | DTO → CO 呈现/清洗（手写显式映射） |
| `{aggregate}/dto/` | 内部视图对象（不对外暴露，可能含乐观锁，审计等内部字段） |
