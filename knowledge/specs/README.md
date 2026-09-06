# knowledge/specs/ —— 行为契约区（法律）

守则（详法见 `.agents/rules/05` 归属法 + 四态分野条款）：

1. **代码违反本文 = 改代码**，或走 `changes/` 流程正式修法；**禁止为迁就代码偷改本文**（那是把法律当地图画）。
2. 改法唯一通道：`changes/<YYYY-MM-slug>/` 立三件套（proposal → spec-delta → tasks）→ 实现 → **归档折叠**进 `capabilities/`，那一刻是文档同步义务唯一发生时点。
3. 业务名在本区合法（`capabilities/order.md` 就该写 Order）——这是与教学区（docs/）的根本不同：契约描述生意，教学描述通识。
4. 每条 SHALL 括注取证源（实现文件:行 或 测试类名）；无证据的意图不写入，用 `<!-- 待 changes/ 补全 -->` 留缺口。
5. 行为断言与 `knowledge/decisions/` 判例互指不复述；机械背书（ArchUnit R##/测试类）写进句尾，与 check-docs 对账。

目录：
- `capabilities/{agg}.md`  现行本（按聚合分册）
- `changes/<slug>/`        修订工作区（proposal.md / spec-delta.md / tasks.md）
- `archive/`               已折叠卷宗（date-slug 命名，只进不改）
