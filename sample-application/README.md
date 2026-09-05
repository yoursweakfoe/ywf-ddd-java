# sample-application

基于 ywf-ddd-common 框架的示例业务服务，演示 DDD 战术模式的完整落地方式。

## 为什么选择电商（订单 + 商品）作为示例场景

### 选型理由

| 维度 | 说明 |
|------|------|
| **领域模型丰富** | 订单具备完整生命周期状态机（7 态），商品具备库存增减行为，两个聚合即可覆盖 DDD 核心战术模式 |
| **业务形态成熟** | 电商是业界研究最充分的领域之一，DDD 社区（Vaughn Vernon《实现领域驱动设计》、eShop 系列）均以电商/订单为主要案例 |
| **外部资料密集** | 无论中文还是英文社区，电商 DDD 的设计方案、代码参考、架构讨论数量远超其他垂直领域，降低读者理解成本 |
| **技术上限高** | 并发超卖（乐观锁）、分布式事务（下单-扣库存-支付）、最终一致性（事件驱动）、幂等性（重复支付）等高阶问题天然存在 |
| **跨聚合协调自然** | 下单 = 订单创建 + 库存扣减，天然需要 Domain Service 协调，无需人为构造场景 |
| **读者零门槛** | 几乎所有开发者都有网购经验，不需要额外解释业务语义 |

### 已知局限

| 局限 | 说明 |
|------|------|
| 场景被过度使用 | 电商示例在 DDD 社区已成"老生常谈"，部分读者可能产生审美疲劳 |
| 简化后丢失复杂度 | 真实电商涉及促销引擎、物流追踪、售后逆向流程、多仓调度等，示例仅保留最小闭环 |
| 不覆盖部分模式 | 审批流（长流程 Saga）、多租户隔离、实时计算等模式在电商最小场景中无法自然体现 |
| 读路径过于简单 | 示例仅演示单表查询 + 分页，未涉及复杂报表、全文搜索、CQRS 物化视图等读侧优化 |

### 最小场景覆盖清单

本示例有意控制在**最小可验证闭环**，确保每个保留的元素都对应一个框架能力演示：

| 保留元素 | 演示的框架能力 |
|----------|---------------|
| Order 聚合（状态机 + 行为方法） | AggregateRoot、显式 if-throw、validate() |
| Product 聚合（库存增减） | 乐观锁（`updateById` 的 SQL 版本条件）、聚合行为封装 |
| PlaceOrderCommand（跨聚合） | CommandHandler、Domain Service 协调 |
| cancel → 库存回补 | 同事务直调 InventoryDomainService（补偿原子化） |
| GetOrderQuery | QueryHandler、读路径 |
| OrderAssembler / OrderConverter | BasicAssembler（应用层）、BasicConverter（基础设施层） |
| OrderController / ProductController（Adapter web） | Spring MVC REST 面、纯透传 |
| ArchUnit 测试 | common-test 架构守护 |
| 并发下单压测 | 手写 XML 乐观锁（`SET version = version + 1 WHERE ... AND version = #{version}`）防超卖 |
| OrderFactory（订单工厂） | Factory 标记接口、创建即合法（校验一步到位）、新建/重建双路径收口 |
| （刻意不演示）Portal/Gateway、Policy | 框架能力已备齐，完整示例见 `docs/application/cookbook/gateway.md`、`docs/application/cookbook/policy-pattern.md`；示例应用有意保持最小闭环，避免样例膨胀 |

> **关于未演示的能力**：Portal/Gateway（外部集成 ACL）、Policy（领域策略）、Specification 属于框架的**按需扩展点**——本示例的场景刻意不覆盖它们，教学路径走 cookbook 对应章节；业务项目按需引入即可，不必为「用上而用上」。

> 真实业务项目引入本框架时，应删除整个 `sample-application/` 目录，以 `docs/application/cookbook/` 为模板从零搭建。

## 构建与运行

```bash
# 前置：安装框架到本地仓库
cd ywf-ddd-common && mvn clean install

# 运行测试（H2 内存库，无需外部依赖）
cd sample-application
mvn clean test

# 完整运行（需要 Nacos + PostgreSQL）
docker compose up -d
```

> **测试编写指引**（分层测试策略、ArchUnit 执行、fixtures 与 H2 约定）统一见 [`.agents/skills/new-test/SKILL.md`](../.agents/skills/new-test/SKILL.md)。

## 目录结构

```
sample-application/
├── docker-compose.yml
├── pom.xml
└── sample-service/
    ├── sample-service-contract/   # 公开契约（Service 接口 + CQE + CO）
    └── sample-service-server/     # 服务实现（adapter + application + domain + infrastructure）
```
