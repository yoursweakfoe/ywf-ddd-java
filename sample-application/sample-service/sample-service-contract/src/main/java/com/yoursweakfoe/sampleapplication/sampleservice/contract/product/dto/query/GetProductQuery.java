package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.query;

import com.yoursweakfoe.common.contract.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 查询商品详情。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetProductQuery implements Query, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    @NotNull
    @Schema(description = "商品 ID")
    private Long productId;
}
