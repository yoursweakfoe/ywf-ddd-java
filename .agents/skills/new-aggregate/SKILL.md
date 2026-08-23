---
name: new-aggregate
description: 从零创建 DDD 聚合（20 个文件，5 阶段）。当需要新增一个完整聚合（如 Payment、Shipment）时使用。
---

# 新建聚合

## 前置阅读

1. `docs/application/cookbook/new-aggregate.md`（完整 20 文件模板）
2. `.agents/rules/02-architecture.md`（分层 + 包结构）
3. `.agents/rules/03-coding-conventions.md`（命名 + 泛型）

## 步骤

按以下顺序创建文件（先外层后内层，确保编译依赖顺序正确）：

### Phase 1: contract 模块

1. `contract/{agg}/api/{Agg}Service.java` — RPC 接口
2. `contract/{agg}/dto/Create{Agg}Command.java` — 写请求
3. `contract/{agg}/dto/Get{Agg}Query.java` — 读请求
4. `contract/{agg}/co/{Agg}CO.java` — 契约输出
5. `contract/{agg}/dto/event/integration/{Agg}CreatedIntegrationEvent.java`（可选）

### Phase 2: domain 层

6. `domain/{agg}/model/{Agg}.java` — 聚合根（extends AggregateRoot<UUID>）
7. `domain/{agg}/model/{Agg}Status.java` — 状态枚举
8. `domain/{agg}/event/domain/{Agg}CreatedEvent.java` — 领域事件
9. `domain/{agg}/repository/{Agg}Repository.java` — 仓储接口

### Phase 3: infrastructure 层

10. `infrastructure/persistence/master/{agg}/mybatisplus/po/{Agg}PO.java` — 持久化对象（MyBatis-Plus 注解载体）
11. `infrastructure/persistence/master/{agg}/mybatisplus/mapper/{Agg}Mapper.java` — MyBatis-Plus Mapper（extends BaseMapper）
12. `infrastructure/persistence/master/{agg}/converter/{Agg}Converter.java` — 转换器（框架 BasicConverter 桥）
13. `infrastructure/persistence/master/{agg}/repository/{Agg}RepositoryImpl.java` — 仓储实现（继承 MybatisPlusPersistence）

### Phase 4: application 层

14. `application/{agg}/dto/{Agg}DTO.java` — 内部视图
15. `application/{agg}/assembler/{Agg}Assembler.java` — Domain → DTO
16. `application/{agg}/presenter/{Agg}Presenter.java` — DTO → CO
17. `application/{agg}/handler/Create{Agg}Handler.java` — 写 Handler
18. `application/{agg}/handler/Get{Agg}Handler.java` — 读 Handler
19. `application/{agg}/{Agg}AppService.java` — 聚合入口

### Phase 5: adapter 层

20. `adapter/web/{Agg}Controller.java` — REST 入口（@RestController 实现 contract 接口）

## 验证

- [ ] `mvn compile -pl sample-application/sample-service/sample-service-server` 编译通过
- [ ] ArchUnit 测试通过（`mvn test -pl ... -Dtest=ArchitectureTest`）
- [ ] Handler 返回 DTO，AppService 返回 CO
- [ ] Domain 层零框架注解
- [ ] PO 有 `@Version` + `@TableLogic` + `@TableName` 含 schema
- [ ] Converter.toDomain() 使用 `reconstitute()`
- [ ] RepositoryImpl 的 `save()`/`update()` 标注 `@Transactional(rollbackFor = Exception.class)`

## 文档同步

- 更新 `docs/application/directory-structure/overview.md`（新增聚合目录）
- 如引入了新模式，更新对应 cookbook 文档
