# Domain 层目录结构

```
domain/
├── {aggregate}/                    # 按聚合自包含
│   ├── model/                      # 必有。聚合根 + 实体 + 值对象 + 枚举 + Factory（复杂创建驻位）
│   │   ├── {xxx}.java                # 聚合根 (extends AggregateRoot<ID>)
│   │   ├── {xxx}Item.java            # 聚合内实体 (extends Entity<ID>)
│   │   ├── {xxx}VO.java              # 值对象 (implements ValueObject，推荐 record)
│   │   ├── {xxx}Enum.java            # 状态枚举等枚举（如有）
│   │   └── {xxx}Factory.java         # 复杂创建逻辑 (implements Factory 标记，common-ddd/domain/factory/；简单场景用构造器/静态工厂方法)
│   ├── repository/                 # 必有。{Agg}Repository（写端口，聚合生命周期；依赖倒置，实现在 Infrastructure）
│   ├── portal/                     # 可选。外部资源访问接口（OSS/RPC/MQ/ES，实现在 infrastructure/gateway）
│   ├── service/                    # 可选。聚合内领域服务
│   └── policy/                     # 可选。可插拔领域规则
└── shared/                         # 跨聚合共享
    ├── service/                    # 跨聚合领域服务
    ├── policy/                     # 通用策略
    └── model/                      # 跨聚合共享值对象
```

## 目录职责

| 子包 | 职责 | 准入规则 |
|------|------|--------|
| `model/` | 聚合根、实体、值对象、枚举；复杂创建逻辑（`{Agg}Factory`）驻位于此 | 零框架依赖，纯 Java + common-ddd 构建块；Factory 仅当构造器不足以表达创建语义时使用 |
| `repository/` | Repository 接口（写端口，`{Agg}Repository`） | 必须为接口，实现在 infrastructure/persistence/{ds}/{agg}/repository/（写读两侧 Impl 同包，类名后缀 RepositoryImpl / QueryRepositoryImpl 区分） |
| `portal/` | 外部资源访问接口（OSS/RPC/MQ/ES） | 必须为接口，实现在 infrastructure/gateway（含 ACL 翻译） |
| `service/` | 聚合内领域服务 | 仅当逻辑不自然归属于任何实体时使用 |
| `policy/` | 可插拔领域规则 | 无状态、纯计算、无副作用 |

## shared/ 包职责

| 子包 | 职责 |
|------|------|
| `shared/service/` | 跨聚合领域服务（协调多个聚合的业务操作） |
| `shared/policy/` | 通用策略（可插拔领域规则） |
| `shared/model/` | 跨聚合共享值对象 |
