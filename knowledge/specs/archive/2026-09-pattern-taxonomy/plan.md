# 施工方案与修卷 delta（v2 · 按裁 4～6 重铸：物理建架＋四宗搬家）
> 本节回答「怎么做」与「改哪些法」，是这两类内容的唯一居所。P-x 是技术决策编号；tasks 条目只引用编号。AC-n 对应 specify 的验收账。

## §1 技术形状
- P-1 四本新卷：`meta/pattern-taxonomy.md`（摆架，新铸编号 meta-1～7）、`meta/layering.md`（分层，**正文沿用原号 WC-1、CC-3**，本卷不自铸新号）、`data-access/persistence-sql.md`（持久化语句，禁令 §6 六条本无编号、首次铸号＝data-access-1～6）、`security/security-chain.md`（安全链，**正文沿用原号 CC-8**；将来本摊新铸号＝security-n）。摆架卷走元法式骨架节序自配；三本搬家卷走通式骨架（§1 条款／§2 形状指针／§3 生效登记）。编号总则（裁 9）：两字母旧前缀存量永久有效，此后全仓不再新增任何缩写前缀，新铸编号一律＝摊目录名-序号。
- P-2 摊卡固定五段：验证问句／管什么（法条正文，meta-3）／界线（法条正文）／摊路径／空穴（§3 行号指针，无则写"无"）。**不设"现行卷"行**（裁 4：路径即名册）。
- P-3 摆架卷新铸编号 meta-1～7（见 delta）；全仓编号总则见 P-1（裁 9：新号=摊目录名-序号，旧缩写存量永久有效、不再新增）。背书一律评审项。
- P-4 空穴证据三形态：现行法条款号／根 README·theory-map 原句定位／本案 §裁决记录。
- P-5 ddd-review 加「新卷归摊」核对行，折叠后挂锚（P-6 先例：法生效后 skill 再挂指）。
- P-6 搬家机制：**原号随身**（裁 9 派生）——条款编号是历史身份，搬家只挪正文、不换号：WC-1、CC-3、CC-7、CC-8、CC-9 的正身迁入收卷后仍以原号立法，全仓"见 WC-1"式引用零断链；出卷原位置行内容替换为一行墓碑指针（判例句式=读用例链 RC-3"承诺立在别卷、本条只互指"）。正身条文**字符内容一字不改**（裁 10/Q7），条文内链接按新位置重锚（属搬家的机械面，不算改字）。唯禁令 §6 六条历史上无编号，首次铸号按新总则用摊名式：data-access-1～6。
- P-7 迁都清单（git mv，人工执行，PB-G1）：
  ```
  meta/            ← attribution-law.md（mv）＋pattern-taxonomy.md、layering.md（新）
  diplomacy/       ← 有牌无卷（空穴摊）
  building-block/ ← aggregate-blueprint.md、application-objects.md、domain-policy.md
  chain/          ← write-chain.md、read-chain.md、batch-write.md、scheduler.md
  collaboration/   ← cross-aggregate.md、optimistic-lock.md
  boundary/      ← distributed-tx.md、external-gateway.md
  data-access/     ← persistence-sql.md（新）
  security/        ← security-chain.md（新）
  discipline/      ← coding-conventions.md、prohibitions.md、testing-conformance.md
  deploy-ops/      ← 有牌无卷（空穴摊）
  ```
  合计 19 卷，patterns/ 根仅剩十个摊目录＋`_template/`。改道复扫判据：正则 `patterns/(attribution-law|write-chain|read-chain|batch-write|cross-aggregate|external-gateway|scheduler|optimistic-lock|distributed-tx|aggregate-blueprint|application-objects|domain-policy|coding-conventions|prohibitions|testing-conformance)\.md` 全仓活面残留 = 0（案卷 changes/archive 豁免区不计）。
- P-8 ⑨⁺ 解读篇 `docs/explanation/pattern-taxonomy.md`《模式法卷为什么是十摊》：六源证据链→四候选轴对比→层轴任务轴否决→粒度裁决（协作拆内外、契约蒸发、性能撤摊、可观测并入）→本轮迁都记（物理架、保号搬家、牌面）。正文折叠时成文，篇脚回指本案 §裁决记录。
- P-9 摊头牌面模板（十份，每摊 `README.md`，固定三行）：摊名与验证问句一句／"摊卡全文、界线与空穴登记 → ../meta/pattern-taxonomy.md §2.x"／"本文件是牌面非法：零条款零形状"。空穴摊加第四行："本摊当前空穴（册内无卷），牌面按 meta-4 预建，洞见摆架卷 §3"。
- P-10 重锚规则：15 卷 mv 后各卷身份行与正文内相对链接深度 +1（`../../changes/`→`../../../changes/`、docs 指针同理）；跨摊互指改 `../<他摊>/<卷>.md`。全仓入站 112+ 处（how-to 卡 governing 列、12 skills 锚、modules 卷互链、glossary、explanation 诸篇、AGENTS.md 路由节、.agents/README、伞 README 链）同 PR 改道。

