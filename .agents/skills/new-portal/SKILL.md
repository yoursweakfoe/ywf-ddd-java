---
name: new-portal
description: 为已有聚合新增外部系统集成（Portal 接口 + Gateway 实现）。当需要对接支付、文件存储、短信、第三方 RPC 等外部能力时使用。
---

# 新增 Portal / Gateway

## 前置阅读

1. `knowledge/specs/current/patterns/boundary/external-gateway.md`（**法卷=唯一权威**：§1 条款 GW-1～GW-9、§2 规范形状 2.1～2.5、§3 生效登记；教例 payment+支付宝为虚构教例、sample 未实现，照形状施工勿在 sample 找真实例）
2. `knowledge/docs/how-to/gateway.md`（设计卡：何时需要外部集成 + 四个设计决策点，零形状代码）
3. `knowledge/specs/current/patterns/discipline/prohibitions.md`（§1 Domain 禁止含 R4 白名单；§4 Infra 禁止，ACL 互指 GW-2）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立四件套（模板在 `knowledge/specs/changes/_template/`）：specify（问题/验收 AC 账/不做/裁决问句）→ plan（技术裁量 + 对 current 的修卷 delta：ADDED/MODIFIED/REMOVED，SHALL+Scenario）→ tasks（纯清单）→ implement（执行账）。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（归属法卷 §4）。

## 概念

> Portal=Domain 层接口声明「我需要什么」（纯领域语言），Gateway=Infra 层实现决定「谁提供、怎么调」（技术调用 + ACL 翻译 + 容错），外部格式不越过 Gateway 边界。调用链路图 → 法卷 §2.1；选型判据（要不要 Portal、返回值落位、能力粒度、在哪边界调用）→ 设计卡四决策点。

## 步骤（与法卷 §2 形状编号对齐）

### 1. Domain 层：定义 Portal 接口（法卷 §2.2 · GW-1/GW-5/GW-9）

- 位置 `domain/{agg}/portal/{Xxx}Portal.java`：以 Portal 结尾、extends common-ddd `Portal` 标记接口（框架侧已在库，见法卷 §3 生效登记）
- 出入参一律**领域语言**（领域对象/值对象/基本类型），禁外部 SDK 类型；接口所在 domain 包零框架运行时依赖（纯标记接口豁免）
- 形状唯一样本 → 法卷 §2.2，本 skill 不复制代码

### 2. Domain 层：定义返回值对象（如需）（法卷 §2.2 · GW-6）

- 它是值对象，落位 `domain/{agg}/model/`（跨聚合可 `domain/shared/model/`），**不放 portal/**（portal/ 只准入接口）
- 纯领域语义，不暴露外部系统的错误码/原始响应（翻译边界 → GW-2）

### 3. Infrastructure 层：实现 Gateway（法卷 §2.3 · GW-2/GW-3/GW-4/GW-7）

- 位置 `infrastructure/gateway/{capability}/{Provider}XxxGateway.java`：一个外部能力一个子包（禁扁平落 gateway 根包），命名表/分包 → 法卷 §2.5
- 标注 `@Component`，构造器注入外部 SDK Client
- 职责三件套（全部收在 Gateway 层，Domain 不感知）：技术调用 → ACL 翻译（外部模型↔领域模型，成功/失败语义映射）→ 容错（超时/重试/幂等键，策略参数经配置注入禁硬编码，对 domain 透明）
- 一个 Portal 只表达一类外部能力，禁「上帝 Gateway」聚合不相关外部系统（GW-4）
- 形状唯一样本 → 法卷 §2.3

### 4. 调用方（法卷 §2.4 · GW-8）

- Handler 或 Domain Service 经构造器注入 **Portal 接口**而非 Gateway 实现（依赖倒置）；事务内调用 Portal 时事务边界仍在 Handler 的 `@Transactional`
- Domain 层禁字段注入 `@Autowired`；`@Service`/`@Component` stereotype 注册在位（禁令卷 §1 R4 白名单）

## 验证

- [ ] Portal 命名/落位/标记接口符合 GW-5，出入参无外部 SDK 类型（GW-1）
- [ ] 返回值对象落 `model/` 不放 portal/（GW-6）
- [ ] Gateway 落 `infrastructure/gateway/{capability}/` 子包、命名 `{Provider}XxxGateway` + `@Component`（GW-7）
- [ ] 外部格式不泄漏进 Domain；Gateway 内仅翻译与容错、无领域规则（GW-2）
- [ ] 超时/重试/幂等经配置注入，不依赖 SDK 默认值、不硬编码（GW-3）
- [ ] 调用方注入 Portal 接口而非 Gateway 实现（GW-8）
- [ ] 外部调用失败抛 `BusinessException` + i18n 位点 `{aggregate}:err.{scene}`（exception 卷 EV-2），新 key 登记 `knowledge/docs/how-to/error-handling.md` 账本（归属法 §5）
- [ ] `mvn compile` + ArchUnit 通过（Domain 中立 R4 不破）

## 文档同步

- 行为契约已在第 0 步闭环（changes/ 四件套归档折叠=同步义务唯一时点），本节只列同时可能触发的其余文档义务
- 新增**通用容错模式 / 统一 HTTP Client 等通用 Gateway 基础设施** = 框架行为变更 → 走 `knowledge/specs/changes/<slug>/` 修订 `knowledge/specs/current/patterns/boundary/external-gateway.md`（或对应 common 模块卷），归档后 docs 设计卡跟随；动了 common 公开 API 则更新 `knowledge/docs/reference/api/common-*.md`，程序走 `modify-common-module` skill
- sample 首次真实落地业务 Gateway：法卷 §3 生效登记业务侧行（现为 ⛔ 虚构教例）须随 changes/ 程序同修，不得直接改法卷
- 仅选型判据变化（何时用/决策点）才动设计卡 `knowledge/docs/how-to/gateway.md`
