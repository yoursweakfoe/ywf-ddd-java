package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto;

import com.yoursweakfoe.common.contract.Command;
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
    private String name;

    /** 初始库存 */
    @Min(0)
    private int stock;
}