## §2 修卷 delta（折叠时全部修订以本节为准写入）

### ADDED
#### Requirement: 摆架法卷（meta/pattern-taxonomy.md）   ← AC-1, AC-2, AC-4, AC-9, AC-10
成卷全文（折叠时原样入位；摊卡正文即 v4 裁决全文＋本轮新裁）：

---
# 用法规范法卷：摆架（知识系统元法 · 严格件）

> **身份**：本卷是模式法卷摆架的宪法——十摊的地图、每摊管什么、和邻摊的界线、哪里有洞，唯住本卷；**哪卷属哪摊以文件路径为唯一登记**（meta-2），本卷不抄卷列表。归架归错了按本卷摊卡裁决改放。本卷也是法：修订（含每张摊卡的"管什么"与"界线"——那是法条正文，不是讲解）只能走 `../../../changes/` 立案。"为什么这样摆"的现行论证住 `../../../../docs/explanation/pattern-taxonomy.md`：摊归属之问本卷赢，动机之问以该篇为据。
> **机器对账**：C3 查符号、C4 查教学中立扫本卷。路径与摊卡的一致性无自动闸——ddd-review「新卷归摊」核对项保障（meta-7）；探测器属装置演化须另案。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| meta-1 | 新增模式法卷 SHALL 先以 §2 摊卡验证问句裁决归摊、再落入对应摊目录；任何摊都裁不进 = 摊地图有缺口，先立案改摊地图再落卷，禁止塞进"最近的摊"凑数 | 案卷 2026-09-pattern-taxonomy §裁决记录 裁 1～10 | 评审项（ddd-review） |
| meta-2 | 卷籍由目录路径唯一承载：卷在哪个摊目录下即属哪摊，一摊一路径一真身；**摊卡与本卷不载卷列表**，任何载体复写"哪卷在哪摊"的名册=违反归属法 §2 单一住所。换摊=`git mv`=立案（附全仓指针改道） | 本卷 §2 摊卡"摊路径"行 ＋ `current/patterns/` 目录现实 | 评审项 |
| meta-3 | 摊卡的"管什么"与"界线"两段是法条正文：承担 meta-1 的裁决理解，改写、删减、移动二者 = 修本卷 = 立案。"为什么这样裁"住解读篇，本卷不载论证 | 案卷 2026-09-pattern-taxonomy 裁 3、Q2 | 评审项 |
| meta-4 | 空穴摊 SHALL 预建摊目录与摊头牌面（README，形状=一句问＋摆架卷指针，零条款零形状）；洞本身登记 §3，每行 SHALL 带证据指针（现行法条款号／根 README 或 theory-map 原句／案卷裁决记录，三形态之一），零证据穴行不得登记。摊的第一卷落入目录时：该摊牌面删"空穴"行、§3 对应穴行注销——同案齐做 | §3；牌面十份在位 | 评审项 |
| meta-5 | 新卷 SHALL 落 `current/patterns/<摊目录>/<卷>.md`；摊目录名的权威=§2 摊卡"摊路径"行；patterns/ 根目录裸卷（除 `_template/` 与摊目录）=违规。各卷身份行 SHALL NOT 携带摊字段（路径已登记，复写即二住） | 目录现实（meta-2 镜像条款） | 评审项 |
| meta-6 | 新铸条款编号 SHALL = 摊目录名-序号（本卷 meta-n、持久化语句卷 data-access-n，皆其例）；两字母旧前缀为历史存量，永久有效但不再新增。搬家条款 SHALL 原号随身（§4 账）：正文迁走、编号不换、出卷留墓碑互指一行；条文字符内容零改动，润色另案 | 案卷 2026-09-pattern-taxonomy §裁决记录 裁 9 | 评审项 |
| meta-7 | 本卷保障=评审项与 ddd-review 检查单；零自动闸新增（案卷 2026-09-pattern-taxonomy 约束节；装置演化另案） | ddd-review 检查单「新卷归摊」行 | 评审项 |

