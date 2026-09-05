# 02 — 分层架构

## 依赖方向（不可违反）

```
adapter → application → domain ← infrastructure
                           ↑
                      零外部依赖
                   （纯 Java + common-ddd）
```

- adapter 依赖 application（调用 AppService）
- application 依赖 domain（编排聚合根 + Repository 接口）
- infrastructure 依赖 domain（实现 Repository / Portal 接口）——**依赖倒置**
- domain 不依赖任何外层

## 各层职责（一句话）

| 层 | 职责 | 关键约束 |
|----|------|---------|
| **contract** | 公开契约（消费方唯一依赖） | 纯类型，零实现，零重依赖 |
| **adapter** | 协议适配（REST/RPC/MQ/定时任务入口） | 纯透传 AppService，无业务判断 |
| **application** | 用例编排（极薄） | 委托 Handler + Presenter 呈现，不含业务逻辑 |
| **domain** | 核心业务逻辑 | 零框架依赖，聚合根封装规则 |
| **infrastructure** | 技术实现 | 实现 Domain 接口，ACL 翻译外部模型 |

## contract 模块

- 仅包含：Controller 契约接口（`adapter/rest/`）、Command/Query、CO、枚举
- 仅依赖 `common-contract`（标记接口）
- 东西向消费方依赖 contract jar；东西向一期为 RestClient 直连（静态地址），Feign 经 common-cloud opt-in（JWT RequestInterceptor 自动透传）

## 依赖倒置

- Domain 层定义 `Repository` 接口（`domain/{agg}/repository/domain/`）
- Domain 层定义 `Portal` 接口（`domain/{agg}/portal/`）
- Infrastructure 层提供实现（`infrastructure/persistence/` / `infrastructure/gateway/`）
- Application 层通过 Domain 接口间接使用 Infrastructure（永远不直接引用 Mapper / PO）

## 按聚合自包含

domain / application / infrastructure / contract 内部以聚合名分包，打开一个聚合目录即可看到该聚合在该层的全部代码（例外：server 的 adapter 层不按聚合分包）：

```
domain/order/             → model/ + repository/domain/ + portal/ + service/ + policy/【按需】
application/order/        → service/（AppService）+ handler/{command,query}/ + assembler/ + presenter/ + dto/ + repository/application/（读端口）
infrastructure/.../order/ → mybatis/{po,mapper}/ + converter/ + repository/{domain,application}/
adapter/rest/controller/  → {Agg}ControllerImpl（实现 contract 契约接口，纯透传）
contract/order/           → adapter/rest/（Controller 契约接口）+ dto/{command,query,co}/ + enums/
```

→ 详见 `docs/application/module-design/{layer}.md`
