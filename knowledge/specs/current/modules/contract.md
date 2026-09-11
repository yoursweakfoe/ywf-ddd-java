# 模块用法法卷：common-contract（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-contract.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-contract</artifactId>
</dependency>
```

引入即生效。消费方只做两件事：业务 CQE 对象实现对应标记接口，并在字段上声明校验约束。

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

总原则一句话：约束声明在契约层，触发在 adapter 层，失败由全局异常处理统一翻译。三层分工：

| 层 | 职责 | 做法 |
|---|---|---|
| contract（声明约束） | 在 CQE 字段上声明校验注解 | `@NotNull` / `@NotBlank` / `@NotEmpty` / `@Min` / `@Max` / `@Size` |
| adapter（触发校验） | `@Valid` 声明在**契约接口方法参数**上；Controller 实现时继承该声明，HTTP 绑定期触发校验 | `@Valid @RequestBody XxxCommand`（写在契约接口上） |
| 全局异常处理（翻译失败） | 把校验失败统一翻译成响应 | `MethodArgumentNotValidException` → 400 + fieldErrors（common-exception 已提供） |

要点：

- **嵌套校验**：容器元素要用类型参数注解，写成 `List<@Valid Xxx>`。不要把 `@Valid` 加在 `List` 字段上，Hibernate Validator 已弃用这种写法。
- **record 分页字段**：`@Min/@Max` 必须声明在 record **组件上**，因为接口方法注解不会被 record 组件继承。运行期另有 `safePageNum()/safePageSize()` 钳制兜底。声明与钳制两层都保留，互为冗余。
- **三类标识的注解选择**：字符串 ID 用 `@NotBlank`，对象 ID 用 `@NotNull`，集合用 `@NotEmpty`。
- **业务规则校验不属于这里**：库存够不够、状态对不对，归 Domain 层 `validate()` 加显式 if-throw 处理，不用 Bean Validation。

无运行时配置：本模块为纯接口 + 注解 jar，无 SPI、无 AutoConfiguration、无 Spring Bean。