## §2 摊名册（十摊摊卡；段式见 P-2，路径即户口）

### 2.1 摊「总论」（元法·基础）
- 验证问句：法本身怎么立、每类事实放哪；这栋楼的分层地基怎么搭。
- 管什么：两类文本。管法的法——每类事实的权威住所、文档同步时钟、改法程序（归属法、摆架法）。楼的地基——四层依赖方向（adapter → application → domain ← infrastructure）、层豁免白名单（stereotype 等）、结构与命名的目录映射律（分层法卷；其条款由写链 WC-1、公约 CC-3 搬家而来）。
- 界线：总论裁"文档与法自身的行为"和"层与层的关系"；"代码怎么写"的普适规矩归纪律摊（2.9）。
- 摊路径：`patterns/meta/`
- 空穴：无

### 2.2 摊「领域划界与领域关系」（外交摊）
- 验证问句：业务切成几个服务；切开之后谁求谁、谁迁就谁；发出去的文书事后能不能改。
- 管什么：三问。划界=切刀——订单、库存、支付切几个服务，判据"切开后少说话、挨着的一起改"。邦交=服务间关系——契约 jar 是两家共用的词典（发布语言），common-* 是共享内核，谁供谁、谁顺从谁要登记在册。文书信用=契约发布后的演化规矩——加字段、删字段、改枚举值算不算毁约、消费方怎么被通知。演化规矩现行零条款，是本案点名的最大裸奔面（§3-1）。
- 界线：本摊裁政策（关系怎么定、毁不毁约）；关系定了之后的工程账（回滚、翻译、护栏）归边界摊（2.6）；文书格式条款随用处归家（链路、构件）。
- 摊路径：`patterns/diplomacy/`
- 空穴：§3-1、§3-2

### 2.3 摊「构件」
- 验证问句：一个东西搭出来长什么样、由哪几块拼成。
- 管什么：对象，不是流程。一个聚合由哪些文件拼装、每件什么形状（槽位清册计数唯住聚合构建宪 §1，本卷不复写）；应用层四种中间对象 DTO、ViewDTO、Params、Record 的准入门槛与形状；可插拔业务规则的策略件落位。契约模块的内容白名单（构成律，由公约 CC-7 搬家而来）也在这里——"契约模块准放什么"是静态构成问题。将来"值对象怎么建模、强类型 ID 用不用"候补本摊。
- 界线：带"一个请求在走"的归链路；"存进库的样子"归数据访问；"名字怎么起"归纪律。
- 摊路径：`patterns/building-block/`
- 空穴：无

### 2.4 摊「链路」
- 验证问句：一个请求从哪个门进、经过哪些站、怎么落库、出门时什么形状。
- 管什么：请求的旅程。四卷按旅程分型：单写（写用例链卷）、单读（读用例链卷）、批量的写（批量写卷——卷内自认"批内单条与单写同构"）、时间催的写（定时任务卷——卷内自认"时间只是又一种触发源"）。入口是链路第一环，不单立摊。契约的输入/输出/分页格式条款是旅程首尾的安检法（写链首尾条款原地；分页律 CC-9 原号随身迁入读链卷）。
- 界线：旅程只有一个请求在走=链路；两个聚合或两个写者之间的事=协作（2.5）；对手在网络上=边界（2.6）。
- 摊路径：`patterns/chain/`
- 空穴：§3-3、§3-4

### 2.5 摊「协作」（自家院内的对手）
- 验证问句：同一服务、同一进程之内，几个聚合、几个写者之间怎么对上。
- 管什么：院内对手方的协调账。对手=另一个聚合：一个事务罩住多聚合，谁编排、补偿怎么原子、批量查询怎么防 N+1（跨聚合卷）。对手=另一个写者：两人同时改同一行，冲突按类型识别、重试按退避表、静默写丢失必须吵醒人（乐观锁卷）。分布式锁（多实例调度防重入）与幂等键规范同属院墙之内，尚未立法。
- 界线：对手出了进程（网络上）=边界摊；与对手无关的单请求旅程=链路摊。
- 摊路径：`patterns/collaboration/`
- 空穴：§3-5、§3-6

