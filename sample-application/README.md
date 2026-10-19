# sample-application

基于 ywf-ddd-common 的订单与商品实验示例，演示 DDD 战术模式的部分实现。最终源代码版本为 **0.2.0**，随实验结束封存，不再开发维护或提供安全更新；非生产就绪，完整定位见[根说明](../README.md)。

## 为什么选择电商（订单 + 商品）作为示例场景

### 选型理由

| 维度 | 说明 |
|------|------|
| **领域模型丰富** | 订单具备完整生命周期状态机（7 态），商品具备库存增减行为，两个聚合即可覆盖 DDD 核心战术模式 |
| **业务形态成熟** | 电商是业界研究最充分的领域之一，DDD 社区（Vaughn Vernon《实现领域驱动设计》、eShop 系列）均以电商/订单为主要案例 |
| **外部资料密集** | 无论中文还是英文社区，电商 DDD 的设计方案、代码参考、架构讨论数量远超其他垂直领域，降低读者理解成本 |
| **便于讨论边界** | 订单与库存便于讨论并发、事务与幂等；本示例不代表已实现分布式事务、事件投递或真实支付 |
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
| （刻意不演示）Portal/Gateway、Policy | 可阅读 `knowledge/docs/how-to/gateway.md`、`knowledge/docs/how-to/policy-pattern.md` 中的扩展讨论；不属于本示例已接入的外部能力 |

> 本示例不覆盖真实支付、物流系统或服务注册发现。扩展点和教学文档不构成成熟度或可用性承诺；如用于自己的研究，需自行评估并维护。

## 构建与运行

需要 JDK 21、Maven，以及外部 **PostgreSQL 18**。当前示例**没有 Nacos 运行时依赖**。先创建运行库 `ddd_sample_application` 与测试库 `ddd_sample_application_test`，按 [db-migration 说明](../db-migration/README.md) 对两个库分别执行迁移；迁移工程不在根 reactor，应用和测试不自动建表。

以下命令从仓库根目录开始，使用 Bash 语法：

```bash
# 测试库已迁移后，再构建并运行测试
mvn clean install

# 本机运行：dev 默认使用 localhost:5432 的运行库
java -jar sample-application/sample-service/sample-service-server/target/sample-service-server-0.2.0.jar --spring.profiles.active=dev
```

示例测试连接通过 `DB_TEST_URL` / `DB_TEST_USER` / `DB_TEST_PASSWORD` 覆盖；根构建还会运行 common 测试，其连接参数为 `DB_IT_HOST` / `DB_IT_PORT` / `DB_IT_DB` / `DB_IT_USER` / `DB_IT_PASSWORD`，需要建库权限。应用连接通过 `DB_MASTER_URL` / `DB_MASTER_USER` / `DB_MASTER_PASSWORD` 覆盖。本地默认用户/口令为 `ywf/ywf-local-123`，自定义 JDBC URL 保留 `connectTimeout=10&socketTimeout=60&tcpKeepAlive=true`。

容器方式同样先准备并迁移外部数据库，然后：

```bash
# Dockerfile 使用已有 JAR，不在镜像内编译
mvn package -DskipTests
cd sample-application
docker compose up --build -d
```

Compose 显式启用 `dev`，默认连接 `host.docker.internal:5432` 的宿主数据库（可用同名 `DB_MASTER_*` 环境变量覆盖），不启动 PostgreSQL 或 Nacos。Docker Desktop 环境需确保 PostgreSQL 的监听、防火墙及访问规则允许容器连接宿主。应用监听 8080，健康检查为 `http://localhost:8080/api/actuator/health`。接口示例见[快速开始](../knowledge/docs/tutorials/quickstart.md)。

> **测试编写指引**（分层测试策略、ArchUnit 执行、fixtures 与真 PG 测试库约定）统一见 [`.agents/skills/new-test/SKILL.md`](../.agents/skills/new-test/SKILL.md)。

## 目录结构

```
sample-application/
├── docker-compose.yml
├── pom.xml
└── sample-service/
    ├── sample-service-contract/   # 公开契约（Service 接口 + CQE + CO）
    └── sample-service-server/     # 服务实现（adapter + application + domain + infrastructure）
```
