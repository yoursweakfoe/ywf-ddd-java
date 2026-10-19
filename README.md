# ywf-ddd-java

一个以 **AI 辅助开发 DDD Java 框架**为主题的实验仓库，保留框架源码、订单与商品示例，以及约束 AI 编码行为的知识产物。

## 实验结束：0.2.0 封存

**0.2.0 是本实验的最终源代码版本。实验已经结束，封存后不再继续开发、维护或提供安全更新。**

本项目**非生产就绪**。源码、测试和文档是一次探索留下的结果，不构成对可用性、技术成熟度、正确性或安全性的承诺。不要将它作为获得持续支持的业务基础设施；如需研究或修改，请自行评估、复现并承担后续维护。

## 探索了什么

实验尝试把 DDD 的聚合、仓储、用例处理与分层约束组织成可复用的 Java 模块，并观察 AI 在源码、行为契约、架构检查和操作技能共同约束下如何工作。仓库既保存实现，也保存设计解释、约束与历史裁决，供阅读和比较；这些产物并不意味着实验已经证明某种工程方法有效。

当前代码基线是 **JDK 21、Spring Boot 4.1.0、MyBatis 与 PostgreSQL 18**。持久化采用手写 XML SQL；数据库变更由独立的 Liquibase 执行器管理。

- `ywf-ddd-common` 包含 8 个公共模块：`common-contract`、`common-ddd`、`common-pg`、`common-security`、`common-test`、`common-observability`、`common-cloud`、`common-exception`。另有不发布的 `common-packages-integration-test` 测试试验场；模块存在不代表相关能力已经完成生产验证。
- `sample-application` 是订单与商品的单服务示例，展示 REST 接口、用例编排、聚合行为、库存协调及数据库读写，不是完整电商系统。
- `db-migration` 是不进入根 Maven reactor 的独立工程，执行数据库迁移并取回 schema 快照。应用与测试都依赖预先迁移好的数据库。
- 示例当前没有 Nacos 运行时依赖。不要把知识文档中的扩展设想理解为已接入的注册发现、事件投递或分布式系统能力。

## 从哪里读起

| 目的 | 入口 |
|---|---|
| 浏览说明与设计背景 | [文档索引](knowledge/docs/README.md) |
| 尝试构建、运行与 HTTP 示例 | [快速开始](knowledge/docs/tutorials/quickstart.md) |
| 阅读订单与商品实现 | [示例应用](sample-application/README.md) |
| 准备运行库与测试库 | [数据库迁移说明](db-migration/README.md) |
| 了解源码所受的行为约束 | [框架规范](knowledge/specs/README.md)、[示例业务规范](sample-application/specs/README.md) |
| 查看 AI 开发入口与技能 | [AGENTS.md](AGENTS.md)、[.agents](.agents/README.md) |

现存知识文档保留了实验过程中的设计语境与扩展讨论；涉及运行方式时，以当前源码、配置及上述运行说明相互核对，不将历史讨论视为后续交付计划。

## 目录

```text
ywf-ddd-java/
├── pom.xml                 # common 与 sample 的根构建入口
├── ywf-ddd-common/          # 公共模块及测试试验场
├── sample-application/      # 订单 / 商品示例与应用容器配置
├── db-migration/            # 独立迁移工程、变更集与 schema 快照
├── knowledge/               # 描述文档、规范、历史案卷与检查脚本
├── .agents/                 # AI 辅助开发技能与入口说明
└── AGENTS.md                # 代理阅读入口
```

## 可选：复现实验

这些步骤用于自行研究，不是已验证的生产部署方案。需要本地 JDK 21、Maven，以及能够解析项目依赖的仓库访问环境；容器方式还需要可用的 Docker Engine / Docker Compose。

1. **自行准备 PostgreSQL 18**、数据库用户和连接权限。仓库的应用 Compose 不提供数据库或 Nacos。创建运行库 `ddd_sample_application` 与测试库 `ddd_sample_application_test`；本地配置默认用户/口令为 `ywf` / `ywf-local-123`，仅供隔离实验使用。
2. 按 [db-migration 说明](db-migration/README.md) 从该目录运行迁移。通过 `DB_MIGRATION_URL` 分别连接两个库，对两库应用同一变更集；自配用户和口令时同时设置 `DB_MIGRATION_USER` / `DB_MIGRATION_PASSWORD`。示例服务的测试不会自动建表。
3. 回到仓库根目录执行 `mvn clean install`。示例测试使用 `DB_TEST_URL` / `DB_TEST_USER` / `DB_TEST_PASSWORD` 覆盖默认连接；common 测试另用 `DB_IT_HOST` / `DB_IT_PORT` / `DB_IT_DB` / `DB_IT_USER` / `DB_IT_PASSWORD`，默认在本机 5432 自动创建 `ddd_framework_test` 并复位其中的专用 schema，需要建库权限。不要将测试配置指向业务数据。
4. 本机运行可使用 `java -jar sample-application/sample-service/sample-service-server/target/sample-service-server-0.2.0.jar --spring.profiles.active=dev`。`dev` 默认连接本机运行库；可用 `DB_MASTER_URL` / `DB_MASTER_USER` / `DB_MASTER_PASSWORD` 覆盖。自配 JDBC URL 保留 `connectTimeout=10&socketTimeout=60&tcpKeepAlive=true`。
5. 如使用容器，先在根目录执行 `mvn package -DskipTests` 生成 JAR，再进入 `sample-application` 执行 `docker compose up --build`。Compose 显式使用 `dev`，默认经 `host.docker.internal:5432` 连接宿主数据库；Docker Desktop 下也须确保宿主 PostgreSQL 允许来自容器的连接。非默认地址通过同名 `DB_MASTER_*` 环境变量覆盖。

服务的实验接口与健康检查入口见 [快速开始](knowledge/docs/tutorials/quickstart.md)。构建及运行还依赖本机环境、依赖仓库访问和数据库准备，不能据此推导生产可用性。

## 许可

采用 [MIT License](LICENSE)，版权信息见许可证原文。软件按原样提供，不附带担保；使用或再分发时请保留相应版权和许可声明。
