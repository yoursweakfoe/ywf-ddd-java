# 执行账本（施工期间唯一记录件。完成一条勾一条）
> 开工 2026-09-11（直令案，两门收据见 specify §裁决记录）。

## §1 执行账（与 tasks 条项 1:1 镜像）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
| 1 | ✅ | 蓝图 §4.⑫ 代码块 `requireNonNull(中文)` → `if (value == null) throw new BusinessException("payment:err.idRequired")`；尾注补「null 闸走统一位点——不站岗指不校验底值、非不走统一通道，NPE 中文＝500 兜底冒充业务位点禁」；BP-13 行补半句并双案卷指针（plan P-2 两处） |
| 2 | ✅ | 框架 `Identifier.java` javadoc `<pre>` 例：`IllegalArgumentException(中文)` → `BusinessException("payment:err.idRequired")`＋「禁硬编码文案」注（与 BusinessException 类 javadoc 原文「禁止使用硬编码的可读文案」对齐） |
| 3 | ✅ | OrderId/ProductId：compact ctor 改抛 `BusinessException("order:err.idRequired")`/`("product:err.idRequired")`，import `Objects`→`BusinessException`（domain 依赖 common-exception＝BP-12 既定，R4 豁免面不扩） |
| 4 | ✅ | IdentifierProbes.PaymentProbeId 同形（补 import、非业务锁零影响——四锁不执行构造守卫路径，复跑 4/4 绿） |
| 5 | ✅ | 新测试 `OrderIdTest`/`ProductIdTest`（of(null) 与 new(null) 双位点断言 `isInstanceOf(BusinessException.class)`，仿 OrderTest 姿势）——sample 129→**131 全绿** |
| 6 | ✅ | error-handling 登记账 +2 行（标真实例；行 25「现存皆虚构」句随之改为「payment/inventory 行属虚构、标真实例者为实际位点」）；new-aggregate SKILL ⑫ 行补 null 闸通道半句 |
| 7 | ✅ | `mvn -B clean install` 全清重建 **15/15 SUCCESS**（193+131 含 PG 门/r15/四锁/守卫）；病面复扫：存活面 **0**（唯二命中=本案卷 delta 前文引与 Why 引，案卷引文合法）；check-docs **7/7 EXIT=0** |
| 8 | ✅ | delta 即教学面写回（task 1 完成＝折叠写回完成，本案零待回填）；四件 BOM 归一、整目录 Move-Item 入 `archive/2026-09-id-guard-site`；§5 全勾 |
## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
| AC-1 | 存活面 grep `requireNonNull(…底值|不接受 null 底值` = 0；`OrderId.java`/`ProductId.java` compact ctor 现文 = BusinessException 位点形 | ✅ |
| AC-2 | `OrderIdTest`/`ProductIdTest` 绿、全量 mvn clean install 15/15（193+131，0F/0E/0S） | ✅ |
| AC-3 | 蓝图 §4.⑫/BP-13 与真实件同形互洽、框架 javadoc 对齐、登记账 2 行在册、skill 半句在位、check-docs 7/7 | ✅ |
## §3 源回填账
delta 取证位施工即兑（无「待回填」形态）：BP-13/§4.⑫ 行取证=真实件+两测试+131 全绿实录。
## §4 漂移记录
无范围漂移。一处裁量落地注：新 key 命名取 `idRequired`（账本 `priceRequired` 词族先例）；ProductId 抛 `product:` 前缀=币种自家聚合位（与 OrderItem 上层 `order:err.productIdRequired` 字段闸两立不互斥，plan P-1 已记）。
## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」
- [x] 七闸全绿（触码案加 mvn clean install 全清重建）
- [x] 瘦身自查毕（直令两门收据在案、supersede=无 已注、条款正文只住 plan、账只住本件）
