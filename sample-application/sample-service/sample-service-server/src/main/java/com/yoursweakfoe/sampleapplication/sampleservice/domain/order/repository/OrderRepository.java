package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.domain.repository.domain.Repository;
import java.util.UUID;

/**
 * 订单仓储接口（领域层契约）—— 仅聚合生命周期（写侧）。
 *
 * <p>读侧已独立为 {@code OrderQueryRepository}（application 查询端口 + infra 实现），
 * 读路径绕过 domain（PO → DTO 直接投影），不经过本接口。
 */
public interface OrderRepository extends Repository<Order, UUID> {
}
