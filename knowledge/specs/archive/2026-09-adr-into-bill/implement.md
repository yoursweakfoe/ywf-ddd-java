# 执行账本（施工期唯一记录件；完成即勾、禁攒批补勾）
> 本案为纯文档＋执法器＋注释措辞案：全部落地集中于折叠 commit（plan P-3 器法一刀），逐条勾验、逐项取证。

## §1 执行账（tasks 条项 1:1）
| 任务项 | 状态 | 取证（命令/测试名/手动记录） |
|---|---|---|
| 归属法 §1（D-1..4） | ✅ | `knowledge/specs/current/patterns/attribution-law.md` §1：判例行删、枚举 `docs\|specs\|scripts`、两类件分家注、清册制注（逐字 = plan D-3/D-4 新文） |
| 归属法 §2（D-5..7） | ✅ | §2 三行换骨：「裁决与当时思考（决策事件）」canonical=`specify §裁决记录`、纯文本禁超链、四归一居 |
| 归属法 §3/§4（D-8..12） | ✅ | §3 辖域枚举删 decisions；§4 新设计决策/瘦身闸/⑨⁺/清册执行四行按 plan 新文；行内仪式步序 ①–⑦ 未误伤 |
| 法区三章（D-13..16） | ✅ | specs/README 守则4＋archive 行；changes/README 三处（四归一居句、案卷引用形）；archive/README 两段 |
| 模板换装（D-17/18） | ✅ | `_template/specify.md` = plan §4-b 全文（BOM 保留）；implement §5 换行 |
| 路由与伞（D-19/20） | ✅ | 根 AGENTS 三态表＋路由行；伞 README 五问/两分类/拆开段/①–⑩ 叙事/锚表整替/树/§4 九处 |
| 外围路由（D-21..23） | ✅ | 根 README 两处、.agents/README 居所行、common AGENTS L9 立案路线 |
| skills（D-24/25） | ✅ | new-bill 步骤1/2/3/4/6.2/6.4/6.5＋反例两行；modify-common-module 三处；ddd-review 复核零命中 |
| 执法器换臂（D-26/27） | ✅ | check-docs.ps1：$decDirs 删、dossierZones=两 archive、Status 豁免臂删、臂(b) 拆常设废号＋空册双支、info 行新形；doc-guards 辖域/豁免列/C6 行 |
| 图换装＋render＋check（D-28） | ✅ | drive-relations.d2=plan §4-a 全文；render `success…60.64ms`、check-diagrams `result: OK`（三方一致）；SVG 含 ①–⑩＋③⁺⑨⁺ 共 12 圈号、无 dec 容器 |
| 锚表整替（§4-c） | ✅ | 伞 README 锚表 ①/②③/④/⑤⑥/⑦/⑧/⑨/⑩/③⁺⑨⁺/沿革注 + current⇢行「新裁决中转」措辞 |
| 字典随动（D-29/32） | ✅ | glossary：三态、§裁决记录新词条、立法十步、案卷先行、⑨⁺、清册缩面、「清册即改元」删；docs/README 两处 |
| 解读架随动（D-30/31/33） | ✅ | theory-map 五处＋裁决索引节；knowledge-system 两份分身整篇（含本案 ⑨⁺ 义务：分身节现行论证＋页脚回指本案卷）；architecture-rules×2、domain×1、security 史语豁免保留 |
| 净空腐点（D-34/35） | ✅ | api×8 同形行全替（假 §migration 宣称根治）；how-to×2（testing 喻义豁免保留） |
| 镜像区（D-36） | ✅ | order.md×5、product.md×2 复质；specs/README、archive/README 复核零命中 |
| 代码注释（D-37） | ✅ | DddArchitectureRules×2、AuditProperties×2、RestAdapter×1、TxProofTest×1、sample pom×1；`mvn -B compile` EXIT=0 负证明 |
| 区体删除（D-38） | ✅ | `git rm -r knowledge/decisions`（4 文件 staged D）；与换臂同批（器法一刀） |
| 复扫闭环 | ✅ | 三式 grep：①活面 `ADR-\d{4}` 初扫 1 红（test.md L60 ADR-0001→案卷指针）修后零；②decisions 路径活面零；③制度义残留仅沿革注/旧称括注/史叙事三处有意保留。账外补账 4 处：test.md、cloud-integration L42（⑦⁺→⑨⁺）、sample AGENTS 契约先行行（旧三件套形→四件套，前案漏随、镜像援引行辖域内）、archive/README 册况行（「空袋」死宣称＋历元二账目语——连带义务行同 PR 修齐）——均 §3 判据内消化，未出范围 |
| 验证三闸 | ✅ | check-docs 7/7 PASS＋SelfTest 6/6（ST5 复用）＋check-diagrams OK＋mvn -B compile EXIT=0；C6 info 新形 `adr-residue=0; slug-residue=0` |
| 折叠收官 | ✅ | 见 §5；本案卷 git mv 入 archive/，末次七闸复跑绿 |

