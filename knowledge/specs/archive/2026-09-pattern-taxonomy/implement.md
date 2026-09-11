# 执行账本（施工期间唯一记录件。完成一条勾一条，不许攒批事后补勾）
## §1 执行账（与 tasks 条项 1:1 镜像）
| 任务项 | 状态 | 取证 |
|---|---|---|
| 解读篇成文 | ✅ | `knowledge/docs/explanation/pattern-taxonomy.md` 五节齐，篇脚含回指行 |
| docs/README 登记＋glossary 判断 | ✅ | docs/README 专论行 6→7 篇含 pattern-taxonomy 链；glossary 文档体系词汇表增三行（摆架／空穴登记／原号随身），各一句＋指针 |
| 迁都操作包编制 | ✅ | 脚本 `migrate-taxonomy.ps1` 执行日志：15 MOVED 全中 |
| 改道清单编制 | ✅ | 同脚本 GLOBAL 段：42 文件改道（12 skills、AGENTS、how-to 13、explanation 8、glossary、两区 README 等），残留复扫 0 |
| 四本新卷全文终校 | ✅ | 机器 diff（git HEAD 原文 vs 新卷条文）：WC-1/CC-3/CC-7/CC-9 OK；CC-8 尾句"。"缺失一处已修平复检；data-access-1～6 条文逐字=禁令原 §6 六行，两处行尾"→ 指针"移列至取证位（P-6 机械面，账此一句） |
| 十份牌面备稿 | ✅ | `patterns/*/README.md` 十份在位（meta/diplomacy/building-block/chain/collaboration/boundary/data-access/security/discipline/deploy-ops），空穴牌两份带穴行 |
| 空穴证据核验 | ✅ | §3 十三行引用条款号（SC-4/SC-8/GW-3/TC-5/WC-7/WC-8）与 README/theory-map 原句逐条磁盘实文核对通过（check-docs C1/C3 绿佐证） |
| 审议期预跑 | ✅ | 迁都后、写入前 check-docs 七闸全绿一次（C2 曾于案卷期抓构建宪槽位总数硬写一次，改指针后绿） |
| 折叠① 迁都执行＋残留复扫 | ✅ | 15 mv + 复扫 RESIDUE: 0；`git mv` 未用（PB-G1），文件系统级移动，commit 时 git 自动识别 rename |
| 折叠② 新卷入位＋MODIFIED 回写 | ✅ | 四新卷入位；墓碑五行（写链 WC-1、公约 CC-3/7/8/9）＋公约 §2.2 指针＋禁令 §6 对照身＋读链/构建宪收卷行＋specs/README 目录节＋伞 README 树两行＋归属法 §2 增行，全部按 delta 落位 |
| 折叠③ 牌面＋连带四件 | ✅ | theory-map 治理表增"模式法卷摆架"一行；.agents/README 计数 15→19＋摊指针；ddd-review SKILL.md「文档与契约」节增"新卷归摊"核对行；伞 README 专论计数 6→7 |
| 测试（以闸代测） | ✅ | `mvn -q compile` EXIT=0；check-diagrams OK（1 图源三方一致）；check-docs 见 AC-10 行 |
| 折叠④ 归档＋瘦身自查 | ✅ | 瘦身四项：两门收据齐、Q1～Q8 与裁 1～10 各有表决行与当时理由、supersede 三处均"无"、条文正文只在 plan、账在本件。整目录移入 `archive/2026-09-pattern-taxonomy/` |
| 收官闸 | ✅ | 见 §5 全勾；归档后终态复跑记录在 §5 末项注 |

