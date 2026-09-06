# 用法规范法卷：读用例链（框架法 · 严格件）

> **身份**：本卷是读侧用例组织规范的**唯一权威**，消费代码必须遵循，违反=修代码；修卷只走 `../../changes/` 程序。docs 同题篇（`how-to/read-path.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| RC-1 | 读侧绕过聚合根：读端口接口定义于 `application/{agg}/repository/`，`extends QueryRepository`；infra 实现直投 PO → DTO，不加载聚合 | R13；R1b 白名单放行 infra 对该端口的实现依赖 | C1 实例化 |
| RC-2 | QueryHandler 只注入读端口，禁止依赖 domain 侧写 Repository | R13（QueryHandler 禁触 domain 仓储） | ArchUnit |
| RC-3 | 分页链路：Query record 经 `@ModelAttribute` 绑定 → 构造 `PageResult`；缺参绑定为原始类型默认值 → 校验注解 → 400；**无默认分页值**（此承诺自卷 `modules/contract.md` §3.1，本条互指不复述） | `modules/ddd.md` 场景 4；binding 案卷 `archive/2026-09-pagequery-default-claim` | C3 |
| RC-4 | 多视图投影用 ViewDTO/ViewPresenter，规则见 `application-objects.md`（AO-1 准入） | `modules/ddd.md` 场景 4 | — |

## 生效登记

RC-1~4 ✅（sample 读路径现行；真实例文件名清单属宽松件）。