## §2 验收映射（AC → 证据）
| AC | 证据（文件:行 / 测试名 / 构建闸结果） | 结论 |
|---|---|---|
| AC-1 区废零悬空 | git rm 4 文件 staged；复扫② 路径零命中；根 AGENTS/伞 README/树/根 README 三态化 | ✅ |
| AC-2 裁决唯一居所 | `_template/specify.md` §裁决记录节（plan §4-b 逐字）；归属法 §1/§2/§4 三处钉节名；changes/README 四归一居句；本案自身两门收据+四裁定落此节（自举实证） | ✅ |
| AC-3 孤案落点 | 归属法 §4「新设计决策」行双载体；theory-map L140 复质「修码就法无案裁决」；编号教义于 §1 清册注/锚表沿革退役 | ✅ |
| AC-4 案卷先行路由 | 根 AGENTS 路由行、new-bill 步骤 2 新钉、伞锚表 ②③ 行、glossary「案卷先行」词条 | ✅ |
| AC-5 supersede 落卷 | 本案 specify §裁决记录 Q4 条点名前案不做条宣告；D-6 行 canonical 引用形 `→ 案卷 <date-slug> §裁决记录` | ✅ |
| AC-6 执法器随法 | check-docs 7/7 PASS、SelfTest 6/6、C6 info `adr-residue=0; slug-residue=0` | ✅ |
| AC-7 图重构 | render 60.64ms success；SVG 文本层验：裁决×11、圈号×12、无 dec；check-diagrams OK | ✅ |
| AC-8 锚表同步 | 伞 README 锚表 ①–⑩/③⁺/⑨⁺/沿革注十一行，图-表一对一 | ✅ |
| AC-9 标识符净空 | 复扫① 修后活面零命中（api×8 假宣称根除、代码裸号×7 复质、d2 内 ADR-0036 陈注随整替消解）；`mvn -B compile` EXIT=0 | ✅ |
| AC-10 波及闭合 | 复扫③ 三式审计＋账外补账 3 处入 §1；豁免按 plan §3 四判据（testing 喻义/security 史语/knowledge-system 旧称括注等保留） | ✅ |
| AC-11 历史不回改 | 冻结臂 `freeze-arm-modified=0`：specs/archive 两袋零改动（git status 可证）；旧 ADR 未回填旧案卷 | ✅ |

## §3 源回填账（plan §delta 每条 SHALL：「待回填」→ 真实取证位）
纯宣称文本案：D-1..D-38 全数落地位 = §1 各行指名文件（折叠 diff 逐处可对）；取证闸输出 = §2 AC-6/7/9 三行。零「待回填」。

## §4 漂移记录（范围扩/新发现：回批准门之日期 + 门次；无漂移=本行写「无」）
无范围扩。复扫账外命中 4 处（test.md/cloud-integration/sample AGENTS 契约先行行/archive README 册况行）属 plan §3 预定义补账与连带义务机制内消化，未出 AC-9/AC-10 口径，不回门。

## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」
- [x] 七闸全绿＋SelfTest（触码注释案 `mvn -B compile` EXIT=0）；触图案 check-diagrams 绿
- [x] 瘦身自查毕（§裁决记录条目全：两门收据齐、各 Q-x 裁定有表决行与当时理由、supersede 宣告点名旧案）
- [x] 本案 ⑨⁺ 义务毕：knowledge-system.md 现行论证复写含本案裁决理由、页脚回指 `→ 案卷 2026-09-adr-into-bill §裁决记录（决策快照）`
- [x] 新建 md 与现行编码一致（BOM 归一主控统一执行：本案卷四件补 BOM；_template 两件原生保留；.d2 无 BOM）
