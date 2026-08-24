package com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto;

import com.yoursweakfoe.common.ddd.application.dto.ApplicationDTO;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品写侧内部视图 —— 写操作用例（CommandHandler）执行后，聚合根状态的完整投影。
 *
 * <p>与读侧 {@link ProductViewDTO} 解耦：写侧 DTO 承载乐观锁版本（version），
 * 供调用方感知写后状态；读侧 DTO 是纯读投影，不携带写侧关注点。
 * 二者不得互相复用，避免"一个肥 DTO 贯穿所有层"的耦合。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements ApplicationDTO, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private Long id;
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
    /** 乐观锁版本（写侧关注点，读侧不承载） */
    private Integer version;
}
