package com.yoursweakfoe.common.ddd.fixtures.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("products.products")
public class ProductPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer stock;
    @Version
    private Integer version;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
}