### 2.6 摊「边界」（自家院外的对手）
- 验证问句：出门找别的服务、别的公司，账怎么对、方言谁翻译、对方翻脸怎么防。
- 管什么：三节。跨服务账=两本账的原子性——本地事务优先的边界裁决、全局事务透传、一起回滚（分布式事务卷），及未来的 Saga 冲正流程。对外打交道=门口翻译官——Portal 定义在域内、Gateway 防腐在域外、外部方言不出门（外部集成卷）。防护=对外的超时、重试、熔断预算与南北向分工（哪些流量归 Higress、哪些归应用自己）——原"性能摊"的防护半边收此处。
- 界线：关系是否成立、毁约算不算=外交摊（2.2）；本摊只裁已定关系之内的工程账。
- 摊路径：`patterns/boundary/`
- 空穴：§3-7、§3-8、§3-9

### 2.7 摊「数据访问」
- 验证问句：数据怎么存、怎么取；表结构怎么演化。
- 管什么：不是 Java 里的形状（构件摊），是语句和库的形状。SQL 文本怎么写——表名、乐观锁版本条件、逻辑删除过滤逐条可见、禁运行时拦截器（持久化语句法卷；其条款由禁令卷 §6 整节搬家而来，本节从此有肉）；缓存怎么摆（读缺回填、失效策略）；表结构演化与 db-migration 的执行承诺。
- 界线：聚合字段映射到列的构件级规定在构建宪槽位里（BP-X 系）；语句文本级与库级演化归本摊。
- 摊路径：`patterns/data-access/`
- 空穴：§3-10、§3-11

### 2.8 摊「安全」
- 验证问句：谁在敲门；验过身份的人走到哪一层还能被想起。
- 管什么：身份与权限的跨层流动——网关进线、JWT 验签、安全上下文逐层取用、域层禁入认证概念、数据权限落位。首卷《安全链》已由公约 CC-8 搬家当家；认证请求链整条走法尚未成文（§3-12 缩窄留册）。
- 界线：common-security 模块自身的 API 用法住 modules/security.md（模块法卷管模块）；"整条请求链怎么带着身份走"的横切法归本摊。
- 摊路径：`patterns/security/`
- 空穴：§3-12（缩窄后）

### 2.9 摊「代码纪律」
- 验证问句：不管写什么，只要动键盘就得守。
- 管什么：与摊性无关的普适规矩。命名后缀、Lombok 分工、时间类型、异常与文案姿势（编码公约卷——其分层、构成、分页四类条款已搬家，见 §4 账）；一行一条的禁止事项总对照表（禁令全表卷——表身住这摊，§6 节正身已迁数据访问，表行改指）；测试代码的四分型、命名、位置镜像（测试符合性卷——测试也是代码，它怎么写是纪律）。
- 界线：纪律裁"写代码的行为"；总论裁"写文档与立法的自身行为"。
- 摊路径：`patterns/discipline/`
- 空穴：无

### 2.10 摊「部署与运维」
- 验证问句：跑起来之后——怎么打包、给多少资源、怎么体面地死、怎么被看见。
- 管什么：运行时形状的法：镜像与容器规范、资源限额、优雅停机（虚拟线程下的在途请求处理）、健康探针、日志格式与链路追踪怎么贯穿服务（可观测是本摊一节，不是独立摊），以及配置注入的规矩。现时这些检查面只存在于 ops-review 技能的 SOP 里——工作台有工序、法律无条款。
- 界线：common-observability 模块 API 用法住 modules/observability.md；"整个服务被看见的形状"横切法归本摊。
- 摊路径：`patterns/deploy-ops/`
- 空穴：§3-13

## §3 空穴登记（每行带证据；填法见 meta-4）

