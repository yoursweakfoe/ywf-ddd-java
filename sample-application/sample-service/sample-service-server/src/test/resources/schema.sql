-- ============================================================================
-- ID 策略说明：
--   orders   — UUID（应用侧工厂铸造：RFC 9562 UUIDv7，时间有序）
--   products — UUID（应用侧工厂铸造：同上；两类聚合身份策略已统一）
-- create_at / update_at 无默认值：由 BasicAutoFillHandler 在应用层填充（非 DB 触发器）。
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS products;

CREATE TABLE IF NOT EXISTS orders.orders (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    items TEXT,
    total_amount DECIMAL(10,2),
    customer_id VARCHAR(50),
    tracking_number VARCHAR(100),
    cancel_reason VARCHAR(500),
    version INT NOT NULL DEFAULT 0,
    create_at TIMESTAMP,
    update_at TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    is_delete BOOLEAN NOT NULL DEFAULT FALSE
);

-- 高频查询索引
CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders.orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders.orders (status);

CREATE TABLE IF NOT EXISTS products.products (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    create_at TIMESTAMP,
    update_at TIMESTAMP,
    created_by VARCHAR(64),
    updated_by VARCHAR(64),
    is_delete BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products.products (name);

-- ============================================================================
-- Outbox 缺省表（全链路 Outbox 可靠性规范；此处为 H2 测试形态）
--   与框架 resources/sql/ddd_*_event_outbox.sql 的 PG 规范结构一致，差异：
--   payload 用 TEXT（H2 无 JSONB）；索引用普通索引（H2 不支持部分索引）。
--   落默认 PUBLIC schema（JdbcTemplate 无 schema 前缀写入）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS ddd_domain_event_outbox (
    id            VARCHAR(36) PRIMARY KEY,
    event_type    VARCHAR(500) NOT NULL,
    payload       TEXT NOT NULL,
    occurred_on   TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts      INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE NULL,
    status        SMALLINT NOT NULL DEFAULT 0,
    last_error    TEXT NULL,
    version       INT NOT NULL DEFAULT 0,
    create_at     TIMESTAMP,
    update_at     TIMESTAMP,
    created_by    VARCHAR(64),
    updated_by    VARCHAR(64),
    is_delete     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_domain_event_outbox_due
    ON ddd_domain_event_outbox (status, next_retry_at, occurred_on);

CREATE TABLE IF NOT EXISTS ddd_integration_event_outbox (
    id              VARCHAR(36) PRIMARY KEY,
    event_type      VARCHAR(500) NOT NULL,
    payload         TEXT NOT NULL,
    occurred_on     TIMESTAMP WITH TIME ZONE NOT NULL,
    source_event_id VARCHAR(36) NULL,
    attempts        INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP WITH TIME ZONE NULL,
    status          SMALLINT NOT NULL DEFAULT 0,
    last_error      TEXT NULL,
    version         INT NOT NULL DEFAULT 0,
    create_at       TIMESTAMP,
    update_at       TIMESTAMP,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    is_delete       BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_integration_event_outbox_due
    ON ddd_integration_event_outbox (status, next_retry_at, occurred_on);
