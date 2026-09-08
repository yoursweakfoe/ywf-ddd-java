# 实现清单（框架案 · 按依赖序，[P]=可并行）

> 施工解锁前置：proposal 第 4 步批准门 + ⚖Q①②③ 拍板。业务代码批在镜像案 tasks，本案 task 与其合流于第 6 步闸门。

- [ ] 1. 基座核对（多数已在配置批落地）：`application-test.yml` 指向 `ddd_sample_application_test` 真 PG、`DB_TEST_*` 覆写通道可用；`mvn test` 在测试库未建形时报错信息可读（缺 schema 的失败指向 db-migration 指引）→ TC-9
- [ ] 2. db-migration 对 `_test` 库建形回归（已实证 Run:1 + SHA256 双库同形；若问句②裁决改隔离策略则先回改 delta 再动）→ TC-5/TC-9
- [ ] 3. [P] 镜像案代码批全绿后（依赖其 tasks 第 7 条），本案进入折叠准备：按 spec-delta 将 MODIFIED/ADDED 合入 `testing-conformance.md`（TC-1/TC-5/§2.4/§2.7 + §3 生效登记追加本 bill 行）→ TC-1/TC-5/TC-9
- [ ] 4. 折叠继续：`aggregate-blueprint.md`（§2 表 BP-6/BP-X1 行替换 + ADDED BP-S1~S3 入位 + 卷末生效登记）+ 机械替换表应用于 §1 槽位树、§4.⑮⑯⑲㉑ 代码块 → BP-S1~S3/BP-6/BP-X1
- [ ] 5. 折叠续：`prohibitions.md` §6 整节替换；`optimistic-lock.md`、`write-chain.md`、`read-chain.md`、`modules/pg.md` 按机械替换表应用 → BP-X1/BP-S1
- [ ] 6. 地图区连带（归属法 §4 同步义务，本刻履行）：`docs/tutorials/quickstart.md` 路线 A 改述（clone 即全绿 → 前置 = postgres 环节 + db-migration 一条命令）；`sample-application/AGENTS.md` 验证闸行改写（H2 test profile → 真 PG 测试库 + 前置）；`sample-application/README.md`「构建与运行」段同步（现文仍称测试 H2 零依赖——配置批后已失配，因呼应在册 TC-5 故随本案折叠统一清账，不提前改）；`docs/how-to/testing.md` 宽松件同题措辞随之 → TC-5
- [ ] 7. 框架代码批（✅Q① 裁决改判后的新范围）：`AuditProperties` 缺省 `createAt`/`updateAt` → `createdAt`/`updatedAt`（操作人缺省不变）+ 本类与 `AuditFieldFiller`/`MybatisPersistence` javadoc 同步 + common 测试树随缺省换名（fixture PO/XML/schema.sql 列名属必然连带）+ `reference/api/common-ddd.md`、`modules/ddd.md` 缺省行同 PR（ywf-common 铁律）+ **ADR-0033/0034 立卷**入 `knowledge/decisions/` 并登记卷宗索引 → BP-S1/TC-9
- [ ] 8. 闸门：根 `mvn -B install` 全绿（框架 + sample，真 PG 双库）+ `check-docs.ps1` 七闸全绿 + `ddd-review` 通过 → delta 全部 SHALL
- [ ] 9. 归档折叠：本目录整袋 `git mv` 至 `knowledge/specs/changes/../archive/2026-09-pg-native-shape-and-real-testdb/`——**文档同步义务唯一时点**，与镜像案同 PR 合流