| # | 空穴 | 落摊 | 证据 |
|---|------|------|------|
| 3-1 | 契约版本演化法（加/删字段、枚举改值算不算毁约、消费方通知义务） | 外交 2.2 | 根 README 架构图原句「contract jar - 消费方唯一依赖」；全仓无一条事后演化条款（注：本宗系造新法非搬家，不入本案） |
| 3-2 | 划界判据与邦交策略登记（一服务一上下文升格成文） | 外交 2.2 | theory-map 在册行「每个微服务 = 一个限界上下文」「contract 模块定义上下文对外边界」——账本有、法卷无 |
| 3-3 | MQ 消费者入口 | 链路 2.4 | 根 README 架构图 adapter 层原句「Facade / Consumer / Scheduler 纯透传」——图上三入口、法上两卷 |
| 3-4 | 缓存读路径 | 链路 2.4 | 本案 §裁决记录 裁 1 点名候补（现行法暂无引用，按 meta-4 裁决落点形态登记） |
| 3-5 | 分布式锁 | 协作 2.5 | 定时任务卷 SC-4 原句「多实例部署的调度必须自带幂等：分布式锁防并发重入」——法点名引用部件，部件无法 |
| 3-6 | 幂等键规范 | 协作 2.5 | 定时任务卷 SC-8、外部集成卷 GW-3 均引用"幂等"语义，无条款定义谁生成谁校验 |
| 3-7 | Saga／冲正流程 | 边界 2.6 | theory-map 采纳行「无主长流程引入独立 Saga 服务」——决定在册、法卷缺席 |
| 3-8 | 南北向分工（网关与应用各管哪段路由/鉴权/限流） | 边界 2.6 | 根 README 技术栈原句「对外经 Higress 网关」；分工条款无 |
| 3-9 | 对外防护三件套（超时/重试/熔断横切规范） | 边界 2.6 | 外部集成卷 GW-3 只管 Gateway 内部；common-cloud 卷管依赖装配——跨切使用法两端都无 |
| 3-10 | 缓存规范（回填策略、失效语义） | 数据访问 2.7 | 本案 §裁决记录 裁 1 点名候补（与 3-4 同宗：链路侧讲走法、本侧讲规矩） |
| 3-11 | 表结构演化承诺（db-migration 与服务 schema 的契约） | 数据访问 2.7 | 测试符合性卷 TC-5 原句「库形状权威 = db-migration」——承认权威、演化规矩无法 |
| 3-12 | 认证请求链（进线→验签→上下文→域层禁入整条走法）与数据权限 | 安全 2.8 | 搬家后缩窄：取用律已有正身（CC-8，原号随身在安全链卷），整链走法仍无——安全链卷 §3 生效登记 ⛔ 行在册 |
| 3-13 | 部署与运维全摊（镜像、资源、优雅停机、探针、日志/链路格式、配置注入） | 部署与运维 2.10 | ops-review 技能全检查面已成 SOP——工作台有料、法律无卷 |

## §4 搬家账（本案执行，meta-6 登记）

| 宗 | 从 | 到 | 状态 |
|---|---|---|---|
| 1 | 写用例链 WC-1（依赖方向与层豁免）、编码公约 CC-3＋§2.2 结构映射表 | 分层法卷——**原号随身**：正文在本卷仍以 WC-1、CC-3 为号（裁 9） | ✅ 本案折叠执行 |
| 2 | 编码公约 CC-7（契约模块白名单）→ 聚合构建宪（原号 CC-7 随身）；CC-9（分页契约）→ 读用例链（原号 CC-9 随身）；"事后能否改"→ **不搬**（无肉可搬，留 §3-1 候新法） | 构件摊／链路摊 | ✅ 两行迁毕，第三家留空穴 |
| 3 | 禁令卷 §6（SQL 铁律六条，本无编号） | 持久化语句法卷，首次铸号 data-access-1～data-access-6（摊名式编号，裁 9）；禁令 §6 节改对照表身 | ✅ 本案折叠执行 |
| 4 | 编码公约 CC-8（安全上下文四层取用） | 安全链法卷（原号 CC-8 随身）；安全摊首卷 | ✅ 本案折叠执行 |

## §5 生效登记

| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| meta-1 至 meta-7 | ✅ 生效（保障=评审项，非机器闸） | ddd-review 检查单「新卷归摊」行 |
| §2 十摊架＋§4 四宗搬家 | ✅ 生效（19 卷全落摊目录，patterns/ 根零裸卷） | 目录现实 |
| §3 空穴 | ⛔ 未填 | 逐穴候案，填法见 meta-4 |

---

#### Requirement: 分层法卷（meta/layering.md）   ← AC-5
---
# 用法规范法卷：分层（框架法 · 严格件）

> **身份**：本卷是四层地基条款的唯一权威：依赖方向、层豁免、目录结构映射律。两条正文由写用例链 WC-1 与编码公约 CC-3（含其 §2.2 表）搬家而来（案卷 2026-09-pattern-taxonomy §4-1），**原号随身**：本卷条款仍以 WC-1、CC-3 为号，永不重铸；条文字符零改动。修订走 `../../../changes/` 立案。docs 侧无同题卡（地基无选型问题，论证在 `../../../../docs/explanation/architecture-rules.md` 与分层设计五篇）。
> **机器对账**：C3/C4 扫本卷。

