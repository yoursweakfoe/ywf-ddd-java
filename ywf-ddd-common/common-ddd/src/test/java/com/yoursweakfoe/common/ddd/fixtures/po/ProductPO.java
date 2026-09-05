package com.yoursweakfoe.common.ddd.fixtures.po;

import java.time.OffsetDateTime;
import lombok.Data;

/** 商品持久化对象测试夹具 —— 纯 POJO，表名 / 主键 / 乐观锁 / 逻辑删除语义全部由手写 XML 承载。 */
@Data
public class ProductPO {

    private Long id;
    private String name;
    private Integer stock;
    private Integer version;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
    private Boolean deleted;
}
