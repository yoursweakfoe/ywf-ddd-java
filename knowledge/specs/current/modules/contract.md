# 模块用法法卷：common-contract（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-contract.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：`2026-09-framework-codification`。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-contract</artifactId>
</dependency>
```

引入即生效。业务 CQE 对象实现对应标记接口，并在字段上声明校验约束：

```java
public record PlaceOrderCommand(
        @NotBlank String customerId,
        @NotEmpty List<@Valid OrderItemDTO> items
) implements Command {}

public record GetOrderPageQuery(
        String status,
        @Min(1) int pageNum,
        @Min(1) @Max(PageableQuery.MAX_PAGE_SIZE) int pageSize
) implements PageableQuery {}
// 组件名 pageNum/pageSize 与接口抽象方法天然匹配——零覆写样板。
// 校验注解必须声明在组件上（见 §3.1）。
```

### 3.1 参数校验规范

契约层声明约束、服务端执行校验，三层协作：

| 层 | 职责 | 做法 |
|---|---|---|
| contract（声明约束） | 在 CQE 字段上声明校验注解 | `@NotNull` / `@NotBlank` / `@NotEmpty` / `@Min` / `@Max` / `@Size` |
| adapter（触发校验） | `@Valid` 声明于**契约接口方法参数**，Controller 实现继承、HTTP 绑定期触发 | `@Valid @RequestBody XxxCommand`（写在契约接口上） |
| 全局异常处理 | 统一翻译校验失败 | `MethodArgumentNotValidException` → 400 + fieldErrors（common-exception 已提供） |

要点：

- **嵌套校验**：容器元素用类型参数注解 `List<@Valid Xxx>`；不要在 `List` 字段上加 `@Valid`（Hibernate Validator 已弃用该用法）
- **record 分页字段**：`@Min/@Max` 声明在 record **组件上**（接口方法注解不被组件继承）；运行期防线由 `safePageNum()/safePageSize()` 兜底，两层互为冗余
- **字符串 ID 用 `@NotBlank`，对象 ID 用 `@NotNull`，集合用 `@NotEmpty`**
- **业务规则校验不在此列**：库存够不够、状态对不对属 Domain 层 `validate()` + 显式 if-throw，不用 Bean Validation

无运行时配置：本模块为纯接口 + 注解 jar，无 SPI、无 AutoConfiguration、无 Spring Bean。
