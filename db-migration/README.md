# db-migration —— Liquibase 迁移执行器（Spring Boot 一次性 Job）

ywf-ddd-java 的基础设施数据库迁移工程：启动 → Liquibase 自动应用变更集 →
**取回 schema 事实现状快照** → 进程退出。执行模型移植自 omate-bi-flyway
（Flyway 版），核心思想相同：**快照取回相当于在建表 schema 级别把全部 migration
合并成一份可 review 的「库内事实」**——变更集表达意图，current 表达结果，两者
成对演进、互相校验。

> 当前暂居本仓库根下的独立目录，**刻意不进根聚合 reactor**：与框架/sample 构建
> 零耦合，未来整体平移为独立 schema 迁移项目即可。

## 目录结构与职责

```
src/main/resources/db/
├── changelog/                     Liquibase 唯一执行源
│   ├── db.changelog-master.yaml   includeAll 结构薄壳（纯 SQL 格式无聚合能力，唯一 YAML）
│   └── <库名>/changes/            变更集正文：一律「纯 SQL formatted」，4 位递增序号 + slug
│       └── 0001-init-schema.sql
└── current/                       schema 事实现状（Job 每次成功后自动重写，入库）
    └── <库名>/<schema>/<表名>.sql
```

- **无 `init/` 冷启动基线**（与 omate-bi-flyway 的差异）：那是 Flyway 纳管存量库的
  历史包袱；本项目 Liquibase 从空库即全量演进，`0001` 变更集本身就是基线。
- `changelog/` 按库分目录：当前仅 `ddd_sample_application`（sample 服务专属库，
  遵循 ywf-infra「谁消费谁自建」约定）；新增库 = 新建同名目录 + 为 Job 传入对应
  `DB_MIGRATION_SNAPSHOT_DIR` / `SCHEMAS`。

## 变更集格式合同

- 正文一律 `--liquibase formatted sql`：DDL 即正文，逐条可审阅、可 grep、应急可
  进 psql 手工重放。
- **已执行的变更集永不修改**（Liquibase 按 checksum 锁定，改历史 = 其他环境报错）；
  修正 = 追加新变更集。
- 形状规范（PG 原生，不受测试脚本旧形束缚）：主键 `UUID DEFAULT uuidv7()`（PG18 内建）；
  时间列 `created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`；软删 `is_deleted`；
  乐观锁 `version BIGINT`；审计 by 列 `UUID`。snake_case；不设 DB 触发器（审计由应用层
  填充，DB 默认只兜手工插入）。
- schema 归属与命名法：业务聚合边界 = schema 边界，且**领域词单数**（领域是概念，概念无
  复数——`product.product`；与 SQL 保留字冲突时升级到更精确的行业术语，如订单域
  `sales_order.sales_order`，不用引号/前缀逃逸）；与 Java 包名单数惯例逐字同构，
  微服务拆分时整 schema 平移。外部工具账表住各自独立 schema（Liquibase 账表 →
  `liquibase`）。工具与测试的局限不得影响业务设计。

## 运行

前提：ywf-infra 的 postgres 环节在跑（`localhost:5432`，超管 `ywf`，基线见
`ywf-infra/stages/postgres/compose.yml`）。

```powershell
# ① 建库（一次性；查询输出为空才建，CREATE DATABASE 无 IF NOT EXISTS）
docker exec postgres psql -U ywf -d postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'ddd_sample_application'"
docker exec postgres psql -U ywf -d postgres -c "CREATE DATABASE ddd_sample_application OWNER ywf"

# ② 跑 Job（工作目录 = 本目录；dev 画像自带本地默认值，见 application-dev.yml）
mvn -B spring-boot:run "-Dspring-boot.run.profiles=dev"
# 或打包后：mvn -B package 然后 java -jar target/db-migration-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

日常节奏：改表结构 = 在 `changelog/<库名>/changes/` 追加新 SQL 文件 → 跑一次 Job →
`db/current/` 自动刷新 → **变更集与快照 diff 成对提交**（PR 里意图与事实同屏）。

生产 / CI（K8s Job、流水线 Step）：不用 dev 画像，激活 prod 并注入全部必需环境变量：

```
SPRING_PROFILES_ACTIVE=prod
DB_MIGRATION_URL / DB_MIGRATION_USER / DB_MIGRATION_PASSWORD
DB_MIGRATION_SNAPSHOT_DIR=<可写持久卷绝对路径>   # prod 画像去默认：事实现状必须落卷，
                                                 # 否则快照随容器销毁蒸发，对账价值归零
```

缺任一必需项 = 启动失败（fail-fast）；不带 profile 裸跑同样是零默认生产形——dev 的
localhost 兜底只能被命令行显式激活，永远不会静默生效。

### 环境变量一览

| 变量 | 必填 | 默认值 | 说明 |
|---|---|---|---|
| `DB_MIGRATION_URL` | 生产必填（dev 有本地默认） | — | PostgreSQL 连接串，须自带 pgjdbc 超时三参数 |
| `DB_MIGRATION_USER` | 生产必填（dev 有本地默认） | — | 数据库用户 |
| `DB_MIGRATION_PASSWORD` | 生产必填（dev 有本地默认） | — | 数据库口令 |
| `DB_MIGRATION_SNAPSHOT_SCHEMAS` | 否 | `orders,products,public` | 快照 schema 清单（逗号分隔） |
| `DB_MIGRATION_SNAPSHOT_DIR` | prod 必填；否则有默认 | `src/main/resources/db/current/ddd_sample_application` | 快照输出根目录；prod 画像已去默认（必须指可写卷绝对路径），见 application-prod.yml |

## 版本基线

| 组件 | 版本 | 来源 |
|---|---|---|
| Spring Boot | 4.1.0 | = ywf-ddd-common 父 pom 同基线 |
| Liquibase | 5.0.3（BOM 纳管，不覆写） | Boot 已测试组合 |
| PostgreSQL JDBC | 42.7.13 | = 框架 pin（`ywf-ddd-common/pom.xml`） |
| JDK | 21 | = 框架基线 |

## 快照覆盖与边界

`SchemaSnapshotRunner` 逐 BASE TABLE 取回：列（类型渲染对齐 pg_dump）/ NOT NULL /
DEFAULT / 表与列注释 / **PK、UNIQUE、CHECK、FK（pg_constraint）** / 索引
（pg_indexes.indexdef）。迁移失败则启动中止，快照不执行——**快照只会反映迁移成功后
的事实**；快照自身失败 = 非 0 退出（事实现状宁可缺席、不可失真，也绝不静默过期）。

当前**不**覆盖：视图、函数、序列、域、触发器、分区、RLS。库内出现这类对象时按表
扩查询（刻意不预建，避免维护用不上的复杂度）。

## 已知边界 / TODO

- Liquibase 账表落在 `public`；若未来按库分配独立 schema，同步改 `spring.liquibase`
  的 changelog-schema 配置与快照清单。
- 多库并存后应支持单次运行遍历多库（当前一跑一库，用环境变量切换）；独立成项目后
  再按真实需求展开。
- 生产化接入（CI Step / K8s Job 镜像）未建——Dockerfile 一个文件的事，等运维环节
  真要做时再立。
