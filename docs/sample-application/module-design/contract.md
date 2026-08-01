# Contract — 公开契约

## 职责

定义服务的公开契约，是消费方（其他微服务）的**唯一依赖**。
涵盖 RPC 接口、CQRS 输入（Command/Query）、契约输出对象（CO）、集成事件（Integration Event）。

## 设计原则

- **纯类型定义**：仅包含接口、Command/Query、CO、Event，无任何实现
- **零重依赖**：仅依赖 `common-contract`（CQRS 标记接口 + OpenAPI 注解 + JAX-RS REST 注解）
- **按聚合分包**：顶层以聚合名划分，内部结构一致

## 包结构

→ [directory-structure/contract/contract.md](../directory-structure/contract/contract.md)

> 完整代码示例 → [cookbook/write-path.md](../cookbook/write-path.md)（Command / CO 定义）| [cookbook/event-flow.md](../cookbook/event-flow.md)（IntegrationEvent）

## 核心组件

| 组件 | 位置 | 职责 |
|------|------|------|
| Service 接口 | `{aggregate}/api/` | Dubbo RPC 接口定义，消费方通过 `@DubboReference` 注入调用 |
| Command / Query | `{aggregate}/dto/` | CQRS 请求对象，实现 `common-contract` 标记接口 |
| PageableQuery | `{aggregate}/dto/` | 分页查询对象，实现 `PageableQuery` 接口（带 pageNum/pageSize + @Min/@Max） |
| CO | `{aggregate}/co/` | Contract Object，对内部 DTO 清洗后的外部安全视图 |
| Integration Event | `{aggregate}/dto/event/` | 跨服务集成事件（MQ 载荷） |
| 枚举 | `{aggregate}/enums/` | 契约共享枚举 |

## 协作关系

```
contract（本模块）                             server
─────────────────                             ──────
api/{Aggregate}Service.java                   ←──  adapter/facade/（@DubboService 实现接口，纯透传 AppService）
{aggregate}/dto/XxxCommand / XxxQuery        ←──  application/handler/（接收 CQE 执行用例）
{aggregate}/co/XxxCO                          ←──  application/presenter/（DTO → CO 输出）
{aggregate}/dto/event/XxxIntegrationEvent    ←──  application/publisher/（翻译并发布到 MQ）
{aggregate}/dto/event/XxxIntegrationEvent    ──→  adapter/consumer/（接收 MQ 并透传 AppService）
```

### 消费方使用

```xml
<dependency>
    <groupId>com.yoursweakfoe.application</groupId>
    <artifactId>sample-service-contract</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

```java
@DubboReference
private XxxService xxxService;

XxxCO xxx = xxxService.xxxFunction();
```

## 规则

| 允许 | 禁止 |
|------|------|
| 接口定义 | 任何实现类 |
| 纯数据载体（Command/Query/CO/Event） | 业务逻辑 |
| 实现 common-contract 标记接口 | 依赖 Spring / Dubbo / MyBatis |
| java.io.Serializable | 依赖 server 模块 |