## §1 条款
| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| WC-1 | 依赖方向单向：`adapter → application → domain ← infrastructure`。domain 层零框架运行时依赖；stereotype 豁免，只允许纯 Java + common-ddd（原号随身；全文迁来，一字未改） | ArchUnit 双端守护，规则集在 common-test | R1/R2 系 |
| CC-3 | 结构映射五条：目录名 kebab-case；groupId 限于 `com.yoursweakfoe(.application)`；Java 包名 = 目录名去连字符；artifactId kebab-case；Spring 服务名纯小写。（原号随身；全文迁来，映射表身随迁本卷 §2，公约 §2.2 改指针） | 原编码公约 §2.2 表；新服务实例见 `new-service` skill | 评审项 |

## §2 规范形状
形状即表：结构映射五条表（原编码公约 §2.2 全文迁此）＋四层依赖图（指针 → 根 README 架构图，禁手抄重制图）。

## §3 生效登记
| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| WC-1/CC-3（原号随身） | ✅ 生效 | 出卷（写链、公约）原位置留墓碑互指行 |
---

#### Requirement: 持久化语句法卷（data-access/persistence-sql.md）   ← AC-5
---
# 用法规范法卷：持久化语句（框架法 · 严格件）

> **身份**：本卷是 SQL 语句文本的写法法正身：六条铁律由禁令卷 §6 整节搬家而来（案卷 2026-09-pattern-taxonomy §4-3），六条历史上无编号、在本卷首次铸号——按摊名式编号总则（案卷 §裁决记录 裁 9）为 data-access-1～data-access-6；条文字符零改动。禁令表 §6 节此后为对照身，逐行指回本卷。修订走 `../../../changes/` 立案。docs 同题篇：无（禁令无选型面）。
> **机器对账**：C3/C4 扫本卷。教例引用真实例（sample `OptimisticLockConcurrencyTest`）仅在取证位。

## §1 条款
| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| data-access-1 | 禁止 MyBatis-Plus 进框架依赖树。持久化 = `DddMapper` 七语句 + 手写 XML，旧案卷判例。dynamic-datasource 是经一手调研证实零耦合的多数据源 opt-in 方案，`@DS` 合法 | 禁令卷原 §6 行一 | 评审项 |
| data-access-2 | 禁止 PO 携带任何 ORM 注解。纯 `@Data` POJO；表名/主键/版本条件/逻辑删除全在 XML SQL 文本 | → `../building-block/aggregate-blueprint.md` BP-X2 | C3 |
| data-access-3 | 禁止 Wrapper 式动态条件。查询一律具名 Mapper 方法 + 具名 XML 语句，`<sql>` 片段复用防漂移 | 禁令卷原 §6 行三 | 评审项 |
| data-access-4 | 建表 DDL 默认含 `version BIGINT NOT NULL DEFAULT 0` + `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`，PO 声明对应字段。显式豁免的聚合 XML 省略对应条件，逐聚合自决，无共享开关 | → BP-12 | 评审项 |
| data-access-5 | `updateById`（有版本列）**必须**携 `SET version = version + 1 ... AND version = #{version} AND is_deleted = false`，无运行时拦截器。0 行后果三分通道 → `../collaboration/optimistic-lock.md` OL-1，行为由 sample `OptimisticLockConcurrencyTest` 实证 | OL 卷互指 | 乐观锁压测 |
| data-access-6 | 逻辑删除聚合的每条 select/update/delete **必须**显式 `AND is_deleted = false`，漏一处即泄漏。豁免聚合写物理 `DELETE` | 禁令卷原 §6 行六 | 评审项 |

## §2 规范形状
语句样本唯住聚合构建宪 BP-X1/X2（构件摊），本卷只立法不重抄形状（归属法 §2）。

## §3 生效登记
| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| data-access-1～data-access-6 | ✅ 生效 | 禁令卷 §6 节改对照表身 |
---

#### Requirement: 安全链法卷（security/security-chain.md）   ← AC-5
---
# 用法规范法卷：安全链（框架法 · 严格件）

> **身份**：本卷是身份与权限跨层流动的横切法正身。唯一在册条款即 CC-8——由编码公约搬家而来、**原号随身**（案卷 2026-09-pattern-taxonomy §4-4），条文字符零改动；将来本摊新铸条款按摊名式编号（security-n，裁 9）。整条认证请求链与数据权限尚未成文——洞登记于摆架卷 §3-12 兼本卷 §3 ⛔ 行。修订走 `../../../changes/` 立案。docs 同题篇 `../../../../docs/explanation/security.md`（原理与权衡）。
> **机器对账**：C3/C4 扫本卷。

