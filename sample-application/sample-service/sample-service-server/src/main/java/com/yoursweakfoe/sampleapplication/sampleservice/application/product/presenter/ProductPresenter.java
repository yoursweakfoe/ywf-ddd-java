package com.yoursweakfoe.sampleapplication.sampleservice.application.product.presenter;

import com.yoursweakfoe.common.ddd.application.presenter.BasicPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import org.springframework.stereotype.Component;

/**
 * 商品 Presenter —— 内部 DTO → CO 单向呈现（契约输出清洗），纯手写显式映射。
 *
 * <p>决定外部消费方看到什么：审计字段（createAt/updateAt）、乐观锁版本（version）
 * 不映射即不暴露。presentList 由 {@code BasicPresenter} default 实现提供。
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
