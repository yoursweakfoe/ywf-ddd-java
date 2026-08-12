package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.config;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.repository.ProductRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务 Bean 注册配置。
 *
 * <p>将领域服务实例的创建与注册职责放在 infrastructure 层，
 * 使 domain 层保持零框架依赖（不使用 {@code @Service} 等 Spring 注解）。
 */
@Configuration
public class DomainServiceConfig {

    /**
     * 注册库存领域服务 Bean。
     *
     * @param productRepository 商品仓储
     * @return 库存领域服务实例
     */
    @Bean
    public InventoryDomainService inventoryDomainService(ProductRepository productRepository) {
        return new InventoryDomainService(productRepository);
    }
}
