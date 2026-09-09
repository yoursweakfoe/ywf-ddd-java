# 对 current/patterns/attribution-law.md 的增删改
> 只描述变化量，不描述全世界（delta 教义）。每条带 SHALL + 场景。本案为清册执行案：唯一法条变化 = §4 清册行补自焚闭环；C6 工具行为变化属 `scripts/` 执法件（行为以代码本体为准），按判例在此登记为 ADDED 供取证。

## ADDED Requirements

### Requirement: C6 清册臂自焚识别
C6 清册臂 (a) SHALL 在磁盘无在途清册 proposal 时仍放行合法自焚：本轮删除集中含 `knowledge/specs/changes/*/proposal.md`、该文件 HEAD 版内容匹配「清册」、删除完成后 `decisions/` 正文数 = 0 且两侧 `archive/` 案卷数 = 0、且空册自洽（臂 b 之 ADR-\d{4} 与 date-slug 净空检查）通过。（源：`knowledge/scripts/check-docs.ps1` C6 节，本变更实现后回填行号）

#### Scenario: 自焚当轮过闸
- GIVEN 三区整删已完成、本 bill 目录亦被删除、活面标识符已净空
- WHEN 跑 check-docs.ps1
- THEN C6 打印「清册臂：自焚 bill diff 证据认可」类 info 行并 PASS，七闸全绿
- AND 若活面残留任一具体标识符，臂 b 必红（自焚不豁免臂 b）

#### Scenario: 无据整删仍红
- GIVEN 两册有整文件被删、但 changes/ 无在途清册 proposal 且 diff 删除集中亦无含「清册」的 proposal
- WHEN 跑 check-docs.ps1
- THEN C6 红（维持现行为，不放松）

## MODIFIED Requirements

### Requirement: 卷宗清册执行（§4 表行整行替换）   <!-- 折叠时整行替换 current/patterns/attribution-law.md §4「卷宗清册执行」行 -->
七步仪式 SHALL 由独立清册 bill 承载：① 逐案再核表（四栏：立因/取舍/被拒方案及拒因/生效边界 → 去向 = 解读篇某节某段，或判「纯史件，随灭」；表随 bill 递交，缺案缺栏则批准门不得通过）② 补沉淀——再核发现的因果缺口，同 bill 内先复写进解读架（⑦⁺ 的追溯执行）③ 整袋删除（whole-directory/whole-file，禁拆件）④ 净账——README 账行同裁，两区 README 回到空壳章程态；镜像区 archive 若无 README 则补立空壳章程（git 不存空目录）⑤ 净面——全活面 `ADR-NNNN`/date-slug 具体标识符删除或转无编号散文（含页脚快照账行整行删、法卷生效注转年份散文），C1 验零幽灵 ⑥ 连带——计数宣称/glossary/docs README/theory-map 账本同步 ⑦ 七闸绿（C6 清册臂 + 空册自洽 + check-diagrams + `mvn compile`）。**清册 bill 自焚**：本案不折叠入 `archive/`、不立 ADR（执行非判例），末步整删自身案卷目录——三区零残留 + 自焚识别过闸是唯一合格终态；批准凭据 = 审议期 diff 中可见的再核表，阅后即焚为审计件正当归宿（法不考古）。此后新案编号自 `ADR-0001` 重启（改元）。

## REMOVED Requirements

（无）
