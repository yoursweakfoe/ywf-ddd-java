--liquibase formatted sql
-- ============================================================================
-- ddd_sample_application 初始 schema（消费方：sample-application / sample-service-server）
--
-- schema 归属与命名规范（2026-09 裁决定案）：
--   业务侧：聚合边界 = schema 边界，且领域词单数——领域是概念，概念没有复数
--   （产品域 = product，不是 products）。与 SQL 保留字冲突时，升级到更精确的
--   行业通用术语（订单域在电商语境的 UB 即销售订单 sales_order，SAP/Magento 同），
--   不用引号（"order" 每条手写 SQL 交税）、不用前缀（词汇被语法绑架）。
--   schema 名与 Java 包名单数惯例逐字同构（domain.product ↔ product.product），
--   微服务拆分时整 schema 平移。真要隔离 = 直接分库，分出的库结构同构。
--   工具侧：Liquibase 账表住独立 schema `liquibase`（bootstrap bean 幂等自建 +
--   spring.liquibase.liquibase-schema 指向，见 application.yml），不与业务对象混列。
--
-- 形状权威规范（2026-09 用户模板裁定）：本文件是 PG 原生形状，不再对齐 H2 测试旧形：
--   id       PG 原生 uuid，DEFAULT uuidv7()（PG18 内建；应用侧工厂显式铸造时传值覆盖，
--            DB 默认只兜手工插入）；
--   审计     created_at/updated_at TIMESTAMPTZ DEFAULT now() NOT NULL——时间戳可供
--            增量同步/按操作时间排查，有日志框架也保留；created_by/updated_by uuid；
--   软删     is_deleted BOOLEAN（软删体现在 update 中，不设 delete_at）；
--   乐观锁   version BIGINT DEFAULT 0，应用层更新时校验并自增。
--   H2 测试脚本（sample-service/.../test/resources/schema.sql）仍是旧形状旧命名，
--   属 sample 接入工程的成对同步项（H2 无 uuidv7()，届时测试侧需自带兼容形态）。
-- ============================================================================

--changeset ywf:0001-init-sample-schema
--comment: 销售订单/产品双聚合 schema + 聚合根表（PG 原生形状）+ 高频查询索引
CREATE SCHEMA IF NOT EXISTS sales_order;
CREATE SCHEMA IF NOT EXISTS product;

CREATE TABLE IF NOT EXISTS sales_order.sales_order (
    id              UUID            PRIMARY KEY DEFAULT uuidv7(),
    status          VARCHAR(20)     NOT NULL,                            -- 订单状态机（7 态）
    items           TEXT,                                                -- 订单行 JSON 快照：应用侧按字符串读写，DB 不解析
    total_amount    NUMERIC(10,2),
    customer_id     VARCHAR(50),
    tracking_number VARCHAR(100),
    cancel_reason   VARCHAR(500),
    created_at      TIMESTAMPTZ     DEFAULT now() NOT NULL,
    created_by      UUID,
    updated_at      TIMESTAMPTZ     DEFAULT now() NOT NULL,
    updated_by      UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,              -- 软删：读写侧一律以 is_deleted = FALSE 过滤
    version         BIGINT          NOT NULL DEFAULT 0                   -- 乐观锁：更新时校验并自增
);

CREATE TABLE IF NOT EXISTS product.product (
    id         UUID           PRIMARY KEY DEFAULT uuidv7(),
    name       VARCHAR(100)   NOT NULL,
    price      NUMERIC(10,2)  NOT NULL,
    stock      INTEGER        NOT NULL,
    created_at TIMESTAMPTZ    DEFAULT now() NOT NULL,
    created_by UUID,
    updated_at TIMESTAMPTZ    DEFAULT now() NOT NULL,
    updated_by UUID,
    is_deleted BOOLEAN        NOT NULL DEFAULT FALSE,
    version    BIGINT         NOT NULL DEFAULT 0
);

-- 高频查询索引（idx_<表>_<列> 惯例，随表单数同步更名）
CREATE INDEX IF NOT EXISTS idx_sales_order_customer_id ON sales_order.sales_order (customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_order_status      ON sales_order.sales_order (status);
CREATE INDEX IF NOT EXISTS idx_product_name            ON product.product (name);

COMMENT ON SCHEMA sales_order IS '销售订单聚合命名空间：承载 sales_order.sales_order 聚合根表';
COMMENT ON SCHEMA product     IS '产品聚合命名空间：承载 product.product 聚合根表';
COMMENT ON TABLE  sales_order.sales_order IS '销售订单聚合根表（示例教例：7 态状态机 + 乐观锁 + 软删）';
COMMENT ON TABLE  product.product         IS '产品聚合根表（示例教例：库存增减乐观锁防超卖）';
COMMENT ON COLUMN sales_order.sales_order.items   IS '订单行 JSON 快照列：DB 侧不解析，序列化契约归应用';
COMMENT ON COLUMN sales_order.sales_order.version IS '乐观锁版本号，应用层更新时校验并自增，防并发写覆盖';
COMMENT ON COLUMN product.product.version         IS '乐观锁版本号，扣库存防超卖';

--rollback DROP SCHEMA IF EXISTS sales_order CASCADE;
--rollback DROP SCHEMA IF EXISTS product CASCADE;
