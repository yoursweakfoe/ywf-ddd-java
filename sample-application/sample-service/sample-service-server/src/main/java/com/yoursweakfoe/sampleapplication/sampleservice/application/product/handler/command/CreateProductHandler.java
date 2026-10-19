package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler.ProductAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.ProductFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
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
    private final ProductFactory productFactory;
    private final ProductAssembler productAssembler;

    public CreateProductHandler(ProductRepository productRepository,
                                ProductFactory productFactory,
                                ProductAssembler productAssembler) {
        this.productRepository = productRepository;
        this.productFactory = productFactory;
        this.productAssembler = productAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO handle(CreateProductCommand command) {
        // 创建即合法：工厂铸造 UUIDv7 身份并完成不变量校验（audit B-01 收口：
        // 自增反查路径消亡——id 在持久化之前即存在，无需按名回查）
        Product product = productFactory.create(command.getName(), command.getPrice(), command.getStock());
        productRepository.save(product);

        log.info("Product created: productId={}", product.getId());
        return productAssembler.toDTO(product);
    }
}
