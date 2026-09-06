# 测试编写指南

> 归属：本文=测试模板唯一载体（D6 裁决：模板住 how-to，`.agents/skills/new-test` 只留步骤+指向本文）。
> v1 曾用 Payment 具体虚构方法名（confirm 等）——**与 new-aggregate 的 Payment canon（仅 create/validate）打架**，遂改 {Agg} 通式骨架（判例：common-exception 场景2 的「{Agg} 教学占位」形态）。具体形状的真实例一律走文末「真实例」指针位。
> 立场：教「这一类东西怎么测」；「为什么这样测」的论证在 explanation/ 与 `reference/api/common-test.md`。

## 业务场景

框架把「测试即验收」当一等公民：三分通道行为、乐观锁并发守恒、契约枚举奇偶都由测试锁死，而非文档口头承诺。新增一个聚合/Handler/Converter 后，按下面四类各就位一份即覆盖该层契约。

## A. Handler 单元测试（Mockito，无容器）

位置：`application/{agg}/handler/command/{Action}{Agg}HandlerTest.java`（读侧镜像 `handler/query/`）

```java
@ExtendWith(MockitoExtension.class)
class {Action}{Agg}HandlerTest {

    @Mock
    private {Agg}Repository {agg}Repository;
    @Mock
    private {Agg}Assembler {agg}Assembler;
    @InjectMocks
    private {Action}{Agg}Handler handler;

    /** 造数入口：惰性重建任意状态（新建路径归 Factory / 语义构造器，见 Fixture 节） */
    private {Agg} create{Agg}InState({Agg}Status status) {
        return Test{Agg}s.rebuilt(status);
    }

    @Test
    void handle_shouldTransitionAndPersist() {
        // Given
        {Agg} agg = create{Agg}InState({Agg}Status.STATE_A);
        when({agg}Repository.findById(any())).thenReturn(Optional.of(agg));
        when({agg}Assembler.toDTO(any({Agg}.class))).thenReturn(new {Agg}DTO());

        // When
        {Agg}DTO result = handler.handle(new {Action}{Agg}Command(agg.getId()));

        // Then：状态迁移由聚合行为方法完成，Handler 只负责 load→行为→save→toDTO
        assertThat(agg.getStatus()).isEqualTo({Agg}Status.STATE_B);
        verify({agg}Repository).update(agg);
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        when({agg}Repository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new {Action}{Agg}Command(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }
}
```

要点：`@Mock`+`@InjectMocks`，禁 `@Autowired` 字段注入（本类）；AssertJ 断言；正常路径 + ≥2 异常路径；断言「委托链」而非重复断言领域规则（规则归 B 类测试）。

## B. Domain 模型测试（纯 JUnit，无 Mock）

位置：`domain/{agg}/model/{Agg}Test.java`

```java
class {Agg}Test {

    /** 行为测试入口：reconstitute 不过状态机；「创建即合法」由 Factory 路径另测 */
    private {Agg} inState({Agg}Status status) {
        return {Agg}.reconstitute(AggregateIds.mint(), status /* …其余字段按聚合形状… */);
    }

    @Test
    void behaviorAction_shouldTransitionFromAtoB() {
        {Agg} agg = inState({Agg}Status.STATE_A);
        agg.behaviorAction();                       // 聚合自己的状态机方法
        assertThat(agg.getStatus()).isEqualTo({Agg}Status.STATE_B);
    }

    @Test
    void behaviorAction_shouldThrowWhenWrongOrigin() {
        {Agg} agg = inState({Agg}Status.STATE_B);
        agg.behaviorAction();                       // 已到目标态

        assertThatThrownBy(agg::behaviorAction).isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_shouldThrowWhenInvariantBroken() {
        {Agg} bad = inState({Agg}Status.STATE_A);
        /* 构造一个违反不变量的形状（如必填引用置空）——经 reconstitute 注入 */

        assertThatThrownBy(bad::validate).isInstanceOf(BusinessException.class);
    }
}
```

注：骨架用 `{Agg}Status.STATE_A/B` 与 `behaviorAction()` 通式——**具体状态与迁移名以目标聚合真实状态机为准**，勿照抄占位符进产码。

## C. Converter 测试（往返一致 + 脏数据快速失败）

位置：`infrastructure/persistence/{ds}/{agg}/converter/{Agg}ConverterTest.java`

