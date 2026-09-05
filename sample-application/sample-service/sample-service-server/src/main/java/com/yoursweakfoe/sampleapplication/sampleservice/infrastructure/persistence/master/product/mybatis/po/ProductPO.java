package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.po;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 商品持久化对象 —— 纯 POJO，零 ORM 注解。
 *
 * <p>表名（{@code products.products}）、主键策略（业务铸造、SQL 显式传参）、乐观锁版本条件、
 * 逻辑删除过滤全部由手写 XML（{@code resources/mapper/product/ProductMapper.xml}）的 SQL 文本承担，
 * 语句契约见框架 {@code DddMapper}；审计列（createAt / updateAt / createdBy / updatedBy）
 * 由 {@code AuditFieldFiller} 在持久化前显式填充。
 */
@Data
public class ProductPO {

    /** 身份由应用侧工厂铸造（UUIDv7），经 Converter toString 写入 —— INSERT 语句显式传参 */
    private String id;

    private String name;

    /** 商品单价 */
    private BigDecimal price;

    private Integer stock;

    /** 乐观锁版本 —— 领域层只读透传，版本条件由 UPDATE 语句文本携带（防超卖关键） */
    private Integer version;

    /** INSERT 时由 AuditFieldFiller 填充 */
    private OffsetDateTime createAt;

    /** 每次 UPDATE / 逻辑删除均刷新 */
    private OffsetDateTime updateAt;

    /** INSERT 时填充（容器中存在 CurrentUserProvider 实现才写） */
    private String createdBy;

    /** 每次 UPDATE / 逻辑删除时刷新（容器中存在 CurrentUserProvider 实现才写） */
    private String updatedBy;

    /** 逻辑删除标记（is_delete 列：INSERT 不枚举靠 DB 默认 FALSE，删除语句置位） */
    private Boolean isDelete;
}
