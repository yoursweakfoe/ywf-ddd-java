package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto;

import com.yoursweakfoe.common.contract.Command;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鍒涘缓鍟嗗搧鍛戒护銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductCommand implements Command, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鍟嗗搧鍚嶇О */
    private String name;

    /** 鍒濆搴撳瓨 */
    private int stock;
}
