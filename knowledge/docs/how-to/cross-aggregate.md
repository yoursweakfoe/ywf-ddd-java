# 跨聚合协调 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该不该协调、放哪里"。一切链路图、代码模板、文件清单见法卷 → [../../specs/current/patterns/cross-aggregate.md](../../specs/current/patterns/cross-aggregate.md)。

> 设计原理 → [../explanation/domain.md](../explanation/domain.md)（领域服务章节）

## 什么时候需要跨聚合协调

一个写用例横跨多个聚合——开票场景：查库存取单价、批量扣减多个库存额度、再创建新账单。判据：**协调逻辑不归属于任何单一聚合**。硬塞进某一个聚合根，它就得注入另一个聚合的 Repository（聚合间耦合、扣减逻辑散落在行为方法里），作废回补时同类逻辑还要复制一份；抽成共用的 Domain Service，扣减与回补对称复用。

## 决策点

1. **协调逻辑放哪**：聚合根（否——注入耦合、规则渗入单聚合）/ Domain Service（是——协调内聚、对称复用）/ Handler 直调他聚合 Repository 拼逻辑（否——用例判断渗入应用层）。条款在法卷 CA-1~CA-3。
2. **与 Policy 怎么分**：副作用有无是边界——Domain Service 可改实体、可调 Repository；Policy 纯决策无副作用。对比表 canonical 在 domain-policy 法卷（本卷 CA-6 互指）。
3. **多实例怎么读**：逐项查询还是按 ID 集合单次批量 IN、同项合并落库——义务与理由（N+1、版本号踩空）在法卷 CA-7。

## 边界与代价

- 事务边界恒在调用方 Handler，Domain Service 刻意不吞事务（法卷 CA-2）；跨聚合一致性 = 同本地事务直调，禁止异步化「尽力而为」（CA-3）
- 本卡教例（开票系）是示例应用真实下单链路的同构改写，落地状态以法卷 §3 为准
- 乐观锁冲突重试包装形态 → 参见 optimistic-lock-retry 设计卡；读写链路义务 → write-path / read-path 走查篇
- 设计原理纵深 → [../explanation/domain.md](../explanation/domain.md)（Factory、聚合边界章节）

## 相关

- **SecurityUtil 获取当前用户** → `knowledge/docs/reference/api/common-security.md` + `knowledge/specs/current/patterns/coding-conventions.md`（SecurityUtil 使用层归属）
- **common-pg TypeHandler** → `knowledge/docs/reference/api/common-pg.md`（UUID/JSONB/数组自动映射）
- **Factory 复杂创建** → [../explanation/domain.md](../explanation/domain.md)（Factory 章节）

## 落地状态

一律以法卷 §3 生效登记为准（框架条款 CA-1~7 ✅ / 下单链路真实例 ✅ / 开票教例系 ⛔ 模板在册）。