## §2 验收映射（AC → 证据）
| AC | 证据 | 结论 |
|---|---|---|
| AC-1 | `current/patterns/meta/pattern-taxonomy.md`（meta-1～7、十摊卡、§3 十三穴、§4 四宗账、§5 生效登记） | ✅ |
| AC-2 | 摊卡十验证问句与裁 1 快照逐项对上；契约三分/性能三分/分层并总论/可观测并入均在卡文；被拒路线拒因住解读篇 §3 与裁决记录 | ✅ |
| AC-3 | 19 卷落十摊目录、patterns/ 根零裸卷（目录清点：3+0+3+4+2+2+1+1+3+0=19）；新铸号仅 meta-1～7 与 data-access-1～6，搬家五号随身 | ✅ |
| AC-4 | §3 十三行证据列全实文；牌面十份、diplomacy 与 deploy-ops 带穴行 | ✅ |
| AC-5 | 机器 diff 记录见 §1 终校行；data-access-2/4/5 指针列位调整已注记 | ✅ |
| AC-6 | specs/README 目录节 19 卷＋摊句；伞 README 树两行；归属法 §2 增行；.agents/README 计数 | ✅ |
| AC-7 | ddd-review SKILL.md「文档与契约」节"新卷归摊"行，锚指 pattern-taxonomy meta-1/2/5 | ✅ |
| AC-8 | theory-map 治理表"模式法卷摆架（十摊入路径）"行，全文→解读篇 | ✅ |
| AC-9 | 复扫 RESIDUE: 0；15 卷身份行深度 +1 重锚（脚本 DEPTH 段）；_template 四式复制目标与相对锚同步 | ✅ |
| AC-10 | 七闸全绿＋check-diagrams OK＋mvn EXIT=0（归档后终态复跑，C6 转绿记录见 §5） | ✅ |

## §3 源回填账
- meta-1～7：取证位即 ddd-review「新卷归摊」行、目录现实、本案裁决记录——全部在册真实 ✓
- 搬家五号＋data-access 六号：diff 记录在本账 §1 终校行 ✓
- 墓碑五行：写链 §1 WC-1 行、公约 §1 CC-3/7/8/9 行、公约 §2.2、禁令 §6 对照身，均在位 ✓
- 空穴 13 行证据：§1 核验行 ✓

## §4 漂移记录
- 2026-09-11：Plan 门审议期项目主指令扩面（物理建架、名册裁撤、空穴立牌、四宗搬家执行）＝裁 4～6，Specify 账随之追加复批（AC-9/AC-10 新增）。扩面发生在 Plan 门批准前，按审议稿修订处理，未触发已批范围回门。
- 2026-09-11（同日二次）：Plan 门问答会话裁 7～10——摊目录定名（diplomacy/、deploy-ops/、全单数）、编号总则改轨（新号=摊名式；搬家原号随身，LT/PS/AU/RC-10/BP 新行诸案作废）、Q7 一字不改与 Q8 一次改道照批。仍系批准前修订，无回门。
- 施工微账三条（批准范围内）：① 盘上先出现一版摆架卷（meta-6 含编号总则，优于案卷 delta），裁决采盘上版、delta 反向同步；② _template 四式随迁做锚深修正与复制目标改摊内（AC-9 一次改道承诺的自然对象）；③ CC-8 迁入文尾"。"一处经 diff 检出修平。均非范围扩张。
- 归档时点补账：终态七闸 C2 检得本账 §5 初稿"案名计数硬写"与收档圈号清单冲突一处；commit 前属封存未生效，依"追加不改既落笔"例，§5 该行改为无计数写法、此行即补账本身。

## §5 折叠收官闸
- [x] 全部 AC 有真实证据且 §3 零「待回填」
- [x] 七闸＋check-diagrams＋`mvn compile` 全绿（迁都终态）；残留复扫=0。归档前 C6（b）报活面案名若干处（数目见迁都脚本日志）属"案卷在途、两册空"的合法中间态，折叠后两册非空、指针合法，终态复跑转绿（记录：归档复跑 0 failing）
- [x] 瘦身自查毕：两门收据齐（Specify ✅＋追加复批注记、Plan ✅）、Q1～Q8 与裁 1～10 各有表决行与当时理由、supersede 三处落"无"（历元首案无旧册可撞）
- [x] 新卷与牌面 UTF-8 BOM 与现行区一致（主控统一归一）
- [x] 搬家六处零字改 diff 存证归档于本账 §1/§2
