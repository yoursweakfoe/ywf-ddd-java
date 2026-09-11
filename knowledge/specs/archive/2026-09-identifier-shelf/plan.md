# 施工方案与修卷 delta
> 怎么做与改哪些法；本案发码面即搬移+引用随动，法面只触 ddd 模块卷一处。

## §1 技术形状
- **P-1 搬移面（全库引用勘验定档）**：`git mv` Identifier.java → `domain/id/`（package 声明改 `…domain.id`）；随动 import/FQCN：`OrderId`、`ProductId`、`IdentifierProbes`、compile-proofs 两件（重跑 javac 取证刷新）、`DddArchitectureRules` 之 `IDENTIFIER_TYPE` 常量串；javadoc 同包链接改全限定：`Identifiable`（两处）+`AggregateIds`（两处）+「共居 model」措辞回归两件套叙事；法卷 `modules/ddd.md` 词汇发行条款与取证位；docs：api/common-ddd.md、glossary、typed-identifier.md 解读篇——凡提及词汇架位者。被拒：连 `AggregateIds` 一并迁（指令只点名 Identifier；mint↔Identifiable 两面叙事在 model 不破）。
- **P-2 执法零改**：R15 判据按名传递闭包（`implementsIdentifier`）与违规消息取常量串——改常量即全改，规则语义/挂载/探针四锁形不动。

## §2 修卷 delta（折叠时以本节为准写入 current/）
### MODIFIED
#### Requirement: 身份词汇发行（ddd 模块卷）   <!-- 节内两处词位替换，条款语义零变 -->
系统 SHALL 于 common-ddd `domain/id` 发行纯 Java 身份词汇接口 `Identifier<V>`（余文逐字不动）。（源：`common-ddd/.../domain/id/Identifier.java` 在库；框架扫描 `DddArchitectureTest` r3/r4 绿＝域纯度不破；全库旧坐标零残留 grep 实录 → implement §1）

### ADDED / REMOVED
（无。）

## §3 波及面与回退
- **代码**：common-ddd 1 文件搬移 + 2 javadoc、common-test 1 常量串、sample 2 import、archproof 1 import、compile-proofs 2 import；契约/PO/XML/schema 零动。
- **文档**：法卷 ddd.md 一处；docs 三处（api/common-ddd、glossary、typed-identifier 解读篇）——措辞级随动，零教义增量。
- **回退**：反向 git mv + 引用还原即净；未折叠前 current/ 一字不动。
- **验证**：根 `mvn -B install` 全量（PG 已起）+ javac 双向取证重跑 + check-docs/check-diagrams + ddd-review 增量项（词汇架位一致性）。
