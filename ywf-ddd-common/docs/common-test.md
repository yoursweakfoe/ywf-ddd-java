# common-test

测试基础设施 —— ArchUnit DDD 架构守护规则 + Spring Boot Test 全套（test scope）。

## 定位

为所有业务服务提供统一的测试依赖版本管理和 DDD 分层合规性自动检查。
面向所有采用 DDD 分层架构的服务，以 test scope 引入。
ArchUnit 规则在编译期守护分层约束，防止架构腐化。

## 设计原则

- **架构守护自动化**：DDD 分层规则编码为 ArchUnit 测试，CI 中自动执行
- **统一版本**：JUnit 5 + Mockito + AssertJ + Spring Test 由本模块统一管理，业务服务无需声明版本
- **不绑定容器**：不引入 Testcontainers 等特定容器依赖，由业务项目按需自行引入

## 核心功能

### ArchUnit 规则清单

公开常量名（位于 `DddArchitectureRules` 类）：

| 常量名 | 守护内容 | 违反示例 | 失败条件 |
|------|---------|---------|--------|
| `LAYERED_ARCHITECTURE` | DDD 四层依赖方向 | Domain import Application | 任何下层 import 上层类 |
| `CONTROLLER_ONLY_DEPENDS_ON_APPLICATION` | Adapter 只依赖 Application | Facade 直接注入 Repository | adapter 包 import domain/infrastructure |
| `DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS` | Domain 不依赖外层 | Entity import MyBatis | domain 包 import 非 java/common-ddd 类 |
| `DOMAIN_MODEL_IS_PURE` | 领域模型纯净 | Entity 上加 @Component | domain.model 包出现 Spring 注解 |
| `DOMAIN_REPOSITORIES_MUST_BE_INTERFACES` | Repository 是接口 | domain/repository/ 下放实现类 | 该包内出现非 interface 类型 |
| `REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE` | 实现在 infrastructure | RepositoryImpl 放在 domain | *Impl 类不在 infrastructure 包 |

检查包路径模式：规则通过 `@AnalyzeClasses(packages = "...")` 指定的根包递归扫描，自动识别 `.domain.` / `.application.` / `.adapter.` / `.infrastructure.` 子包归属。

### Spring Boot Test 统一版本

JUnit 5 + Mockito + AssertJ + Spring Test，版本由 Spring Boot BOM 管理。

## 使用方式

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
    static final ArchRule r2 = DddArchitectureRules.CONTROLLER_ONLY_DEPENDS_ON_APPLICATION;

    @ArchTest
    static final ArchRule r3 = DddArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;

    @ArchTest
    static final ArchRule r4 = DddArchitectureRules.DOMAIN_MODEL_IS_PURE;

    @ArchTest
    static final ArchRule r5a = DddArchitectureRules.DOMAIN_REPOSITORIES_MUST_BE_INTERFACES;

    @ArchTest
    static final ArchRule r5b = DddArchitectureRules.REPOSITORY_IMPL_LIVES_IN_INFRASTRUCTURE;
}
```

> `packages` 修改为你项目的根包名。规则在 CI 中自动执行，违反分层约束时测试失败。

### 场景 2：Spring Boot 集成测试

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderAppServiceTest {

    @Autowired
    private OrderAppService orderAppService;

    @Test
    void placeOrder_shouldCreatePendingOrder() {
        // Given
        PlaceOrderCommand command = new PlaceOrderCommand("customer-1",
                List.of(new PlaceOrderCommand.OrderItemDTO(1L, 2)));

        // When
        OrderCO result = orderAppService.placeOrder(command);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getTotalAmount()).isPositive();
    }
}
```

### 场景 3：单元测试（Mockito）

```java
@ExtendWith(MockitoExtension.class)
class PayOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAssembler orderAssembler;
    @InjectMocks
    private PayOrderHandler handler;

    @Test
    void handle_shouldTransitionToPaid() {
        // Given
        Order order = new Order(UUID.randomUUID(), List.of(item), "customer-1");
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any())).thenReturn(new OrderDTO());

        // When
        OrderDTO result = handler.handle(new PayOrderCommand(order.getId()));

        // Then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).update(order);
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new PayOrderCommand(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }
}
```

## 设计决策与未实现功能

| 决策 | 理由 |
|------|------|
| ArchUnit 而非人工 Code Review | 规则编码为测试，CI 自动执行，不依赖人的纪律性 |
| 规则集为静态常量 | 业务服务直接引用 `DddArchitectureRules.XXX`，无需重复定义 |
| **未实现** Testcontainers | 各服务数据库/中间件组合不同，由业务项目自行引入 |
| **未实现** 测试数据工厂（Fixture Builder） | 领域对象构造与业务强相关，通用工厂反而增加维护成本 |
| **未实现** 契约测试（Spring Cloud Contract / Pact） | 东西向服务间通过 proto 契约（强类型，编译期生成 stub）通信，编译期即可发现契约不兼容 |
| **未实现** 性能/压力测试工具 | 属于 CI/CD 流水线职责（JMeter / k6），不纳入代码仓库依赖 |

## 依赖关系

```
common-test（独立，test scope 使用）
├── spring-boot-starter-test（JUnit 5 + Mockito + AssertJ）
└── archunit-junit5
```

### 消费方

| 模块 | scope | 用途 |
|------|-------|------|
| common-ddd | test | 框架组件自测（领域模型、仓储、事件发布、自动填充） |
| common-exception | test | 异常体系自测（BusinessException、REST/RPC 异常处理） |
| common-security | test | 安全组件自测（SecurityWebFilter、gRPC 身份拦截器、SecurityUtil） |
| 业务服务 | test | ArchUnit 架构守护 + 集成测试 |

> test scope 不传递：业务服务引入 common-ddd 时不会自动获得 common-test，需显式声明。
