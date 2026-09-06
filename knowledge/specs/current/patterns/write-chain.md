# 用法规范法卷：写用例链（框架法 · 严格件）

> **身份**：本卷是写侧用例组织规范的**唯一权威**，消费代码必须遵循，违反=修代码；修卷只走 `../../changes/` 程序。docs 同题篇（`how-to/write-path.md`）为宽松件，冲突以本卷为准（宽严双份，2026-09-06）。
> **机器对账**：C1（`{agg}` 实例化）/ C3（符号）/ C4（中立）扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| WC-1 | 依赖方向单向：`adapter → application → domain ← infrastructure`；domain 零框架运行时依赖（stereotype 豁免，纯 Java + common-ddd） | ArchUnit 双端守护（common-test 规则集） | R1/R2 系 |
| WC-2 | 写侧 Handler 执行形态固定四拍：load 聚合 → 调聚合行为 → save → toDTO；Handler 标 `@Transactional` | `modules/ddd.md` 场景 3；AGENTS 九条 3 | ArchUnit |
| WC-3 | 业务规则（if-throw）封在聚合根内；Handler 内禁止业务分支判断 | AGENTS 九条 4；`modules/ddd.md` 场景 1 | 聚合根守卫测试 |
| WC-4 | Handler 返回 DTO；AppService 返回 CO；Adapter 实现契约接口纯透传（零逻辑） | AGENTS 九条 2；`modules/ddd.md` | R10b（DTO 标记）|
| WC-5 | Assembler（domain→DTO）与 Presenter（DTO→CO）强制分离，禁止一类通吃 | AGENTS 九条 6；`BasicAssembler`/`BasicPresenter` 框架基类 | C3 |
| WC-6 | 异常场景在聚合行为方法内显式 `if + throw BusinessException(messageKey)`，禁止吞错返回 null/布尔 | `modules/exception.md` EV-1 | — |

## 生效登记

全部 ✅（sample 示例聚合现行遵循；真实例逐文件清单属宽松件，本卷不载）。
