package com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderItem;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.Product;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.domain.ProductRepository;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.List;
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
        Product product = new Product(1L, "Widget", BigDecimal.TEN, 10);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        service.deductStock(List.of(new OrderItem(1L, 3, BigDecimal.TEN)));

        assertThat(product.getStock()).isEqualTo(7);
        verify(productRepository).update(product);
    }

    @Test
    void deductStock_shouldThrowWhenProductNotFound() {
        when(productRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.deductStock(List.of(new OrderItem(99L, 1, BigDecimal.TEN))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("product:err.notFound");
    }

    @Test
    void deductStock_shouldThrowWhenInsufficientStock() {
        Product product = new Product(1L, "Widget", BigDecimal.TEN, 2);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> service.deductStock(List.of(new OrderItem(1L, 5, BigDecimal.TEN))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void replenishStock_shouldRestoreStock() {
        Product product = new Product(1L, "Widget", BigDecimal.TEN, 5);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        service.replenishStock(List.of(new OrderItem(1L, 3, BigDecimal.TEN)));

        assertThat(product.getStock()).isEqualTo(8);
        verify(productRepository).update(product);
    }

    @Test
    void deductStock_multipleItems() {
        Product p1 = new Product(1L, "A", BigDecimal.TEN, 10);
        Product p2 = new Product(2L, "B", BigDecimal.ONE, 20);
        when(productRepository.findAllById(any())).thenReturn(List.of(p1, p2));

        service.deductStock(List.of(
                new OrderItem(1L, 2, BigDecimal.TEN),
                new OrderItem(2L, 5, BigDecimal.ONE)));

        assertThat(p1.getStock()).isEqualTo(8);
        assertThat(p2.getStock()).isEqualTo(15);
    }

    @Test
    void deductStock_shouldMergeDuplicateProductItems() {
        // 同一商品出现在多个订单项：数量合并为一次聚合行为 + 一次持久化
        // （避免对同一聚合连续两次乐观锁 UPDATE 导致版本号踩空）
        Product p1 = new Product(1L, "A", BigDecimal.TEN, 10);
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(p1));

        service.deductStock(List.of(
                new OrderItem(1L, 2, BigDecimal.TEN),
                new OrderItem(1L, 3, BigDecimal.TEN)));

        assertThat(p1.getStock()).isEqualTo(5);
        verify(productRepository, org.mockito.Mockito.times(1)).update(p1);
    }
}
