# 用法规范法卷：持久化语句（框架法 · 严格件）

> **身份**：本卷是 SQL 语句文本的写法法正身：六条铁律自禁令卷 §6 整节搬家而来（案卷 2026-09-pattern-taxonomy 搬家账 3）。六条历史上无编号，在本卷首次铸号——按摆架卷编号总则（meta-6）取摊名式：data-access-1～data-access-6；条文字符零改动。禁令卷 §6 节此后为对照身，逐行指回本卷。代码违反本卷就修代码；修订本卷走 `../../../changes/` 立案。docs 同题篇：无（禁令无选型面）。
> **机器对账**：C3 查符号、C4 查教学中立扫本卷。真实例名（sample 测试类）仅出现在带「实证」语义的取证位，与迁出前同形。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| data-access-1 | 禁止 MyBatis-Plus 进框架依赖树。持久化 = `DddMapper` 七语句 + 手写 XML，旧案卷判例。dynamic-datasource 是经一手调研证实零耦合的多数据源 opt-in 方案，`@DS` 合法 | 禁令卷原 §6 行一（迁前磁盘文可 diff） | 评审项 |
| data-access-2 | 禁止 PO 携带任何 ORM 注解。纯 `@Data` POJO；表名/主键/版本条件/逻辑删除全在 XML SQL 文本 | [aggregate-blueprint BP-X2](../building-block/aggregate-blueprint.md) | C3 |
| data-access-3 | 禁止 Wrapper 式动态条件。查询一律具名 Mapper 方法 + 具名 XML 语句，`<sql>` 片段复用防漂移 | 禁令卷原 §6 行三 | 评审项 |
| data-access-4 | 建表 DDL 默认含 `version BIGINT NOT NULL DEFAULT 0` + `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`，PO 声明对应字段。显式豁免的聚合 XML 省略对应条件，逐聚合自决，无共享开关 | [aggregate-blueprint BP-12](../building-block/aggregate-blueprint.md) | 评审项 |
| data-access-5 | `updateById`（有版本列）**必须**携 `SET version = version + 1 ... AND version = #{version} AND is_deleted = false`，无运行时拦截器。0 行后果三分通道 → [optimistic-lock OL-1](../collaboration/optimistic-lock.md)，行为由 sample `OptimisticLockConcurrencyTest` 实证 | OL 卷互指 | 乐观锁压测 |
| data-access-6 | 逻辑删除聚合的每条 select/update/delete **必须**显式 `AND is_deleted = false`，漏一处即泄漏。豁免聚合写物理 `DELETE` | 禁令卷原 §6 行六 | 评审项 |

## §2 规范形状（统一用法唯一样本）

语句形状（XML 七条）唯住 [aggregate-blueprint.md](../building-block/aggregate-blueprint.md) §4 槽位走查（BP-X1/X2），本卷立法不重抄形状（归属法 §2 单一住所）。

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| data-access-1～6 | ✅ 生效 | [../discipline/prohibitions.md](../discipline/prohibitions.md) §6 对照身在册 |
