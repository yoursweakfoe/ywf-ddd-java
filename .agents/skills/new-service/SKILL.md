---
name: new-service
description: 从框架骨架创建新的 DDD 微服务（Maven 模块 + 分层包结构 + 配置）。当需要新增一个独立部署的业务服务时使用。
---

# 新建微服务

## 前置阅读

1. `sample-application/`（全仓唯一真实骨架参照：pom 层级 / 配置 / Docker / compose 皆有活体样本）
2. `knowledge/specs/current/patterns/aggregate-blueprint.md` §5（服务骨架通式：一个服务该有哪些目录、各目录住什么）
3. `knowledge/specs/current/patterns/coding-conventions.md` §2（CC-3 结构映射：目录 / groupId / 包名 / 服务名命名，本文件不复述）
4. `ywf-ddd-common/README.md`（模块依赖拓扑 + BOM 引入方式）

## 步骤

### Phase 1: Maven 模块骨架

1. 三层 POM 结构：应用根 POM（parent = `spring-boot-starter-parent`，packaging=pom）→ 服务聚合 POM `{service}/`（packaging=pom）→ 两个叶子模块 `{service}-contract/`（公开契约 jar）与 `{service}-server/`（Spring Boot 应用）。形状逐级照抄 `sample-application/pom.xml` → `sample-application/sample-service/pom.xml`
2. 命名映射（目录 kebab-case、groupId、Java 包名、Spring 服务名）→ CC-3（`knowledge/specs/current/patterns/coding-conventions.md` §2.2），本文件不复述
3. 应用根 POM `properties`：`common.version` + `lombok.version`（annotationProcessorPaths 不走 dependencyManagement，须显式声明）+ 用 `maven.compiler.release=21` 压过 Boot parent 的缺省值；若引入 `common-cloud`，Spring Cloud / Spring Cloud Alibaba BOM 也须自行 import（BOM 不传递 properties，参照样本注释）
4. 应用根 POM `dependencyManagement` import `ywf-ddd-common` BOM、`build/pluginManagement` 配 lombok 注解处理器——两段完整 XML 以 `sample-application/pom.xml` 为参照照抄，不在此重写

### Phase 2: contract 模块

1. 只依赖 `common-contract`（标记接口 + 注解级依赖：HTTP 映射 / 校验 / 文档注解一律声明在 Controller 契约接口上，CC-7）
2. 模块内容白名单 = Controller 契约接口 + CQE 三件套 + CO + 契约枚举（CC-7；目录槽位 `contract/{agg}/adapter/rest/controller + dto/{command,query,co} + enums` → blueprint §5，逐件清单 → blueprint §1 ①-④+㉒）。聚合建成走 skill `new-aggregate`，本文件不手抄包树

### Phase 3: server 模块

1. server 装配（全部 opt-in，角色详见各模块卷卷首，本文件不复述）：
   - `common-ddd`（必选：DDD 构建块 + MyBatis 仓储定型装配）
   - `common-exception`（必选：Domain 直用 `BusinessException`，显式声明而非依赖 common-ddd 传递——样本惯例）
   - `spring-boot-starter-webmvc`（必选：REST 面 + 内嵌容器）
   - `common-observability`（推荐：Actuator + Prometheus）
   - `common-pg`（用 PostgreSQL 时：TypeHandler 自动注册）
   - `common-security`（需要身份上下文取用时，层位约束 → CC-8）
   - `common-cloud`（按需：Feign / Nacos / Seata / LB / 熔断——引入后各能力还须逐个显式声明对应 starter，→ `knowledge/specs/current/modules/cloud.md` §3.1）
   - `common-test`（test scope：ArchUnit 共享规则集 + Boot Test）
   - 最小充分原则：禁止引入当前不使用的组件（禁令卷 §4；各模块身份登记表 → 禁令卷 §9）
2. 分层目录骨架照 blueprint §5 通式逐级创建（adapter 不按聚合分包、persistence 按数据源→聚合自包含、gateway 按外部能力分子包、XML 归 `resources/mapper/{agg}/` 等口径皆以该节为准）——通式只裁「该有哪些目录、各住什么」，逐槽形状由 §1 清单 + §2/§4 条款约束，真实包树不设二手地图，核对走 glob
3. 创建 `Application.java`（`@SpringBootApplication`，居包根，真实例见 `sample-application/sample-service/sample-service-server/src/main/java` 源码树根）

### Phase 4: 配置文件

1. `application.yml`（主配置：服务端口 / 数据源 / MyBatis / Actuator）——canonical 全文 `sample-application/sample-service/sample-service-server/src/main/resources/application.yml`（文件头注释即权责边界说明）。数据源用普通单 `spring.datasource` 且占位符一律不带默认值（漏配环境变量即启动失败，杜绝静默回落）；多数据源需求经 dynamic-datasource opt-in（→ `knowledge/docs/explanation/infrastructure.md`「多数据源规则」）；`mybatis.*` yaml 块 canonical → 同篇「MyBatis 配置」节，照抄参照不在此重写
2. `application-dev.yml` / `application-prod.yml`（环境差异项：dev 本地默认值 + Nacos 注释预留；prod springdoc 禁用 + 管理端口收紧）——形状照抄样本同名两文件

### Phase 5: 部署

1. `Dockerfile`（居 `{service}-server/`）——canonical 参照 `sample-application/sample-service/sample-service-server/Dockerfile`：temurin 21 JRE alpine（钉小版本）+ 构建期下载 OTel Agent + 默认 profile=prod + Actuator 健康检查 + 容器感知 JVM 参数（镜像 tag / Agent 版本属易变事实，以样本现值为准，本文件不抄）
2. `docker-compose.yml`（居应用根）——参照 `sample-application/docker-compose.yml`：服务条目 + build context 指向 server + 环境变量注入（OTel / DB 凭证），容器内 PG/Nacos 为注释预留段

## 验证

- [ ] `mvn clean compile` 编译通过
- [ ] BOM import 版本与 ywf-ddd-common 一致
- [ ] 目录骨架符合 `knowledge/specs/current/patterns/aggregate-blueprint.md` §5 通式（聚合逐槽建满另按 §1 清单走 `new-aggregate` 对账）
- [ ] application.yml 数据源为普通单 `spring.datasource`（多数据源需显式引入 dynamic-datasource 并说明理由）
- [ ] ArchUnit 通过（引入 common-test 后即挂共享规则集，`knowledge/specs/current/modules/test.md` 场景 1）
- [ ] Dockerfile 默认 profile=prod，OTel 环境变量齐全
- [ ] 无多余依赖（每个引入的 common 模块都有明确用途，对照禁令卷 §9 登记表）

## 文档同步

- 服务根目录创建 README.md（说明服务职责 + 构建方式，参照 `sample-application/README.md`）
