# 施工任务（tasks）

> 依赖序推进，完成即勾。每条尾巴指回 spec-delta 的 Requirement。批准门（proposal 递出）未过之前不动码。

## 阶段 0 · 批准

- [x] 0.1 用户拍板 Q1–Q4（库名/schema 名、模块名拼写、split-package 白盒、三段生命周期） → proposal「待裁决问句」

## 阶段 1 · 模块骨架（TC-10）

- [x] 1.1 `ywf-ddd-common/pom.xml` 挂 `<module>common-packages-integration-test</module>`；新模块 pom：全 8 common 模块 compile + common-test + spring-boot-starter-test/上下文栈 + pgjdbc + dynamic-datasource（compile 或 test）+ `maven.deploy.skip` → TC-10/§9 试验场
- [x] 1.2 `src/test/resources/application-test.yml`：dynamic 双 PG 源（master `currentSchema=common_it`、second `currentSchema=common_it_vacant`），凭据 env 可覆写默认本地 ywf → TC-9 框架轨
- [x] 1.3 `PgSchemaLifecycle`（test 基类/静态门闩）：maintenance 库自建行 CREATE DATABASE → 每 JVM 复位 schema×2 + 执行 DDL → shutdown hook 清空 → TC-9 框架轨三段

## 阶段 2 · 资产迁居（TC-10，保原包）

- [x] 2.1 迁 common-ddd：19 测试/fixture java + `schema.sql`（表名去限定、schema 归 `common_it`）+ 3 个 mapper XML（FROM 去限定）+ yml 并入 → TC-10/TC-9
- [x] 2.2 迁 common-pg 12、security 5、exception 2、contract 2、cloud 1（java + 各自 fixture/资源若有）→ TC-10
- [x] 2.3 各源模块删 `src/test/` 整树 + POM 测试栈依赖清零（含 common-ddd 的 h2/dynamic-datasource/common-pg-test）→ TC-1/TC-10

## 阶段 3 · 验证

- [x] 3.1 `mvn -B -f ywf-ddd-common/pom.xml install` 全绿（真 PG；首轮验证自建自清；复跑二轮验证残留复位）→ 全条
- [x] 3.2 `mvn -B -f sample-application/pom.xml test` 119 绿回归不破（common POM 瘦身后重装传递正常）→ TC-1

## 阶段 4 · 折叠归档（new-bill 第 6 步）

- [x] 4.1 delta 写回 `current/patterns/testing-conformance.md`（TC-1/TC-9 改文 + TC-10 入册 + §2.4/检查单波及句）与 `prohibitions.md` §9 登记表 → 各 MODIFIED/ADDED
- [x] 4.2 `ADR-0035-common-it-consolidation-real-pg.md` 立卷；ADR-0034 仅 Status 行加 `Superseded in part by ADR-0035` → TC-1 豁免废除
- [x] 4.3 案卷整目录 git mv 入 `knowledge/specs/archive/2026-09-common-it-consolidation/`
- [x] 4.4 地图连带：ywf-ddd-common/README（拓扑 9 模块）、根 README、api/common-ddd.md §4 去 h2、quickstart/skill 指针核对 → delta「地图连带」
- [x] 4.5 `check-docs.ps1` 七闸绿 + `ddd-review` → new-bill 第 7 步
