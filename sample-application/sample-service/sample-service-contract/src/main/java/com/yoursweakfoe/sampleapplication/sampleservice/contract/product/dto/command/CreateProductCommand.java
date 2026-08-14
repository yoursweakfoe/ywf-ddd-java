package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command;

import com.yoursweakfoe.common.contract.dto.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;
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

    /** 商品名称 */
    @NotBlank
    @Schema(description = "商品名称")
    private String name;

    /** 初始库存 */
    @Min(0)
    @Schema(description = "初始库存")
    private int stock;
}
