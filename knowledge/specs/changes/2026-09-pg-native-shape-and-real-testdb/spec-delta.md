# 对 current/patterns/*（testing-conformance、aggregate-blueprint、prohibitions、optimistic-lock、write-chain、read-chain）与 current/modules/pg 的增删改

> 只描述变化量（delta 教义）。⚖ **问句绑定声明**：标注 ⚖Q①/②/③ 处按 proposal 推荐答案起草；问句裁决若不同，以裁决改写本 delta 后再施工——折叠前审议稿随便改。
> 行号取证以 2026-09-08 工作区为准；标「实施后回填」的在折叠时点以真实行号替换。

## ADDED Requirements

### Requirement: BP-S1（PG 原生建表形状法）
框架法卷 SHALL 承认：库表形状的唯一权威是 `db-migration/src/main/resources/db/changelog/<库>/changes/*.sql`（纯 SQL formatted），形状基线为 PG 原生——`id UUID PRIMARY KEY DEFAULT uuidv7()`（PG18 内建；应用侧工厂铸造时传值覆盖，DB 默认只兜手工插入）、`created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL`、`created_by/updated_by UUID`、`is_deleted BOOLEAN NOT NULL DEFAULT FALSE`（软删体现在 UPDATE，不设 delete_at）、`version BIGINT NOT NULL DEFAULT 0`。（源：`db-migration/.../ddd_sample_application/changes/0001-init-schema.sql` 实跑建形，`ddd_sample_application` 与 `_test` 双库 SHA256 字节级同形实证；`information_schema` 列核对本 bill 立卷时点）
#### Scenario: 新聚合建表
- GIVEN 消费方需为新聚合落 PG 库
- WHEN 在 `db/changelog/<库>/changes/` 追加 4 位序号新变更集
- THEN 表形状逐列符合 BP-S1 基线；H2/测试侧形状不得反向绑架产库形状
#### Scenario: 审计列与框架填充器的桥接（⚖Q①）
- GIVEN 框架 `AuditProperties` 默认字段名保持 `createAt`/`updateAt`（不动默认值）
- WHEN 消费方 PO 采用 BP-S1 属性名 `createdAt`/`updatedAt`
- THEN 消费方 SHALL 经 `ywf.ddd.audit.create-field/update-field` 配置桥接；框架不为此改默认值

### Requirement: BP-S2（schema 命名法：领域词单数 + 保留字术语升级）
schema SHALL 按聚合/领域切分且**领域词取单数**（`product.product`、`sales_order.sales_order`——领域是概念，概念无复数；复数系行集视角的 DB 遗习）。领域词恰为 SQL 保留字（如 order）时 SHALL 升级到更精确的行业通用语言术语（`sales_order`），**禁止**引号（`"order"` 全语句交税）与前缀（`o_order` 词汇被语法绑架）逃逸。schema 名 SHALL 与 Java 聚合包名（单数惯例）逐字同构，微服务拆分时整 schema 平移。（源：0001 变更集 + 用户 2026-09 三连裁决记录）
#### Scenario: 新聚合落 schema
- GIVEN 新聚合领域词为 X（非保留字）
- WHEN 建表
- THEN schema=表=`X.X`（单数双名）；冲突保留字场景以行业 UB 术语替代并在此卷留映射注记

### Requirement: BP-S3（工具账表独立 schema）
外部工具的维护性表（Liquibase 账表、未来 Seata TC 表等）SHALL 住各自独立 schema（当前实例：`liquibase` schema 承载 `databasechangelog`/`databasechangeloglock`），与业务聚合 schema、`public` 互不混列。账表 schema 由执行器引导件幂等自建（消费方零手工 DDL）。（源：`db-migration/.../config/LiquibaseSchemaBootstrapConfig.java`；实证=双库建形后 `pg_tables` 按 schema 清点 4 表各归各位）
#### Scenario: Seata 升级 db-store 时的落位
- GIVEN TC 四表（`global_table` 等）SQL 全部不具名（源码实证）
- WHEN 启用 db store
- THEN 优先分库（TC 独立服务→独立库→自家 public，与本仓"隔离即分库"教义一致）；同库则 `store.db.url` 加 `currentSchema=`；`undo_log` 因序列名类加载期由裸表名派生，随业务连接落 public——各归其位即不违反本条（undo 系客户端运行时表，非 TC 账表）

