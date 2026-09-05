package com.yoursweakfoe.common.ddd.fixtures.po;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/** 订单持久化对象测试夹具 —— 纯 POJO，表名 / 主键 / 逻辑删除语义全部由手写 XML 承载。 */
@Data
public class OrderPO {

    private String id;
    private String status;
    private String items;
    private BigDecimal totalAmount;
    private String customerId;
    private OffsetDateTime createAt;
    private OffsetDateTime updateAt;
    private Boolean deleted;
}
