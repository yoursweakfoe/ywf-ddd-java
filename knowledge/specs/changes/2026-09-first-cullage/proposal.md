# 首期卷宗清册：两册彻底归零，清册 bill 自焚（历元一终仪式）
- Slug: 2026-09-first-cullage ｜ 日期: 2026-09-09 ｜ 关联 ADR: 无（本 bill 是对已生效清册法的**执行**，执行不立判例；随带的自焚条款系 §4 清册行的仪式闭环补全，见 spec-delta）

## Why（为什么现在改）

卷宗清册制已生效并完成五例演练，十一案四栏因果经倒查审计全部沉淀活面（本轮补抄九缺口），基线 commit `6dd588d` 即历元一终态。发布节点要求 `decisions/` 与两侧 `specs/archive/` 空壳交付。残余漏洞：§4 清册行未写明清册 bill 自身的终末动作——照常规折叠，bill 本体会入住 archive/，导致 archive 永远差一格、清册仪式自我否定。本 bill 携带自焚条款补全闭环，并执行首期清册。

## What changes（行为级）

1. 归属法 §4「卷宗清册执行」行追加**自焚条款**：清册 bill 不折叠、不立 ADR，末步整删自身案卷目录，三区零残留是唯一合格终态；批准凭据 = 审议期 diff 中可见的本再核表。
2. C6 清册臂 (a) 补认自焚证据：磁盘无在途 proposal 时，若本轮删除的 diff 中含 `specs/changes/*/proposal.md` 且其 HEAD 版内容含「清册」、删后两册归零、空册自洽（臂 b）通过，则整删合法。
3. 执行七步仪式（归属法 §4）：整袋删除历元一两册全部本体（`decisions/` 37 件 ADR 正文 + 框架 archive 10 案 + 镜像 1 案），净账（各 README 回空壳章程态）、净面（活面全部具体 ADR 编号与 date-slug 标识符删或转无编号散文）、连带（解读篇/字典/skill/工具说明同步）、改元（历元二编号自 ADR-0001 重启）。

## 不做（范围边界）

- 不新立任何 ADR、不新增案卷；十一案再核判定「已足」，②补沉淀环节本轮零缺口。
- current/ 法卷除 §4 清册行自焚条款与生效注标识符转散文外，条款内容一字不动。
- 代码、测试、d2 图零改动（`mvn compile` 仅例行闸）。
- 不清 git 历史、不依赖 git 历史（法不考古：知识层零依赖；历史层不销毁，那是工程保险不是知识载体）。

## 逐案再核表（11 案；四栏 = 立因 / 取舍 / 被拒方案及拒因 / 生效边界 → 活面去向）

