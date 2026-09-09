# 实现清单（按依赖序，[P]=可并行）
> 基线 = `6dd588d`（历元一终态，已提交）。本案**没有**「折叠归档」一步——终末动作是自焚（R自焚条款）。

- [x] T0 前置：工作树清白基线提交「历元一终态」（项目主已完成 @6dd588d）
- [ ] T1 §4 清册行整行替换（delta MODIFIED 落地 current/）——法条先行生效，自焚才有法可依 → R卷宗清册执行
- [ ] T2 [P] 法卷净面：specs/current 各卷生效注 date-slug 转年份散文、卷内 `ADR-\d{4}` 指称转无编号散文（约 42 行，条款内容零改动，C4/C1 复核）→ R自焚条款（仪式⑤）
- [ ] T3 [P] docs 净面：五架 `ADR-\d{4}` 约 45 行化散文/页脚「决策快照账」整行删（含 theory-map 账行去号存理、knowledge/README §2.1 表两笔）；date-slug 残留清零 → R（仪式⑤）
- [ ] T4 [P] 镜像与工具面净空：sample `specs/current/order.md、product.md` 指称化散文、sample `specs/changes/README.md` 样例 slug 清零、`.agents/skills/new-bill/SKILL.md` 与 `.agents/README.md` 的 slug/幽灵样例引用清理 → R（仪式⑤⑥）
- [ ] T5 C6 自焚识别实现：臂 (a) 增 diff 证据路径（`git diff HEAD --diff-filter=D` 内 changes/*/proposal.md 且 HEAD 版含「清册」→ 视同在途凭据）+ SelfTest 追加臂 b 残留负例 + 脚本头注释去号 → R C6清册臂自焚识别
- [ ] T6 [P] doc-guards.md C6 节重写（两臂新行为 + 自焚识别说明，去 date-slug）→ R
- [ ] T7 整袋删除（仪式③）：`knowledge/decisions/ADR-*.md` × 37；框架 archive 10 案目录；镜像 archive 1 案目录
- [ ] T8 净账（仪式④）：decisions/README 回空壳章程态（裁历元新案表与逐案账行，留宪章+改元规则）；框架 archive/README 裁十案工作表与 ※ 注留章程；镜像 archive 补立空壳章程 README（防 git 丢目录）；specs/README、伞 README、AGENTS.md 波及行同步
- [ ] T9 三闸绿（bill 仍在途，磁盘证据路径）：check-docs 全量 + check-diagrams + `mvn -B compile` → R（仪式⑦）
- [ ] T10 提交「历元一→历元二：首期卷宗清册」
- [ ] T11 **自焚**：整删 `knowledge/specs/changes/2026-09-first-cullage/` → 复跑三闸必须仍全绿（本轮走 T5 的 diff 证据路径，即 R C6 场景「自焚当轮过闸」的实证）→ 追加提交终态
- [ ] 验收：`decisions/`=README 空壳、两侧 archive/=空壳、changes/=只剩 `_template/`、活面 ADR/date-slug 标识符 = 0、新案下一判例编号应为 ADR-0001（历元二）
