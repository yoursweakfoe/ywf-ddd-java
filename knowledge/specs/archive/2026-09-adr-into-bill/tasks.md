# 实现清单（按依赖序，[P]=可并行）
> 纯清单：条项 = 一个可验收动作；条内零设计参数（指 P-x）；尾巴指 AC-n；执行与取证记 implement。本案=纯文档＋执法器＋注释措辞案，全部落地集中折叠 commit（P-3 器法一刀），逐条勾验。

- [ ] 元法换骨：归属法 §1 判例行删、分区枚举删 decisions、三类件分家→两类件分家、清册注新形（D-1..D-4）（P-1/P-3 ｜ → AC-1/2/3/5）
- [ ] 归属法 §2 三行：设计论证行换号、架构决策行→「裁决与当时思考」行、过程事实行复质（D-5..D-7）（P-1/P-6 ｜ → AC-2/3/4）
- [ ] 归属法 §3/§4 四行：C4 辖域删枚举、新设计决策行、瘦身闸行、⑦⁺→⑨⁺ 行、清册执行行（D-8..D-12）（P-1/P-2/P-7 ｜ → AC-2/3/4/6/8）
- [ ] [P] 法区三章：specs/README 守则 4 与 archive 行、changes/README 三处、archive/README 两段（D-13..D-16）（→ AC-2/4/5/6/8）
- [ ] [P] 模板换装：_template/specify.md 全文替换（plan §4-b）、implement.md §5 换行（D-17/18）（P-1 ｜ → AC-2）
- [ ] [P] 路由与伞：根 AGENTS 三态表＋路由行、伞 README 五问/§1/§2.1 叙事/§3 树/§4（D-19/20）（P-4 ｜ → AC-1/4/8）
- [ ] [P] 外围路由：根 README、.agents/README、ywf-ddd-common/AGENTS L9（D-21..23）（→ AC-1/4）
- [ ] [P] skills：new-bill 六处＋反例增行、modify-common-module 三处（D-24/25）（P-1/P-2/P-6 ｜ → AC-2/3/4/5）
- [ ] 执法器换臂：check-docs.ps1 按 plan §1 P-7/D-27 逐处改＋头注释；doc-guards 辖域段/豁免列/C6 行重写（D-26/27）（P-7 ｜ → AC-6/9）
- [ ] 图换装：drive-relations.d2 整文件替换（plan §4-a）→ render-diagrams.ps1 重刷 → check-diagrams 绿（D-28）（P-4/P-5 ｜ → AC-7/9）
- [ ] [P] 锚表整替：knowledge/README §2.1 表按 plan §4-c 换（D-20 联动）（→ AC-8）
- [ ] [P] 字典随动：glossary 八处换/两词条删/新词条「§裁决记录」＋「案卷先行」改题（D-29）、docs/README 两处（D-32）（P-4 ｜ → AC-1/3/4/8）
- [ ] [P] 解读架随动：theory-map 五行＋索引节整替（D-30）、knowledge-system 整篇修订含本案 ⑨⁺ 义务（D-31）、architecture-rules/security/domain 就近（D-33）（P-2/P-4 ｜ → AC-3/4/9/10）
- [ ] [P] 净空腐点：api×8 假宣称行（D-34）、how-to×3（D-35）（→ AC-9）
- [ ] [P] 镜像区六件复质（D-36）（P-8 ｜ → AC-10）
- [ ] [P] 代码注释五文件复质（D-37）（→ AC-9）
- [ ] 区体删除：git rm -r knowledge/decisions（与执法器换臂同 commit）（D-38）（P-3 ｜ → AC-1）
- [ ] 复扫闭环：全仓 grep（plan §3 模式）账外命中逐个裁决入表或豁免；判据过（§3 豁免清单四条）（→ AC-9/10）
- [ ] 验证：check-docs 七闸＋SelfTest 绿；check-diagrams 绿；`mvn -B compile` 绿（负证明）（→ AC-6/7/9）
- [ ] 折叠：D 表合入 current/（含各章程）、整目录 git mv 入 archive/、implement §5 收官闸全勾（→ 全部 AC）
