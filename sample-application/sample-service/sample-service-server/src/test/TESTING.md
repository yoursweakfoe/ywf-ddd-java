# 测试目录结构 & 分层 Topic

> 通用测试注解来自 `common-test` 模块，安全 Mock 来自 `common-security` 的 `test` 包。
> 按以下分包创建测试类，每个 topic 对应一种测试策略。

---

## 目录规划

```
src/test/java/com/yoursweakfoe/application/service/
├── application/service/            # Topic A
├── architecture/                   # Topic B
├── domain/model/entity/            # Topic C
├── fixtures/                       # Topic D
├── infrastructure/repository/impl/ # Topic E
├── interfaces/controller/          # Topic F
└── layers/                         # Topic G
src/test/resources/
└── application-test.yml            # 测试专用配置
```

---

## Topic 说明

| Topic | 包 | 注解 | 测什么 |
|-------|---|------|--------|
| A | `application/service/` | `@ApplicationLayerTest` | AppService 编排逻辑：Mock 掉 Repository/DomainService，验证调用顺序、参数传递、异常转换 |
| B | `architecture/` | `@AnalyzeClasses` + `DDDArchitectureRules` | ArchUnit 分层守护：四层依赖方向、Domain 纯净性、Repository 接口约束 |
| C | `domain/model/entity/` | `@DomainLayerTest` | 领域模型业务规则：实体创建校验、状态流转、值对象不可变性、身份判等 |
| D | `fixtures/` | 无（工具类） | 测试数据工厂：提供构建 Entity/Command/DTO 的静态方法，减少各测试重复 setup |
| E | `infrastructure/repository/impl/` | `@InfrastructureLayerTest` | 仓储集成测试：真实数据库 CRUD、乐观锁、快照脏检查、Converter 映射 |
| F | `interfaces/controller/` | `@InterfaceLayerTest` | Controller 端到端：MockMvc 全链路（HTTP → 参数绑定 → 鉴权 → AppService → DB → JSON） |
| G | `layers/` | `@SpringBootTest` | Bean 加载验证：确认各层 Spring 组件正确装配、无循环依赖 |

---

## 快速上手

```java
// Topic A 示例
@ApplicationLayerTest
class XxxAppServiceTest {
    @Mock private XxxRepository repository;
    @InjectMocks private XxxAppService service;
    // ...
}

// Topic B 示例
@AnalyzeClasses(
    packages = "com.yoursweakfoe.application.service",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest static final ArchRule r1 = DDDArchitectureRules.LAYERED_ARCHITECTURE;
    @ArchTest static final ArchRule r3 = DDDArchitectureRules.DOMAIN_DOES_NOT_DEPEND_ON_OUTER_LAYERS;
    @ArchTest static final ArchRule r4 = DDDArchitectureRules.DOMAIN_MODEL_IS_PURE;
}

// Topic C 示例
@DomainLayerTest
class XxxEntityTest {
    // 纯业务规则，无 Spring、无 DB
}

// Topic F 示例
@InterfaceLayerTest
@WithMockUser(userId = 1, roles = "admin")
class XxxControllerTest {
    @Autowired private MockMvc mockMvc;
    // ...
}
```
