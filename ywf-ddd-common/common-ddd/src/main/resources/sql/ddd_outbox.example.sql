-- ============================================================================
-- ddd_outbox.example.sql —— Outbox 消息表参考结构（PostgreSQL）
--
-- 这是「参考样例」而非框架组件：框架只提供捕获契约（OutboxStore SPI），
-- 不提供缺省表/缺省实现。业务按自己的表结构实现 OutboxStore 与排空器，
-- 本文件展示与本仓表标准对齐的起点。
--
-- 标准结构列（与本仓所有业务表一致）：
--   version / create_at / update_at / created_by / updated_by / is_delete
--   —— create_at/update_at 无默认值，由 BasicAutoFillHandler 在应用层填充；
--      审计列/乐观锁/逻辑删除的框架约定见 .agents/rules/04。
--
-- 信封列（捕获与投递之间唯一的跨边界约定，见 common-ddd.md Outbox 节）：
--   id = DomainEvent.eventId（幂等键与行身份合一）
--   event_type = 事件类全限定名（反序列化锚点）
--   payload = 事件 JSON 载荷（原生 JSON 存储）
--   occurred_on = 事件发生时间（UTC）
-- 簿记列（attempts/next_retry_at/status/last_error）形状由排空器自定，
-- 此处为参考值。
-- ============================================================================

CREATE TABLE IF NOT EXISTS ddd_outbox (
    -- ── 信封 ────────────────────────────────────────────────────────────────
    id            VARCHAR(36) PRIMARY KEY,              -- = DomainEvent.eventId
    event_type    VARCHAR(500) NOT NULL,                -- 事件类全限定名
    payload       JSONB NOT NULL,                       -- 事件 JSON 载荷（原生 JSON）
    occurred_on   TIMESTAMP WITH TIME ZONE NOT NULL,    -- 事件发生时间（UTC，规则 09）

    -- ── 簿记（形状由排空器自定） ────────────────────────────────────────────
    attempts      INT NOT NULL DEFAULT 0,               -- 累计投递失败次数
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,        -- 下次重试时间
    status        SMALLINT NOT NULL DEFAULT 0,          -- 0=PENDING 1=DEAD
    last_error    TEXT NULL,                            -- 最近失败原因（TEXT 不截断；截断策略由排空器定）

    -- ── 标准结构（与本仓所有表一致） ────────────────────────────────────────
    version       INT NOT NULL DEFAULT 0,               -- 乐观锁（排空器并发认领可用）
    create_at     TIMESTAMP,
    update_at     TIMESTAMP,
    created_by    VARCHAR(64),
    updated_by    VARCHAR(64),
    is_delete     BOOLEAN NOT NULL DEFAULT FALSE        -- 逻辑删除（投递完成可软删留痕，替代物理 DELETE）
);

-- 认领扫描索引：status + 到期过滤 + FIFO 排序。
-- 微调点：部分索引（partial index）把已软删行挡在索引之外——
-- 软删留痕场景下行数只增不减，部分索引保持认领扫描的索引体积稳定。
CREATE INDEX IF NOT EXISTS idx_ddd_outbox_due
    ON ddd_outbox (status, next_retry_at, occurred_on)
    WHERE is_delete = FALSE;

-- 可选：按事件类型排查/对账时再加（默认不需要，避免写入放大）
-- CREATE INDEX IF NOT EXISTS idx_ddd_outbox_event_type
--     ON ddd_outbox (event_type)
--     WHERE is_delete = FALSE;

-- ============================================================================
-- payload 容量策略（平均 payload > 2KB 时评估）：
--   1. 改用/保持 JSONB（当前即 JSONB）——适合下游需要 DB 内查询/过滤载荷字段
--   2. 保持文本但启用压缩——应用层 gzip + base64，牺牲 CPU 换 I/O
--   3. 大 payload 拆分——outbox 仅存元数据 + 引用键，载荷存独立表 / OSS
-- ============================================================================
