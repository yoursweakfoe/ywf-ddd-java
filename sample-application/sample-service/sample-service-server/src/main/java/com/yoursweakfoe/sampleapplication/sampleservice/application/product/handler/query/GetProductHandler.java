package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.query;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler.ProductAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.query.GetProductQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.common.ddd.application.handler.QueryHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import org.springframework.stereotype.Component;

/** 查询商品详情。 */
@Component
public class GetProductHandler implements QueryHandler<GetProductQuery, ProductViewDTO> {

    // region 依赖注入
    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;

    public GetProductHandler(ProductRepository productRepository,
                             ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.productAssembler = productAssembler;
    }
    // endregion

    @Override
    public ProductViewDTO handle(GetProductQuery query) {
        Product product = productRepository.findById(query.getProductId())
                .orElseThrow(() -> new BusinessException("product:err.notFound"));
        return productAssembler.toDTO(product);
    }
}
