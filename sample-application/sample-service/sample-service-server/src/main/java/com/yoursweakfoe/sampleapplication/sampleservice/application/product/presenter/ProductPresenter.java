package com.yoursweakfoe.sampleapplication.sampleservice.application.product.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import org.springframework.stereotype.Component;

/**
 * 商品写侧 Presenter —— 写侧 {@link ProductDTO} → {@link ProductCO} 单向呈现（契约输出清洗）。
 *
 * <p>写/读 Presenter 解耦：写侧由本类呈现 {@link ProductDTO}（含 version），读侧由
 * {@link ProductViewPresenter} 呈现 {@code ProductViewDTO}（不含 version）。
 * 审计字段（createAt/updateAt）、乐观锁版本（version）不映射即不暴露。
 */
@Component
public class ProductPresenter implements BasicPresenter<ProductDTO, ProductCO> {

    @Override
    public ProductCO present(ProductDTO dto) {
        ProductCO co = new ProductCO();
        co.setId(dto.getId());
        co.setName(dto.getName());
        co.setStock(dto.getStock());
        // createAt / updateAt / version 为内部字段，不暴露给消费方
        return co;
    }
}
