# 执行账本（施工期间唯一记录件。完成一条勾一条）
> 开工 2026-09-11（项目主直令案，两门收据见 specify §裁决记录）。

## §1 执行账（与 tasks 条项 1:1 镜像）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
| 1 | ✅ | `domain/id/Identifier.java` 成件（untracked 件 Move-Item 搬移，package 声明改 `…domain.id`；javadoc 添镜像对偶句与案卷指针，同包链接全限定化） |
| 2 | ✅ | Identifiable javadoc：`{@link}` 全限定×2＋@see 添 Identifier＋共居句回归「可取/从何而来两面」旧序；AggregateIds：链接×2 全限定＋「共居 model」句零改（两件套仍在架）＋顺手修用法示例 `new Order(AggregateIds.mint()…)` → `new Payment(PaymentId.of(AggregateIds.mint())…)`（旧例违已折 BP-13 且触教学中立，见 §4） |
| 3 | ✅ | `DddArchitectureRules` `IDENTIFIER_TYPE` 常量串 → `…domain.id.Identifier`（一行，规则语义/挂载/探针形不动）；AggregateRoot 常量（domain.model）核对无误动 |
| 4 | ✅ | OrderId/ProductId/IdentifierProbes import 随动；批量补丁后按 code 树 noBOM 律归一（§4 记） |
| 5 | ✅ | compile-proofs 两件 import 随动＋javac 双向取证重跑（`-encoding UTF-8`，classpath=重编译 common-ddd/target/classes）：PROBE **EXIT=1**「incompatible types: ProductId cannot be converted to OrderId」、PASS **EXIT=0**——新坐标同错同锁 |
| 6 | ✅ | ddd.md 词汇发行条款 `domain/model`→`domain/id`＋镜像对偶半句＋取证位；docs 全域扫「旧坐标/共居/住 model」＝**零命中**（宽松件皆裸名，无需动） |
| 7 | ✅ | 根 `mvn -B install` **15/15 BUILD SUCCESS**（sample 129 含 r15+四锁探针、试验场全量，PG 门绿）；check-docs C1–C7 **7/7 EXIT=0**；check-diagrams 三方一致 OK |
| 8 | ✅ | §3 清账毕、§5 收官全勾、整目录 Move-Item 入 `archive/2026-09-identifier-shelf`（untracked 四件，git mv 不适用——先例同款）；归档后 AC-2 复扫：旧坐标残留仅存 2026-09-typed-identifier 案卷历史正文×2（在位不改），存活面零 |
## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
| AC-1 | 根 mvn -B install 15/15 SUCCESS（含 `IdentifierRuleProofTest` 4/4、`ApplicationArchitectureTest` 21/21 r15 按新坐标判据、混放 javac EXIT=1/0 双向） | ✅ |
| AC-2 | 全库 FQCN 扫描：code/current/docs/skills 零旧坐标；`domain/model` 目录清点无 Identifier | ✅ |
| AC-3 | check-docs 7/7（施工后+归档后各一遍）、ddd.md 条款与磁盘实位一致、指针无幽灵（宽松件零坐标引用故零动） | ✅ |
## §3 源回填账（plan §delta 每条 SHALL 的取证位兑现）
词汇发行条款取证位已随 task 6 直落 current/（`common-ddd/.../domain/id/Identifier.java` 在库＋框架扫描 r3/r4 绿 11/11 实录＋全库零残留 grep 见 §1-8 行），零「待回填」残留。
## §4 漂移记录（无漂移也要写「无」）
- 范围零扩大：项目主指令原样执行（仅迁 Identifier，AggregateIds/Identifiable 不动）。
- 施工时点顺手修×2（均为已折法之机械承接，非新法）：① AggregateIds 用法示例旧形 `new Order(AggregateIds.mint(),…)` 违 BP-13 币种教义且触 common 树教学中立（Order 词），改为 Payment 家族币种形；② 发现 WP1/WP2 误给 5 件新 java 文件带 BOM（code 树纪律=noBOM，BOM 仅 knowledge 区 md 惯例；裸 javac 取证在 BOM 下炸 illegal character），本案一并归一。
- 教训入库：批量正则补丁写文件须按该文件原编码约定归一（读字节判 BOM 再写回），勿一刀切。
## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」
- [x] 七闸全绿（触码案加 mvn）
- [x] 瘦身自查毕（直令收据在案、supersede 宣告点名旧案 plan P-1 架位；条款正文只住 plan、账只住本件）
