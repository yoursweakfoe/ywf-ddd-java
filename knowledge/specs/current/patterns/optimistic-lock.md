# 用法规范法卷：乐观锁与冲突重试（框架法 · 严格件）

> **身份**：本卷规范版本冲突识别与重试形态；修卷走 `../../changes/`。docs 同题篇（`how-to/optimistic-lock-retry.md`）为宽松件，冲突以本卷为准。
> **机器对账**：C1/C3/C4 扫本卷。开册法案：`2026-09-howto-codification`。

## 条款

| # | SHALL | 取证 | 背书 |
|---|---|---|---|
| OL-1 | 冲突识别只走**类型通道**：UPDATE 版本条件 0 行 → 框架抛 `OptimisticLockConflictException`（extends `IllegalStateException`）；INSERT/DELETE 0 行 → `SilentWriteLossException`。二者消费路径不同（409 冲突通道 vs 500 静默丢失通道，`modules/exception.md` EV-4） | 框架 `MybatisPersistence.updateDomain()` 失败路径 javadoc | 守恒测试（冲突通道实证） |
| OL-2 | **禁止按异常消息文本判断冲突**；新增冲突通道时禁止要求消费方改文本匹配（文本是给人看的，类型是给机器跟的） | 原篇「消费方按类型分支即可……新通道禁止依赖消息文本判断」入法 | — |
| OL-3 | 重试方只 catch `OptimisticLockConflictException`；每次重试必须**重新 load** 聚合再执行行为，禁止复用内存中旧实例 | 原篇「Handler 层 findById 已保证每次加载新实例」入法；WC-2 互指 | — |
| OL-4 | `version` 字段由框架 SQL 维护（`SET version = version + 1 ... AND version = #{version}`），业务层禁止手工读写版本号 | `aggregate-blueprint.md` BP-X1 互引（XML 语句契约）；ADR-0002 全量更新无脏检查判例 | 乐观锁压测 |

## 生效登记

OL-1~4 ✅（框架通道与示例聚合行为均有测试实证在册）。
