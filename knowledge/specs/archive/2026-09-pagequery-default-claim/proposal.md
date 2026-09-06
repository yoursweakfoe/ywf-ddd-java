# 分页「默认值」宣称与线上行为不符（只修宣称，不改行为）
- Slug: 2026-09-pagequery-default-claim ｜ 日期: 2026-09-06 ｜ 关联 ADR: 无（宣称真相化，不触决策）
## Why
`GetOrderPageQuery` javadoc/@Schema、`PageableQuery.DEFAULT_PAGE_SIZE` 注释、rules/03、common-contract、read-path、capabilities 共 7 处宣称「默认 1 / 默认 20」；但 record 组件为原始 int、无 @DefaultValue、DEFAULT_PAGE_SIZE 全仓零消费——缺参实绑 0 → @Min(1) 拒 → 400。「默认」在线上不存在（safe* 钳制是第二道防线，非缺省注入）。
## What changes
只修宣称文本使其与线上行为一致（用户裁决 2026-09-06：方案 b）。不改绑定行为、不删常量、不新增测试面。
## 不做
- 不引入 Integer/@DefaultValue 的默认值注入（那是方案 a，已否决）
- 不删除 DEFAULT_PAGE_SIZE（公开 API，删=破坏性变更，另案）
