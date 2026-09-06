# ADR-0006 时间统一 OffsetDateTime + 统一注入 Clock

**Status**: Accepted（2026-09 补录，论证经 pgjdbc / MyBatis / 业界 ORM 一手对照调研）
**迁移来源**: docs/common/common-ddd.md §6 · 旧 ADR-0006（WP-3 预提取 2026-09-06，未定稿）

## Context and Problem Statement

时间类型贯穿 domain / 持久化 / 契约 / 序列化四层，时区错误是系统性风险，故收敛为一型一源：全框架统一 `java.time.OffsetDateTime`，唯一时间源 = 框架级 `Clock` Bean。

## Decision Drivers

- 时区错误的系统性风险（跨四层扩散）
- 可测试性（业务测试需固定时钟）
- pgjdbc / MyBatis / 业界 ORM 的类型映射矩阵事实（见下）

## Considered Options

- `OffsetDateTime`（选定）
- `ZonedDateTime`：双向 `PSQLException`（驱动不支持）——不可行
- `LocalDateTime`：写入依赖会话时区（值漂移）、读 timestamptz 抛异常——不可行
- `Instant`：非原生（仅 `Timestamp` 桥，跨库语义漂移），不作迁移目标

## Decision Outcome

全框架统一 `java.time.OffsetDateTime`；唯一时间源 = 框架级 `Clock` Bean（`ClockAutoConfiguration` 缺省 `Clock.systemUTC()`，`@ConditionalOnMissingBean(Clock.class)` 挂在 `@Bean` 方法级退位——Boot 正统姿势，类级条件会依据不可靠的求值顺序误判，业务测试以 `Clock.fixed(instant, ZoneOffset.UTC)` 覆盖）。

**关键事实（三条）**：

1. **写入丢弃偏移**：`timestamptz` 被 PG 归一化为绝对瞬时、以 UTC 存储，原始偏移不保留（PG 官方文档 §8.5.3）
2. **读回恒 +00:00**：pgjdbc 二进制 / 文本路径均恒以 UTC 偏移返回——与会话时区、JVM 时区、传输模式无关；`OffsetDateTime` 亦是 pgjdbc 映射矩阵中 timestamptz 唯一双向原生类型、MyBatis 3.5.0+ 内置原生 TypeHandler、Hibernate 6 / jOOQ / Spring Data JDBC 的同一收敛选择
3. **禁用 LocalDateTime / ZonedDateTime 的原因**：见 Considered Options 逐项

## Consequences

**配套规则**：
- 表达「同一瞬时」一律 `isEqual()`（`equals` 要求偏移亦相等，写读恒 UTC 后被结构性消除）
- PG 分辨率 1µs，Java 纳秒精度落库必丢失，内存值与 DB 回显比较时注意
- 展示层禁用 `getString()` 取时间（`prepareThreshold` 后文本 / 二进制切换致显示格式不一致）
- 容器统一 `TZ=UTC` 纵深防御
- 禁止对 `OffsetDateTime` 实例加锁（value-based，与虚拟线程规则同向）

## Confirmation

机械背书：
- 框架：`ClockAutoConfigurationTest.providesUtcClockByDefault` / `consumerClockBean_backsOffFrameworkDefault` / `userNonUtcClock_isTheOnlyClock`（方法级退位实证）；`AuditFieldFillerTest`（审计填充时间源）
- 注意：「时间类型统一 OffsetDateTime（禁 LocalDateTime/ZonedDateTime 持久化）」**无 ArchUnit 规则**——`DddArchitectureRules` 类头「已知缺口」明示登记，纯靠 `.agents/rules/04` 纪律；机械背书仅覆盖 Clock Bean 装配与审计填充环节
- 原记锚点：`ClockAutoConfiguration`（`systemUTC` 缺省）、`AuditFieldFiller`（`OffsetDateTime.now(clock)` 填充审计字段）、PO 审计列 `createAt`/`updateAt` 均为 `OffsetDateTime`
