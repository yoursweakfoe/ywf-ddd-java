# 测试编写 · 设计卡

> **本篇 = 设计卡（宽松件）**：只回答"该测什么、四类怎么分"。四型模板、Fixture、命名、验收终板与全部条款见法卷 → [../../specs/current/patterns/discipline/testing-conformance.md](../../specs/current/patterns/discipline/testing-conformance.md)。条款冲突以法卷为准，见归属法卷 §2。技能 `.agents/skills/new-test` 的模板指向随本次归卷重定向至法卷，技能文件本次不动。

> 立场：教「这一类东西怎么测」；「为什么这样测」的论证在 [../explanation/](../explanation/) 与 [../reference/api/common-test.md](../reference/api/common-test.md)。

## 业务场景

框架把「测试即验收」当一等公民：三分通道行为、乐观锁并发守恒、契约枚举奇偶，都由测试锁死，不靠文档口头承诺。

新增一个聚合、Handler 或 Converter 之后，按下面四类各就位一份，即覆盖该层契约。

## 四类选型（形状唯一样本在法卷 §2）

| 型 | 测什么 | 法卷锚点 |
|---|---|---|
| A Handler 单测（Mockito 零容器） | 委托链 + 异常路径，不复断领域规则 | §2.1 / TC-1·4·6 |
| B Domain 纯 JUnit | 状态机迁移与 validate 不变量 | §2.2 / TC-2 |
| C Converter 往返 | PO↔domain 等价 + 脏数据快速失败 | §2.3 / TC-1 |
| D 集成（test profile + 真 PG 测试库） | 全链路真 SQL 语义 + 400/422 分界 | §2.4 / TC-1、5、9 |

## 设计判断

- 骨架用 `{Agg}` 通式，不用具体虚构方法名。这个通式形态沿用 common-exception 卷场景 2 的「{Agg} 教学占位」先例。具体形状一律走法卷 §2.8 真实例指针位。
- 造数只经 Factory / `reconstitute()` 两条合法路径。用反射造数等于教出框架没定的形状，禁止，见 TC-2。Fixture 模式见法卷 §2.5。
- 模板 canonical 随统一用法归卷从本篇移到法卷 §2，这是 D6 归属移交的结果。本篇不再复写任何代码形状。

## 边界与代价

- 容器测试只走 test profile 和真 PG 测试库。前置条件是 postgres 环节加 db-migration 建形，见 TC-5。隔离双轨见 TC-9，入口见 [../tutorials/quickstart.md](../tutorials/quickstart.md)。
- 涉及新行为或新通道：先立 delta，再写断言，见法卷 TC-3。
- 提交前对单：法卷 §2.7 验收终板五项清单。类名、方法名、Fixture 的命名规范见法卷 §2.6。

## 落地状态

一律以法卷 §3 生效登记为准。TC-1~8 ✅，119 测试基线在册对账。
