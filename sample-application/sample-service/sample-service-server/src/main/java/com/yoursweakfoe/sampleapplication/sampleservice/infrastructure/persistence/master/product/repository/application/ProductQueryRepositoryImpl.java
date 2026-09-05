package com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.repository.application;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.repository.application.ProductQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.mapper.ProductMapper;
import com.yoursweakfoe.sampleapplication.sampleservice.infrastructure.persistence.master.product.mybatis.po.ProductPO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 商品读侧查询实现 —— PO → 读 DTO 直接投影（绕过 domain）。
 *
 * <p>读侧（CQRS 查询）不经过领域聚合根：直接用 Mapper 查询 PO，逐字段投影为读 DTO
 * {@link ProductViewDTO}。业务规则不在读侧计算。
 */
@Component
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

    private final ProductMapper productMapper;

    public ProductQueryRepositoryImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public Optional<ProductViewDTO> findById(UUID id) {
        ProductPO po = productMapper.selectById(id.toString());
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toViewDTO(po));
    }

    /** PO → 读 DTO 直接投影（不经过 domain）。 */
    private ProductViewDTO toViewDTO(ProductPO po) {
        ProductViewDTO dto = new ProductViewDTO();
        dto.setId(UUID.fromString(po.getId()));
        dto.setName(po.getName());
        dto.setPrice(po.getPrice());
        dto.setStock(po.getStock() == null ? 0 : po.getStock());
        dto.setCreateAt(po.getCreateAt());
        dto.setUpdateAt(po.getUpdateAt());
        return dto;
    }
}
