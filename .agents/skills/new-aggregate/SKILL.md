---
name: new-aggregate
description: 从零创建 DDD 聚合（22 个文件 = 20+2，5 阶段：20 最小闭环 + 读端口配对 2 文件）。当需要新增一个完整聚合（如 Payment、Shipment）时使用。
---

# 新建聚合

## 前置阅读

1. `knowledge/specs/current/patterns/aggregate-blueprint.md`（聚合构建宪：§1 槽位清单 ①-㉒、§2 BP 条款、§3 验收单、§4 逐件规范形状、§5 服务骨架——本技能只承载顺序与清单，下文「法卷」即指本卷，形状以法卷为唯一权威）
2. `knowledge/docs/how-to/new-aggregate.md`（设计卡：拆分信号与决策点，尚未定案是否新建时先读）
3. `knowledge/specs/current/patterns/prohibitions.md`（分层禁令 + §6 持久化与 SQL 铁律）
4. `knowledge/specs/current/patterns/coding-conventions.md`（命名 + 泛型）

## 第 0 步：契约先行（spec-first）

动手实现前，在 `sample-application/specs/changes/<YYYY-MM-slug>/` 立四件套（specify → plan → tasks → implement，模板 = `knowledge/specs/changes/_template/`）。测试全绿后归档折叠进 `sample-application/specs/current/<agg>.md`——文档同步义务只在那一刻发生（归属法卷 §4）。
## 步骤

按阶段顺序创建（顺序 = 法卷 BP-3：contract → domain → infrastructure → application → adapter，接口先行、依赖倒序；槽位编号 ①-㉒ = 法卷 §1，逐件形状走查 = 法卷 §4）：

### Phase 1: contract 模块（①-④ + ㉒）

1. ① `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` — REST 契约接口（HTTP 映射与文档注解声明于此，BP-4）→ 法卷 §4.①
2. ② `contract/{agg}/dto/co/{Agg}CO.java` — 契约输出（状态字段用契约枚举 ㉒ 类型而非 String，BP-7）→ 法卷 §4.②
3. ③ `contract/{agg}/dto/command/Create{Agg}Command.java` — 写请求 → 法卷 §4.③
4. ④ `contract/{agg}/dto/query/Get{Agg}Query.java` — 读请求 → 法卷 §4.④
5. ㉒ `contract/{agg}/enums/{Agg}Status.java` — 契约枚举（与 ⑬ 值域镜像、两枚举并存禁合并，奇偶锁 = ContractEnumParityTest，BP-2）→ 法卷 §4.㉒

### Phase 2: domain 层（⑫-⑭）

6. ⑫ `domain/{agg}/model/{Agg}.java` — 聚合根（纯状态机，业务规则收口 `validate()`，BP-8/BP-12）→ 法卷 §4.⑫
7. ⑬ `domain/{agg}/model/{Agg}Status.java` — 状态枚举 → 法卷 §4.⑬
8. ⑭ `domain/{agg}/repository/{Agg}Repository.java` — 写侧仓储接口（`extends Repository<{Agg}, UUID>`；读方法走读端口 ⑳，R13）→ 法卷 §4.⑭

### Phase 3: infrastructure 层（⑮-⑲）

9. ⑮ `infrastructure/persistence/master/{agg}/mybatis/po/{Agg}PO.java` — PO（纯 `@Data` 零 ORM 注解，SQL 语义全在 XML 文本，BP-X2）→ 法卷 §4.⑮
10. ⑰ `infrastructure/persistence/master/{agg}/mybatis/mapper/{Agg}Mapper.java` — Mapper（`@Mapper` + `extends DddMapper<{Agg}PO>`，BP-X2）→ 法卷 §4.⑰
11. ⑯ `infrastructure/persistence/master/{agg}/converter/{Agg}Converter.java` — Converter（`BasicConverter` 桥，`toDomain()` 走 `reconstitute()`，BP-X2）→ 法卷 §4.⑯
12. ⑲ `src/main/resources/mapper/{agg}/{Agg}Mapper.xml` — 手写 SQL（DddMapper 七条语句契约，BP-X1）→ 法卷 §4.⑲
13. ⑱ `infrastructure/persistence/master/{agg}/repository/{Agg}RepositoryImpl.java` — RepositoryImpl（继承 `MybatisPersistence`，BP-X2；不标 `@Transactional`，事务边界在 Handler，BP-10/R11）→ 法卷 §4.⑱

