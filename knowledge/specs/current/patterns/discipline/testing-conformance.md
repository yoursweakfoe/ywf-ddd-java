# 用法规范法卷：测试符合性（框架法 · 严格件）

> **身份**：本卷是测试写法统一用法的唯一权威。条款与全套规范形状只在本卷，全仓其他文档不得复写形状。违反本卷 = 修代码；修卷走 `../../../changes/`。docs 同题篇（`../../../../docs/how-to/testing.md`）是设计卡，只讲选型与边界叙事，零形状代码。
> **机器对账**：C1/C3/C4 扫本卷。教例家族为 `{Agg}`/`{Action}` 通式 + 虚构 Fixture（`Test{Agg}s`）；真实例只准出现在带「真实例」标记的指针位，即 §2.8。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| TC-1 | 测试四分型强制：A Handler 单测（Mockito，零 Spring 容器）；B Domain 纯 JUnit；C Converter 往返（PO↔domain 等价）；D 集成（test profile + 真 PG 测试库，TC-9）。容器测试只准走 test profile。**真库测试基座全仓唯一 = PostgreSQL，H2 已全仓退役，含 common（TC-10）** | `modules/test.md`：common-test 场景 2/3；设计卡同题篇四型节入法 | mvn 全绿 |
| TC-2 | 聚合合法实例**只能**经 Factory 或 `reconstitute()` 构造。测试不得裸构造绕过不变式——绕过等于教坏后来者，合法路径的守门人正是这两处 | 设计卡同题篇「测试禁止绕过……不得手动 new」入法；`modules/ddd.md` 场景 1；AO-4 互指 | 聚合根测试 |
| TC-3 | 涉及新行为/新通道的测试，断言与 `changes/<slug>/` delta 的 Scenario 一一对应（spec-first：先法案后断言） | 归属法卷 §2「skill 首步产出 delta」；本区 README 守则 2 | bill 对账 |
| TC-4 | 单测类禁止 `@Autowired` 字段注入；断言统一 AssertJ | 设计卡同题篇检查单入法 | 评审项 |
| TC-5 | 演示契约：全仓测试零网络出口。运行前置 = ywf-infra postgres 环节 + db-migration 对 `ddd_sample_application_test` 建形，一条命令即可（`db-migration/README.md`）。common 试验场库 `ddd_framework_test` 由 `PgTestSupport` 自建自清，零附加前置（TC-9/TC-10） | 示例树宪法 `sample-application/AGENTS.md` 在册承诺 | CI 复跑 |
| TC-9 | 真库测试基座与隔离双轨。**服务轨**：test profile 直连 PG 测试库，库形状权威 = db-migration，测试不建表；容器测试默认 `@Transactional` 回滚；真 HTTP 链教例走独立事务提交、回滚不可覆盖，SHALL 类级 `@Sql TRUNCATE … RESTART IDENTITY` 清场。**框架轨**：common fixture 形状非服务形状、不入库 db-migration，形状权威 = `PgTestSupport` 三段生命周期，即自建→每 JVM 复位→shutdown 清空，schema 为 `integration_test` / `integration_test_vacant`；fixture 表名 SHALL 不带 schema 限定，可见性由数据源 `currentSchema` 裁决；「空库」教例 = 真空 schema | `integration/RestEndpointIntegrationTest` / `OptimisticLockConcurrencyTest` 类级 @Sql；`common.integration.pg.PgTestSupport` + `MybatisPersistenceTest.dynamicRouting_secondSourceIsBare_masterHasSchema` | mvn 全绿（真 PG ×2 轮 + 轮间零残留实证） |
| TC-10 | common 诸包测试集中法：`ywf-ddd-common` 下一切测试（含单模块独立单测）只住 `common-packages-integration-test/src/test/`；各 library 模块 `src/test/` 不得存在、POM 不得声明测试栈。迁入卷 SHALL 保持与被测类全同包名，classpath split package 合法，工程不用 JPMS；**禁止**为测试搬迁扩大产码可见性。试验场 `src/main` 永空置、`maven.deploy.skip`（身份登记 → 禁令卷 §9「试验场」） | `common-packages-integration-test/pom.xml` + 模块树（55 卷迁居实证）；`MybatisPersistenceTest`，原包保白盒 | check-docs C2 + mvn 全绿 |
| TC-6 | 单元类覆盖下限：正常路径 + ≥2 异常路径。Handler 单测断言「委托链」（load→行为→save→toDTO），**不**重复断言领域规则——规则归 B 型测试 | 设计卡同题篇 A 节要点 + 验收清单入法；形状 §2.1 | 评审项 |
| TC-7 | 测试文件位置镜像产码路径，辖域 = 服务树；common 试验场按 TC-10 保原包镜像，不受本条 `integration/` 集中句约束。A 型 `application/{agg}/handler/command/`，读侧镜像 `handler/query/`；B 型 `domain/{agg}/model/`；C 型 `infrastructure/persistence/{ds}/{agg}/converter/`；集成测试统一住 `integration/` | 设计卡同题篇 A–D 各节「位置」行入法；形状 §2.1–§2.4 | 评审项 |
| TC-8 | 测试命名：类 `{ClassName}Test`、方法 `{method}_should{Expected}`、Fixture `{Agg}Fixtures` / `Test{Agg}s` | 设计卡同题篇命名规范表入法；形状 §2.6 | 评审项 |

