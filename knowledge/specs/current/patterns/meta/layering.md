# 用法规范法卷：分层（框架法 · 严格件）

> **身份**：本卷是四层地基条款的唯一权威：依赖方向、层豁免、目录结构映射律。两条正文自写用例链与编码公约搬家而来（案卷 2026-09-pattern-taxonomy 搬家账 1），**原号随身**：条款仍以 WC-1、CC-3 为号，永不重铸；条文字符零改动，出卷留墓碑互指。违反本卷就改代码；修订本卷走 `../../../changes/` 立案。docs 侧无同题设计卡（地基无选型问题）；原理与论证在 `../../../../docs/explanation/` 分层设计五篇与 [architecture-rules.md](../../../../docs/explanation/architecture-rules.md)。
> **机器对账**：C1 检查 `{agg}` 实例化（本卷无槽位，天然过）、C3 查符号、C4 查教学中立扫本卷。

## §1 条款

| # | SHALL | 取证 | 背书 |
|---|-------|------|------|
| WC-1 | 依赖方向单向：`adapter → application → domain ← infrastructure`。domain 层零框架运行时依赖；stereotype 豁免，只允许纯 Java + common-ddd（原号随身，自写链卷整条迁来，一字未改） | ArchUnit 双端守护，规则集在 common-test | R1/R2 系 |
| CC-3 | 结构映射五条：目录名 kebab-case；groupId 限于 `com.yoursweakfoe(.application)`；Java 包名 = 目录名去连字符；artifactId kebab-case；Spring 服务名纯小写。（原号随身，自公约卷整条迁来，表身随迁本卷 §2，一字未改） | 本卷 §2；新服务实例见 `new-service` skill | 评审项 |

## §2 规范形状（结构映射表——原公约卷 §2.2 表身，逐字迁来）

| 层面 | 规则 | 示例 |
|------|------|------|
| 目录名 | kebab-case | `sample-application/`、`common-ddd/` |
| groupId（common） | `com.yoursweakfoe` | `com.yoursweakfoe:common-ddd` |
| groupId（业务服务） | `com.yoursweakfoe.application` | `com.yoursweakfoe.application:sample-service` |
| Java 包名 | 全小写无分隔符 | `com.yoursweakfoe.sampleapplication.sampleservice` |
| artifactId | kebab-case | `sample-service-server`、`common-exception` |
| 服务名（Spring） | 纯小写 | `service`（`spring.application.name`） |

四层依赖图唯住根 README 架构图（单一真身），本卷只指针不重画。

## §3 生效登记

| 条款/环节 | 状态 | 位置 |
|-----------|------|------|
| WC-1 | ✅ 生效 | [../chain/write-chain.md](../chain/write-chain.md) §1 墓碑互指行在册 |
| CC-3 | ✅ 生效 | [../discipline/coding-conventions.md](../discipline/coding-conventions.md) §1 墓碑行与 §2.2 指针在册 |
