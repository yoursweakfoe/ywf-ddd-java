# 对 capabilities/order.md 的增删改
## MODIFIED Requirements
### Requirement: OR-6（分页教义）
pageNum/pageSize **无缺省注入**：缺参绑 0，被 `@Min(1)` 拒 → **400**；上限 1000（@Max）。
`safePageNum()/safePageSize()`（钳 1..1000）是**仓储执行侧第二道防线**（护未走 @Valid 的直调），不是默认值机制。
`DEFAULT_PAGE_SIZE=20` 仅为框架提供的建议值，注入与否属消费方策略（本仓 sample 要求显式传参）。
