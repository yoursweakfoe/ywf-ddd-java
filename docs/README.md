# docs — 完整文档索引（全仓唯一登记处）

> **唯一索引**：新增文档只登记本文件（根 `README.md` 与 `AGENTS.md` 均指向此处，不再各自维护文档树）。
> 人类入口 → [根 README](../README.md)；AI 代理入口 → [AGENTS.md](../AGENTS.md)；规则与技能 → [.agents/](../.agents/)。

## docs/common/ — 公共框架模块文档（8 篇，框架设计决策与 ADR 所在）

| 文件 | 用途 |
|------|------|
| [common-contract.md](common/common-contract.md) | CQRS 契约标记接口（Command / Query / CO / IntegrationEvent）与重契约设计 |
| [common-ddd.md](common/common-ddd.md) | DDD 战术框架核心：领域构建块 + MybatisPersistence / DddMapper 手写 XML 持久化契约 + 审计填充，含模块 ADR |
| [common-exception.md](common/common-exception.md) | 统一异常体系（BusinessException + i18n 错误码位点 + RFC 9457 错误响应） |
| [common-cloud.md](common/common-cloud.md) | 微服务治理：Nacos / Seata / Feign / LoadBalancer / Resilience4j（均 opt-in）+ 东西向 JWT 透传 |
| [common-pg.md](common/common-pg.md) | PostgreSQL 类型映射（UUID / JSONB / 数组 TypeHandler 自动注册） |
| [common-security.md](common/common-security.md) | 零信任身份：服务自验 JWT（资源服务器）+ 边界 permit-all 链 + 方法级鉴权 |
| [common-observability.md](common/common-observability.md) | 可观测性：结构化日志（stdout）+ Actuator + OTel Java Agent |
| [common-test.md](common/common-test.md) | 测试基础设施：ArchUnit 规则集（R1–R15 系）+ Spring Boot Test 支撑 |

## docs/application/module-design/ — 应用架构设计（5 篇，「每层怎么设计」）

| 文件 | 用途 |
|------|------|
| [contract.md](application/module-design/contract.md) | Contract 层：Controller 契约接口 + CQE + CO，限界上下文边界 |
| [adapter.md](application/module-design/adapter.md) | Adapter 层：REST / 定时 / MQ 入口，纯透传 |
| [application.md](application/module-design/application.md) | Application 层：AppService / Handler / Assembler / Presenter 用例编排 |
| [domain.md](application/module-design/domain.md) | Domain 层：聚合根 / 值对象 / Policy / Domain Service，零框架依赖 |
| [infrastructure.md](application/module-design/infrastructure.md) | Infrastructure 层：Repository / Gateway 实现、持久化与配置 |

## docs/application/directory-structure/ — 目录结构速查（6 篇，「包怎么组织」）

| 文件 | 用途 |
|------|------|
| [overview.md](application/directory-structure/overview.md) | 服务全局目录结构（模块划分与层依赖总览） |
| [contract/contract.md](application/directory-structure/contract/contract.md) | contract 模块包结构（契约命名 canonical） |
| [server/adapter.md](application/directory-structure/server/adapter.md) | server 模块 adapter 层包结构 |
| [server/application.md](application/directory-structure/server/application.md) | server 模块 application 层包结构 |
| [server/domain.md](application/directory-structure/server/domain.md) | server 模块 domain 层包结构 |
| [server/infrastructure.md](application/directory-structure/server/infrastructure.md) | server 模块 infrastructure 层包结构 |

## docs/application/cookbook/ — 端到端代码实战（12 篇，「具体怎么写」）

子索引与逐篇导航表见 [cookbook/README.md](application/cookbook/README.md)（完整可编译代码走查）。本文件只链子索引、不复制其文件清单。

## 项目级文档（2 篇，「为什么这么选 / 术语指什么」）

| 文件 | 用途 |
|------|------|
| [glossary.md](glossary.md) | 项目术语表：框架术语 + 命名映射规范 + 订单域业务词汇（Ubiquitous Language） |
| [references.md](references.md) | 架构决策账本：采纳 / 未采纳理论模式及原因 + **ADR 总索引**（全仓 8 篇 common 文档 ADR 唯一对照表） |

## 与 .agents/ 的关系

`docs/`（本目录）面向**人类开发者**：设计论证 + 代码走查；[`.agents/`](../.agents/) 面向 **AI 编码代理**：指令式规则与任务技能，其规则从本目录文档提炼并按路径回引原理。

受众 / 风格 / 修改频率的分工角色表 canonical 在 [`.agents/README.md`](../.agents/README.md) 的「与 docs/ 的关系」一节，本文件不重复维护。
