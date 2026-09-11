# 用法规范法卷：安全链（框架法 · 严格件）

> **身份**：本卷是身份与权限跨层流动的横切法正身。唯一在册条文即 CC-8——自编码公约卷搬家而来、**原号随身**（案卷 2026-09-pattern-taxonomy 搬家账 4），条文字符零改动；将来本摊新铸条款按摊名式编号（security-n，摆架卷 meta-6）。整条认证请求链与数据权限尚未成文——洞登记于摆架卷 §3-12 兼本卷 §3 ⛔ 行。代码违反本卷就修代码；修订本卷走 `../../../changes/` 立案。docs 同题篇 `../../../../docs/explanation/security.md`（原理与权衡，零形状）。
> **机器对账**：C3 查符号、C4 查教学中立扫本卷。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| CC-8 | 安全上下文按层取用：`SecurityUtil` 只允许在 Application 和 Adapter 层使用；Controller 优先用 `@AuthenticationPrincipal` 注入已验签 JWT；**Domain 层禁止**感知认证上下文；角色判断用 `@PreAuthorize`，角色 claim 的名称经配置键指定。（原号随身，自公约卷整条迁来，一字未改） | `common-security` javadoc/配置；[security 卷](../../modules/security.md) | 评审项 |

## §2 规范形状（统一用法唯一样本）

进线→验签→上下文→逐层取用的链图：待认证请求链条款成文时一并立法（§3 ⛔ 行），无证据不造形。

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| CC-8（原号随身） | ✅ 生效 | [../discipline/coding-conventions.md](../discipline/coding-conventions.md) §1 墓碑互指行在册 |
| 认证请求链整条走法 | ⛔ 未落地 | 候案（摆架卷 §3-12） |
| 数据权限 | ⛔ 未落地 | 候案（摆架卷 §3-12） |
