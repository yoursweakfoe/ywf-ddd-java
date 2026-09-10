# 对 current/modules/test.md 的增删改
> 只描述变化量，不描述全世界（delta 教义）。每条带 SHALL + 场景。问 C 否决则本节整体撤案、只余解读篇（纯 docs 动作，无需 delta）。

## ADDED Requirements

### Requirement: 规则集治理（TR-1 编号稳定性）
`DddArchitectureRules` 各规则的 R##/C# 编号 SHALL 为教义锚点：docs、法卷、skills、提交信息一律以编号互指；编号永不重排、永不复用，规则删除时编号作废并留一行作废记录。现行规范行自类 javadoc 上收入卷，javadoc 保留一句复述 + 法卷指针（归属法 §2「规范行」行）。
（源：`DddArchitectureRules.java:26–28` 类头「编号纪律」段；本卷 TR-1 为规范行 home）

### Requirement: 规则集治理（TR-2 论证载体分工）
规则集整体与逐规则的设计论证现行版（立因/怎么判之理/被拒方案/沿革）SHALL canonical 居 `docs/explanation/` 同题解读篇（`architecture-rules.md`）；代码侧每常量 javadoc SHALL 只携挂载最小契约——守护什么、怎么判一句、挂载扫描入口、空集/空转态；类头 SHALL 保留模块地图与安全必读警示（空集通过机制、已知缺口清单）及解读篇指针。违反此分工（论证回流代码面 / 契约剥空）= 按归属法 §1 裁决修容器。
（源：`DddArchitectureRules.java:20–24` 类头「载体分工」段 ＋ `docs/explanation/architecture-rules.md`〔同题解读篇，在位〕）

### Requirement: 规则集治理（TR-3 缺口登记账）
规则覆盖缺口的**现状清单** SHALL 随代码登记于类头「已知缺口」节（描述性事实，代码驱动，地图义务）；缺口的**论证与待裁决账**（为何未实现、改规则还是改教义的二选一悬案）SHALL canonical 居解读篇缺口账节，每条含「立因/拒因或未决原因」。
（源：`DddArchitectureRules.java:53–65` 类头「安全必读」一行清单 ＋ `architecture-rules.md`「空转防线与缺口账」节）

#### Scenario: 修改规则集
- GIVEN 开发者向规则集新增或改写一支规则
- WHEN 提交同一 PR
- THEN 代码 javadoc 只更新挂载最小契约四栏，新论证写进解读篇对应节
- AND 新规则取新编号槽（作废号不复用），类头规则速览与 `reference/api/common-test.md` §2 表同步（C3/C5 对账面）

#### Scenario: 文档互指规则
- GIVEN docs / how-to / glossary 需要引用某支规则的理由
- WHEN 落笔
- THEN 只准编号 + 指向解读篇的指针，禁复写论证正文（D6 零复制）

## MODIFIED Requirements
<!-- 无：testing-conformance.md 通识节与 testing.md 总纲不动，仅按登记法/指针义务加行，属施工细节非条款变化量 -->

## REMOVED Requirements
<!-- 无 -->