## §1 条款
| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| CC-8 | 安全上下文按层取用：`SecurityUtil` 只允许在 Application 和 Adapter 层使用；Controller 优先用 `@AuthenticationPrincipal` 注入已验签 JWT；**Domain 层禁止**感知认证上下文；角色判断用 `@PreAuthorize`，角色 claim 的名称经配置键指定（原号随身；全文迁来，一字未改） | `common-security` javadoc/配置；`../../modules/security.md` | 评审项 |

## §2 规范形状
进线→验签→上下文→逐层取用的链图：待认证请求链条款成文时一并立法（§3 ⛔ 行），无证据不造形。

## §3 生效登记
| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| CC-8（原号随身） | ✅ 生效 | 公约卷原位置留墓碑互指行 |
| 认证请求链整条走法 | ⛔ 未落地 | 候案（摆架卷 §3-12） |
| 数据权限 | ⛔ 未落地 | 候案（摆架卷 §3-12） |
---

#### Requirement: 摊头牌面十份（各摊 `README.md`）   ← AC-4
按 P-9 三行模板生成，验证问句各取其摊卡首行；`diplomacy/`、`deploy-ops/` 两摊牌面带"空穴"行。牌面正文不入 plan（零条款零形状，非法条），施工时按模板实例化，交 ddd-review 核对无复写摊卡正文。

#### Requirement: 解读篇《模式法卷为什么是十摊》   ← AC-2（连带沉淀）
`docs/explanation/pattern-taxonomy.md` 按 P-8 节序撰写并入 docs/README 登记；正文属地图件，折叠时刻成文。

### MODIFIED
#### Requirement: 写用例链 §1 条款表（WC-1 行）   ← AC-5   <!-- 整节替换：仅 WC-1 行改墓碑，余 11 行与身份行照磁盘原文（身份行相对锚按 P-10 重锚），折叠时程序化保留 -->
| WC-1 | 本条正身已迁入 `../meta/layering.md`，**仍号 WC-1**（原号随身，裁 9）；本行为墓碑互指，不复述 | 案卷 2026-09-pattern-taxonomy §4-1 | layering.md WC-1 |

#### Requirement: 编码公约 §1 条款表＋§2.2   ← AC-5   <!-- §1 整节替换：CC-3/CC-7/CC-8/CC-9 四行改墓碑，余行照磁盘原文；§2.2 表身迁分层卷 -->
| CC-3 | 正身已迁入 `../meta/layering.md`，仍号 CC-3（原号随身）；本行墓碑互指（§2.2 表身随迁） | 案卷 2026-09-pattern-taxonomy §4-1 | layering.md CC-3 |
| CC-7 | 正身已迁入 `../building-block/aggregate-blueprint.md`，仍号 CC-7（原号随身）；本行墓碑互指 | 同上 §4-2 | aggregate-blueprint.md CC-7 |
| CC-8 | 正身已迁入 `../security/security-chain.md`，仍号 CC-8（原号随身）；本行墓碑互指 | 同上 §4-4 | security-chain.md CC-8 |
| CC-9 | 正身已迁入 `../chain/read-chain.md`，仍号 CC-9（原号随身）；本行墓碑互指 | 同上 §4-2 | read-chain.md CC-9 |

#### Requirement: 读用例链 §1 条款表（正身迁入 CC-9）   ← AC-5   <!-- 整节替换：原 9 行照磁盘＋下新行；相对锚按 P-10 重锚 -->
| CC-9 | 分页契约：`PageableQuery` 由 Query record 实现；页码从 **1** 起。`pageNum` 和 `pageSize` 必须显式传入，不设默认注入：缺参会落成 0，被 `@Min(1)` 拦下返回 400。页大小上限为 `MAX_PAGE_SIZE`；`safe*()` 是执行侧的第二道防线。`@Valid` 标在契约接口的方法参数上。（原号随身，自公约卷整条迁来，一字未改） | contract 模块卷 + `PageableQuery` javadoc | 400 三通道测试 |

#### Requirement: 聚合构建宪 §2 条款表（正身迁入 CC-7）   ← AC-5   <!-- 整节替换：原表照磁盘＋下行（编号即原号 CC-7，不占 BP 号段） -->
| CC-7 | contract 模块内容有白名单：只放 Controller 契约接口、CQE、CO、枚举；只依赖 common-contract 的标记接口。东西向调用复用同一契约接口：一期走 RestClient 静态直连，Feign 为选项，需引入 common-cloud 才启用。（原号随身，自公约卷整条迁来，一字未改） | [contract 卷](../../modules/contract.md) | ArchUnit |

