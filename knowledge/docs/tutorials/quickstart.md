# quickstart —— 从零跑通示例服务

> 本教程是「真实例操作手册」：直接操作 sample 的真实 Order/Product 端点（tutorials 属教学业务词豁免区，§2.1 辖域裁定）。
> 端点与请求体字段均取自 2026-09-06 源码取证（contract 扫描 + mvn 日志实证方法签名）。

# 从零跑通示例服务

## 0. 前置

- JDK 21 + Maven（无其它要求，Node/IDE 可选）
- 仅跑「路线 B 起服务」时需要 PostgreSQL 16（本机或容器）；路线 A 零基础设施

## 路线 A｜一条命令跑全量验收（推荐第一步）

```bash
mvn clean install
```

- 14 个模块 reactor 顺序构建；common 6 模块测试 + sample 119 用例全跑
- **测试基座 = 真 PG 测试库**：前置为 ywf-infra `postgres` 环节在跑 + 对 `ddd_sample_application_test` 建形（见路线 A 步 0；TC-5/TC-9，库形状权威 = db-migration，测试不建表）
- 内含 ArchUnit 双端规则集与 `ContractEnumParityTest`、`OptimisticLockConcurrencyTest` 等行为实证——绿了即证明：你拿到的是文档承诺的那套框架

预期尾行：`BUILD SUCCESS`，sample 模块 `Tests run: 119, Failures: 0, Errors: 0, Skipped: 0`。

## 路线 B｜起服务打真实 HTTP

### 1. 建库 + 灌 schema

```bash
# 0. 测试/运行前一次性建形（幂等，库形状权威 = db-migration）：
#   建两库（postgres 环节在跑时）：docker exec postgres psql -U ywf -d postgres -c 'CREATE DATABASE ddd_sample_application OWNER ywf'（_test 库同法）
#   跑 Job（换 DB_MIGRATION_URL 即切库，命令见 db-migration/README.md）

DDL 权威唯一 = `db-migration` 变更集（BP-S1）：测试与运行共两库（`ddd_sample_application` / `ddd_sample_application_test`），同份变更集双库实证字节级同形；不再存在手工 schema 文件。

### 2. 启动（dev profile）

```bash
cd sample-application/sample-service/sample-service-server
mvn -q package -DskipTests
SPRING_PROFILES_ACTIVE=dev java -jar target/sample-service-server-0.0.1-SNAPSHOT.jar
```

- 缺省连 `jdbc:postgresql://localhost:5432/ddd_sample_application`（user/pass `ywf/ywf-local-123`）——全部可被 `DB_MASTER_URL` / `DB_MASTER_USER` / `DB_MASTER_PASSWORD` 覆盖；自配 URL 必须带 pgjdbc 超时三参数 `connectTimeout=10&socketTimeout=60&tcpKeepAlive=true`（教义见 datasource 配置注释）
- **不加 profile 会 fail-fast 启动失败**——这是 B1 轮的刻意设计（无默认 profile；`${DB_MASTER_URL}` 占位符无兜底），不是 bug
- 容器路线：`docker compose up --build` 起 app 容器（8080）；PG/Nacos 服务在 `docker-compose.yml` 内是注释态，取消注释即得全套

### 3. 健康确认

```bash
curl http://localhost:8080/api/actuator/health   # context-path=/api 同样罩住 actuator
# {"status":"UP",...}
```

### 4. 跑通订单全生命周期（八步）

```bash
# ① 建商品（拿到 productId）
curl -sX POST localhost:8080/api/products -H 'content-type: application/json' \
  -d '{"name":"widget","price":9.90,"stock":100}'

# ② 下单（customerId 任意串；扣库存，PENDING）
curl -sX POST localhost:8080/api/orders -H 'content-type: application/json' \
  -d '{"customerId":"c-001","productId":"<上一步id>","quantity":2}'

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

每步返回 OrderCO（含 status 流转）；`items` 明细仅详情端点给出（分页面是精简投影）。

### 5. 体验失败通道（文档三分法的手感版）

```bash
# 状态机违规 → 422 + i18n 位点（detail 为 messageKey，前端负责渲染文案）
curl -sX PUT localhost:8080/api/orders/$ID/pay | head -c200        # 已完成单再支付

# 输入越界 → 400（@Size 在绑定层拦截，先于业务；字段名在 fieldErrors，不回显脏值）
curl -sX PUT "localhost:8080/api/orders/$(uuidgen)/ship?trackingNumber=$(python -c 'print("T"*101)')"

# 取消带原因（≤500 字符）→ 200 后订单 CANCELLED
curl -sX PUT localhost:8080/api/orders/$(新单id)/cancel -H 'content-type: application/json' -d '{"orderId":"$(新单id)","reason":"user-request"}'
```

## 实现状态表（本文不演示的、docs 里存在但 sample 未落地）

| 能力 | 状态 | 指北 |
|---|---|---|
| 定时任务 / Gateway / 批量操作 / 分布式事务(Seata) / Nacos 注册发现 | ⛔ 未实现（教学教例） | 对应 how-to 篇首「实现状态」段 |
| Security JWT 资源服务器链 | ✅ 可启用（配置开关见 ide-dev.env 注释区） | common-security |

## 下一步

- 读懂每层为什么这样设计 → `knowledge/docs/explanation/`
- 动手加自己的用例 → 先立 `sample-application/specs/changes/`，再用 `.agents/skills/new-usecase`
