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

```
contract/
└── {aggregate}/                     # 顶层按聚合分包
    ├── api/                         # Dubbo Service 接口
    ├── co/                          # Contract Object（契约输出对象）
    ├── dto/                         # Command + Query（CQE 请求对象）
    │   └── event/                   # Integration Event（集成事件）
    └── enums/                       # 契约共享枚举

```

| 目录 | 职责 |
|------|------|
| `{aggregate}/api/` | Dubbo Service 接口定义 |
| `{aggregate}/co/` | Contract Object（对内部 DTO 进行字段清洗后的外部安全视图） |
| `{aggregate}/dto/` | CQE 请求对象（Command / Query） |
| `{aggregate}/dto/event/` | Integration Event（跨服务集成事件） |
| `{aggregate}/enums/` | 契约共享枚举 |

---

## Server 模块

```
server/
├── adapter/                         # 入站适配器（driving adapter）
│   ├── {aggregate}/
│   │   ├── facade/                    # @DubboService 实现（纯透传）
│   │   ├── consumer/                  # MQ 消费入口（Integration Event 入站）【按需】
│   │   └── scheduler/                 # 定时任务入口【按需】
│   └── shared/                        # 跨聚合/系统级入口【按需】
│
├── application/                     # 用例编排
│   └── {aggregate}/
│       ├── {Aggregate}AppService.java # 聚合入口（全部用例方法）
│       ├── handler/                   # Command/Query Handler【按需】
│       │   └── event/                 # Domain Event Handler（域内反应）
│       ├── publisher/                 # MQ 出站投递【按需】
│       ├── assembler/                 # Domain → DTO（手写显式映射）
│       ├── presenter/                 # DTO → CO（手写显式映射）
│       └── dto/                       # 内部视图
│
├── domain/                          # 领域模型
│   ├── {aggregate}/
│   │   ├── model/                     # 聚合根 + 实体 + 值对象 + 枚举
│   │   │   └── event/                 # 领域事件
│   │   ├── repository/                # Repository 接口
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
    │   │       ├── po/                # 持久化对象
    │   │       ├── converter/         # Domain ↔ PO 转换
    │   │       ├── mapper/            # MyBatis-Plus Mapper
    │   │       │   └── xml/           # MyBatis XML（复杂 SQL）
    │   │       └── repository/        # Repository 实现
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

- **adapter** 依赖 application（透传用例调用）+ contract（实现 Service 接口、使用 CO / CQE）
- **application** 依赖 domain（编排领域对象）+ contract（使用 CO / CQE）
- **infrastructure** 依赖 domain（实现 Repository / Portal 接口）
- **domain** 零外部依赖（纯 Java + common-ddd 构建块）
- **contract** 零内部依赖（仅依赖 common-contract 框架包）

---

## 各层详细文档

| 模块 / 层 | 文档 |
|-----------|------|
| Contract | [contract/contract.md](contract/contract.md) |
| Server - Adapter | [server/adapter.md](server/adapter.md) |
| Server - Application | [server/application.md](server/application.md) |
| Server - Domain | [server/domain.md](server/domain.md) |
| Server - Infrastructure | [server/infrastructure.md](server/infrastructure.md) |
