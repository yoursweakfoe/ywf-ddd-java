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
   contract/{agg}/api/       → Service 接口
   contract/{agg}/dto/       → Command / Query
   contract/{agg}/co/        → CO（契约输出）
   contract/{agg}/enums/     → 枚举（可选）
   ```

### Phase 3: server 模块

6. 依赖（按需选择）：
   - `common-ddd`（必选，DDD 构建块 + MyBatis-Plus）
   - `common-cloud`（必选，Boot 原生 gRPC + springdoc + Seata）
   - `spring-boot-starter-webmvc`（必选，REST 面 + 内嵌 Tomcat）
   - `common-observability`（推荐，Actuator + 日志）
   - `common-pg`（使用 PostgreSQL 时）
   - `common-security`（需要身份上下文时）
   - `common-test`（test scope）
7. 创建分层包结构：
   ```
   adapter/web/（Controller）+ adapter/grpc/（gRPC service，如有东西向接口）
   application/{agg}/handler/ + assembler/ + presenter/ + dto/
   domain/{agg}/model/ + repository/ + portal/
   domain/shared/service/
   infrastructure/persistence/master/{agg}/po/ + converter/ + mapper/ + repository/
   infrastructure/gateway/
   infrastructure/config/
   ```
8. 创建 `Application.java`（@SpringBootApplication）

### Phase 4: 配置文件

9. `application.yml`（主配置：REST 端口 / gRPC 端口 / 数据源 / MyBatis-Plus / Actuator）
10. `application-dev.yml`（开发环境：gRPC 客户端通道地址）
11. `application-prod.yml`（生产环境：springdoc 禁用 + 管理端口收紧）

### Phase 5: 部署

12. `Dockerfile`（eclipse-temurin:21-jre-alpine + OTel Agent）
13. `docker-compose.yml`（服务 + 外部网络）

## 验证

- [ ] `mvn clean compile` 编译通过
- [ ] BOM import 版本与 ywf-ddd-common 一致
- [ ] 包结构符合 `.agents/rules/02-architecture.md`
- [ ] application.yml 中数据源使用 dynamic-datasource 格式
- [ ] Dockerfile 默认 profile=prod，OTel 环境变量齐全
- [ ] 无多余依赖（每个引入的 common 模块都有明确用途）

## 文档同步

- 更新 `docs/sample-application/directory-structure/overview.md`（如为示例服务）
- 服务根目录创建 README.md（说明服务职责 + 构建方式）
