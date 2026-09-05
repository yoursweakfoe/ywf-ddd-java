-- ============================================================================
-- ID 策略说明：
--   orders   — UUID（应用侧工厂铸造：RFC 9562 UUIDv7，时间有序）
--   products — UUID（应用侧工厂铸造：同上；两类聚合身份策略已统一）
-- create_at / update_at 无默认值：由 AuditFieldFiller 在应用层填充（非 DB 触发器）。
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
