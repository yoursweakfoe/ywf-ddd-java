---
name: new-aggregate
description: 从零创建 DDD 聚合（19+2 个文件，5 阶段：19 最小闭环 + 读端口配对 2 文件）。当需要新增一个完整聚合（如 Payment、Shipment）时使用。
---

# 新建聚合

## 前置阅读

1. `docs/application/cookbook/new-aggregate.md`（①-⑲ 全部代码模板——本技能只承载顺序与清单，逐文件模板以该文档编号为准）
2. `.agents/rules/02-architecture.md`（分层 + 包结构）
3. `.agents/rules/03-coding-conventions.md`（命名 + 泛型）

## 步骤

按阶段顺序创建（与 cookbook「创建顺序建议」一致；条目编号 = cookbook new-aggregate.md 文件清单 ①-⑲）：

### Phase 1: contract 模块（①-④）

1. ① `contract/{agg}/adapter/rest/controller/{Agg}Controller.java` — REST 契约接口（HTTP 映射 + `@Valid` / `@Operation` 注解声明于此，重契约）
2. ② `contract/{agg}/dto/co/{Agg}CO.java` — 契约输出
3. ③ `contract/{agg}/dto/command/Create{Agg}Command.java` — 写请求
4. ④ `contract/{agg}/dto/query/Get{Agg}Query.java` — 读请求

### Phase 2: domain 层（⑫-⑭）

5. ⑫ `domain/{agg}/model/{Agg}.java` — 聚合根（`extends AggregateRoot<UUID>`，纯状态机 + validate，见 cookbook ⑫）
6. ⑬ `domain/{agg}/model/{Agg}Status.java` — 状态枚举
7. ⑭ `domain/{agg}/repository/{Agg}Repository.java` — 写侧仓储接口（仅聚合生命周期；读方法走读端口，R13）

### Phase 3: infrastructure 层（⑮-⑲）

8. ⑮ `infrastructure/persistence/master/{agg}/mybatis/po/{Agg}PO.java` — 纯 `@Data` POJO，零 ORM 注解（表名 / 版本条件 / 逻辑删除全在 SQL 文本，见 cookbook ⑮）
9. `infrastructure/persistence/master/{agg}/mybatis/mapper/{Agg}Mapper.java` — `@Mapper extends DddMapper<{Agg}PO>`（见 cookbook ⑯）
10. `infrastructure/persistence/master/{agg}/converter/{Agg}Converter.java` — `BasicConverter` 桥，`toDomain()` 走 `reconstitute()`（见 cookbook ⑰）
11. ⑲ `src/main/resources/mapper/{agg}/{Agg}Mapper.xml` — 手写 DddMapper 七条语句（法条见 rules 04「持久化与 SQL」，逐条模板见 cookbook ⑲）
12. ⑱ `infrastructure/persistence/master/{agg}/repository/{Agg}RepositoryImpl.java` — 继承 `MybatisPersistence`，构造器注入 Mapper + Converter + `Clock` + `AuditProperties` + `ObjectProvider<CurrentUserProvider>`；不标 `@Transactional`（R11，见 cookbook ⑱）

### Phase 4: application 层（⑥-⑪）

13. ⑦ `application/{agg}/dto/{Agg}DTO.java` — 内部视图（实现 `ApplicationDTO` 标记，R10a/R10b）
14. ⑧ `application/{agg}/assembler/{Agg}Assembler.java` — Domain → DTO
15. ⑨ `application/{agg}/presenter/{Agg}Presenter.java` — DTO → CO
16. ⑩ `application/{agg}/handler/command/Create{Agg}Handler.java` — 实现 `CommandHandler`；load → 行为 → save → toDTO；标注 `@Transactional(rollbackFor = Exception.class)`（R11 强制）
17. ⑪ `application/{agg}/handler/query/Get{Agg}Handler.java` — 实现 `QueryHandler`，只注入读端口 QueryRepository（R13 禁止触碰写侧仓储）
18. ⑥ `application/{agg}/service/{Agg}AppService.java` — 聚合入口（实现 `ApplicationService` 标记，返回 CO）

### Phase 5: adapter 层（⑤）

19. ⑤ `adapter/rest/controller/{Agg}ControllerImpl.java` — `@RestController` 实现契约接口 + `RestAdapter` 标记（R8a/R8b），纯透传（见 cookbook ⑤）

> **读端口配对**（⑪ 依赖，即 cookbook 完整模板 21 文件中的 ⑳㉑，不计入 19 最小闭环）：`application/{agg}/repository/{Agg}QueryRepository.java`（`extends QueryRepository` 标记）+ `infrastructure/persistence/master/{agg}/repository/{Agg}QueryRepositoryImpl.java`（与 ⑱ 写侧 Impl 同包，PO → 读 DTO 直接投影，不 reconstitute 聚合根）。流程模板 → `docs/application/cookbook/read-path.md`。

## 验证

- [ ] `mvn compile -pl sample-application/sample-service/sample-service-server` 编译通过
- [ ] ArchUnit 通过：`mvn test -pl sample-application/sample-service/sample-service-server -Dtest="*ArchitectureTest"`（DddArchitectureTest + ApplicationArchitectureTest；规则编号表见 `docs/common/common-test.md` §2）
- [ ] Handler 返回 DTO，AppService 返回 CO（经 Presenter）
- [ ] Domain 零框架运行时依赖（唯一例外 `org.springframework.stereotype`，R4 白名单）
- [ ] 持久化契约满足（法条 rules 04「持久化与 SQL」：PO 零 ORM 注解；XML 七语句含 schema 前缀 / version 条件 / `AND is_delete = false` / insert 不枚举 is_delete / existsById 恒返回一行 boolean）
- [ ] 事务边界在 Handler（RepositoryImpl 不标注，R11）

## 文档同步

- 更新 `docs/application/directory-structure/overview.md`（新增聚合目录）
- 如引入了新模式，更新对应 cookbook 文档（含本清单的 canonical 模板 new-aggregate.md）
