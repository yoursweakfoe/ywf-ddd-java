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

## 规则集治理（TR-1~3）

> 2026-09 ArchUnit 论证载体分工案（案卷 2026-09-archunit-rule-doc）折叠入卷。设计论证的现行版住解读架 [`docs/explanation/architecture-rules.md`](../../../docs/explanation/architecture-rules.md)，本节只载治理条款（严格件）。

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| TR-1 | `DddArchitectureRules` 各规则的 R##/C# 编号为**教义锚点**：docs、法卷、skills、提交信息一律以编号互指；编号**永不重排、永不复用**，规则删除时编号作废并留一行作废记录。规范行本卷为 home，javadoc 保留一句复述 + 指针（归属法 §2「规范行」行） | `DddArchitectureRules.java:26–28`（类头「编号纪律」段）；作废账 → 解读篇「空转防线与缺口账」节 | 字典表 `reference/api/common-test.md` §2 编号与 `as()` 前缀自对账 + ddd-review 锚点抽查 |
| TR-2 | 规则集整体与逐规则的**设计论证现行版**（立因/怎么判之理/被拒方案/沿革）canonical 居 `docs/explanation/architecture-rules.md`；代码侧每常量 javadoc 只携**挂载最小契约**——守护什么、怎么判一句、挂载扫描入口、空集/空转态；类头保留模块地图与安全必读警示（空集通过机制、已知缺口清单）及解读篇指针。违反分工（论证回流代码面 / 契约剥空）= 按归属法 §1 裁决修容器 | `DddArchitectureRules.java:20–24`（类头「载体分工」段）+ 解读篇在位 | ddd-review + 归属法 §2「设计论证」行 |
| TR-3 | 规则覆盖**缺口的现状清单**随代码登记于类头「安全必读」节（描述性事实，代码驱动，地图义务）；缺口的**论证与待裁决账**（为何未实现、改规则还是改教义的悬案，含立因/拒因）canonical 居解读篇「空转防线与缺口账」节 | `DddArchitectureRules.java:53–65` + 解读篇缺口账表 | ddd-review |
