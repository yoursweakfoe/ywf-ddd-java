# 模块用法法卷：common-test（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的**唯一权威**（严格件）——消费代码必须遵循，违反本卷=修代码；修卷只走 `../../changes/` 程序。docs 同题节（`reference/api/common-test.md` §3）为宽松件：语感与指针，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：本卷在 check-docs C1（`{agg}` 模板实例化）/ C3（符号解析）/ C4（教学中立）扫描面内。开册法案：2026-09 框架成典案。

---

```xml
<dependency>
    <groupId>com.yoursweakfoe</groupId>
    <artifactId>common-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 场景 1：ArchUnit 架构守护

```java
@AnalyzeClasses(
        packages = "com.yoursweakfoe.sampleapplication.sampleservice",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
    @ArchTest
    static final ArchRule r1 = DddArchitectureRules.LAYERED_ARCHITECTURE;
    @ArchTest
    static final ArchRule r2 = DddArchitectureRules.ADAPTER_ONLY_DEPENDS_ON_APPLICATION;
    @ArchTest
    static final ArchRule r3 = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
    // ... 其余规则同式引用
}
```

### 场景 2：Spring Boot 集成测试

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderAppServiceTest {
    @Autowired private OrderAppService orderAppService;

    @Test
    void placeOrder_shouldCreatePendingOrder() {
        PlaceOrderCommand command = new PlaceOrderCommand("customer-1",
                List.of(new PlaceOrderCommand.OrderItemView(UUID.randomUUID(), 2)));
        OrderCO result = orderAppService.placeOrder(command);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
}
```

### 场景 3：单元测试（Mockito）

> 测试模板 canonical：`.agents/skills/new-test/SKILL.md`（模板 A = Handler 单元测试 `PayOrderHandlerTest`——Mock 写侧仓储 + Assembler，断言聚合状态变迁与 `update` 调用），本文不承载业务测试体。

与架构守护的分工：Handler 单元测试断言**行为语义**（状态变迁、异常路径），写侧事务边界与读侧读写隔离的**结构约束**由 §2 规则表中的 ArchUnit R11 / R13 机器保证——同一违规只需在一处拦截，测试与规则互不代偿。
