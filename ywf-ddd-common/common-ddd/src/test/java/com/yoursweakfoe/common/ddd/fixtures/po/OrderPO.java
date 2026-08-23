package com.yoursweakfoe.common.ddd.fixtures.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("orders.orders")
public class OrderPO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String status;
    private String items;
    private BigDecimal totalAmount;
    private String customerId;
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateAt;
    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
