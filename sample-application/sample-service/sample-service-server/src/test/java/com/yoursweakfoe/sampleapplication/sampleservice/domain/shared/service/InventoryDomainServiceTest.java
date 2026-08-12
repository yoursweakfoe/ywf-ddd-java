package com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryDomainServiceTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private InventoryDomainService service;

    @Test
    void deductStock_shouldReduceStock() {
        Product product = new Product(1L, "Widget", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.deductStock(List.of(new OrderItem(1L, 3, BigDecimal.TEN)));

        assertThat(product.getStock()).isEqualTo(7);
        verify(productRepository).update(product);
    }

    @Test
    void deductStock_shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deductStock(List.of(new OrderItem(99L, 1, BigDecimal.TEN))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.notFound");
    }

    @Test
    void deductStock_shouldThrowWhenInsufficientStock() {
        Product product = new Product(1L, "Widget", 2);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.deductStock(List.of(new OrderItem(1L, 5, BigDecimal.TEN))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void replenishStock_shouldRestoreStock() {
        Product product = new Product(1L, "Widget", 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.replenishStock(List.of(new OrderItem(1L, 3, BigDecimal.TEN)));

        assertThat(product.getStock()).isEqualTo(8);
        verify(productRepository).update(product);
    }

    @Test
    void deductStock_multipleItems() {
        Product p1 = new Product(1L, "A", 10);
        Product p2 = new Product(2L, "B", 20);
        when(productRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(p2));

        service.deductStock(List.of(
                new OrderItem(1L, 2, BigDecimal.TEN),
                new OrderItem(2L, 5, BigDecimal.ONE)));

        assertThat(p1.getStock()).isEqualTo(8);
        assertThat(p2.getStock()).isEqualTo(15);
    }
}
