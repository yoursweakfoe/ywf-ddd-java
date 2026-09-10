# 实现清单（按依赖序，[P]=可并行）
- [x] 1.（批准后第一步）拍板当刻新立 ADR「ArchUnit 规则集论证载体分工」+ `explanation/theory-map.md` 账本登记（归属法 §4「新设计决策」行；ADR Confirmation 必填）→ 全部 TR
- [x] 2. 新立 `docs/explanation/architecture-rules.md`（或按问 A 并入 testing.md）：怎么读一支规则（主语/宾语/段匹配/`allowEmptyShould` 空集语义）→ 块1–5 分工哲学 → 类型锚点教义与命名对偶 → 「保留段唯一语义」不变量（含迁移坐标对照表迁架）→ 双扫描入口与挂载策略（含空集教义例外逐条）→ 空转防线（R4 负证明三锁）→ 已知缺口账（每条含未决原因）→ 沿革（命名税三部曲）；`docs/README.md` 登记一处（登记法）→ TR-2/TR-3
- [x] 3. 按问 B 裁决瘦身 `DddArchitectureRules.java` javadoc：类头重排（保模块地图/空集警示/缺口清单/编号纪律一句+指针，沿革与论证迁出）；逐常量四栏化；顺手修复过期残引 `.agents/rules/0X` → 法卷名指针（规则逻辑与编号零变更）→ TR-1/TR-2/TR-3
- [x] 4. [P] 连带互指：`reference/api/common-test.md` §2 表首与 §5 加解读篇指针；`reference/api/common-contract.md`「见类头」对照指针改指新载体；`explanation/testing.md` 规则消费面节末加一行指针；`explanation/domain.md`「ArchUnit A2 白名单」过期修复 → R4；`glossary.md` DddArchitectureRules 行指针核对 → TR-2
- [x] 5. 测试：`mvn -B compile` 全绿（纯文本档·触 javadoc 不触行为，判例定档）+ `check-docs.ps1` 七闸全绿 + `ddd-review` 末步（触码案）→ 全部
- [x] 6. 归档折叠：TR-1/2/3 条款合入 `current/modules/test.md`「规则集治理」节，本目录整袋 `git mv` 入 `archive/`——**文档同步义务唯一时点**；⑦⁺ 论证复写已在步骤 2 前置完成，折叠时核对页脚回指 ADR 号
