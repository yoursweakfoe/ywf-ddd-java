# 施工方案与修卷 delta
> 怎么做与改哪些法。

## §1 技术形状
- **P-1 位点命名**：`order:err.idRequired`、`product:err.idRequired`（教例 `payment:err.idRequired`）；词族循账本 `priceRequired` 先例，聚合前缀=币种自家聚合（ProductId 抛 product 位——OrderItem 的 order:err.productIdRequired 仍为其上层字段守卫位，两闸不互斥、各司其职：币种出生证与列表字段检查）。
- **P-2 真实件形状**：紧凑构造器 `if (value == null) { throw new BusinessException("{agg}:err.idRequired"); }`；import 换 `java.util.Objects`→`com.yoursweakfoe.common.exception.type.BusinessException`；javadoc「只装箱不站岗」句尾补「null 闸走统一位点」。测试仿 `OrderTest` 姿势 `assertThatThrownBy(…of(null)).isInstanceOf(BusinessException.class)`，落 `domain/order/id/OrderIdTest`、`domain/product/id/ProductIdTest` 两件。
- **P-3 教学面**：蓝图 §4.⑫ 代码块与尾注、BP-13 行半句、框架 Identifier javadoc `<pre>` 例、IdentifierProbes 夹具形、new-aggregate SKILL ⑫ 行半句、error-handling 登记账两行。被拒：抽公共断言工具/基类（币种守卫一聚合一词，重复即教义，不为两处样板建机制）。

## §2 修卷 delta（折叠时以本节为准写入 current/）
### MODIFIED
#### Requirement: 蓝图 §4.⑫ 教例代码块与尾注 + BP-13 行半句（聚合蓝图卷）   <!-- §4.⑫ 代码块内一行替换 + 尾注补一短句；BP-13 行「不校验底值」后补半句 -->
§4.⑫ 代码块：`Objects.requireNonNull(value, "PaymentId 不接受 null 底值");` → `if (value == null) { throw new BusinessException("payment:err.idRequired"); }`；尾注「只装箱不站岗」段补：「null 闸抛 `BusinessException` + `{agg}:err.idRequired` 统一位点（BP-12 同法——装箱不站岗指**不校验底值**，非**不走统一通道**；NPE/中文硬编码=500 兜底冒充业务位点，禁）」。（源：真实件 `domain/order/id/OrderId.java`、`domain/product/id/ProductId.java`＋测试 `OrderIdTest`/`ProductIdTest` 绿实录）
BP-13 行：「紧凑构造器仅 null 检查、不校验底值」后补「，null 闸异常走统一 BusinessException 位点（§4.⑫ 同形）」。（源：同上）

### ADDED / REMOVED
（法面无新增条款——本案系 BP-12 对币种教学件的收编；登记账两行为 docs 义务非修法。）

## §3 波及面与回退
- **代码**：OrderId/ProductId（主件）、IdentifierProbes（测试夹具）、Identifier.java javadoc（注释）、新测试两件；契约/PO/XML/schema 零动。
- **文档**：current/ 蓝图两处（§4.⑫/BP-13）、docs 一处（error-handling 登记账）、skill 一处（new-aggregate ⑫ 行）。
- **HTTP 行为面**：币种 null 入参（仅内路可达，Web 有 @NotNull 前闸）从 500 → 422+key——向既有异常教义收敛，非新行为。
- **回退**：整体反 patch 即净。
