# Application 层目录结构

```
application/
└── {aggregate}/                        # 按聚合自包含
    ├── service/                        # 应用服务（聚合入口，全部用例方法）
    │   └── {Aggregate}AppService.java
    ├── handler/                        # CQRS Handler【按需】
    │   ├── command/                    # CommandHandler（写用例，与 CQE 1:1）
    │   └── query/                      # QueryHandler（读用例，与 CQE 1:1）
    ├── event/                          # 事件处理【按需】
    │   ├── listener/                   # DomainEventListener（域内反应）
    │   │   └── {Xxx}DomainEventListener.java   # 监听领域事件 → 委托 DomainService / Capture
    │   └── capture/                    # Capture（出站翻译 + 捕获）
    │       └── {Xxx}IntegrationEventCapture.java    # 被 CommandHandler/DomainEventListener 调用 → 翻译为集成事件 → 同事务捕获入集成 Outbox
    ├── assembler/                      # Domain → DTO（手写显式映射）
    ├── presenter/                      # DTO → CO（手写显式映射）
    └── dto/                            # 内部视图

```

## 目录职责

| 目录 | 职责 |
|------|------|
| `{aggregate}/service/{xxx}AppService.java` | 聚合入口，全部用例方法（简单直接实现，复杂委托 Handler）。实现 `ApplicationService` 标记接口（common-ddd），与 domain 层 `DomainService` 对偶 |
| `{aggregate}/handler/` | Command/Query Handler（与 CQE 1:1）；用例完成后如需通知外部则调用 capture |
| `{aggregate}/event/listener/` | DomainEventListener，监听 Spring ApplicationEvent 执行域内反应（如取消→回补库存）；如需通知外部则调用 capture |
| `{aggregate}/event/capture/` | 被 CommandHandler/DomainEventListener 显式调用 → 翻译为契约 IntegrationEvent → 经 `IntegrationEventOutboxStore` 同事务捕获入集成 Outbox（投递 MQ 归框架排空器）。AppService 不直接依赖 capture |
| `{aggregate}/assembler/` | Domain → DTO 转换（手写显式映射） |
| `{aggregate}/presenter/` | DTO → CO 呈现/清洗（手写显式映射） |
| `{aggregate}/dto/` | 内部视图对象（不对外暴露，可能含乐观锁，审计等内部字段）。顶层 DTO 实现 `ApplicationDTO` 标记（common-ddd/application/dto/），嵌套项随外层定型 |
