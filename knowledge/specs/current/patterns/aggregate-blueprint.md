# 用法规范法卷：聚合构建宪（框架法 · 严格件）

> **身份**：本卷是「一个聚合 = 哪些文件、什么形状」的建筑宪章——**22 个文件**完整清单（20 最小闭环 + 读端口配对 ⑳㉑ 两文件，契约枚举 ㉒ 计入契约段）；修卷走 `../../changes/`。docs 同题篇（`how-to/new-aggregate.md`）为宽松件（逐件教学走查全套），冲突以本卷为准。
> **机器对账**：C1（下列 `{agg}` 位以真实聚合代入核验）/ C3 / C4 扫本卷；教例家族 = Payment（虚构教例，sample 未实现）。开册法案：`2026-09-howto-codification`。

## §1 文件清单（建筑宪位置表，㉠-㉒ 为全局槽位号）

```
sample-service/
├── sample-service-contract/src/main/java/.../contract/
│   └── payment/
│       ├── adapter/rest/controller/PaymentController.java ← ① Controller 契约接口
│       ├── dto/co/PaymentCO.java                    ← ② 契约输出
│       ├── dto/command/CreatePaymentCommand.java    ← ③ Command
│       ├── dto/query/GetPaymentQuery.java           ← ④ Query
│       └── enums/PaymentStatus.java                 ← ㉒ 契约枚举（CO 值域镜像 domain 状态机）
│
└── sample-service-server/src/main/java/.../
    ├── adapter/rest/controller/
    │   └── PaymentControllerImpl.java           ← ⑤ Controller 实现（REST 入口）
    ├── application/payment/
    │   ├── service/PaymentAppService.java       ← ⑥ AppService
    │   ├── dto/PaymentDTO.java                  ← ⑦ 内部 DTO
    │   ├── assembler/PaymentAssembler.java      ← ⑧ Assembler
    │   ├── presenter/PaymentPresenter.java      ← ⑨ Presenter
    │   ├── handler/
    │   │   ├── command/CreatePaymentHandler.java ← ⑩ CommandHandler
    │   │   └── query/GetPaymentHandler.java      ← ⑪ QueryHandler
    │   └── repository/PaymentQueryRepository.java ← ⑳ 读端口（extends QueryRepository）
    ├── domain/payment/
    │   ├── model/Payment.java                   ← ⑫ 聚合根
    │   ├── model/PaymentStatus.java             ← ⑬ 枚举
    │   └── repository/PaymentRepository.java    ← ⑭ Repository 接口（写侧）
    └── infrastructure/persistence/master/payment/
        ├── mybatis/po/PaymentPO.java              ← ⑮ PO（纯 POJO，零 ORM 注解）
        ├── converter/PaymentConverter.java        ← ⑯ Converter（框架 BasicConverter 桥）
        ├── mybatis/mapper/PaymentMapper.java      ← ⑰ Mapper（extends DddMapper）
        ├── repository/PaymentRepositoryImpl.java  ← ⑱ RepositoryImpl（继承 MybatisPersistence）
        └── repository/PaymentQueryRepositoryImpl.java ← ㉑ 读实现（PO → DTO 直投，与 ⑱ 同包）

sample-service-server/src/main/resources/
└── mapper/payment/PaymentMapper.xml               ← ⑲ 手写 SQL（DddMapper 七条语句契约）
```

## §2 形状条款

| # | SHALL | 取证 |
|---|---|---|
| BP-1 | 每个新聚合按 §1 槽位建满 22 文件（读端口 ⑳㉑ 可随首个读用例补齐；除此之外缺一即违反建筑宪） | 本卷 §1；skill `new-aggregate` 第 0 步对账 |
| BP-2 | 契约枚举 ㉒ 与 domain 枚举 ⑬ 值域镜像，契约段以枚举追节登记奇偶 | 奇偶守恒锁 = `ContractEnumParityTest` |
| BP-3 | 创建顺序：contract（①-④+㉒）→ domain（⑫-⑭）→ infrastructure（⑮-⑲+㉑）→ application（⑥-⑪+⑳）→ adapter（⑤） | 原篇顺序节入法（接口先行、依赖倒序） |
| BP-X1 | XML 七条语句契约：表名含 schema 前缀；select/update/delete（逻辑删除聚合）显式 `AND is_delete = false`；`insert` 不枚举 `is_delete`；`existsById` 恒返一行 boolean；`updateById` 携 `SET version = version + 1 ... AND version = #{version}`；文件位 `resources/mapper/{agg}/` 且 namespace = Mapper 全限定名 | `modules/ddd.md` 场景 2；OL-4 互指 |
| BP-X2 | PO 纯 `@Data` 零 ORM 注解；Mapper `extends DddMapper`；RepositoryImpl 继承 `MybatisPersistence`；Converter.toDomain 一律经 `reconstitute()` 重建 | AGENTS 九条 9；`modules/ddd.md`；TC-2 互指 |
| BP-X3 | 读端口 ⑳ 必须 `extends QueryRepository` 且位于 `application/{agg}/repository/`（R13） | read-chain RC-1 互指 |

## §3 验收单（交付闸，逐条可机械化）

- [ ] `mvn compile` 通过（无循环依赖）
- [ ] ArchUnit 测试通过（common-test 规则集）
- [ ] domain 层零框架注解（纯 Java + common-ddd）
- [ ] 应用层 DTO 实现 `ApplicationDTO` 标记（R10b）；CO 实现 `CO` 标记
- [ ] BP-X1~X3 逐条勾验
- [ ] 新行为断言 = delta Scenario（TC-3）

## 生效登记

全部 ✅（sample 两聚合按本宪建成，槽位真实性由 C1 逐槽代入对账）。
