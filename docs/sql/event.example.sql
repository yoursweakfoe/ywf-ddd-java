-- ============================================================================
-- event.example.sql —— 事件表建表样例（供数据抽取层参考）
--
-- 【用途】数据抽取层建表样例：参考约定，非框架强制。框架对存储形态零假设
--   （SPI-only、零 SQL），使用方可整体替换存储实现；本文件仅给出与 sample
--   参考实现（JdbcOutboxRowAccess 等）配套的标准表结构。
--
-- 【完成语义】is_delete = TRUE 即「已投递」（软删留痕）——抽取层以此判定
--   可搬运的行；is_delete = FALSE 的行尚未投递完成，不可搬运。
--
-- 【审计约定】框架只写不清——无保留期、无清除任务，事件行永不删除。
--   搬运 / 归档节奏由抽取层自定（建议按 update_at 或 occurred_on 分批）。
--   排空正确性不受留存行影响：认领过滤只取未完成行
--   （is_delete = FALSE AND status = 0），且部分索引把已软删行挡在索引之外，
--   认领扫描的索引体积保持稳定——行数只增不减是有意设计，勿"修复"。
--
-- 【身份与血缘】
--   领域表 ddd_domain_event_outbox.id = DomainEvent.eventId（消费端幂等键）；
--   集成表 ddd_integration_event_outbox.id = MQ 信封 messageId（下游幂等键）、
--   source_event_id = 产生本集成事件的源领域事件 eventId（fan-out 血缘，
--   一对一 / 一对多均可溯源；入站集成事件再发出时为 NULL）；
--   occurred_on 为 UTC（TIMESTAMP WITH TIME ZONE）。
--
-- 【簿记列】attempts / next_retry_at / status / last_error 由排空器维护：
--   status：0 = PENDING，1 = DEAD（重试耗尽转死信）。
--   status = 1 的死信行需人工介入（排查后修复或人工处置），
--   同样属于抽取 / 归档职责范围之外的处理——抽取层搬运时不应移动死信行。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 1：ddd_domain_event_outbox —— 领域事件 Outbox
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ddd_domain_event_outbox (
    -- ── 信封 ────────────────────────────────────────────────────────────────
    id            VARCHAR(36) PRIMARY KEY,              -- = DomainEvent.eventId
    event_type    VARCHAR(500) NOT NULL,                -- 领域事件类全限定名
    payload       TEXT NOT NULL,                        -- 领域事件 JSON 载荷（TEXT 可移植）
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
    is_delete     BOOLEAN NOT NULL DEFAULT FALSE        -- 投递完成软删留痕（框架只写不清）
);

-- 认领扫描索引：仅 PENDING + 到期过滤 + FIFO 排序。
-- 部分索引（partial index）把已软删行挡在索引之外——软删留痕场景下行数只增不减，
-- 部分索引保持认领扫描的索引体积稳定。
CREATE INDEX IF NOT EXISTS idx_domain_event_outbox_due
    ON ddd_domain_event_outbox (status, next_retry_at, occurred_on)
    WHERE is_delete = FALSE;

-- ----------------------------------------------------------------------------
-- 表 2：ddd_integration_event_outbox —— 集成事件 Outbox
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ddd_integration_event_outbox (
    -- ── 信封 ────────────────────────────────────────────────────────────────
    id              VARCHAR(36) PRIMARY KEY,              -- 集成事件行身份（= MQ messageId）
    event_type      VARCHAR(500) NOT NULL,                -- 集成事件类全限定名
    payload         TEXT NOT NULL,                        -- 集成事件 JSON 载荷（最终 MQ 载荷）
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
    is_delete       BOOLEAN NOT NULL DEFAULT FALSE        -- 投递完成软删留痕（框架只写不清）
);

-- 认领扫描索引：仅 PENDING + 到期过滤 + FIFO 排序。
-- 部分索引把已软删行挡在索引之外，保持认领扫描的索引体积稳定。
CREATE INDEX IF NOT EXISTS idx_integration_event_outbox_due
    ON ddd_integration_event_outbox (status, next_retry_at, occurred_on)
    WHERE is_delete = FALSE;

-- ============================================================================
-- 抽取层搬运示例（示意，按自身节奏改写）：
-- 典型搬运：分批读取「已投递完成」的行，按 occurred_on 排序、按 id 断点续传，
-- 搬运至归档存储（数仓 / 对象存储 / 冷表）后由抽取层自行决定去留——框架不做任何删除。
--
-- SELECT id, event_type, payload, occurred_on, source_event_id,
--        attempts, status, create_at, update_at
--   FROM ddd_domain_event_outbox
--  WHERE is_delete = TRUE            -- 已投递（软删留痕），抽取层可搬运
--    AND status = 0                  -- 排除死信行（status=1 需人工介入，不在搬运范围）
--    AND id > :last_seen_id          -- 断点续传（按主键分页，避免 OFFSET 漂移）
--    -- 可选：AND occurred_on < :cutoff —— 只搬运截止时刻前的历史条目
--  ORDER BY occurred_on, id
--  LIMIT :batch_size;
--
-- 集成表同形（多取 source_event_id 作血缘字段）：
-- SELECT id, event_type, payload, occurred_on, source_event_id,
--        attempts, status, create_at, update_at
--   FROM ddd_integration_event_outbox
--  WHERE is_delete = TRUE
--    AND status = 0
--    AND id > :last_seen_id
--  ORDER BY occurred_on, id
--  LIMIT :batch_size;
-- ============================================================================
