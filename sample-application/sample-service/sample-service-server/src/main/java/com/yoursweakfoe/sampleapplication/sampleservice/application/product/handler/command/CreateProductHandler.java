package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler.ProductAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 创建商品。 */
@Slf4j
@Component
public class CreateProductHandler implements CommandHandler<CreateProductCommand, ProductDTO> {

    // region 依赖注入
    private final ProductRepository productRepository;
    private final ProductAssembler productAssembler;

    public CreateProductHandler(ProductRepository productRepository,
                                ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.productAssembler = productAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO handle(CreateProductCommand command) {
        Product product = new Product(null, command.getName(), command.getStock());
        productRepository.save(product);

        // 自增 ID 回填在 PO 上，重新查询获取完整实体
        Product saved = productRepository.findByName(command.getName())
                .orElseThrow(() -> new IllegalStateException("Product save failed"));
        log.info("Product created: productId={}", saved.getId());
        return productAssembler.toDTO(saved);
    }
}
