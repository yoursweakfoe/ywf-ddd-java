# 01 — 项目概览

> 触发条件：接手任何本仓工作前（约 1 分钟）；项目定位/技术栈口径疑问时。

## 定位

DDD 战术模式微服务框架骨架 + 示例应用。目标是将 DDD 落地为可复用公共模块 + 可参照的教学示例。

> **当前状态**：探索性脚手架项目，外围环境暂未备齐，未经历真实生产考验；适合结构参考与学习，暂不推荐直接用于生产。

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
├── sample-application/    # 示例业务服务（真实例：两个示范聚合）
├── knowledge/               # 知识伞：docs 地图 / specs 法律 / decisions 判例卷宗 / scripts 执法工具链
├── .agents/               # AI 代理规范（本目录，dotagents 约定）
└── AGENTS.md              # AI 代理入口文件（厂商中立标准）
```

## 技术栈

Java 21 / Spring Boot 4.1 / Spring MVC REST / MyBatis（手写 XML SQL）/ PostgreSQL / Seata。完整技术栈与网关、东西向调用口径见根 `README.md` §项目背景。

## 模块设计原则

- common 模块 **opt-in**：业务服务按需引入，不强制全量依赖
- common 模块 **零业务逻辑**：仅提供技术骨架和构建块
- common 模块**依赖哲学按身份分叉**：工具库审最小化，定型装配审命运清单（构件身份二分法 + 模块登记表，唯一事实源见 rules 04「Common 模块约束」）
- sample-application 是**教学示例**：展示框架最佳实践，非生产代码
- 框架时间类型统一使用 **OffsetDateTime**（带时区，跨地域无歧义）

## 关键路径

| 需要了解 | 去哪里看 |
|---------|----------|
| 每层怎么设计（规则） | `knowledge/docs/explanation/` |
| 具体怎么写（代码） | `knowledge/docs/how-to/` |
| 包结构怎么组织 | `knowledge/specs/current/patterns/aggregate-blueprint.md` §5 通式 |
| 为什么这么选 | `knowledge/docs/explanation/theory-map.md` |
| common 模块怎么用 | `knowledge/docs/reference/api/*.md` |
| 示例场景选型理由 | `sample-application/README.md` |
| AI 代理规范详情 | `.agents/README.md` |
