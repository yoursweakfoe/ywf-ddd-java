package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command;

import com.yoursweakfoe.common.contract.dto.command.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 创建商品命令。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品名称（上界对齐 product.product.name VARCHAR(100)） */
    @NotBlank
    @Size(max = 100)
    @Schema(description = "商品名称（≤100 字符）")
    private String name;

    /** 商品单价（精度对齐 DECIMAL(10,2)：整数位 ≤8、小数位 ≤2） */
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    @Schema(description = "商品单价（正数，≤99999999.99）")
    private BigDecimal price;

    /** 初始库存 */
    @Min(0)
    @Schema(description = "初始库存")
    private int stock;
}
