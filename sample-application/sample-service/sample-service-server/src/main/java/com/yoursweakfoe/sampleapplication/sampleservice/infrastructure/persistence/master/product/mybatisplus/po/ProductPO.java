package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatisplus.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * 商品持久化对象。
 */
@Data
@TableName("products.products")
public class ProductPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer stock;

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
