package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatis.po;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * 订单持久化对象 —— 纯 POJO，零 ORM 注解。
 *
 * <p>表名（{@code sales_order.sales_order}）、主键策略（业务铸造、SQL 显式传参）、乐观锁版本条件、
 * 逻辑删除过滤全部由手写 XML（{@code resources/mapper/order/OrderMapper.xml}）的 SQL 文本承担，
 * 语句契约见框架 {@code DddMapper}；审计列（createdAt / updatedAt / createdBy / updatedBy）
 * 由 {@code AuditFieldFiller} 在持久化前显式填充。
 */
@Data
public class OrderPO {

    /** 身份由应用侧工厂铸造（UUIDv7），经 Converter 写入 —— INSERT 语句显式传参 */
    private UUID id;

    private String status;

    /** JSON 序列化的订单项列表 */
    private String items;

    private BigDecimal totalAmount;

    private String customerId;

    private String trackingNumber;

    private String cancelReason;

    /** 乐观锁版本 —— 领域层只读透传，版本条件由 UPDATE 语句文本携带 */
    private Long version;

    /** INSERT 时由 AuditFieldFiller 填充 */
    private OffsetDateTime createdAt;

    /** 每次 UPDATE / 逻辑删除均刷新 */
    private OffsetDateTime updatedAt;

    /** INSERT 时填充（容器中存在 CurrentUserProvider 实现才写） */
    private UUID createdBy;

    /** 每次 UPDATE / 逻辑删除时刷新（容器中存在 CurrentUserProvider 实现才写） */
    private UUID updatedBy;

    /** 逻辑删除标记（is_deleted 列：INSERT 不枚举靠 DB 默认 FALSE，删除语句置位） */
    private Boolean isDeleted;
}
