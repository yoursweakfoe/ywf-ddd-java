---
name: new-service
description: 从框架骨架创建新的 DDD 微服务（Maven 模块 + 分层包结构 + 配置）。当需要新增一个独立部署的业务服务时使用。
---

# 新建微服务

## 前置阅读

1. `sample-application/`（完整参照）
2. `.agents/rules/02-architecture.md`（分层 + 包结构）
3. `ywf-ddd-common/README.md`（模块依赖拓扑 + BOM 引入方式）

## 步骤

### Phase 1: Maven 模块骨架

1. 创建服务根 POM（parent = spring-boot-starter-parent）
2. `properties` 中声明：
   - `common.version`（ywf-ddd-common 版本）
   - `lombok.version`（注解处理器不走 dependencyManagement，必须显式声明）
3. `dependencyManagement` 中 BOM import `ywf-ddd-common`：
   ```xml
   <dependency>
       <groupId>com.yoursweakfoe</groupId>
       <artifactId>ywf-ddd-common</artifactId>
       <version>${common.version}</version>
       <type>pom</type>
       <scope>import</scope>
   </dependency>
   ```
4. `build/pluginManagement` 中配置 maven-compiler-plugin 注解处理器：
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.projectlombok</groupId>
                   <artifactId>lombok</artifactId>
                   <version>${lombok.version}</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```
5. 创建两个子模块：
   - `{service}-contract/`（公开契约 jar）
   - `{service}-server/`（Spring Boot 应用）

> 完整参照：`sample-application/pom.xml`

### Phase 2: contract 模块

4. 依赖 `common-contract`（标记接口 + OpenAPI 注解）
5. 创建包结构：
   ```
   contract/{agg}/adapter/rest/   → Controller 契约接口（HTTP 映射 + 校验注解声明于此）
   contract/{agg}/dto/command/    → Command
   contract/{agg}/dto/query/      → Query
   contract/{agg}/dto/co/         → CO（契约输出）
   contract/{agg}/enums/          → 枚举（可选）
   ```

### Phase 3: server 模块

6. 依赖（按需选择）：
   - `common-ddd`（必选，DDD 构建块 + MyBatis 仓储支撑，starter 已传递）
   - `common-cloud`（必选，springdoc + Nacos 预留 + Seata）
   - `spring-boot-starter-webmvc`（必选，REST 面 + 内嵌 Tomcat）
   - `common-observability`（推荐，Actuator + 日志）
   - `common-pg`（使用 PostgreSQL 时）
   - `common-security`（需要身份上下文时）
   - `common-test`（test scope）
7. 创建分层包结构：
   ```
   adapter/rest/controller/（ControllerImpl；东西向同样经 HTTP 消费契约接口，一期 RestClient 直连）
   application/{agg}/service/ + handler/command/ + handler/query/ + assembler/ + presenter/ + dto/ + repository/application/
   domain/{agg}/model/ + repository/domain/ + portal/
   domain/shared/service/
   infrastructure/persistence/master/{agg}/mybatis/po/ + mybatis/mapper/ + converter/ + repository/domain/ + repository/application/
    （SQL 手写 XML 归 resources/mapper/{agg}/）
   infrastructure/gateway/
   infrastructure/config/
   ```
 8. 创建 `Application.java`（@SpringBootApplication）

### Phase 4: 配置文件

9. `application.yml`（主配置：REST 端口 / 数据源 / MyBatis / Actuator）——数据源用普通单 `spring.datasource`（多数据源需求经 dynamic-datasource opt-in）；MyBatis 用 `mybatis.*` 命名空间，**yaml 块 canonical 见 `docs/application/module-design/infrastructure.md`**（`type-aliases-package` 指向 `infrastructure.persistence.master`、`mapper-locations: classpath*:/mapper/**/*.xml`、`map-underscore-to-camel-case: true`、`log-impl: Slf4jImpl` 四键，照抄不重写）
10. `application-dev.yml`（开发环境：与 prod 的差异项，如 Nacos 配置中心预留）
11. `application-prod.yml`（生产环境：springdoc 禁用 + 管理端口收紧）

### Phase 5: 部署

12. `Dockerfile`（eclipse-temurin:21-jre-alpine + OTel Agent）
13. `docker-compose.yml`（服务 + 外部网络）

## 验证

- [ ] `mvn clean compile` 编译通过
- [ ] BOM import 版本与 ywf-ddd-common 一致
- [ ] 包结构符合 `.agents/rules/02-architecture.md`
- [ ] application.yml 数据源为普通单 `spring.datasource`（多数据源需显式引入 dynamic-datasource 并说明理由）
- [ ] Dockerfile 默认 profile=prod，OTel 环境变量齐全
- [ ] 无多余依赖（每个引入的 common 模块都有明确用途）

## 文档同步

- 更新 `docs/application/directory-structure/overview.md`（如为示例服务）
- 服务根目录创建 README.md（说明服务职责 + 构建方式）
