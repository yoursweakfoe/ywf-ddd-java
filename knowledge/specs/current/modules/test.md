# 模块用法法卷：common-test（框架法 · 严格件）

> **身份**：本卷是用法规范与规范代码形状的唯一权威（严格件）。消费代码必须遵循本卷；违反本卷就修代码。修改本卷只能走 `../../changes/` 程序。docs 同题节（`reference/api/common-test.md` §3）是宽松件，只承载语感与指针；两者冲突时以本卷为准。
> **机器对账**：本卷在 check-docs 扫描面内。C1 校验 `{agg}` 模板实例化，C3 校验符号解析，C4 校验教学中立。

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

> 测试模板的 canonical 住所：`.agents/skills/new-test/SKILL.md`。其中模板 A 是 Handler 单元测试 `PayOrderHandlerTest`：Mock 写侧仓储与 Assembler，断言聚合状态变迁和 `update` 调用。本卷不承载业务测试体。

与架构守护的分工如下。Handler 单元测试断言**行为语义**，即状态变迁与异常路径。写侧事务边界、读侧读写隔离这类**结构约束**由 ArchUnit R11 / R13 机器保证，规则清单见 `reference/api/common-test.md` §2。同一违规只需在一处拦截，测试与规则不互相代偿。

## 规则集治理（TR-1~3）

> 设计论证的现行版住解读架 [`docs/explanation/architecture-rules.md`](../../../docs/explanation/architecture-rules.md)，本节只载治理条款。

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| TR-1 | `DddArchitectureRules` 各规则的 R##/C# 编号是**教义锚点**：docs、法卷、skills、提交信息一律以编号互指。编号永不重排；规则删除时编号作废、留一行作废记录，**废号准后续规则顶位**（顶位=新语义接管，旧作废账原位不改，作废账处补一行顶位承接注）。编号是教义锚点不是历史文物（§裁决记录 Q8，案卷 2026-09-typed-identifier）。R15 现行语义 = 聚合根身份终类型化（BP-13），旧 R15（baomidou 全仓禁令）作废账保留。规范行的 home 在本卷；javadoc 只保留一句复述加指针。归属依据：归属法 §2「规范行」行 | `DddArchitectureRules` 类头「编号纪律」段现文（随本案更新）＋顶位注；作废账 → `docs/explanation/architecture-rules.md`「废止也是账」段顶位承接句 | 字典 `reference/api/common-test.md` §2 的编号与各规则 `as()` 前缀自对账；ddd-review 锚点抽查 |
| TR-2 | 规则集整体与逐规则的设计论证现行版（立因、怎么判之理、被拒方案、沿革）canonical 居 `docs/explanation/architecture-rules.md`。代码侧每常量 javadoc 只携**挂载最小契约**：守护什么、怎么判一句、挂载扫描入口、空集/空转态。类头保留模块地图、安全必读警示与解读篇指针；警示含两项：空集通过机制、已知缺口清单。两种违规都按归属法 §1 裁决修容器：论证回流代码面、契约剥空 | `DddArchitectureRules.java:20–24`，即类头「载体分工」段；解读篇在位 | ddd-review 抽查；归属依据：归属法 §2「设计论证」行 |
| TR-3 | 规则覆盖缺口的现状清单随代码登记在类头「安全必读」节，属描述性事实，代码驱动，承担地图义务。缺口的论证与待裁决账（为何未实现、改规则还是改教义，含立因与拒因）canonical 居解读篇「空转防线与缺口账」节 | `DddArchitectureRules.java:53–65`；解读篇缺口账表 | ddd-review |
