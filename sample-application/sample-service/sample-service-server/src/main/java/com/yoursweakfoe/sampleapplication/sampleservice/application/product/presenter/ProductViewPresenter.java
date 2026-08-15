package com.yoursweakfoe.sampleapplication.sampleservice.application.product.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import org.springframework.stereotype.Component;

/**
 * 商品读侧 Presenter —— 读侧 {@link ProductViewDTO} → {@link ProductCO} 单向呈现（契约输出清洗）。
 *
 * <p>写/读 Presenter 解耦：写侧由 {@link ProductPresenter} 呈现写侧 DTO，读侧由本类呈现读侧 DTO。
 * 审计字段（createAt/updateAt）、乐观锁版本（version）不映射即不暴露。
 */
@Component
public class ProductViewPresenter implements BasicPresenter<ProductViewDTO, ProductCO> {

    @Override
    public ProductCO present(ProductViewDTO dto) {
        ProductCO co = new ProductCO();
        co.setId(dto.getId());
        co.setName(dto.getName());
        co.setStock(dto.getStock());
        // createAt / updateAt 为内部字段，不暴露给消费方
        return co;
    }
}