```java
class {Agg}ConverterTest {

    private final {Agg}Converter converter = new {Agg}Converter();

    @Test
    void toDomain_and_toPO_shouldRoundTrip() {
        {Agg}PO po = build{Agg}PO();               // 含 String 化 id 与各复杂列的合法文本
        {Agg} domain = converter.toDomain(po);
        {Agg}PO result = converter.toPO(domain);

        assertThat(result.getId()).isEqualTo(po.getId());
        assertThat(result.getStatus()).isEqualTo(po.getStatus());
    }

    @Test
    void toDomain_shouldThrowOnCorruptComplexColumn() {
        {Agg}PO po = build{Agg}PO();
        // 把某复杂列（JSON/枚举文本）置为非法值——快速失败，不静默兜底
        // 真实例形态见文末指针（OrderConverter 的 items JSON → IllegalStateException）

        assertThatThrownBy(() -> converter.toDomain(po))
                .isInstanceOf(RuntimeException.class);
    }
}
```

## D. 集成测试（@SpringBootTest + test profile，H2 内存库）

位置：`integration/{Feature}IntegrationTest.java`。test profile 用 H2（`MODE=PostgreSQL` + `INIT=RUNSCRIPT FROM 'classpath:schema.sql'`），**零外部基础设施**——这条是「clone 即 `mvn test` 全绿」的根基（入口见 `tutorials/quickstart.md` 路线 A）。

```java
@SpringBootTest
@ActiveProfiles("test")
class {Agg}FlowIntegrationTest {

    @Autowired
    private {Agg}AppService {agg}AppService;

    @Test
    void fullHappyPath_shouldSucceed() {
        // Given → When → Then（完整业务流穿透真库语义：H2 PG 兼容模式下真 SQL 真执行）
    }

    @Test
    void illegalTransition_shouldReturn422Shape() {
        // 走 HTTP 面的集成参照 RestEndpointIntegrationTest 风格（含绑定层 400 与领域 422 的分界实证）
    }
}
```

## Fixture 模式

- 共享造数工厂放 `fixtures/` 或 `support/` 包，命名 `{Agg}Fixtures` / `Test{Agg}s`；方法 `create{Agg}()` / `create{Agg}PO()`（惰性重建 `rebuilt(status)`，工厂新建用语义名如 `placed()`）。
- 业务构造器收私有（Factory 收口「创建即合法」）后，测试**必须**经 Factory / `reconstitute()` 两条合法路径造数，**禁止反射绕过**——绕过 = 测试在教框架没定的形状。

## 命名规范

| 类型 | 命名 | 例（通式） |
|------|------|------|
| 测试类 | `{ClassName}Test` | `{Action}{Agg}HandlerTest` |
| 测试方法 | `{method}_should{Expected}` | `handle_shouldThrowWhenNotFound` |
| Fixture | `{Agg}Fixtures` / `Test{Agg}s` | `Test{Agg}s` |

## 验收清单（与 new-test skill 检查项同源；canonical=本文，skill 指过来）

- [ ] `mvn test -pl {module}` 通过
- [ ] 正常路径 + ≥2 异常路径
- [ ] 单元测试零 Spring 容器（Mockito）；容器测试只走 test profile/H2
- [ ] AssertJ 断言；单元类无 `@Autowired` 字段注入
- [ ] 涉及新行为/新通道的，同步在 `knowledge/specs/changes/<slug>/` 立 delta（spec-first：断言 = delta 的 Scenario）

## 真实例参照（同构形状，代码块外指针位）

sample 实证可对照（真实例，全部类名经源码探针实证）：`PayOrderHandlerTest`（A 形）、`OrderTest`（B 形，含 items 不变量）、`OrderConverterTest`（C 形，items JSON 脏值 → ISE）、`RestEndpointIntegrationTest`（D 形：全链路流 + @Size 绑定 400 三例 + 422/400 通道 HTTP 面实证）、`OptimisticLockConcurrencyTest`（守恒律）、`ContractEnumParityTest`（奇偶锁）、`RetryablePlaceOrderHandlerTest`（乐观锁重试模板）；造数 `TestOrders` / `OrderFixtures`。位置 `Glob **/<类名>.java`；行为语义以源码为准，本文模板只教形状。
