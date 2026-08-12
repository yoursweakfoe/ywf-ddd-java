package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto;

import com.yoursweakfoe.common.contract.Query;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鏌ヨ鍟嗗搧璇︽儏銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetProductQuery implements Query, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鍟嗗搧 ID */
    private Long productId;
}
