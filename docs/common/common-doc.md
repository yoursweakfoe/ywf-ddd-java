# common-doc

REST API 文档 —— springdoc OpenAPI 3.0 + Swagger UI。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

面向需要对外暴露 REST 端点的业务服务，引入即获得 OpenAPI 3.0 文档（`/v3/api-docs` + Swagger UI），配合 Apifox 导入同步。本模块为纯聚合 pom（无自有 Java 代码），直接传递官方 starter。

> 与 common-cloud 解耦：API 文档属于开发期接口契约关注点，不属于微服务治理（Nacos/Seata 所在域）。按 opt-in 原则，由业务服务按需引入。

## 2. 核心能力

| 能力 | 说明 |
|------|------|
| OpenAPI 3.0 文档 | `/v3/api-docs` 端点 |
| Swagger UI | `/swagger-ui.html` 交互式文档 |
| 注解支持 | `@Tag` / `@Operation` 等 springdoc 原生注解 |

## 3. 使用方式

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-doc</artifactId>
</dependency>
```

引入即生效。Swagger 注解挂在 adapter 层 Controller 上：

```java
@Tag(name = "订单", description = "订单读写用例")
@RestController
public class OrderController {
    @Operation(summary = "下单")
    @PostMapping("/orders")
    public OrderCO placeOrder(@RequestBody PlaceOrderCommand command) {
        // ...
    }
}
```

访问 `http://localhost:8080/swagger-ui.html` 查看文档。

## 4. 依赖关系

```
common-doc → springdoc-openapi-starter-webmvc-ui
```

## 5. 设计原则

- **引入即生效**：springdoc 注解经 Boot 自动配置注册，Swagger UI 零配置可用
- **聚合不封装**：直接传递官方 starter，业务服务直接使用原生 API（`@Tag` / `@Operation` 等）

## 6. 设计决策

### ADR-0001 纯聚合 pom，不封装 springdoc

- 状态：accepted

**背景**：是否需要二次包装 springdoc。

**决策**：不封装。直接传递官方 starter，业务服务使用原生 springdoc API，无学习成本。springdoc 版本在 ywf-ddd-common dependencyManagement 独立声明。

**确认**：common-doc 无自有 Java 代码。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：API 文档与微服务治理的耦合 | 与 common-cloud 解耦，文档属开发期契约关注点 |
