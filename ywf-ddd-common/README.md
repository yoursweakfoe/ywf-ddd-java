# ywf-ddd-common

DDD 公共基础模块，为业务项目提供领域建模、持久化基础设施、微服务治理等通用能力。
所有模块均为 opt-in 设计，业务服务按需引入。各模块详细文档见 [docs/](docs/)。

## 模块结构

```
ywf-ddd-common/
├── docs/                  # 各模块详细文档
├── common-contract/       # CQRS 契约标记接口（Command / Query / Event）
├── common-ddd/            # DDD 框架（领域模型 + CQRS 契约 + MyBatis-Plus 仓储 + 领域事件）
├── common-exception/      # 统一异常体系（BusinessException + REST 全局异常处理）
├── common-pg/             # PostgreSQL TypeHandler 扩展（UUID / JSONB / 数组）
├── common-security/       # 身份上下文（REST 边界 Header 解析 → SecurityContext）
├── common-cloud/          # 微服务治理（springdoc + Nacos 预留 + Seata）
├── common-observability/  # 可观测性（结构化日志 + Actuator + Prometheus）
└── common-test/           # 测试基础设施（ArchUnit 架构守护 + Spring Boot Test）
```

## 模块依赖拓扑

```
common-contract（独立，纯标记接口）
     ↑
common-exception（独立，REST 异常处理）
     ↑
common-ddd → common-contract + common-exception
     ↑
common-pg → common-ddd（TypeHandler 依赖 MyBatis 基础设施）

common-cloud → common-exception（聚合异常处理 + 微服务组件）
common-security（独立）
common-observability（独立）

common-test（独立，test scope）
     ↑ test scope
common-ddd / common-exception / common-security（框架模块自测）
```

## 快速开始

```bash
cd ywf-ddd-common
mvn clean install -DskipTests
```

业务项目引入（BOM 方式）：

```xml
<!-- 父 POM dependencyManagement -->
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>ywf-ddd-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- 子模块 dependencies（按需选择） -->
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-ddd</artifactId>
</dependency>
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-cloud</artifactId>
</dependency>
```