### Phase 4: application 层（⑥-⑪）

14. ⑦ `application/{agg}/dto/{Agg}DTO.java` — 内部视图（实现 `ApplicationDTO` 标记，R10a/R10b）→ 法卷 §4.⑦
15. ⑧ `application/{agg}/assembler/{Agg}Assembler.java` — Assembler（Domain → DTO 单向契约，BP-9）→ 法卷 §4.⑧
16. ⑨ `application/{agg}/presenter/{Agg}Presenter.java` — Presenter（DTO → CO，`valueOf` 在此收口契约枚举并过滤内部字段，BP-7/BP-9）→ 法卷 §4.⑨
17. ⑩ `application/{agg}/handler/command/Create{Agg}Handler.java` — CommandHandler（写侧四拍链 load → 聚合行为 → save → toDTO，必标 `@Transactional`，BP-10/R11）→ 法卷 §4.⑩
18. ⑪ `application/{agg}/handler/query/Get{Agg}Handler.java` — QueryHandler（只注入读端口 ⑳，禁触写侧仓储，R13）→ 法卷 §4.⑪
19. ⑥ `application/{agg}/service/{Agg}AppService.java` — 聚合入口（实现 `ApplicationService` 标记，返回 CO）→ 法卷 §4.⑥

### Phase 5: adapter 层（⑤）

20. ⑤ `adapter/rest/controller/{Agg}ControllerImpl.java` — ControllerImpl（`@RestController` 实现契约接口 + `RestAdapter` 标记，纯透传零逻辑，BP-4，R8a/R8b）→ 法卷 §4.⑤

> **读端口配对**（⑳㉑，⑪ 依赖，= 法卷 §1 清单 22 文件中不计入 20 最小闭环的 2 件，BP-1 允许随首个读用例补齐）：`application/{agg}/repository/{Agg}QueryRepository.java`（`extends QueryRepository` 标记，BP-X3）+ `infrastructure/persistence/master/{agg}/repository/{Agg}QueryRepositoryImpl.java`（与 ⑱ 写侧 Impl 同包，PO → 读 DTO 直接投影，BP-11）。形状 → 法卷 §4.⑳㉑；读链路条款 → `knowledge/specs/current/patterns/read-chain.md` 法卷（选型设计卡 → `knowledge/docs/how-to/read-path.md`）。

## 验证

- [ ] `mvn compile -pl sample-application/sample-service/sample-service-server` 编译通过
- [ ] ArchUnit 通过：`mvn test -pl sample-application/sample-service/sample-service-server -Dtest="*ArchitectureTest"`（DddArchitectureTest + ApplicationArchitectureTest；规则编号表 → `knowledge/docs/reference/api/common-test.md` §2「ArchUnit 规则清单」）
- [ ] 契约枚举奇偶：新枚举对登记进 `ContractEnumParityTest` 的 `PAIRS` 清单（BP-2，法卷 §3 验收单未单列此条，故在此补位）
- [ ] 法卷 §3 验收单逐条勾验（Handler→DTO / AppService→CO、domain 框架中立 R4、事务边界 BP-10/R11、读侧 R13、持久化契约 BP-X1~X3 皆有其位，本清单不复述法条）

## 文档同步

- 引入新模式 / 新形状 → 先走 `knowledge/specs/changes/` 修法程序（聚合形状正身 = aggregate-blueprint 法卷）；how-to 设计卡与 api 描述镜像随归档折叠同步（归属法 §4）
- 本技能只保步骤与指针，不新增法条正文或模板（D6 纪律，归属法 §2）