## §2 规范形状（统一用法唯一样本）

### 2.1 A 型 · Handler 单元测试（Mockito，无容器）

位置：`application/{agg}/handler/command/{Action}{Agg}HandlerTest.java`（读侧镜像 `handler/query/`）

```java
@ExtendWith(MockitoExtension.class)                        // TC-1：A 型=Mockito 单测，零 Spring 容器
class {Action}{Agg}HandlerTest {

    @Mock                                                  // TC-4：@Mock + @InjectMocks，禁 @Autowired 字段注入
    private {Agg}Repository {agg}Repository;
    @Mock
    private {Agg}Assembler {agg}Assembler;
    @InjectMocks
    private {Action}{Agg}Handler handler;

    /** 造数入口：惰性重建任意状态（新建路径归 Factory / 语义构造器，见 Fixture 节） */
    private {Agg} create{Agg}InState({Agg}Status status) {
        return Test{Agg}s.rebuilt(status);                 // TC-2：造数只走合法路径（Factory / reconstitute）
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
        assertThat(agg.getStatus()).isEqualTo({Agg}Status.STATE_B);   // TC-4：AssertJ
        verify({agg}Repository).update(agg);                          // TC-6：断言委托链，不复断领域规则
    }

    @Test
    void handle_shouldThrowWhenNotFound() {
        when({agg}Repository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new {Action}{Agg}Command(UUID.randomUUID())))
                .isInstanceOf(BusinessException.class);
    }
}
```

要点：`@Mock`+`@InjectMocks`，禁 `@Autowired` 字段注入；断言用 AssertJ；覆盖正常路径 + ≥2 异常路径；断言「委托链」而非重复断言领域规则，规则归 B 类测试。

### 2.2 B 型 · Domain 模型测试（纯 JUnit，无 Mock）

位置：`domain/{agg}/model/{Agg}Test.java`

```java
class {Agg}Test {                                          // TC-1：纯 JUnit，无 Mock，零容器

    /** 行为测试入口：reconstitute 不过状态机；「创建即合法」由 Factory 路径另测 */
    private {Agg} inState({Agg}Status status) {
        return {Agg}.reconstitute(AggregateIds.mint(), status /* …其余字段按聚合形状… */);   // TC-2
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

注：骨架用 `{Agg}Status.STATE_A/B` 与 `behaviorAction()` 通式。**具体状态与迁移名以目标聚合真实状态机为准**，勿照抄占位符进产码。

### 2.3 C 型 · Converter 测试（往返一致 + 脏数据快速失败）

位置：`infrastructure/persistence/{ds}/{agg}/converter/{Agg}ConverterTest.java`

```java
class {Agg}ConverterTest {                                 // TC-1：C 型=往返一致 + 脏数据快速失败

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
        // 真实例形态见 §2.8 指针（OrderConverter 的 items JSON → IllegalStateException）

