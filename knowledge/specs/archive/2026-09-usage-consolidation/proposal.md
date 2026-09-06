# Proposal：usage-consolidation（三期：统一用法全量归卷）

## Why

二期止步于「条款入法、形状留守」——同一形状存在两份渲染（法卷骨架 + docs 全套走查），正是本仓头号病「多拷贝漂移」的复发。用户裁定（2026-09-06）：**规则=具体如何用，全部放 specs（统一用法）；how-to 只是浅层指引（设计卡）**。样板 `batch-write` 卷/卡（98 行 / 23 行）已认可。

## What

12 篇 how-to 全量分裂：形状/选择表/状态表/验收 → 并入对应法卷（条款旁注化）；how-to 降为 ≤45 行设计卡（何时用/决策点/边界代价/指针，零形状代码）。error-handling → `modules/exception.md`。tutorials 不动（真实例操作非统一用法）。

## 连带登记（lead 自办，不属各手术组）

how-to/README 子索引口径、docs/README 象限行、rules/05 宽松件定义（零形状代码）、new-test skill 模板指针改向法卷、全量 BOM 归一。
