# 实现清单（按依赖序，[P]=可并行）
> 纯清单：参数与全文一律指 plan P-x / D-x；执行与取证记 implement。

- [ ] 0a. 批准门·Specify 档（项目主拍验收账 AC-1..12 与 Q1）（→ 全 AC 之前提）
- [ ] 0b. 批准门·Plan 档（项目主拍 P-1..8 裁量与 D-1..17 全文、新模板 §4 全文）（→ 施工闸门）
- [x] 1. 新立 `decisions/ADR-0003`（四段 SDD 采纳，supersede 两阶段批准判例；MADR 骨架 + Confirmation 必填）+ 同案 theory-map 开采纳账行（③⁺）（P-4/P-7 ｜ → AC-7/AC-11）
- [x] 2. `_template/` 换装：git rm 旧三件、落新四件全文（=plan §4，UTF-8 BOM 归一）（P-1/P-2/P-5 ｜ → AC-1..AC-6）
- [x] 3. 章程/法卷替换对逐执行 D-1..D-14（specs/README、AGENTS、attribution、changes/README、archive/README、glossary、knowledge/README 锚表、镜像×2、theory-map 随动、knowledge-system 随动+⑦⁺ 复写）（→ AC-8/AC-10/AC-11/AC-12）
- [x] 4. 流程件改写 D-15..D-16（new-bill 重写 + 五工序 skill 统一句 + ddd-review 勾项）（→ AC-8）
- [x] 5. 执法器 D-17（check-docs C6 臂 + doc-guards 措辞），装置与法同日落地（P-8 ｜ → AC-9）
- [x] [P] 6. d2 四处改写 D-12 → render-diagrams.ps1 重刷 → check-diagrams.ps1 过闸（→ AC-10）
- [x] 7. 验证：全仓复扫「三件套|spec-delta|proposal」，法案义命中零残留、泛称豁免清单（plan §3）逐处确认未动；check-docs 七闸全绿；`mvn -B install` 全量绿（意外触码负证明）（→ AC-8/AC-9）
- [x] 8. 折叠：plan §delta 即本案全部 D 表（无 current/ 法卷条文增量者跳过）；整目录入 archive/；implement §1–§5 清账后收官闸全勾；ddd-review 末步内置闸跑毕（→ 全 AC）
