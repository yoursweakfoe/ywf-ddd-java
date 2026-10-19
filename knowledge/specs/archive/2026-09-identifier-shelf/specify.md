# 身份词汇上架：Identifier 迁入 common-ddd domain.id
- Slug: 2026-09-identifier-shelf ｜ 日期: 2026-09-11

## Why（为什么现在改）
2026-09-typed-identifier 案把 `Identifier<V>` 发行在 common-ddd `domain/model`（与 `Identifiable` 等同架）。项目主裁：框架词汇架应与业务落位段**镜像对偶**——业务身份终类型住 `domain/{agg}/id/`（BP-13 已定档），其词汇契约就该住 `com.yoursweakfoe.common.ddd.domain.id`；「框架词汇包名 ↔ 业务实装包名」的镜像惯例自此有据（段名 id ↔ id），补上此前不成文的缺口。

## What changes（做什么，非怎么做）
- 为聚合作者：`Identifier` 的 import 坐标改为 `com.yoursweakfoe.common.ddd.domain.id.Identifier`；其余不变。
- 为框架：common-ddd 新增 `domain.id` 子段（一词之架）；`domain.model` 架面回归「本体基类+铸造入口」旧序。
- 为执法：R15 的词汇判据按新坐标解析（语义零变）。
- 为文档：法卷 ddd 模块卷词汇发行条款改位、docs 同题指针随动。施工动作住 tasks，本件不复制。

## 验收标准（AC 账 — 人话摘要；法条正文住 plan §delta）
- AC-1 全仓编译 + 既有测试阵（322 测试、R15 四锁探针、混放 javac 双向取证）在新坐标下全绿。
- AC-2 词汇唯一居位：`domain/model` 下不再有 Identifier 文件或链接残留；全库 FQCN 扫描只认 `domain.id` 形。
- AC-3 文档七闸绿、法卷词汇条款与磁盘实位一致、⑨⁺/解读篇/字典指针无幽灵。

## 约束
- 仅迁 `Identifier` 一类；`Identifiable`/`AggregateIds`/`AggregateRoot`/`Entity`/`ValueObject` 不动（项目主原语只点名 Identifier）。
- 业务落位 `domain/{agg}/id/`、R15 语义、四件套既有裁决一律不动——本案只改上架坐标，不改任何行为教义。
- 案卷 2026-09-typed-identifier 在档不回改（在位不改），其 plan P-1 之架位裁量由本案改写。

## 不做（范围边界）
- 不拆 `AggregateIds`（铸造入口与 Identifiable 的「一枚硬币两面」共居 model 叙事保留）。
- 不为 `domain.id` 增设第二条词汇、不预留包骨架清单（首用者即 Identifier）。
- 不动 ArchUnit 规则集其他条目与挂载拓扑。

## 待你裁决的 N 问
（无——形状、范围、工序皆由项目主明示指令定死，见 §裁决记录）

## §裁决记录（裁决唯一居所；裁一条落一条，落笔不改）
- 门 Specify 门 + 门 Plan 门 ✅（直令即门）2026-09-11 ｜项目主原语「我觉得你在common包里单独开一个com.yoursweakfoe.common.ddd.domain.id路径，然后把这个identifier放进去不就行了？就按我说的办」——指令含设计（新开 domain.id 架、只放 Identifier）与放行（按我说的办）双重明示，两门并释为直令放行，先例参照 2026-09-cull 案「两门经项目主会话明示直令豁免」；AC 账与边界按指令范围成文 ｜supersede：案卷 2026-09-typed-identifier plan P-1 之词汇架位选择（`domain/model`）由本案改判 `domain.id`——该条系起草技术裁量非项目主裁决，业务落位 id 包、Q2 词汇发行裁决、R15/BP-13~17 语义全部承接不动
