# 实现清单（按依赖序，[P]=可并行）
> 纯清单，不论证。每条 = 一个可验收动作；条内不写设计参数，指 P-x；末尾 → AC-n；执行记录与取证记 implement.md。折叠（含一切 current/ 写入与 git mv）严格在 Plan 门批准后。

- [ ] [P] 解读篇成文：`docs/explanation/pattern-taxonomy.md` 按 P-8 节序（六源→四轴→否决→粒度→迁都记），篇脚回指本案 §裁决记录（P-8 ｜ → AC-2）
- [ ] [P] docs/README 登记解读篇；glossary 判新词（摊／摊卡／空穴登记／牌面／保号互指），入册者一句＋指针（P-8 ｜ → AC-2）
- [ ] [P] 迁都操作包编制：19 条 git mv 命令＋新卷落位表（P-7 ｜ → AC-3）
- [ ] [P] 改道清单编制：全仓 `patterns/<卷>.md` 入站引用逐文件枚举（实测基线 112 行起），生成逐处替换表含各卷身份行深度重锚（P-10 ｜ → AC-9）
- [ ] [P] 四本新卷全文终校（delta 即正文，逐条与收/出卷磁盘原文 diff——搬家六处零字改验证；摆架 meta-1～7 与 data-access-1～6 取证位回填准备）（P-1/P-6 ｜ → AC-5）
- [ ] [P] 十份摊头牌面按 P-9 模板实例化备稿（P-9 ｜ → AC-4）
- [ ] 空穴证据核验：§3 十三行引用的条款号/原句逐条对磁盘实文（SC-4/SC-8/GW-3/TC-5/WC-1…及 README、theory-map 原句），零幽灵（P-4 ｜ → AC-4）
- [ ] 审议期预跑：check-docs 七闸＋check-diagrams（案卷内新增件，current/ 未动）（→ AC-10 基线）
- [ ] ——Plan 门批准后—— 折叠①：执行迁都操作包（git mv 由人工跑，Agent 核对结果与复扫残留=0）（P-7 ｜ → AC-3, AC-9）
- [ ] 折叠②：四本新卷写入 current/；MODIFIED 全部按 delta 回写（写链/公约/读链/构建宪/禁令/两 README/归属法）；六处保号互指落位（→ AC-1, AC-5, AC-6）
- [ ] 折叠③：十份牌面落位；连带四件——theory-map 账本一行、.agents/README 计数、ddd-review 加「新卷归摊」行挂锚、伞 README 链改道收尾（→ AC-4, AC-7, AC-8, AC-9）
- [ ] 测试（以闸代测）：七闸＋lychee＋check-diagrams＋`mvn compile` 全绿——迁都终态（→ AC-10）
- [ ] 折叠④：implement §3 源回填清账、瘦身自查（门收据＋各裁齐）、整目录 `git mv` 入 archive/（→ 全部 AC）
- [ ] 收官闸：implement §5 全勾（→ AC-10）
