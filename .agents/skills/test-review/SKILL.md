---
name: test-review
description: 测试工程师视角审查代码变更（覆盖率、边界情况、Mock 策略、ArchUnit 合规）。当需要测试充分性审查、补测建议、或评审测试代码时使用。
---

# 测试审查

## Role

以测试工程师视角检视代码变更，确保覆盖率、边界情况、造数与 Mock 策略、ArchUnit 合规、测试基础设施不被忽略。
本技能不载法：测试条款唯一权威 = `knowledge/specs/current/patterns/testing-conformance.md`（TC-1~8），各检查点按指针到条款对账，此处不复述规范原文（归属法卷 §2 / D6）。
审查结果按严重度分级：FAIL（必须补测）/ WARN（建议补测）/ PASS。

## 前置阅读

| 读什么 | 文件 | 取什么 |
|---|---|---|
| 测试法卷（唯一权威） | `knowledge/specs/current/patterns/testing-conformance.md` | §1 条款 TC-1~8；§2 规范形状（§2.7 验收终板、§2.8 真实例指针位） |
| common-test 法卷 | `knowledge/specs/current/modules/test.md` | 场景 1 ArchUnit 守护挂载式 / 场景 2·3 容器集成与单元形状 |
| ArchUnit 规则编号表 | `knowledge/docs/reference/api/common-test.md` §2 | R1~R14b / C1 教义唯一事实源，判违规只引编号不复述 |
| 横切法卷（按维度取） | `knowledge/specs/current/patterns/` 下 `read-chain.md`（RC-3/5/7）、`optimistic-lock.md`（OL-1/5）、`cross-aggregate.md`（CA-3）、`write-chain.md`（WC-7）；`knowledge/specs/current/modules/pg.md` 场景 1~3 | 边界与基础设施检查点的条款锚 |
| 设计卡（宽松件） | `knowledge/docs/how-to/testing.md` | 选型判据存疑才读；冲突法卷赢 |

## 审查维度

### 1. 覆盖完整性

- 新增/修改的 Handler 是否有 A 型单测：正常路径 + ≥2 异常路径，断言委托链（load→行为→save→toDTO）而不复断领域规则（TC-6，形状 §2.1）？
- 聚合根行为方法是否有 B 型测试：非法状态转换与 validate 不变量均断言 `BusinessException`（TC-2，形状 §2.2）？
- Converter 是否有 C 型测试：PO↔domain 往返一致 + 脏复杂列快速失败、不静默兜底（§2.3）？
- 跨聚合联动是否测了同事务补偿原子性：第二聚合失败时第一聚合回滚有实证（CA-3）？
- 新行为/新通道的断言是否与 `changes/<slug>/` delta 的 Scenario 一一对应；delta 缺席本身即 FAIL（TC-3）？

### 2. 边界与极端情况

- 分页参数：缺参 / 0 / 负数 / 超 `PageableQuery.MAX_PAGE_SIZE` 各有实证——缺参无默认值、绑定层 `@Min/@Max` 先拦成 400（RC-3/RC-5），实现侧 `safe*()` 双通道钳制兜底（RC-7）？
- 集合入参：null / 空列表 / 超大列表是否覆盖？
- 金额/数量：0 / 负数 / BigDecimal 精度——`@Digits` 是否对齐 schema 列精度、超界在绑定层 400 而非穿透成 500（WC-7）？
- 并发：乐观锁 version 不匹配是否有守恒测试实证冲突通道（OL-1；真实例件见 testing-conformance §2.8）？
- 时间：`OffsetDateTime` 统一前提下，跨时区 / 夏令时边界序列化是否经 D 型真库通路考虑？

### 3. 造数与 Mock 策略

- 造数是否只走 Factory / `reconstitute()` 两条合法路径；反射注入造出的不可达路径 = 假场景，一律 FAIL（TC-2）？
- A 型是否 Mockito 隔离 Repository / Assembler / Portal 等外部依赖、零 Spring 容器；容器测试是否只走 test profile（TC-1/TC-5）？
- 断言是否统一 AssertJ、单元类无 `@Autowired` 字段注入（TC-4）？
- D 型是否实证真实 HTTP 通路与真 SQL 语义，含 400（绑定层）/ 422（领域）/ 409（冲突）通道分界（§2.4；实证件清单见 §2.8）？
- Mock 行为是否与真实实现一致（返回值语义、异常类型），隔离不扭曲契约语义？

### 4. ArchUnit 合规

- 服务的分层守护类是否按 `knowledge/specs/current/modules/test.md` 场景 1 挂载 `DddArchitectureRules` 共享常量、零本地覆写（真实例 ApplicationArchitectureTest，见 common-test 字典 §2 沿革注记）？
- 新增/移包类是否 ArchUnit 全绿？判违规引 R 编号（唯一事实源 = common-test 字典 §2 编号表）；逐条分层深查归 `ddd-review` 技能，此处只核「守护在位、未被放宽、跑过且绿」。

### 5. 测试基础设施

- D 型的真 PG 测试库（形状权威 = db-migration 建形，§2.4/TC-9）前置与连接正确？JSONB / ARRAY / UUID 复杂列在真库上按原生类型直验——任何兼容模式兜底期待即为坑（类型形态见 `knowledge/specs/current/modules/pg.md` 场景 1~3）？
- PO 字段新增/变更是否同步 db-migration 变更集（C 型往返与 D 型真 SQL 执行都依赖 PO/XML/变更集三者同构，TC-9）？
- 测试数据是否自包含：状态清理、不依赖执行顺序——「一条前置（postgres 环节在跑）即全绿」契约的根基（TC-5；common 试验场库自建自清，`PgTestSupport` 三段式，TC-9 框架轨）？
- common 包侧测试是否一律住试验场 `common-packages-integration-test` 且保原包（TC-10）？各 library 模块 POM 是否零测试栈、`src/test/` 是否缺席？

## 输出格式

```
COVERED: N scenarios（对照法卷 §2.7 验收终板逐项自查）
MISSING: (补测清单，每条给建议方法名 {method}_should{Expected} 与所依据条款编号 TC/RC/OL/CA/WC)
RISK: (未覆盖高危路径 → FAIL；建议补测 → WARN；各维皆净 → PASS)
```
