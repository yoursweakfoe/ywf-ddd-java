---
name: new-test
description: 为已有聚合、Handler、Domain 模型或基础设施组件编写测试。当需要补充单元测试或集成测试时使用。
---

# 新增测试

## 前置阅读

1. `docs/common/common-test.md`（测试基础设施 + ArchUnit 规则）
2. `ywf-ddd-common/common-ddd/src/test/`（Fixture 模式参照）
3. `.agents/rules/03-coding-conventions.md`（命名规范）

## 测试分类与模板

### A. Handler 单元测试（Mockito）

位置：`src/test/java/.../application/{agg}/handler/command/{Action}{Agg}HandlerTest.java`（读侧 Handler 测试对应位于 `handler/query/`，与被测类包路径镜像）

```java
@ExtendWith(MockitoExtension.class)
class PayOrderHandlerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAssembler orderAssembler;
    @InjectMocks
    private PayOrderHandler handler;

    /** 造数入口：惰性重建任意状态（业务构造器已收私有，新建路径归 OrderFactory，见下方 Fixture 模式） */
    private Order createPendingOrder() {
        return TestOrders.rebuilt(OrderStatus.PENDING);
    }

    @Test
    void handle_shouldTransitionToPaid() {
        // Given
        Order order = createPendingOrder();
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderAssembler.toDTO(any(Order.class))).thenReturn(new OrderDTO());

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

### B. Domain 模型测试（纯 JUnit，无 Mock）

位置：`src/test/java/.../domain/{agg}/model/{Agg}Test.java`

```java
class OrderTest {

    private static final OrderItem ITEM = new OrderItem(UUID.randomUUID(), 2, BigDecimal.TEN);

    /** 行为测试入口：惰性重建 PENDING（reconstitute 不过状态机；新建路径经 OrderFactory「创建即合法」单独测） */
    private Order createPendingOrder() {
        return Order.reconstitute(UUID.randomUUID(), OrderStatus.PENDING, List.of(ITEM),
                ITEM.subtotal(), "customer-1", null, null, null, null, 0);
    }

    @Test
    void pay_shouldTransitionFromPendingToPaid() {
        Order order = createPendingOrder();
        order.pay();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void pay_shouldThrowWhenNotPending() {
        Order order = createPendingOrder();
        order.pay();  // PENDING → PAID

        assertThatThrownBy(order::pay)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenItemsEmpty() {
        Order bad = Order.reconstitute(UUID.randomUUID(), OrderStatus.PENDING, List.of(),
                BigDecimal.ZERO, "customer-1", null, null, null, null, 0);

        assertThatThrownBy(bad::validate)
                .isInstanceOf(BusinessException.class);
    }
}
```

### C. Converter 测试

位置：`src/test/java/.../infrastructure/persistence/{ds}/{agg}/converter/{Agg}ConverterTest.java`

```java
class OrderConverterTest {

    private final OrderConverter converter = new OrderConverter();

    @Test
    void toDomain_and_toPO_shouldRoundTrip() {
        OrderPO po = buildOrderPO();
        Order domain = converter.toDomain(po);
        OrderPO result = converter.toPO(domain);

        assertThat(result.getId()).isEqualTo(po.getId());
        assertThat(result.getStatus()).isEqualTo(po.getStatus());
    }

    @Test
    void toDomain_shouldThrowOnInvalidJson() {
        OrderPO po = buildOrderPO();
        po.setItems("invalid-json{{{");

        assertThatThrownBy(() -> converter.toDomain(po))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

### D. 集成测试（@SpringBootTest）

位置：`src/test/java/.../integration/{Feature}IntegrationTest.java`

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    private OrderAppService orderAppService;

    @Test
    void placeOrder_then_payOrder_shouldSucceed() {
        // Given → When → Then（完整业务流）
    }
}
```

## Fixture 模式

- 复用测试夹具工厂：参照 `common-ddd/src/test/.../fixtures/OrderFixtures.java`；sample 侧造数工具参照 `.../sampleservice/support/TestOrders.java`
- 每个测试模块可建立 `fixtures/` / `support/` 包存放共享测试数据
- Fixture 方法命名：`create{Agg}()` / `create{Agg}PO()`（+ `WithStatus` 变体；惰性重建用 `rebuilt(...)`，工厂新建用 `placed()` 类语义命名）
- 聚合业务构造器收私有后，测试**必须**经 Factory / `reconstitute()` 两条合法路径造数，禁止反射绕过

## 命名规范

| 类型 | 命名 | 示例 |
|------|------|------|
| 测试类 | `{ClassName}Test` | `PayOrderHandlerTest` |
| 测试方法 | `{method}_should{Expected}` | `handle_shouldThrowWhenNotFound` |
| Fixture | `{Agg}Fixtures` | `OrderFixtures` |

## 验证

- [ ] `mvn test -pl {module}` 全部通过
- [ ] 测试覆盖正常路径 + 至少 2 个异常路径
- [ ] 无 Spring 容器依赖（单元测试用 Mockito，不启动 ApplicationContext）
- [ ] 断言使用 AssertJ（`assertThat` / `assertThatThrownBy`）
- [ ] 无 `@Autowired` 字段注入（测试类用 `@Mock` + `@InjectMocks`）

## 文档同步

- 如新增了 Fixture 模式或测试基础设施，更新 `docs/common/common-test.md`
