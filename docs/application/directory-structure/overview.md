# 服务全局目录结构

## Maven 模块总览

```
sample-service/
├── sample-service-contract/         # 契约模块（对外发布的轻量 jar）
└── sample-service-server/           # 服务实现（部署单元）
```

依赖方向：`server → contract`（server 实现 contract 定义的接口）

---

## Contract 模块

→ 目录树与目录职责表（canonical）见 [contract/contract.md](contract/contract.md)；IntegrationEvent 路径的唯一完整表述在 [module-design/contract.md](../module-design/contract.md) 核心组件表。

---

## Server 模块

```
server/
├── adapter/                         # 入站适配器（driving adapter，协议伞/角色两级式）
│   ├── rest/
│   │   └── controller/                # @RestController 实现 contract 的 Controller 契约接口（纯透传）
│   ├── task/
│   │   └── scheduler/                 # 定时任务入口【按需】
│   └── shared/                        # 跨聚合/系统级入口【按需】
│
├── application/                     # 用例编排
│   └── {aggregate}/
│       ├── service/                   # 应用服务（聚合入口）
│       │   └── {Aggregate}AppService.java
│       ├── handler/                   # CQRS Handler【按需】
│       │   ├── command/               # CommandHandler（写用例）
│       │   └── query/                 # QueryHandler（读用例）
│       ├── assembler/                 # Domain → DTO（手写显式映射）
│       ├── presenter/                 # DTO → CO（手写显式映射）
│       ├── dto/                       # 内部视图
│       └── repository/                # 读端口（读侧查询接口，对偶 infra repository/application）
│           └── application/           #   XxxQueryRepository（CQRS 读端口，绕过 domain）
│
├── domain/                          # 领域模型
│   ├── {aggregate}/
│   │   ├── model/                     # 聚合根 + 实体 + 值对象 + 枚举
│   │   ├── repository/                # Repository 接口（对偶 infra repository/domain）
│   │   │   └── domain/                #   XxxRepository（写侧，聚合生命周期）
│   │   ├── portal/                    # 外部资源访问接口【按需】
│   │   ├── service/                   # 聚合内领域服务【按需】
│   │   ├── factory/                   # 复杂创建逻辑【按需】
│   │   └── policy/                    # 可插拔领域规则【按需】
│   └── shared/                        # 跨聚合共享
│       ├── service/                   # 跨聚合领域服务
│       ├── policy/                    # 通用策略
│       └── model/                     # 跨聚合共享值对象
│
└── infrastructure/                  # 基础设施
    ├── persistence/                   # 持久化（实现 Domain Repository）
    │   ├── master/                    # 主数据源（框架默认回退值）
    │   │   └── {aggregate}/           # 按聚合命名空间隔离
    │   │       ├── mybatis/           # MyBatis 技术位（撤换 ORM 时整体删除）
    │   │       │   ├── po/            # 持久化对象（纯 POJO，零 ORM 注解）
    │   │       │   └── mapper/        # Mapper 接口（extends DddMapper）
    │   │       │                      #   SQL 全部手写：resources/mapper/{aggregate}/XxxMapper.xml
    │   │       ├── converter/         # Domain ↔ PO 转换（框架 BasicConverter 桥）
    │   │       └── repository/        # Repository 实现
    │   │           ├── application/   # XxxQueryRepositoryImpl（读侧，对偶 application 读端口）
    │   │           └── domain/        # XxxRepositoryImpl（写侧，对偶 domain Repository）
    │   └── {other}/                   # 其他数据源（结构同 master）【按需】
    ├── gateway/                       # 外部系统网关（实现 Domain Portal）
    │   └── {capability}/              # 按外部能力分包
    └── config/                        # Spring @Configuration
```

### 依赖方向

```
adapter ──→ application ──→ domain ←── infrastructure
   │            │
   └─────┬──────┘
         ↓
      contract
```

> 各层依赖方向的逐条法条与职责一句话 → canonical 见 [.agents/rules/02-architecture.md](../../../.agents/rules/02-architecture.md)「依赖方向」与「各层职责」表，此处不复述。

---

## 各层详细文档

| 模块 / 层 | 文档 |
|-----------|------|
| Contract | [contract/contract.md](contract/contract.md) |
| Server - Adapter | [server/adapter.md](server/adapter.md) |
| Server - Application | [server/application.md](server/application.md) |
| Server - Domain | [server/domain.md](server/domain.md) |
| Server - Infrastructure | [server/infrastructure.md](server/infrastructure.md) |
