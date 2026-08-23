package com.yoursweakfoe.sampleapplication.sampleservice.application.product.service;

import com.yoursweakfoe.common.ddd.application.service.ApplicationService;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.command.CreateProductHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.query.GetProductHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.presenter.ProductPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.presenter.ProductViewPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.query.GetProductQuery;
import org.springframework.stereotype.Service;

/**
 * 商品应用服务 —— 聚合协调入口。
 *
 * <p>所有用例委托 Handler 执行（返回 DTO），本类仅做 DTO → CO 呈现。
 *
 * <p>实现 {@link ApplicationService} 标记接口定型「应用服务」角色（ArchUnit 守护）。
 */
@Service
public class ProductAppService implements ApplicationService {

    // region 依赖注入
    private final ProductPresenter productPresenter;
    private final ProductViewPresenter productViewPresenter;
    private final CreateProductHandler createProductHandler;
    private final GetProductHandler getProductHandler;

    public ProductAppService(ProductPresenter productPresenter,
                             ProductViewPresenter productViewPresenter,
                             CreateProductHandler createProductHandler,
                             GetProductHandler getProductHandler) {
        this.productPresenter = productPresenter;
        this.productViewPresenter = productViewPresenter;
        this.createProductHandler = createProductHandler;
        this.getProductHandler = getProductHandler;
    }
    // endregion

    public ProductCO createProduct(CreateProductCommand command) {
        return productPresenter.present(createProductHandler.handle(command));
    }

    public ProductCO getProduct(GetProductQuery query) {
        return productViewPresenter.present(getProductHandler.handle(query));
    }
}
