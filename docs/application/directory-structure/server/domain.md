# Domain 层目录结构

```
domain/
├── {aggregate}/                    # 按聚合自包含
│   ├── model/                      # 必有。聚合根 + 实体 + 值对象 + 枚举
│   │   ├── {xxx}.java                # 聚合根 (extends AggregateRoot<ID>)
│   │   ├── {xxx}Item.java            # 聚合内实体 (extends Entity<ID>)
│   │   ├── {xxx}VO.java              # 值对象 (implements ValueObject，推荐 record)
│   │   └── {xxx}Enum.java          # 状态枚举等枚举（如有）
│   ├── event/                      # 领域事件（对偶 common-ddd/domain/event/{domain,publisher}，事件不再归属 model/）
│   │   ├── domain/                 # 领域事件定义（extends DomainEvent）
│   │   │   └── {xxx}ActionEvent.java
│   │   └── publisher/              # 业务自定义领域事件发布器（可选，默认框架 InProcessDomainEventPublisher）
│   ├── repository/                 # 必有。Repository 接口（依赖倒置，实现在 Infrastructure）
│   ├── portal/                     # 可选。外部资源访问接口（OSS/RPC/MQ/ES，实现在 infrastructure/gateway）
│   ├── service/                    # 可选。聚合内领域服务
│   ├── factory/                    # 可选。复杂创建逻辑（简单场景用构造器/静态方法）
│   └── policy/                     # 可选。可插拔领域规则
└── shared/                         # 跨聚合共享
    ├── service/                    # 跨聚合领域服务
    ├── policy/                     # 通用策略
    └── model/                      # 跨聚合共享值对象
```

## 目录职责

| 子包 | 职责 | 准入规则 |
|------|------|--------|
| `model/` | 聚合根、实体、值对象、枚举 | 零框架依赖，纯 Java + common-ddd 构建块 |
| `event/domain/` | 领域事件定义（extends DomainEvent） | 事件是模型的组成部分，仅进程内消费 |
| `event/publisher/` | 业务自定义领域事件发布器 | 可选；默认使用框架 `InProcessDomainEventPublisher` |
| `repository/` | Repository 接口 | 必须为接口，实现在 infrastructure/persistence |
| `portal/` | 外部资源访问接口（OSS/RPC/MQ/ES） | 必须为接口，实现在 infrastructure/gateway（含 ACL 翻译） |
| `service/` | 聚合内领域服务 | 仅当逻辑不自然归属于任何实体时使用（见下方说明） |
| `factory/` | 复杂创建逻辑（implements Factory） | 仅当构造器不足以表达创建语义时使用 |
| `policy/` | 可插拔领域规则 | 无状态、纯计算、无副作用 |

## shared/ 包职责

| 子包 | 职责 |
|------|------|
| `shared/service/` | 跨聚合领域服务（协调多个聚合的业务操作） |
| `shared/policy/` | 通用策略（可插拔领域规则） |
| `shared/model/` | 跨聚合共享值对象 |
