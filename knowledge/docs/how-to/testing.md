# 测试编写 · 设计卡

> **本篇=设计卡（2026-09-06 统一用法归卷裁定）**：只回答"该测什么、四类怎么分"。四型模板、Fixture、命名、验收终板与全部条款见法卷 → [../../specs/current/patterns/testing-conformance.md](../../specs/current/patterns/testing-conformance.md)；条款冲突以法卷为准（rules/05 §2）。技能 `.agents/skills/new-test` 的模板指向将随本归卷重定向至法卷（技能文件本次不动）。

> 立场：教「这一类东西怎么测」；「为什么这样测」的论证在 [../explanation/](../explanation/) 与 [../reference/api/common-test.md](../reference/api/common-test.md)。

## 业务场景

框架把「测试即验收」当一等公民：三分通道行为、乐观锁并发守恒、契约枚举奇偶都由测试锁死，而非文档口头承诺。新增一个聚合/Handler/Converter 后，按下面四类各就位一份即覆盖该层契约。

## 四类选型（形状唯一样本在法卷 §2）

| 型 | 测什么 | 法卷锚点 |
|---|---|---|
| A Handler 单测（Mockito 零容器） | 委托链 + 异常路径，不复断领域规则 | §2.1 / TC-1·4·6 |
| B Domain 纯 JUnit | 状态机迁移与 validate 不变量 | §2.2 / TC-2 |
| C Converter 往返 | PO↔domain 等价 + 脏数据快速失败 | §2.3 / TC-1 |
| D 集成（test profile + H2） | 全链路真 SQL 语义 + 400/422 分界 | §2.4 / TC-1·5 |

## 设计判断

- 骨架用 `{Agg}` 通式而非具体虚构方法名——v1 曾用 confirm 等 Payment 具体方法名，与 new-aggregate 的 Payment canon（仅 create/validate）打架，遂改通式（判例：common-exception 卷场景 2 的「{Agg} 教学占位」形态）。具体形状一律走法卷 §2.8 真实例指针位。
- 造数只经 Factory / `reconstitute()` 两条合法路径，反射绕过=教坏框架没定的形状（TC-2；Fixture 模式见法卷 §2.5）。
- D6 归属随统一用法归卷移交：模板 canonical 从本篇移至法卷 §2，本篇不再复写任何代码形状。

## 边界与代价

- 容器测试只走 test profile/H2，「clone 即 `mvn test` 全绿」是根基契约（TC-5；入口见 [../tutorials/quickstart.md](../tutorials/quickstart.md) 路线 A）
- 涉及新行为/新通道：先立 delta 再写断言（TC-3）
- 提交前对单：法卷 §2.7 验收终板五项清单；类名/方法名/Fixture 命名规范见法卷 §2.6

## 落地状态

一律以法卷 §3 生效登记为准（TC-1~8 ✅，119 测试基线在册对账）。
