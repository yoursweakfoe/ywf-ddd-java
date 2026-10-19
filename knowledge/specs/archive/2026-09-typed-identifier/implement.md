# 执行账本（施工期间唯一记录件。完成一条勾一条，不许攒一批事后补勾）
> 施工已开工（2026-09-11，施工会话受项目主明示指派；两门收据见 specify §裁决记录）。协调闸实况：`2026-09-pattern-taxonomy` 案**已折叠归档**（archive/ 在册）→ 19 条闸开，折叠写回按该案迁都后新路径解析卷锚。

## §1 执行账（与 tasks 条项 1:1 镜像）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
| 1 | ✅ | 新建 `common-ddd/.../domain/model/Identifier.java`（纯 JDK 接口 `V value()`，零默认方法/基类；javadoc Payment 家族教例、「装箱不站岗」注、锚 BP-13）；`mvn -B install -DskipTests -pl ywf-ddd-common/common-ddd,ywf-ddd-common/common-test -am` EXIT=0 |
| 2 | ✅ | Identifiable/AggregateIds/MybatisPersistence 三文件 javadoc-only（git diff 纯注释行实证）：配对句、mint() 仍发裸 UUID（Q4/甲）、toPersistenceId 唯一转换位注（P-4） |
| 3 | ✅ | `DddArchitectureRules.AGGREGATE_ROOTS_USE_TYPED_IDENTIFIERS`（R15，反射判泛型槽：根链上溯+端口 BFS；等式臂备胎见 §4 F-boundary 勘获）挂业务扫描 `ApplicationArchitectureTest.r15_typed_identifiers`（框架扫描不挂＝结构性空转禁则）；类头 TR-1 段改顶位语义（Q8）、模块地图块4 添 R15；`IdentifierProbes`（archproof.domain，两扫描根外）+ `IdentifierRuleProofTest` 四锁 **4/4 绿** |
| 4 | ✅ | 混放锁取证：`src/test/resources/compile-proofs/IdMixingCompileProbe.java` javac **EXIT=1**「incompatible types: ProductId cannot be converted to OrderId」＋对照件 `IdMixingCompilePass.java` **EXIT=0**；版本位反证落地并绿：`OrderConverterTest.acceptsAnyUuidOnLoad`（v4 固定值断言非 v7 前置、`OrderId.of`/`ProductId.of` 不抛、PO 装载往返+items JSON 存量形状照过、回写逐字还原）；迁型前后弹对照：r15 先恰咬 4 处（Order/OrderRepository/Product/ProductRepository）→ 迁型后 0 咬（12 条） |
| 5 | ✅ | `domain/order/id/OrderId.java`、`domain/product/id/ProductId.java`（record + Identifier + Serializable、of() 仅 null 检查、⑫ 新槽位实形、javadoc 锚 BP-13） |
| 6 | ✅ | Order/Product 根 `AggregateRoot<{Agg}Id>`、端口 `Repository<…, {Agg}Id>`、Factory `OrderId.of(AggregateIds.mint())`、reconstitute/Converter 装箱 `OrderId.of(po.getId())`↔拆箱 `getId().value()`（编译+测试绿实证） |
| 7 | ✅ | OrderItem.productId→`ProductId`、InventoryDomainService 签名换币种、跨聚合仅 import id 包（`OrderTest`/`InventoryDomainServiceTest` 绿） |
| 8 | ✅ | 写 Handler 入口一点定型抽查实录：`PayOrderHandler:34` 恰一行 `OrderId.of(command.getOrderId())`；`PlaceOrderHandler:68/91` 批量行项 `ProductId.of(...)` 定型后进链路；WC-13 无隐式转换（契约/全局 Converter 零动） |
| 9 | ✅ | `OrderRepositoryImpl:55` / `ProductRepositoryImpl:54` 各恰一处 `protected Serializable toPersistenceId({Agg}Id id)`；AC-4 四方零动实证：`git diff` 无 XML/PO/db-migration 命中（grep 闸 EMPTY） |
| 10 | ✅ | `git diff --stat -- sample-service-contract` **EMPTY**（CQE/CO/枚举/接口零动）；wire 出口拆箱住 Presenter（CO 字段形状不变）；仓内无既有 OpenAPI schema 测试 → AC-3 采静态零 diff 取证 + Converter/Presenter 往返测试背书（items JSON 经 Converter 私有中间形状 `ItemJson` 守字节形态、不开全局配置） |
| 11 | ✅ | 夹具随动毕、根 `mvn -B install` **15/15 SUCCESS / MVN_EXIT=0**：common-packages-integration-test **193** tests 0F/0E/0S、sample-server **129** tests 0F/0E/0S（PG 门含 `OptimisticLockConcurrencyTest` 压测绿〔success=10/conflict=9，wire 上 productId 裸 v7 UUID〕、`RestEndpointIntegrationTest` 18/18 绿——HTTP 面 UUID 形状、异常映射、校验全行为不变实证） |
| 12 | ✅ | `ApplicationArchitectureTest` **21/21 绿**（r15 零违例＝迁型闭环 + 豁免位实弹不误伤：读端口/读 DTO/子实体 JSON 底值/PO 全裸值照绿）；`DddArchitectureTest` 11/11、`IdentifierRuleProofTest` 4/4 同跑绿（三合一阵 110/110 实录） |
| 13 | ✅ | skills 三件重号（new-aggregate 全篇旧 ⑫–㉒ 顺移成 ⑬–㉓＋新 ⑫ 模板＋套件数 22→23、最小闭环 20→21；scheduled-task ⑮/⑱；new-service ㉓）；全库清扫 registry 在册（current/ 蓝图两处＋test.md TR-1 按计划留折叠日；工序号/列举号/㊞ 界零触碰实证）；check-docs C1–C7 **7/7 EXIT=0**、C2 手算 declared{23,21}×glyphs=23 ✓ |
| 14 | ✅ | glossary 三行＋顶位措辞、api/common-ddd 登记表＋宽松节、api/common-test §2 R15 行（与落地代码实名逐字对账）、architecture-rules 顶位承接句＋十五支、theory-map ③⁺ 采纳行接管旧未采纳账、how-to 卡 23/23、testing 篇残余、common/AGENTS 闸行 R1–R15 |
| 15 | ✅ | new-usecase 入口 `of()` 定型＋读侧裸值行、batch-operations BP-6 保持＋of() 注、ddd-review R15 检查项＋三处槽位重号 |
| 16 | ✅ | 折叠日换号毕：蓝图全卷（§1 树添 `id/PaymentId.java ⑫` 行、⑫–㉒→⑬–㉓、BP 行内嵌引用、§4 节题、§5 通式、计数 23/21）、write-chain ⑨/⑳ 两行、scheduler SC-6 行＋L124 计外补换（裁④）；存疑两件判毕（docs/README 工序号零改、architecture-rules 列举号零改）；㊞ 界/工序族零触碰实扫 |
| 17 | ✅ | 终闸实录：check-docs C1–C7 **7/7 EXIT=0**（折叠后与归档后各一遍）；check-diagrams 三方一致 **OK**（drive-relations 既存产物脱钩系 HEAD 旧账〔efc19be 路径简化合并未重渲〕，render-diagrams v0.9.0 重渲归零，非本案引入）；ddd-review 全清单 **PASS**（零 WARN：id 包 import 纯 JDK 直扫、无 Jackson 魔法、无 synchronized/死代码、锚点抽查 §4.⑫/BP-13~17/WC-13/R15 全可解析）；mvn 15/15 见 11 行 |
| 18 | ✅ | delta 全量写回十卷（git diff 终态 +143/−101，全在 current/）：ADDED=词汇发行→modules/ddd.md、BP-13–17→蓝图 §2、WC-13→写链 §1＋§3 登记行；MODIFIED=BP-6 行、§4.⑫ 新节＋⑬⑮⑰⑲ 走查、§5、§生效登记换号映射行、写链 §2.5–§2.12 教学一致化（含主控亲修 §2.6/§2.7 实形桥）、跨聚合 CA-7/§2.2/§2.3、批量 §2.2、防腐 §2.2/§2.3、应用对象 AO-8/§2.3、编码公约 §2.1 行、ddd.md 场景 1/2、test.md 构造示例（裁⑦=无操作）＋TR-1 顶位行；`← AC-n`/折叠指令注释零残留 |
| 19 | ✅ | 协调闸开工前即勘：pattern-taxonomy 已归档（archive/ 在册）→ 按其迁都后摊路径锚定（building-block/chain/collaboration/boundary/discipline/modules）落位毕，无需停折 |
| 20 | ✅ | ⑨⁺ 复写立篇 `docs/explanation/typed-identifier.md`（BOM+CRLF 合族、篇脚回指本案卷 §裁决记录）＋docs/README、knowledge/README 树行、doc-templates 三处计数兑现；§3 清账毕；`git mv` 整目录入 `archive/2026-09-typed-identifier`（未拆件、未留守）；本 §5 收官闸全勾 |
## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
| AC-1 | 混放编译锁双向：compile-proofs javac EXIT=1（ProductId→OrderId 位）/ EXIT=0（正位+非 ID 位）；`IdentifierRuleProofTest` 四锁 4/4；R15 实弹迁型前恰咬 4 处→迁型后 `ApplicationArchitectureTest` 21/21；入口定型 `PayOrderHandler:34`、`PlaceOrderHandler:68/91` | ✅ |
| AC-2 | `Identifier.java` 零 JDK 外依赖（纯接口）；框架扫描 `DddArchitectureTest` 11/11（R3/R4 对新增骨架文件真实开火且绿）；common 双模块 install EXIT=0 | ✅ |
| AC-3 | `git diff --stat -- sample-service-contract` EMPTY（CQE/CO/接口/枚举零动）；拆箱收口 Presenter；`OrderConverterTest` 往返锁 items JSON 字节形态（`ItemJson` 中间形状，PO 列文本不变） | ✅ |
| AC-4 | 四类零动 grep 闸 EMPTY（XML/PO/schema/db-migration）；转换恰一位：`OrderRepositoryImpl:55`、`ProductRepositoryImpl:54` | ✅ |
| AC-5 | 机制类型无感：`Identifier<V>` 泛型 + R15 反射臂只验接口不验承载 + of() 无版本位/格式门槛（`acceptsAnyUuidOnLoad`）；Long 承载教例=法卷 BP-16 纸面条款（P-7），取证随折叠落位 | ✅（纸面项以折叠生效为准） |
| AC-6 | 两聚合全套迁型毕；根 `mvn -B install` 15/15 SUCCESS（193+129 全绿 0F/0E/0S，含 PG 门）；折叠日 check-docs+ddd-review 终闸随 task 17 | ✅ |
| AC-7 | 折叠日换号清扫+delta 十卷落位+指针兑现+⑨⁺ 立篇+登记计数毕；check-docs C1–C7 **7/7 EXIT=0**（归档后终跑）、check-diagrams 三方一致、ddd-review PASS | ✅ |
## §3 源回填账（plan §delta 每条 SHALL：把「待回填」换成真实取证位）
2026-09-11 清账毕：plan §2 八处「待回填」已全部替换为真实位（词汇发行→Identifier.java+框架扫描绿；BP-13→规则常量+id 记录件+四锁探针；BP-14→OrderItem/InventoryDomainService；BP-15→读端口/PO/contract diff-zero+探针锁三；WC-13→PayOrderHandler:34/PlaceOrderHandler:68/91；BP-16→泛型位+Factory 源①+DddMapper 源③半槽；BP-17→R15 主语谓词+豁免实录；TR-1→类头纪律段现文+解读篇承接句）。混放 javac 实录全文存 §1 task 4 行与 compile-proofs/ 资源位。零「待回填」残留（grep 自查随折叠终闸）。
## §4 漂移记录（范围扩大或新发现事项：记回批准门的日期与门次；无漂移也要在本节写「无」）
- 2026-09-11 施工时点补裁 Q8（Plan 门后、非范围扩大）：P-5 新规则号 R15 与 test 模块卷 TR-1 现文「编号永不复用」相触，项目主裁「没有永不复用这种说法，占位缺失就顶上」→ 顶位用 R15、TR-1 条款随裁补入 plan §修卷 delta MODIFIED 一行（test 模块卷），§3 波及面补 Q8 顶位三处（glossary 行、解读篇顶位承接注、common/AGENTS.md 守则行）。案卷外零范围变化。
- 2026-09-11 施工时点勘获（WP1，非裁决、事实修正，P-5 审议稿已随改）：探针第四锁原形状「端口-根槽不一致必咬」**javac 不可构造**——框架既有 F-边界 `Repository<Domain extends Identifiable<ID>, ID>` 已在编译期锁死端口槽=根槽（实证：`Repository<TypedRoot, UUID>` 被 bound 拒绝）。R15 一致性等式臂降为裸继承/类型擦除逃路的防御备胎；第四锁改实形为**端口裸槽必咬**（`Repository<裸根, UUID>` 端口身 + 裸继承根身双形，探针 `rule_fails_for_untyped_port_slots`）。咬合面不缩：迁型前对真实 sample 恰咬 4 处（含两端口令牌）。
- 并行施工面记账：WP1（代码）与 WP3（文档/技能）两会话文件零交集，双向对账毕（字典 R15 行取实名）。
- 2026-09-11 **折叠时点裁断**（scribe 呈报 1 STOP + 6 范围勘获，主控照裁；均系已折条款之机械推论或起草笔误，零新法）：① 写链教学族=Reservation，delta「聚合根形状换 PaymentId」为起草笔误→按教学族正字 **ReservationId** 执行，plan 原文不改（审议记录如实存，执行解释住本条）；② 写链 §2.9/2.10/2.12 与蓝图 §4.⑦⑧⑨⑩ 教学件随 BP-13/15/16、WC-13、P-3 币种域做**内部一致化**（delta 未及点名之连带面，折叠即修）；③ 教例跨聚合字段（Payment.orderId 等）加一句 BP-14 指针注、型不入教例世界（铸 OrderId=造新法，拒）；④ scheduler L124 同对圈号随 SC-6 行补换（plan「2 处」计数笔误）；⑤ 编码公约 §2.2 表身已迁摆架卷墓碑位——{Agg}Id 位之事实家=蓝图 §5（已折），layering 卷有子槽列举才补行、无则不动（scribe 勘分支回报）；⑥ 蓝图 §3 验收单区间 BP-4~12→BP-4~17；⑦ **test 模块卷构造示例位不存在=无操作**——该卷唯一构造位是契约 CQE 造数字段（恰属 BP-15 豁免面），delta 此条系对卷内容之起草误想；AC-6 测试改型取证实际住 sample（`TestOrders` 夹具、`OrderConverterTest.acceptsAnyUuidOnLoad`），法条本体（WC-13/BP-13）不受影响。裁断后 current/ 教学件互洽复扫随 task 17 终闸。
- 2026-09-11 **折叠一致性手术回报落账**（F1A 续轮 + 主控补裁）：裁② 分支实况——蓝图 §4.⑦ PaymentDTO 兼作读投影（§4.⑪ 复用句在卷），携币种即 RC-6 泄漏，判**双役豁免留裸**并注记（写专用 DTO 准携币种，实形=真实例 OrderDTO:35 `OrderId`）；写链无读复用臂→§2.6/§2.7 主控照实形亲修（Assembler typed 直通、Presenter `value().toString()` wire 收口，OrderAssembler:23/OrderPresenter:26 同构）；裁⑤ 分支＝layering 卷无子槽列举层→不动，事实家=蓝图 §5+§1 树；裁⑦ test.md 无操作确认（唯一构造位=契约 CQE 豁免槽）。current/ 十卷 diff 终态 +143/−101；裸 P-x/implement/裁定 引用消歧扫零残留（WC-13/词汇发行行审计=本无裸引）。
- 2026-09-11 WP2 施工事故注记：sample 迁型会话曾被主控误取消（harness 操作失误，非评审信号），原会话恢复后确认代码面已全部落地，其后编译/架构三合一阵/非 PG 单测 110 由主控直接执闸取证（实录见 §1 各条与 §2），验证结论不依赖该会话自报。
- PG 门清单（11 条余量）：sample `OptimisticLockConcurrencyTest`/`RestEndpointIntegrationTest`（真库 ddd_sample_application_test）+ `common-packages-integration-test` 全量（PgTestSupport 自建基座）；前置=docker/postgres 起。
## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」——AC-1~7 全 ✅（§2 映射表逐行有 文件:行/测试名/闸结果；§3 清账毕，plan §delta 八处待回填全兑）
- [x] 七闸全绿（触码案加 mvn）；触图案 check-diagrams 绿——check-docs 7/7 EXIT=0（归档后终验）、check-diagrams 三方一致 OK、mvn -B install 15/15（193+129 全绿含 PG 门）
- [x] 瘦身自查毕——两门收据齐（Specify 门/Plan 门 2026-09-11 原语在册）、Q1–Q8 各裁有表决行与当时理由、supersede 全「无」无需点名；条文正文只住 plan（specify 零条文、tasks 零参数、账只住 implement）；「今天仍成立」论证已随 ⑨⁺ 复写解读篇 typed-identifier.md，快照留 §裁决记录（在位不改自此生效）
