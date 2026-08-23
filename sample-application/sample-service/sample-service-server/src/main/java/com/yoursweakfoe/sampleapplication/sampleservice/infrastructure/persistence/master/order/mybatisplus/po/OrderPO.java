package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.order.mybatisplus.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 订单持久化对象。
 */
@Data
@TableName("orders.orders")
public class OrderPO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String status;

    /** JSON 序列化的订单项列表 */
    private String items;

    private BigDecimal totalAmount;

    private String customerId;

    private String trackingNumber;

    private String cancelReason;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableLogic
    private Boolean isDelete;
}
