# quickstart —— 从零跑通示例服务

> 本教程用于自行复现实验中的 Order/Product 示例。最终源代码版本为 0.2.0，实验已结束并封存；项目非生产就绪，后续不再维护或提供安全更新，详见[根说明](../../../README.md)。
> 命令采用 Bash 语法；请根据所用 shell 调整环境变量与引号。

# 从零跑通示例服务

## 0. 前置

- JDK 21 + Maven；容器运行另需 Docker Engine / Docker Compose
- PostgreSQL 18（本机或自行准备的容器）。**两条路线都需要外部数据库**：先创建运行库 `ddd_sample_application` 和测试库 `ddd_sample_application_test`，再按 [db-migration 说明](../../../db-migration/README.md) 分别迁移两库；通过 `DB_MIGRATION_URL` 切换目标库。测试不负责建表，必须在路线 A 之前完成测试库迁移。

## 路线 A｜一条命令跑全量验收（推荐第一步）

```bash
mvn clean install
```

- 根 reactor 构建公共模块与 sample；common 包含 8 个公共模块与 1 个不发布的测试试验场，测试数量以本次执行输出为准
- **测试基座是真 PostgreSQL 测试库**：默认连接 `localhost:5432/ddd_sample_application_test`，用户/口令为 `ywf/ywf-local-123`，可通过 `DB_TEST_URL` / `DB_TEST_USER` / `DB_TEST_PASSWORD` 覆盖。迁移工程不在根 reactor 内，不能靠此命令自动迁移
- common 测试另用 `DB_IT_HOST` / `DB_IT_PORT` / `DB_IT_DB` / `DB_IT_USER` / `DB_IT_PASSWORD`，默认连接本机 5432，以 `ywf/ywf-local-123` 自动创建 `ddd_framework_test` 并复位专用 schema；用户需要建库权限。不要指向业务数据库
- 包含 ArchUnit 规则与行为测试；构建成功仅说明当前环境下这些检查通过，不证明生产可用性

检查实际构建日志中的 `BUILD SUCCESS` 及测试汇总。

## 路线 B｜起服务打真实 HTTP

### 1. 建库 + 灌 schema

```bash
# 0. 测试/运行前一次性建形（幂等，库形状权威 = db-migration）：
#   建两库（postgres 环节在跑时）：docker exec postgres psql -U ywf -d postgres -c 'CREATE DATABASE ddd_sample_application OWNER ywf'（_test 库同法）
#   跑 Job（换 DB_MIGRATION_URL 即切库，命令见 db-migration/README.md）
```

数据库变更集位于 `db-migration`。运行库与测试库分别执行同一变更集，应用和测试均不自动建表；自建 PostgreSQL 不要求使用名为 `postgres` 的容器，上面的 `docker exec` 仅适用于该容器名和用户。

### 2. 启动（dev profile）

```bash
# 在仓库根目录构建，确保框架与 contract 等依赖一同参与 reactor
mvn package -DskipTests
java -jar sample-application/sample-service/sample-service-server/target/sample-service-server-0.2.0.jar --spring.profiles.active=dev
```

- 缺省连 `jdbc:postgresql://localhost:5432/ddd_sample_application`，user/pass `ywf/ywf-local-123`。三项均可被 `DB_MASTER_URL` / `DB_MASTER_USER` / `DB_MASTER_PASSWORD` 覆盖；自配 URL 必须带 pgjdbc 超时三参数 `connectTimeout=10&socketTimeout=60&tcpKeepAlive=true`（教义见 datasource 配置注释）
- 不加 profile 且未注入所需环境变量时会启动失败：主配置的数据源占位符无默认值。本地运行显式使用 `dev`。
- 容器路线：先在仓库根执行 `mvn package -DskipTests` 得到 JAR，再进入 `sample-application` 执行 `docker compose up --build`（应用端口 8080）。Compose 显式使用 `dev`，数据库默认连接 `host.docker.internal:5432`，同名 `DB_MASTER_*` 环境变量可覆盖；Docker Desktop 下需允许容器访问宿主 PostgreSQL。Compose 不包含数据库或 Nacos，不能提供完整基础设施。

### 3. 健康确认

```bash
curl http://localhost:8080/api/actuator/health   # context-path=/api 同样罩住 actuator
# {"status":"UP",...}
```

### 4. 跑通订单全生命周期（八步）

```bash
# ① 建商品（取响应中的 id 作为 productId）
curl -sX POST localhost:8080/api/products -H 'content-type: application/json' \
  -d '{"name":"widget","price":9.90,"stock":100}'

# ② 下单（customerId 任意串；扣库存，PENDING）
curl -sX POST localhost:8080/api/orders -H 'content-type: application/json' \
  -d '{"customerId":"c-001","items":[{"productId":"<上一步id>","quantity":2}]}'

# ③-⑦ 状态机一路推：pay → confirm → ship → deliver → complete（orderId 用 ② 返回的 id）
curl -sX PUT localhost:8080/api/orders/$ID/pay
curl -sX PUT localhost:8080/api/orders/$ID/confirm
curl -sX PUT "localhost:8080/api/orders/$ID/ship?trackingNumber=TRK-0001"
curl -sX PUT localhost:8080/api/orders/$ID/deliver
curl -sX PUT localhost:8080/api/orders/$ID/complete

# ⑧ 读侧：详情 + 分页
curl -s localhost:8080/api/orders/$ID
curl -s "localhost:8080/api/orders/page?pageNum=1&pageSize=20&status=COMPLETED"
```

下单与状态流转返回 OrderCO，含 status；详情包含 `items`，分页返回不含明细的概要。取消接口返回空响应体，随后查询详情确认状态。

### 5. 体验失败通道（文档三分法的手感版）

```bash
# 状态机违规 → 422 + i18n 位点（detail 为 messageKey，前端负责渲染文案）
curl -sX PUT localhost:8080/api/orders/$ID/pay | head -c200        # 已完成单再支付

# 输入越界 → 400（@Size 在绑定层拦截，先于业务；字段名在 fieldErrors，不回显脏值）
curl -sX PUT "localhost:8080/api/orders/$(uuidgen)/ship?trackingNumber=$(python -c 'print("T"*101)')"

# 取消一张新建订单：先设置 NEW_ORDER_ID 为另一张 PENDING 订单的 id
curl -iX PUT "localhost:8080/api/orders/$NEW_ORDER_ID/cancel" -H 'content-type: application/json' -d '{"reason":"user-request"}'
curl -s "localhost:8080/api/orders/$NEW_ORDER_ID"
```

## 实现状态表（本文不演示的、docs 里存在但 sample 未落地）

| 能力 | 状态 | 指北 |
|---|---|---|
| 定时任务 / Gateway / 批量操作 / 分布式事务(Seata) / Nacos 注册发现 | ⛔ 未实现（教学教例） | 对应 how-to 篇首「实现状态」段 |
| Security JWT 资源服务器链 | ⛔ 示例运行时未接入（`common-security` 仅为 test scope），不能仅靠配置开关启用 | common-security |

## 下一步

- 阅读分层设计解释 → `knowledge/docs/explanation/`
- 对照保留的业务约束与开发技能研究实现；这些材料是实验产物，不构成封存后的开发计划。