        assertThatThrownBy(() -> converter.toDomain(po))
                .isInstanceOf(RuntimeException.class);
    }
}
```

### 2.4 D 型 · 集成测试（@SpringBootTest + test profile，真 PG 测试库）

位置：`integration/{Feature}IntegrationTest.java`。test profile 直连真 PG 测试库，运行前置见 TC-5；库形状权威 = db-migration，测试代码不建表、不管 DDL。真 PG 真方言真 SQL，无兼容模式转译层，入口见 [../../docs/tutorials/quickstart.md](../../../../docs/tutorials/quickstart.md) 路线 A。框架轨即 common 试验场（TC-10）同走真 PG，唯形状权威换 `PgTestSupport` 三段生命周期、卷位置保原包镜像。

```java
@SpringBootTest
@ActiveProfiles("test")                            // TC-1/TC-9：容器测试只走 test profile（真 PG 测试库；真 HTTP 链教例类级 @Sql 清场见 TC-9）
class {Agg}FlowIntegrationTest {

    @Autowired
    private {Agg}AppService {agg}AppService;

    @Test
    void fullHappyPath_shouldSucceed() {
        // Given → When → Then（完整业务流穿透真库语义：PG 真方言真 SQL 真执行）
    }

    @Test
    void illegalTransition_shouldReturn422Shape() {
        // 走 HTTP 面的集成参照 RestEndpointIntegrationTest 风格（含绑定层 400 与领域 422 的分界实证）
    }
}
```

### 2.5 Fixture 模式

- 共享造数工厂放 `fixtures/` 或 `support/` 包，命名 `{Agg}Fixtures` / `Test{Agg}s`。方法 `create{Agg}()` / `create{Agg}PO()`；惰性重建用 `rebuilt(status)`，工厂新建用语义名如 `placed()`。
- 业务构造器收私有，由 Factory 收口「创建即合法」。此后测试**必须**经 Factory / `reconstitute()` 两条合法路径造数，**禁止反射绕过**。绕过的后果：测试在教框架没定的形状。（TC-2）

### 2.6 命名规范（TC-8）

| 类型 | 命名 | 例（通式） |
|------|------|------|
| 测试类 | `{ClassName}Test` | `{Action}{Agg}HandlerTest` |
| 测试方法 | `{method}_should{Expected}` | `handle_shouldThrowWhenNotFound` |
| Fixture | `{Agg}Fixtures` / `Test{Agg}s` | `Test{Agg}s` |

### 2.7 验收终板（与 new-test skill 检查项同源；canonical=本卷，skill 指过来）

- [ ] `mvn test -pl {module}` 通过
- [ ] 正常路径 + ≥2 异常路径
- [ ] 单元测试零 Spring 容器（Mockito）；容器测试只走 test profile/真 PG 测试库（TC-9/TC-10，H2 全仓禁回）
- [ ] AssertJ 断言；单元类无 `@Autowired` 字段注入
- [ ] 涉及新行为/新通道的，同步在行为所属区立 delta：框架走 `knowledge/specs/changes/<slug>/`，业务走 `sample-application/specs/changes/<slug>/`（spec-first：断言 = delta 的 Scenario）

### 2.8 真实例参照（同构形状，代码块外指针位）

sample 实证可对照。以下全部为真实例类名，经源码探针实证：`PayOrderHandlerTest`（A 形）、`OrderTest`（B 形，含 items 不变量）、`OrderConverterTest`（C 形，items JSON 脏值 → ISE）、`RestEndpointIntegrationTest`（D 形：全链路流 + @Size 绑定 400 三例 + 422/400 通道 HTTP 面实证）、`OptimisticLockConcurrencyTest`（守恒律）、`ContractEnumParityTest`（奇偶锁）、`RetryablePlaceOrderHandlerTest`（乐观锁重试模板）；造数 `TestOrders` / `OrderFixtures`。位置用 `Glob **/<类名>.java` 查；行为语义以源码为准，本卷模板只教形状。

## §3 生效登记

| 环节 | 状态 | 位置 |
|---|---|---|
| TC-1~5 既有条款 | ✅ | 119 测试基线在册对账 |
| TC-6~8 归卷新增条款：覆盖下限 / 位置 / 命名，源=how-to/testing.md 入法 | ✅ | §2 形状在册，同一 119 测试基线对账 |
| TC-1/TC-5/§2.4 改卷 + TC-9 开卷（2026-09 PG 形状案 折叠） | ✅ | 119 测试基线（真 PG）对账 |
| TC-1/TC-5/TC-7/TC-9 改卷 + TC-10 开卷（2026-09 试验场归并案 折叠：common 试验场 + H2 全仓退役） | ✅ | 试验场 193 基线（真 PG ×2 轮）+ sample 119 回归对账 |