### Requirement: TC-9（真库测试基座与数据隔离）（⚖Q②）
test profile SHALL 直连 PostgreSQL 测试库（当前实例 `ddd_sample_application_test`，`DB_TEST_URL/USER/PASSWORD` 可覆写）；测试库形状权威同属 `db-migration`（TC-5 前置件），测试代码不建表、不管 DDL。数据隔离 SHALL 双轨：默认 D 型用例走 `@Transactional` 回滚；**非事务**教例（如并发压测 `OptimisticLockConcurrencyTest`）SHALL 以 `@Sql` `TRUNCATE ... RESTART IDENTITY` 于测试前清场。
#### Scenario: 本地跑 D 型集成测试
- GIVEN ywf-infra postgres 在跑且测试库已经 db-migration 建形
- WHEN `mvn test`
- THEN 全部 D 型用例通过且库内无残留行（回滚/clear 双轨保证可重复执行）

## MODIFIED Requirements

### Requirement: TC-1（四分型表行）
| TC-1 | 测试四分型强制：Handler 单测（Mockito，零 Spring 容器）/ Domain 纯 JUnit / Converter 往返（PO↔domain 等价）/ 集成（test profile + 真 PG 测试库，TC-9）；容器测试只准走 test profile | `modules/test.md`（common-test 场景 2/3）+ 本 bill 实施后回填 | mvn 全绿 |

### Requirement: TC-5（演示契约改述）
| TC-5 | 演示契约改述：全仓测试**离线逻辑**（无网络出口依赖），运行前置 = ywf-infra postgres 环节 + db-migration 对 `ddd_sample_application_test` 建形（一条命令，见 `db-migration/README.md`）；不再承诺零基础设施 | 示例树宪法（`sample-application/AGENTS.md` 验证闸行随折叠同批改写）| CI 复跑 |

### Requirement: §2.4 D 型 · 集成测试（整节替换）
### 2.4 D 型 · 集成测试（@SpringBootTest + test profile，真 PG 测试库）

位置：`integration/{Feature}IntegrationTest.java`。test profile 直连 PostgreSQL 测试库（形状由 `db-migration` 供给，见 TC-9/BP-S1）；库内数据隔离经「事务回滚 + 非事务教例 TRUNCATE 清场」双轨（TC-9）。运行前置见 TC-5。

```java
@SpringBootTest
@ActiveProfiles("test")                            // TC-1/TC-9：容器测试只走 test profile（真 PG 测试库）
class {Agg}FlowIntegrationTest {

    @Autowired
    private {Agg}AppService {agg}AppService;

    @Test
    void fullHappyPath_shouldSucceed() {
        // Given → When → Then（真库真方言：PG 语义即生产语义，无兼容模式转译）
    }

    @Test
    void illegalTransition_shouldReturn422Shape() {
        // 走 HTTP 面的集成参照 RestEndpointIntegrationTest 风格（含绑定层 400 与领域 422 的分界实证）
    }
}
```

（非事务教例模板：类上 `@Sql(statements = "TRUNCATE TABLE <schema>.<表> RESTART IDENTITY", executionPhase = BEFORE_TEST_METHOD)`——真实例形状折叠时以 `OptimisticLockConcurrencyTest` 实施态为准。）

### Requirement: §2.7 验收单（行内改述）
- [ ] 单元测试零 Spring 容器（Mockito）；容器测试只走 test profile/真 PG 测试库（TC-9）

### Requirement: BP-6（UUID 字符串化边界收缩）
| BP-6 | 契约层聚合 ID 引用一律 UUID（B12 教义）；字符串化仅存在于 JSON wire 序列化形态（Jackson 输出），**禁止** PO/DTO/领域层以 String 承载 id 或经 `toString()`/`fromString()` 手工跨越边界（旧「PO.id 列可 String」通道随 BP-S1 关闭） | 本卷 §4.③, §4.⑦, §4.⑮（教学形状随 §5 机械替换表换形）；`ContractEnumParityTest` 奇偶锁不受涉 |

