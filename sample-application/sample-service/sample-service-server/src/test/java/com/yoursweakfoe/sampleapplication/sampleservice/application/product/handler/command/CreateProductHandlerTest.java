package com.yoursweakfoe.sampleapplication.sampleservice.application.product.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.assembler.ProductAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateProductHandlerTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductAssembler productAssembler;
    @InjectMocks private CreateProductHandler handler;

    @Test
    void handle_shouldCreateAndReturnProduct() {
        Product saved = new Product(1L, "Widget", 50);
        when(productRepository.findByName("Widget")).thenReturn(Optional.of(saved));
        when(productAssembler.toDTO(any())).thenReturn(new ProductViewDTO());

        CreateProductCommand command = new CreateProductCommand();
        command.setName("Widget");
        command.setStock(50);

        ProductViewDTO result = handler.handle(command);

        verify(productRepository).save(any(Product.class));
        assertThat(result).isNotNull();
    }
}
