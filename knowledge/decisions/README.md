# knowledge/decisions/ —— 判例卷宗（态3：冻结 · 可清册存储）

**册况**：历元二 · 空册。本区一切案卷由新裁决逐案立起，无继承正文。

**一案一身（本区法律，append-only）**：ADR 正文永不回改。推翻旧决定 = 新立一案（取新编号），旧案仅 Status 行改为 `Superseded by ADR-NNNN`；本 README 的历元工作表登状态列，为主记账位。判例先行：拍板之前必先翻本区确认无撞案。机器执法：`knowledge/scripts/check-docs.ps1` C6 冻结臂（在位涂改即红，仅豁免 Status 行 1:1 对替）。

**只当收据，不当论证**：一案只登决策事件、当时思考快照、Confirmation 授权收据；论证的现行版（"今天为什么如此"）canonical 住 `docs/explanation/`，折叠时刻（⑦⁺）强制复写、篇脚回指快照——回指仅活册期有效，清册时整行随灭（行灭理存）。

**编号与改元**：`ADR-NNNN` 四位历元编号，历元内唯一递增，Void 号不复用；历元终了本区整册归零，新历元自 `0001` 重启（跨历元同号，法不救济）。

**清册制**：本区与两侧 `specs/archive/` 同为**可清册存储**——发布节点经清册 bill 彻底抹除（本体整袋删 + 本 README 账行同裁 + 活面具体标识符净空），前置自足判据：每案四栏因果（立因/取舍/被拒方案及拒因/生效边界）须先全文住进解读架，逐案再核表缺案不批；清册 bill 末步自焚、不立 ADR。法条全文 → [归属法](../specs/current/patterns/attribution-law.md) §1「卷宗清册制」注、§4「卷宗清册执行」行；制度原理 → [knowledge-system.md](../docs/explanation/knowledge-system.md)「清册制」节。
