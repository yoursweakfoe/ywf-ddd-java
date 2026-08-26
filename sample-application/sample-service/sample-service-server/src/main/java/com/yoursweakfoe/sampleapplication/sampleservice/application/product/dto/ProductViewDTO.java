package com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto;

import com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品读侧内部视图 —— 读操作用例（QueryHandler）的投影结果。
 *
 * <p>与写侧 {@link ProductDTO} 解耦：读侧 DTO 是纯读投影，不承载乐观锁版本等写侧关注点。
 * 二者不得互相复用，避免"一个肥 DTO 贯穿所有层"的耦合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewDTO implements ApplicationDTO, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private UUID id;
    /** 商品名称 */
    private String name;
    /** 商品单价 */
    private BigDecimal price;
    /** 库存数量 */
    private int stock;
    /** 创建时间 */
    private OffsetDateTime createAt;
    /** 更新时间 */
    private OffsetDateTime updateAt;
}
