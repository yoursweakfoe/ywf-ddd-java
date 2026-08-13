package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co;


import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 商品 CO（契约输出对象）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    @Schema(description = "商品 ID")
    private Long id;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String name;

    /** 库存数量 */
    @Schema(description = "库存数量")
    private int stock;
}
