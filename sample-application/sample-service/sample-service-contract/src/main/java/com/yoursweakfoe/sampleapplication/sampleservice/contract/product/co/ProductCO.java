package com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co;


import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 鍟嗗搧 CO锛堝绾﹁緭鍑哄璞★級銆?*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 鍟嗗搧 ID */
    private Long id;

    /** 鍟嗗搧鍚嶇О */
    private String name;

    /** 搴撳瓨鏁伴噺 */
    private int stock;
}
