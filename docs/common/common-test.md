# common-test

测试基础设施 —— ArchUnit DDD 架构守护规则 + Spring Boot Test 全套（test scope）。

> 本文分两段：§1–4 面向使用者（怎么用），§5–7 面向设计者（为什么这么设计）。

## 1. 定位与边界

为所有业务服务提供统一的测试依赖版本管理和 DDD 分层合规性自动检查。面向所有采用 DDD 分层架构的服务，以 test scope 引入。ArchUnit 规则在编译期守护分层约束，防止架构腐化。

> 不引入 Testcontainers 等特定容器依赖：各服务数据库/中间件组合不同，由业务项目自行引入。

## 2. 核心能力

### ArchUnit 规则清单

公开常量位于 `DDDArchitectureRules` 类：

| 常量名 | 守护内容 |
|------|---------|
| `LAYERED_ARCHITECTURE` | DDD 四层依赖方向（Domain 不得 import Application） |
| `CONTROLLER_ONLY_DEPENDS_ON_APPLICATION` | Adapter 只依赖 Application |
| `DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS` | Domain 不依赖外层 |
| `DOMAIN_MODEL_IS_PURE` | 领域模型纯净（domain.model 无 Spring 注解） |
| `DOMAIN_REPOSITORIES_MUST_BE_INTERFACES` | Repository 是接口 |
| `REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE` | 实现在 infrastructure |

规则通过 `@AnalyzeClasses(packages = "...")` 指定的根包递归扫描，自动识别 `.domain.` / `.application.` / `.adapter.` / `.infrastructure.` 子包归属。

### Spring Boot Test 统一版本

JUnit 5 + Mockito + AssertJ + Spring Test，版本由 Spring Boot BOM 管理。

## 3. 使用方式

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
    static final ArchRule r1 = DDDArchitectureRules.LAYERED_ARCHITECTURE;
    @ArchTest
    static final ArchRule r2 = DDDArchitectureRules.CONTROLLER_ONLY_DEPENDS_ON_APPLICATION;
    @ArchTest
    static final ArchRule r3 = DDDArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
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
                List.of(new PlaceOrderCommand.OrderItemDTO(1L, 2)));
        OrderCO result = orderAppService.placeOrder(command);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }
}
```

### 场景 3：单元测试（Mockito）

```java
@ExtendWith(MockitoExtension.class)
class PayOrderHandlerTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderAssembler orderAssembler;
    @InjectMocks private PayOrderHandler handler;

    @Test
    void handle_shouldTransitionToPaid() {
        Order order = new Order(UUID.randomUUID(), List.of(item), "customer-1");
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any())).thenReturn(new OrderDTO());

        OrderDTO result = handler.handle(new PayOrderCommand(order.getId()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).update(order);
    }
}
```

## 4. 依赖关系

```
common-test（独立，test scope 使用）
├── spring-boot-starter-test（JUnit 5 + Mockito + AssertJ）
└── archunit-junit5
```

### 消费方

| 模块 | scope | 用途 |
|------|-------|------|
| common-ddd | test | 框架组件自测 |
| common-exception | test | 异常体系自测 |
| common-security | test | 安全组件自测 |
| 业务服务 | test | ArchUnit 架构守护 + 集成测试 |

> test scope 不传递：业务服务引入 common-ddd 时不会自动获得 common-test，需显式声明。

## 5. 设计原则

- **架构守护自动化**：DDD 分层规则编码为 ArchUnit 测试，CI 中自动执行
- **统一版本**：JUnit 5 + Mockito + AssertJ + Spring Test 由本模块统一管理
- **不绑定容器**：不引入 Testcontainers 等特定容器依赖

## 6. 设计决策

### ADR-0001 ArchUnit 而非人工 Code Review

- 状态：accepted

**背景**：分层约束靠人工 review 还是自动化。

**决策**：选 ArchUnit 自动化。规则编码为测试，CI 自动执行，不依赖人的纪律性。

**确认**：`DDDArchitectureRules` 提供静态常量规则集。

### ADR-0002 规则集为静态常量

- 状态：accepted

**背景**：规则如何暴露给业务服务。

**决策**：公开静态常量。业务服务直接引用 `DDDArchitectureRules.XXX`，无需重复定义。

**确认**：`DDDArchitectureRules` 类公开 `ArchRule` 静态字段。

## 7. 职责边界与技术债

| 项 | 说明 |
|---|---|
| 边界：Testcontainers | 各服务数据库/中间件组合不同，由业务项目自行引入 |
| 边界：测试数据工厂（Fixture Builder） | 领域对象构造与业务强相关，通用工厂增加维护成本 |
| 边界：契约测试（Spring Cloud Contract / Pact） | contract jar 的 Java 接口 + CO 类型即方法签名单一事实源，编译期即可发现契约变更 |
| 边界：性能/压力测试工具 | 属于 CI/CD 流水线职责（JMeter / k6），不纳入代码仓库依赖 |
