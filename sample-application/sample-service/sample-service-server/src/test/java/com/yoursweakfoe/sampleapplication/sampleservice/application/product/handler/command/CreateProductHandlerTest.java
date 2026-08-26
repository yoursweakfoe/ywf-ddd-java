package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.uuid.Generators;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler.ProductAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.ProductFactory;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 创建商品处理器测试 —— 工厂铸造身份 + 仓储落库 + 组装返回。
 *
 * <p>B-01 收口后：Handler 不再按名反查（findByName 已移除），
 * 工厂在构造期即铸好 UUIDv7 身份，save 后直接可用。
 */
@ExtendWith(MockitoExtension.class)
class CreateProductHandlerTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductAssembler productAssembler;
    /** 真实工厂实例（@Spy）：创建逻辑无外部依赖，直接复用生产实现 */
    @Spy
    private ProductFactory productFactory = new ProductFactory();
    @InjectMocks private CreateProductHandler handler;

    @Test
    void handle_shouldCreateViaFactoryAndSave() {
        var dto = new ProductDTO();
        dto.setId(UUID.randomUUID());
        when(productAssembler.toDTO(any())).thenReturn(dto);

        CreateProductCommand command = new CreateProductCommand();
        command.setName("Widget");
        command.setPrice(BigDecimal.TEN);
        command.setStock(50);

        ProductDTO result = handler.handle(command);

        // 工厂被调用且商品落库
        verify(productFactory).create("Widget", BigDecimal.TEN, 50);
        verify(productRepository).save(any(Product.class));
        assertThat(result).isNotNull();
    }

    @Test
    void handle_createdProductId_isUuidV7() {
        // 用 JUG 生成真正的 v7 UUID 作为测试数据（与生产生成器同版本）
        var dto = new ProductDTO();
        dto.setId(Generators.timeBasedEpochGenerator().generate());
        when(productAssembler.toDTO(any())).thenReturn(dto);

        var result = handler.handle(command());

        // 应用侧 UUIDv7：id 在持久化前即存在，无需反查
        assertThat(result.getId()).isNotNull();
        assertThat(result.getId().version()).isEqualTo(7);
    }

    private CreateProductCommand command() {
        CreateProductCommand command = new CreateProductCommand();
        command.setName("Widget");
        command.setPrice(BigDecimal.TEN);
        command.setStock(50);
        return command;
    }
}
