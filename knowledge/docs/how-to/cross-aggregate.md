# 跨聚合协调 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该不该协调、放哪里"。链路图、代码模板、文件清单全在法卷 → [../../specs/current/patterns/cross-aggregate.md](../../specs/current/patterns/cross-aggregate.md)。

> 设计原理 → [../explanation/domain.md](../explanation/domain.md)（领域服务章节）

## 什么时候需要跨聚合协调

一个写用例横跨多个聚合时用。教例是开票：查库存取单价、批量扣减多个库存额度、再创建新账单。

判据一句话：**这段协调逻辑不归属于任何单一聚合**。

为什么不硬塞进某一个聚合根？那个聚合就得注入另一个聚合的 Repository，聚合之间从此耦合，扣减逻辑散落在行为方法里。到了作废回补，同类逻辑还要再复制一份。抽成共用的 Domain Service，扣减与回补就能对称复用。

## 决策点

1. **协调逻辑放哪**：三种选法只有一种对。放聚合根：否，注入耦合、规则渗进单个聚合。放 Domain Service：是，协调内聚、对称复用。Handler 直调他聚合 Repository 拼逻辑：否，用例判断渗入应用层。条款在法卷 CA-1~CA-3。
2. **与 Policy 怎么分**：边界是有无副作用。Domain Service 可改实体、可调 Repository。Policy 纯决策、无副作用。两者对比表的 canonical 在 domain-policy 法卷，本卷 CA-6 与它互指。
3. **多实例怎么读**：逐项查询，还是按 ID 集合单次批量 IN、同项合并落库。义务和理由都在法卷 CA-7，理由是 N+1 与版本号踩空。

## 边界与代价

- 事务边界恒在调用方 Handler，Domain Service 刻意不吞事务，见法卷 CA-2。
- 跨聚合一致性就是同一本地事务内直调，禁止改成异步"尽力而为"，见法卷 CA-3。
- 本卡教例即开票系，是示例应用真实下单链路的同构改写。落地状态以法卷 §3 为准。
- 乐观锁冲突的重试包装形态 → 参见 optimistic-lock-retry 设计卡。读写链路义务 → write-path / read-path 两篇走查卡。
- 设计原理纵深 → [../explanation/domain.md](../explanation/domain.md)（Factory、聚合边界章节）。

## 相关

- **SecurityUtil 获取当前用户** → `knowledge/docs/reference/api/common-security.md` + `knowledge/specs/current/patterns/coding-conventions.md`（SecurityUtil 使用层归属）
- **common-pg TypeHandler** → `knowledge/docs/reference/api/common-pg.md`（UUID/JSONB/数组自动映射）
- **Factory 复杂创建** → [../explanation/domain.md](../explanation/domain.md)（Factory 章节）

## 落地状态

一律以法卷 §3 生效登记为准。框架条款 CA-1~7 ✅；下单链路真实例 ✅；开票教例系 ⛔ 模板在册。
