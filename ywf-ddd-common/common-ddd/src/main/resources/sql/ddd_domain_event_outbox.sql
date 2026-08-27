-- ============================================================================
-- ddd_domain_event_outbox.sql —— 领域事件 Outbox 缺省表（PostgreSQL）
--
-- 全链路 Outbox 可靠性规范的领域侧标准表：框架缺省实现
-- （JdbcDomainEventOutboxStore 捕获 / OutboxRelay 领域实例排空）直接读写本表。
-- 领域事件与业务写入【同事务】入箱——「聚合状态已提交 ⇒ 事件必然已落库」；
-- 业务回滚则事件随行回滚。入箱后由排空器在自有事务内派发（先清后发不再适用，
-- 派发 / 内部反应 / 标记完成三者原子，见 OutboxRelay）。
--
-- 信封列（捕获与投递之间唯一的跨边界约定）：
--   id          = DomainEvent.eventId（幂等键与行身份合一）
--   event_type  = 领域事件类全限定名（反序列化锚点）
--   payload     = 领域事件 JSON 载荷（TEXT 存储，跨 H2/PG 可移植）
--   occurred_on = 事件发生时间（UTC，规则 09）
-- 簿记列（排空器重试 / 死信，形状由 OutboxRelay 钉死）：
--   attempts / next_retry_at / status / last_error
-- 标准结构列（与本仓所有业务表一致，见 .agents/rules/04）：
--   version / create_at / update_at / created_by / updated_by / is_delete
--   —— create_at/update_at 无默认值：捕获时由 JdbcDomainEventOutboxStore 填充，
--      排空簿记由 OutboxRelay 填充；投递完成 = is_delete=TRUE（软删留痕），
--      保留期（默认 7 天）后由 purge 物理清除。
-- ============================================================================

CREATE TABLE IF NOT EXISTS ddd_domain_event_outbox (
    -- ── 信封 ────────────────────────────────────────────────────────────────
    id            VARCHAR(36) PRIMARY KEY,              -- = DomainEvent.eventId
    event_type    VARCHAR(500) NOT NULL,                -- 领域事件类全限定名
    payload       TEXT NOT NULL,                        -- 领域事件 JSON 载荷（TEXT 可移植，见文末说明）
    occurred_on   TIMESTAMP WITH TIME ZONE NOT NULL,    -- 事件发生时间（UTC）

    -- ── 簿记（排空器重试 / 死信） ───────────────────────────────────────────
    attempts      INT NOT NULL DEFAULT 0,               -- 累计投递失败次数
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,        -- 下次重试时间（NULL = 立即可投）
    status        SMALLINT NOT NULL DEFAULT 0,          -- 0=PENDING 1=DEAD
    last_error    TEXT NULL,                            -- 最近失败原因（TEXT 不截断）

    -- ── 标准结构（与本仓所有业务表一致） ────────────────────────────────────
    version       INT NOT NULL DEFAULT 0,               -- 乐观锁
    create_at     TIMESTAMP,
    update_at     TIMESTAMP,
    created_by    VARCHAR(64),
    updated_by    VARCHAR(64),
    is_delete     BOOLEAN NOT NULL DEFAULT FALSE        -- 投递完成软删留痕（替代物理 DELETE）
);

-- 认领扫描索引：仅 PENDING + 到期过滤 + FIFO 排序。
-- 部分索引（partial index）把已软删行挡在索引之外——软删留痕场景下行数只增不减，
-- 部分索引保持认领扫描的索引体积稳定。
CREATE INDEX IF NOT EXISTS idx_domain_event_outbox_due
    ON ddd_domain_event_outbox (status, next_retry_at, occurred_on)
    WHERE is_delete = FALSE;

-- ============================================================================
-- payload 列类型说明：
--   缺省 JdbcDomainEventOutboxStore 以一条可移植 INSERT 写入（字符串绑定），
--   框架从不查询载荷内部字段，故用 TEXT 即可跨 H2（测试）/PostgreSQL（生产）。
--   PG 侧若需在库内直接查询/过滤载荷字段，可将本列改为 JSONB，
--   并自行提供 DomainEventOutboxStore 实现（写入时 ?::jsonb，经 SPI 替换缺省实现）。
-- 载荷容量策略（平均 payload > 2KB 时评估）：
--   1. 压缩 —— 应用层 gzip + base64，牺牲 CPU 换 I/O
--   2. 拆分 —— outbox 仅存元数据 + 引用键，载荷存独立表 / OSS
-- ============================================================================
