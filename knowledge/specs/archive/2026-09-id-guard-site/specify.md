# 币种守岗位点：{Agg}Id null 检查改统一异常通道
- Slug: 2026-09-id-guard-site ｜ 日期: 2026-09-11

## Why（为什么现在改）
2026-09-typed-identifier 施工产物 `OrderId`/`ProductId` 的紧凑构造器用 `Objects.requireNonNull(value, "OrderId 不接受 null 底值")`——硬编码中文可读文案 + NPE 走 500 兜底通道，违两条现行法：BP-12「异常统一 BusinessException，message 存 i18n 位点 `{aggregate}:err.{scene}`」与 `BusinessException` 类 javadoc「禁止使用硬编码的可读文案」。同树先例 `OrderItem` 守卫全走 `BusinessException("order:err.productIdRequired")`，正确姿势在码上、教学件却教了反面。病面全扫定档：真实件 2（OrderId/ProductId）、法卷教学位 1（蓝图 §4.⑫ 代码块——法卷自身与 BP-12 自相矛盾，须随案修卷）、框架 javadoc 示例 1（Identifier.java）、archproof 探针夹具形 1（IdentifierProbes.PaymentProbeId）、登记账 2 key 未入。compile-proofs（自包含 javac 约束、守卫不在教学面）与 common-ddd 内部英文 JDK 前置（Specification 等，框架内部件非业务位点）不在病面。

## What changes（做什么，非怎么做）
- 为聚合作者：币种 null 守卫改抛 `BusinessException` + `{agg}:err.idRequired` 位点，教学件（法卷 §4.⑫/BP-13 半句/框架 javadoc/skill 模板行）同形收口。
- 为消费方：币种底值缺失从「500 泛化文案」改归「422 + i18n key」业务通道（HTTP 行为面变化，方向即法）。
- 为登记账：新 key 两行入 error-handling 篇登记簿（EV-2 载明义务）。

## 验收标准（AC 账）
- AC-1 全扫零病残留：id 形文件中 `requireNonNull.*不接受 null`/`IllegalArgumentException.*中文` 归零；真实守卫抛 BusinessException。
- AC-2 新增守卫测试绿（两币种 null 位点断言），全量 mvn 绿（触行为案）。
- AC-3 法卷 §4.⑫/BP-13 教学形、框架 javadoc、skill 行、登记账四处与真实件互洽；check-docs 七闸绿。

## 约束
- 「只装箱不站岗」教义不动——仍仅 null 检查，零新增校验，只改**抛出物的通道**。
- 位点命名循账本现形 `{agg}:err.{camelCase}`，取 `idRequired`（同 `priceRequired` 词族）。
- BP-13 条款语义不变（终类型要求本体零动），只补半句守卫通道形状并修 §4.⑫ 教学片段。

## 不做（范围边界）
- 不补录存量未登记真实 key（order:err.notFound 等入表 = 登记账补欠独立小案，本案只登新增两行）。
- 不动 compile-proofs 两件套与 common-ddd 内部框架前置（理由见 Why 病面定档）。
- 不动 BP-12/EV 系任何条款原文。

## 待你裁决的 N 问
（无——设计、通道、命名皆现行法推论，项目主原语已含方向）

## §裁决记录（裁决唯一居所；裁一条落一条，落笔不改）
- 门 Specify 门 + 门 Plan 门 ✅（直令即门）2026-09-11 ｜项目主原语「你在id的实现中写了很多硬编码的中文哦，这个不对……这两个要改成我们的一套报错位点方案才对吧？」——观察属实（主控复核证成：BusinessException javadoc「禁止硬编码可读文案」+ BP-12 统一位点 + OrderItem 先例同形），指令含方向（改位点方案）与放行（要改…才对吧 = 明示施工），两门并释直令放行，先例 cull 案/identifier-shelf 案 ｜落点 → 全 AC ｜supersede：无（现行法 BP-12 对教学件的收编，非翻裁；2026-09-typed-identifier plan P-2「仅 null 检查」裁语保持——通道换形不触「查什么」只触「怎么报」）
