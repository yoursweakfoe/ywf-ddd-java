# 01 — 项目概览

## 定位

DDD 战术模式微服务框架骨架 + 示例应用。目标是将 DDD 落地为可复用公共模块 + 可参照的教学示例。

> **当前状态**：v0.0.1 纯骨架阶段，稳定性优先，未经历真实生产考验。

## 仓库结构

```
ywf-ddd-java/
├── ywf-ddd-common/        # 公共基础框架（8 个 opt-in 模块）
│   ├── common-ddd/        # DDD 构建块（核心）
│   ├── common-contract/   # CQRS 标记接口
│   ├── common-exception/  # 统一异常
│   ├── common-pg/         # PostgreSQL 类型映射
│   ├── common-security/   # 身份上下文
│   ├── common-cloud/      # 微服务治理
│   ├── common-observability/ # 可观测性
│   └── common-test/       # ArchUnit 守护 + 测试基础设施
├── sample-application/    # 示例业务服务（电商：Order + Product）
├── docs/                  # 项目级文档（设计原理 + 代码模板）
├── .agents/               # AI 代理规范（本目录，dotagents 约定）
└── AGENTS.md              # AI 代理入口文件（厂商中立标准）
```

## 技术栈

Java 21 / Spring Boot 4.1 / Spring MVC（REST）+ Boot 原生 gRPC（东西向）/ MyBatis-Plus 3.5 / PostgreSQL / Nacos 客户端（预留）/ Seata 2.6 / Higress

## 模块设计原则

- common 模块 **opt-in**：业务服务按需引入，不强制全量依赖
- common 模块 **零业务逻辑**：仅提供技术骨架和构建块
- common 模块 **依赖最小化**：每个模块仅声明自身编译必需的最窄依赖
- sample-application 是**教学示例**：展示框架最佳实践，非生产代码
- 框架时间类型统一使用 **OffsetDateTime**（带时区，跨地域无歧义）

## 关键路径

| 需要了解 | 去哪里看 |
|---------|----------|
| 每层怎么设计（规则） | `docs/sample-application/module-design/` |
| 具体怎么写（代码） | `docs/sample-application/cookbook/` |
| 包怎么组织 | `docs/sample-application/directory-structure/` |
| 为什么这么选 | `docs/references.md` |
| common 模块怎么用 | `ywf-ddd-common/docs/common-*.md` |
| 示例场景选型理由 | `sample-application/README.md` |
| AI 代理规范详情 | `.agents/README.md` |