### Requirement: BP-X1（XML 七语句契约随形）
| BP-X1 | XML 七条语句契约：表名含 schema 前缀（单数领域词，BP-S2）；select/update/delete（逻辑删除聚合）显式 `AND is_deleted = false`；`insert` 不枚举 `is_deleted`；`existsById` 恒返一行 boolean；`updateById` 携 `SET version = version + 1 ... AND version = #{version}`；文件位 `resources/mapper/{agg}/` 且 namespace = Mapper 全限定名 | `modules/ddd.md` 场景 2；OL-4 互指；BP-S1 |

### Requirement: prohibitions §6 持久化与 SQL 铁律（整节替换，仅第 1–3 bullet 变动，禁令 1/2/3 逐字不动）
## §6 持久化与 SQL 铁律

- 禁止 MyBatis-Plus 进框架依赖树（持久化 = `DddMapper` 七语句 + 手写 XML；ADR-0007 判例）。dynamic-datasource 为经一手调研证实零耦合的多数据源 opt-in 方案，`@DS` 合法
- 禁止 PO 携带任何 ORM 注解（纯 `@Data` POJO；表名/主键/版本条件/逻辑删除全在 XML SQL 文本 → BP-X2）
- 禁止 Wrapper 式动态条件——查询一律具名 Mapper 方法 + 具名 XML 语句（`<sql>` 片段复用防漂移）
- 建表 DDL 形状基线见 [BP-S1](aggregate-blueprint.md)（`version BIGINT NOT NULL DEFAULT 0` + `is_deleted BOOLEAN NOT NULL DEFAULT FALSE` + 审计四列）；显式豁免的聚合 XML 省略对应条件（逐聚合自决，无共享开关 → BP-12）
- `updateById`（有版本列）**必须**携 `SET version = version + 1 ... AND version = #{version} AND is_deleted = false`，无运行时拦截器；0 行后果三分通道 → [optimistic-lock OL-1](optimistic-lock.md)（行为由 sample `OptimisticLockConcurrencyTest` 实证）
- 逻辑删除聚合的每条 select/update/delete **必须**显式 `AND is_deleted = false`——漏一处即泄漏；豁免聚合写物理 `DELETE`

## 机械形状替换表（条款语义不变，折叠时按位点应用；覆盖各卷教学代码块与 prose 的旧形 token）

| 位点 | 旧 | 新 |
|---|---|---|
| testing-conformance §2.4 已含（整节替换，不入表） | — | — |
| aggregate-blueprint §1 槽位树 L42 注释、§4.⑲ 标题与全 XML 块、§4.㉑ 块 | `payments.payments` | `payment.payment`（BP-S2 单数示范；虚构 Payment 家族不变） |
| aggregate-blueprint §4.⑮ PO 块 | `String id` / `Integer version` / `createAt/updateAt/isDelete` | `UUID id` / `Long version` / `createdAt/updatedAt/isDeleted` |
| aggregate-blueprint §4.⑯ Converter 块 | `UUID.fromString(po.getId())` / `setId(...toString())` | 直传 `po.getId()` / `setId(domain.getId())`（BP-6 收缩后手工转换即违规） |
| aggregate-blueprint §4.㉑ | `selectById(id.toString())` / `dto.setCreateAt` | `selectById(id)` / `setCreatedAt` |
| 同上各 XML 块列 token | `create_at` / `update_at` / `is_delete` / `#{createAt}` / `#{updateAt}` | `created_at` / `updated_at` / `is_deleted` / `#{createdAt}` / `#{updatedAt}` |
| write-chain §2.11 prose L349、optimistic-lock §2.1 代码行 L30、read-chain XML 块 L277/284/286 | 同 token + `reservations.reservations` | `is_deleted`/`created_at` + `reservation.reservation` |
| modules/pg.md 场景 2 示例 L47 | `products.products` | `product.product`（真实例指针随样树） |

## 生效登记（折叠时 blueprint 卷末追加）
| 环节 | 状态 | 位置 |
|---|---|---|
| BP-S1~S3 / TC-9 开卷（本 bill 折叠） | ✅（折叠时点） | 本 bill `archive/` 案卷 |
