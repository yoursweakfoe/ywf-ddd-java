# knowledge/decisions/ —— 判例卷宗（态3：冻结）

**本区法律（append-only）**：卷宗正文永不回改。推翻旧决定 = 新立一案，旧案仅 Status 行改为 `Superseded by ADR-NNNN`。机器执法：`knowledge/scripts/check-docs.ps1` C6（diff 超出「新增文件 / Status 行」即红）。

**论证与过程分家**（2026-09 `2026-09-archive-dossier-boundary`）：决策论证 canonical 住本区 ADR；specs/ 案卷内裁决只登表决行（何人何时批/否/裁何事，至多附一句因）+ → ADR-NNNN 指针，论证正文不得两处双写——全文 → [归属法 §1/§2](../specs/current/patterns/attribution-law.md)。

## 已知历史违例（史注，不掩盖）

- 旧「模块内各自从 0001 起编」时代，common-ddd 的事件类 ADR（旧 0003/0006/0007/0008）于 2026-09 事件留白（5bc4b4b）整删后，**0006/0007 号被复用承载了全新内容**——违反旧账本自述的「编号单调递增不复用」教义。全局重编号（本区 0001-0032）根治此患；旧→新以 §migration 为准，术语正名：**Superseded 墓碑**（有正文保留，如 ADR-0009/0014）≠ **Void**（编号空置无正文：ADR-0004/0015/0017）。

## migration（旧模块限定号 → 新全局号，32 位）

| 旧号（模块 §ADR-000N） | 新号 | 标题 | 状态 |
|---|---|---|---|
| ddd §ADR-0001 | [ADR-0001](ADR-0001-ddd-base-class-no-id-version.md) | 基类不持有 id/version 字段 | Accepted |
| ddd §ADR-0002 | [ADR-0002](ADR-0002-ddd-full-update-no-dirty-check.md) | 全量 UPDATE 而非脏检查 | Accepted |
| ddd §ADR-0004 | [ADR-0003](ADR-0003-ddd-manual-conversion-no-mapstruct.md) | 对象转换纯手写，不用 MapStruct | Accepted |
| ddd §ADR-0003 | [ADR-0004](ADR-0004-ddd-domain-event-auto-publish-void.md) | 领域事件自动发布 | **Void** |
| ddd §ADR-0005 | [ADR-0005](ADR-0005-ddd-query-pure-marker.md) | CQRS 契约：Query 纯标记 | Accepted（2026-08 修订） |
| ddd §ADR-0006 | [ADR-0006](ADR-0006-ddd-offsetdatetime-and-clock.md) | 时间统一 OffsetDateTime + 统一注入 Clock | Accepted（2026-09 补录） |
| ddd §ADR-0007 | [ADR-0007](ADR-0007-ddd-remove-mybatis-plus.md) | 持久化手写 XML SQL 全面接管，移除 MyBatis-Plus | Accepted（2026-09） |
| contract §ADR-0001 | [ADR-0008](ADR-0008-contract-marker-interface-no-generic.md) | 标记接口不含泛型 | Accepted |
| contract §ADR-0002 | [ADR-0009](ADR-0009-contract-light-contract.md) | 轻契约（不含 REST/RPC 注解） | **Superseded by ADR-0010** |
| contract §ADR-0003 | [ADR-0010](ADR-0010-contract-heavy-contract-http-mapping.md) | 契约承载 HTTP 映射 + 文档注解（重契约） | Accepted |
| exception §ADR-0001 | [ADR-0011](ADR-0011-exception-i18n-message-key.md) | i18n 位点（字符串 key）而非数字错误码 | Accepted |
| exception §ADR-0002 | [ADR-0012](ADR-0012-exception-rfc9457-problem-json.md) | RFC 9457 响应格式 | Accepted |
| exception §ADR-0003 | [ADR-0013](ADR-0013-exception-ise-to-409.md) | IllegalStateException → 409 | Accepted |
| security §ADR-0001 | [ADR-0014](ADR-0014-security-gateway-trusted-header-identity.md) | Header 透传身份（网关验签 + 服务信任 Header） | **Superseded by ADR-0018** |
| security §ADR-0002 | [ADR-0015](ADR-0015-security-edge-identity-source-void.md) | 身份来源标记（EDGE）（历史题，已无正文） | **Void** |
| security §ADR-0003 | [ADR-0016](ADR-0016-security-permit-all-filter-chain.md) | 边界 permit-all SecurityFilterChain | Accepted |
| security §ADR-0004 | [ADR-0017](ADR-0017-security-header-preauth-filter-void.md) | 预认证 + 链内注册（Header 解析过滤器）（历史题，已无正文） | **Void** |
| security §ADR-0005 | [ADR-0018](ADR-0018-security-zero-trust-jwt-self-verify.md) | 零信任：服务自验 JWT（资源服务器） | Accepted |
| security §ADR-0006 | [ADR-0019](ADR-0019-security-native-jwt-no-projection.md) | 身份不投影：原生 Jwt + 按名字自取 | Accepted |
| security §ADR-0007 | [ADR-0020](ADR-0020-security-pluggable-jwt-decoder.md) | 验签可插拔：JwtDecoder 抽象 + 多方案分发 | Accepted |
| cloud §ADR-0001 | [ADR-0021](ADR-0021-cloud-east-west-http.md) | 东西向通信统一 HTTP | Accepted |
| cloud §ADR-0002 | [ADR-0022](ADR-0022-cloud-resilience4j-over-sentinel.md) | 熔断降级用 Resilience4j 而非 Sentinel | Accepted |
| cloud §ADR-0003 | [ADR-0023](ADR-0023-cloud-nacos-via-sca-starter.md) | Nacos 经 SCA starter 引入，client 版本独立管理 | Accepted |
| cloud §ADR-0004 | [ADR-0024](ADR-0024-cloud-seata-independent-artifact.md) | Seata 独立构件 + 版本独立管理 | Accepted |
| cloud §ADR-0005 | [ADR-0025](ADR-0025-cloud-seata-xid-not-builtin.md) | Seata XID 透传不内置 | Accepted |
| cloud §ADR-0006 | [ADR-0026](ADR-0026-cloud-east-west-jwt-propagation.md) | 东西向身份传播：透传已验签 JWT（零信任） | Accepted |
| pg §ADR-0001 | [ADR-0027](ADR-0027-pg-auto-register-typehandlers.md) | 自动注册而非手动配置 | Accepted |
| pg §ADR-0002 | [ADR-0028](ADR-0028-pg-jsonb-explicit-typehandler.md) | JSONB 需显式指定 typeHandler | Accepted |
| observability §ADR-0001 | [ADR-0029](ADR-0029-observability-stdout-only.md) | stdout 输出，不落盘文件 | Accepted |
| observability §ADR-0002 | [ADR-0030](ADR-0030-observability-otel-agent-not-sdk.md) | OTel Agent 而非 SDK | Accepted |
| test §ADR-0001 | [ADR-0031](ADR-0031-test-archunit-over-manual-review.md) | ArchUnit 而非人工 Code Review | Accepted |
| test §ADR-0002 | [ADR-0032](ADR-0032-test-static-constant-ruleset.md) | 规则集为静态常量 | Accepted |
