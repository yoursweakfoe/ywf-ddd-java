-- ============================================================================
-- ddd_integration_event_outbox.sql —— 集成事件 Outbox 缺省表（PostgreSQL）
--
-- 全链路 Outbox 可靠性规范的集成侧标准表：框架缺省实现
-- （JdbcIntegrationEventOutboxStore 捕获 / OutboxRelay 集成实例排空）直接读写本表。
-- 集成事件（最终 MQ 载荷）由应用层 Publisher 在【领域排空事务内】翻译并捕获入箱，
-- 关闭「领域事件已派发 → 集成事件投 MQ」之间的 dual-write 窗口；
-- 随后由集成排空器经 IntegrationEventSender 投递 MQ（messageId = 本表行 id）。
--
-- 信封列（捕获与投递之间唯一的跨边界约定）：
--   id              = 集成事件行身份（捕获时铸造的 UUID；= MQ 信封 messageId，
--                     下游消费端按它幂等去重——跨重投稳定，因为它就是行本身）
--   event_type      = 集成事件类全限定名（反序列化锚点）
--   payload         = 集成事件 JSON 载荷（最终 MQ 载荷；TEXT 存储，跨 H2/PG 可移植）
--   occurred_on     = 捕获时间（UTC，规则 09）
-- 溯源列：
--   source_event_id = 产生本集成事件的领域事件 eventId（一对一 / 一对多 fan-out 的血缘）；
--                     入站集成事件再发出（无领域来源）时为 NULL
-- 簿记列（排空器重试 / 死信，形状由 OutboxRelay 钉死）：
--   attempts / next_retry_at / status / last_error
-- 标准结构列（与本仓所有业务表一致，见 .agents/rules/04）：
--   version / create_at / update_at / created_by / updated_by / is_delete
--   —— create_at/update_at 无默认值：捕获时由 JdbcIntegrationEventOutboxStore 填充；
--      投递完成 = is_delete=TRUE（软删留痕），保留期（默认 7 天）后由 purge 物理清除。
-- ============================================================================

CREATE TABLE IF NOT EXISTS ddd_integration_event_outbox (
    -- ── 信封 ────────────────────────────────────────────────────────────────
    id              VARCHAR(36) PRIMARY KEY,              -- 集成事件行身份（= MQ messageId）
    event_type      VARCHAR(500) NOT NULL,                -- 集成事件类全限定名
    payload         TEXT NOT NULL,                        -- 集成事件 JSON 载荷（TEXT 可移植，见文末说明）
    occurred_on     TIMESTAMP WITH TIME ZONE NOT NULL,    -- 捕获时间（UTC）

    -- ── 溯源 ────────────────────────────────────────────────────────────────
    source_event_id VARCHAR(36) NULL,                     -- 源领域事件 eventId（入站再发出为 NULL）

    -- ── 簿记（排空器重试 / 死信） ───────────────────────────────────────────
    attempts        INT NOT NULL DEFAULT 0,               -- 累计投递失败次数
    next_retry_at   TIMESTAMP WITH TIME ZONE NULL,        -- 下次重试时间（NULL = 立即可投）
    status          SMALLINT NOT NULL DEFAULT 0,          -- 0=PENDING 1=DEAD
    last_error      TEXT NULL,                            -- 最近失败原因（TEXT 不截断）

    -- ── 标准结构（与本仓所有业务表一致） ────────────────────────────────────
    version         INT NOT NULL DEFAULT 0,               -- 乐观锁
    create_at       TIMESTAMP,
    update_at       TIMESTAMP,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    is_delete       BOOLEAN NOT NULL DEFAULT FALSE        -- 投递完成软删留痕（替代物理 DELETE）
);

-- 认领扫描索引：仅 PENDING + 到期过滤 + FIFO 排序。
-- 部分索引把已软删行挡在索引之外，保持认领扫描的索引体积稳定。
CREATE INDEX IF NOT EXISTS idx_integration_event_outbox_due
    ON ddd_integration_event_outbox (status, next_retry_at, occurred_on)
    WHERE is_delete = FALSE;

-- ============================================================================
-- payload 列类型说明：
--   缺省 JdbcIntegrationEventOutboxStore 以一条可移植 INSERT 写入（字符串绑定），
--   框架从不查询载荷内部字段，故用 TEXT 即可跨 H2（测试）/PostgreSQL（生产）。
--   PG 侧若需在库内直接查询/过滤载荷字段，可将本列改为 JSONB，
--   并自行提供 IntegrationEventOutboxStore 实现（写入时 ?::jsonb，经 SPI 替换缺省实现）。
-- 载荷容量策略（平均 payload > 2KB 时评估）：
--   1. 压缩 —— 应用层 gzip + base64，牺牲 CPU 换 I/O
--   2. 拆分 —— outbox 仅存元数据 + 引用键，载荷存独立表 / OSS
-- ============================================================================
