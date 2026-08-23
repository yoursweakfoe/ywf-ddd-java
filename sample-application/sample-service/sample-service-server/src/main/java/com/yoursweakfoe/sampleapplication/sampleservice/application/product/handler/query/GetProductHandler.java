package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.query;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.repository.application.ProductQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.query.GetProductQuery;
import com.yoursweakfoe.common.ddd.application.handler.query.QueryHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import org.springframework.stereotype.Component;

/** 查询商品详情 —— 读侧绕过 domain，PO → DTO 直接投影。 */
@Component
public class GetProductHandler implements QueryHandler<GetProductQuery, ProductViewDTO> {

    private final ProductQueryRepository productQueryRepository;

    public GetProductHandler(ProductQueryRepository productQueryRepository) {
        this.productQueryRepository = productQueryRepository;
    }

    @Override
    public ProductViewDTO handle(GetProductQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 投影，不 reconstitute 聚合根
        return productQueryRepository.findById(query.getProductId())
                .orElseThrow(() -> new BusinessException("product:err.notFound"));
    }
}
