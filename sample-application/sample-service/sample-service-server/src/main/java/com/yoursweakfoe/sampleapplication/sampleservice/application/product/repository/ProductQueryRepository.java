package com.yoursweakfoe.sampleapplication.sampleservice.application.product.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.dto.ProductViewDTO;
import java.util.Optional;

/**
 * 商品读侧查询端口 —— 读路径绕过 domain，PO → DTO 直接投影。
 *
 * <p>CQRS 读侧：本接口是 application 层的查询端口，基础设施层实现（{@code ProductQueryRepositoryImpl}），
 * 直接由 PO 投影为读 DTO {@link ProductViewDTO}，不经过领域聚合根、不建领域读模型。
 * 与写侧 {@code ProductRepository}（domain 层，聚合生命周期）分离，互不耦合。
 */
public interface ProductQueryRepository {

    /** 按 ID 投影商品读 DTO（不存在返回 empty）。 */
    Optional<ProductViewDTO> findById(Long id);
}