#### Requirement: 禁令全表 §6 节   ← AC-5   <!-- 整节替换为对照身 -->
## §6 持久化与 SQL 铁律
六条正身已迁 → [data-access/persistence-sql.md](../data-access/persistence-sql.md) data-access-1～data-access-6（案卷 2026-09-pattern-taxonomy §4-3）。本节保留对照：评审逐条查 data-access-1～data-access-6；违任一即架构违规。

#### Requirement: specs/README §目录：一个名字一种身份   ← AC-6, AC-9   <!-- 整节替换 -->
| 目录 | 身份 | 改法 |
|---|---|---|
| `current/modules/<module>.md` | **模块法卷**（8 卷）：单个 common 模块的用法规范，与模块同名 | 只能经 changes/ 折叠写入 |
| `current/patterns/<摊>/<pattern>.md` | **模式法卷**（19 卷，十个摊目录）：跨模块的横切规范，条款编号 + 生效登记；摊卡、空穴登记、搬家账 → [摆架法卷](current/patterns/meta/pattern-taxonomy.md)；**卷籍以路径为唯一名册**，册内无卷的摊（diplomacy/、deploy-ops/）有牌面待法 | 同上 |
| `changes/<slug>/` | **审议中**：还没生效的修订案 | 定稿前随便改。`_template/` 四件套模板两区共用（立法流程是业务无关通识，住知识区） |
| `archive/` | **已归档**：折叠完成的案卷（date-slug 命名）。裁决快照住案卷自己的 specify §裁决记录；现行论证复写在 `docs/explanation/` | 只进不改（连错字都不改）。到期时由清册 bill 整册归零——删除前必须满足自足判据（见归属法卷）。README 常驻，只记章程不记账目 |
（同节内及本 README 他节对归属法卷的路径引用一并改 `current/patterns/meta/attribution-law.md`。）

#### Requirement: 伞 README §3 树（specs 行与 explanation 计数行）   ← AC-6, AC-9   <!-- 整节替换：树代码块内两行更新 -->
- explanation 行：专论 6 篇 → 7 篇（增 摆架 rationale）
- specs 行：`current/{modules|patterns}/ 法卷` → `current/modules/ 模块法卷 · current/patterns/<十摊>/ 模式法卷（摊进路径；摆架宪法 → patterns/meta/pattern-taxonomy.md）`
- 本页他处 attribution-law 链接同步改道。

#### Requirement: 归属法卷 §2 事实归属表（增行）   ← AC-6, Q3   <!-- 整节替换：下表插入于「元法」行前，余 13 行照磁盘原文（行内 patterns 路径按 P-10 改道） -->
| 法卷摊归属与摆架判据（摊地图、牌面、空穴登记） | 低 | `specs/current/patterns/meta/pattern-taxonomy.md` §2/§3 ＋ 目录路径本身（meta-2：路径即名册） | 一句守则 + 指针（specs/README 目录表、伞 README 树行、摊头牌面）；禁复写摊卡正文 | 评审项（ddd-review 新卷归摊） |

### REMOVED
无。搬家=原号随身、零换号、零废号（全仓"见 WC-1"式引用零断链，P-6 机制）。

## §3 波及面与回退
- **迁都操作包（人工执行）**：19 条 `git mv`/新卷落位命令清单 + 全仓改道 sed 清单，施工期产出、折叠日核对执行；复扫判据见 P-7。
- **波及文件**（改道，不改义）：根 `AGENTS.md`（路由节三处卷路径＋法条正文句）、`.agents/README.md` L14 计数、12 个 SKILL.md 内法卷锚路径、`knowledge/docs/` 伞/索引/how-to 13 卡 governing 列/glossary/explanation 诸篇指针、modules 八卷对 patterns 卷互链、`knowledge/README.md`（辖域句与链归属法两处）。
- **零触**：check-docs 与白名单、条款文字（除搬家六处正身行——五处原号随身＋一处首次铸号，全部零字改）、镜像区、Java 码、diagrams 源（无新图）。
- **回退**：未折叠则法未变；git mv 未执行前撤案卷目录即全复原；折叠日当天改道与 mv 同 PR，出问题整 PR revert。