| # | 案卷 | 立因 | 取舍（选择了什么） | 被拒方案及拒因 | 生效边界 / 活面去向 | 判定 |
|---|------|------|------|------|------|------|
| 1 | `2026-10-rules-codification` | 规范散居 docs，无权威，漂移时轮不到文档说不 | 法卷成典：current/{modules,patterns} 编号条款 + 取证源 | 单一总卷（按读者处境分架难导航）；docs 就地加严（宽松语言溶剂掉执法力） | 23 卷现行 + C 闸执法 → 归属法 §1/§2、knowledge-system「宽严双份」节、theory-map 账行 | 准抹 |
| 2 | `2026-09-framework-codification` | api 文档里藏规范句，严松一身两职 | api §3 用法节入典为模块法卷，api 降纯描述 | api 保留严格件（地图区守则被动跟随=溶剂） | common-*.md 指针态 → theory-map「宽严双份三部曲」行（立因句本轮补抄） | 准抹（缺口已补） |
| 3 | `2026-09-howto-codification` | how-to 夹带形状代码，与法卷双权威漂移 | how-to 降设计卡：判据+指针，零形状代码 | 删架（读者仍需选择判据层）；形状保留（违一词一身） | 13/13 卡原理槽 → knowledge-system 宽严节、theory-map 行 | 准抹 |
| 4 | `2026-09-usage-consolidation` | 各法卷用法形状口径不一，样本互漂 | 统一用法形状归卷，全仓唯一样本 = sample | 硬限设计卡 ≤45 行——项目主 2026-09-09 裁「特殊文档照样会超出，不设限制」，**上限裁随案灭，不继立条款** | 法卷形状条款现行 → usage 指针态；上限无任何活面残留（合规：无继承受体） | 准抹（判灭项已核） |
| 5 | `2026-09-pagequery-default-claim` | 分页缺参静默成「第 1 页」，调用错误被伪装成成功 | 缺参原始绑定 0 → @Min(1) → 400（CC-9/RC-3/OR-6 已入卷） | @DefaultValue 静默兜底——「逼迫显式传参、拒不兜底，项目主代码品味」（拒因本轮补抄 theory-map 未采纳表；唯「当年还议过的第三形态」原文未存，属表决现场纯史，准随灭） | theory-map 未采纳行 + 三条法卷条款 | 准抹（缺口已补） |
| 6 | `2026-09-common-it-consolidation` | 框架 IT 零散挂各模块、基座混杂 | 并入 common-it 真 PG 双库试验场（DDD_FRAMEWORK_TEST / _VACANT） | 每次自动起容器（环境不可见、调试贵——前置摊开比藏在测试代码里诚实）；H2 兼容层（隔一层方言「量不到真东西」） | TC 全族 + testing-conformance → theory-map「H2 退役」行（本轮补账）、testing.md | 准抹（缺口已补） |
| 7 | `2026-09-pg-native-shape-and-real-testdb` | 库表形状是迁就 H2 天花板成形的，工具局限反向侵蚀业务设计 | 形状跟终库：uuidv7 / timestamptz / 全词蛇形 / jsonb / schema=聚合边界·领域词单数 / 保留字升格 UB | 桥接案（默认保留旧缩写 + 消费方配置映射——每人为框架缩写错误缴配置税，本轮补抄 infrastructure 桥接段）；引号/前缀逃逸（把冲突藏进每次书写，「库教业务改名」） | db-migration 变更集 + blueprint 形状条款 → infrastructure「为何 PG 原生形状」三理段（本轮补全）、theory-map 采纳行 | 准抹（缺口已补） |
| 8 | `2026-09-adr-argument-sedimentation` | ADR 冻结正文养着过时论证，读法被误导 | 论证现行版 canonical 住解读架（⑦⁺ 强制复写），卷宗只留事件+收据（ADR-0036） | 卷宗继续当论证家（冻结件跟不上法演进）；自动抽取工具（「哪段值得沉淀」无机器判据，暂缓裁决本轮补抄 knowledge-system）；行文法（目录名主词、旧称呼进括注）同案入 knowledge-system 节 | §4 ⑦⁺ 行 + knowledge-system 两节 + theory-map 沉淀账行 | 准抹（缺口已补） |
| 9 | `2026-09-archive-dossier-boundary` | 案卷与判例职责糊连，论证两头搬家 | 案卷=过程收据、判例=授权快照、解读架=论证现行版，三分家 | 案卷留论证（与 ⑦⁺ 复写重复，违一词一身） | 归属法 §1 分家注 + §4 瘦身闸行、knowledge-system「三份分身」表 | 准抹 |
| 10 | `2026-09-dossier-retirement` | 两册无限增殖，「判例先行」翻账成本吃掉回指收益；发布态要求空壳脚手架 | 卷宗清册制：到期整册彻底抹除、账行同裁、活面标识符净空、清册即改元、法不考古、自足判据前置 | v1「身可死而账不可失」（账行残留使空壳目标落空，项目主激进化为彻底抹除）；只清 archive 留 decisions（两册同病，分治无益）——双拒因全文在 knowledge-system「清册制」节 | §1 清册注 + §2 两行 + §4 清册行 + theory-map 账行 + glossary 三词条 + C6 两臂 + 五例演练取证（取证表随本 bill 同焚，结论已在法） | 准抹 |
| 11 | 镜像 `2026-09-order-product-pg-shape` | sample 库形状欠账，随框架新法落地 | order/product 表形全量转 PG 原生（含 sales_order 升格实例） | 与 #7 共担（桥接/引号逃逸同案同拒）；「Java 类名随库改 SalesOrder」——拒因=口语自然词优先，限定向库层，映射句落 glossary（本轮兑现当年承诺） | current/order.md、product.md 形状条款 + db-migration 变更集 | 准抹（缺口已补） |

**清点凭据（基线 `6dd588d`）**：`knowledge/decisions/` = 37 件正文（ADR-0001…0037，含 0004/0015/0017 三件 void）+ README；`knowledge/specs/archive/` = 10 案目录 + README 工作表；`sample-application/specs/archive/` = 1 案目录（无 README，git 不存空目录——仪式内含补立空壳章程 README 一步，防新 clone 缺树位）。活面标识符扫描基线：`ADR-\d{4}` 约 66 行（docs 五架 + specs 各卷 + sample 业务卷 + skill + 工具），date-slug 约 38 行（法卷生效注为主）。

## 待你裁决后即施工；施工末步 = 本目录自我删除（自焚），故本 bill 无折叠归档一步
