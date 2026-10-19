package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;

/**
 * 订单仓储接口（领域层契约）—— 仅聚合生命周期（写侧）。
 *
 * <p>ID 槽与根身份槽同为专属币种 {@link OrderId}（R15 锁死，法卷锚 BP-13，
 * 案卷 2026-09-typed-identifier）。
 *
 * <p>读侧已独立为 {@code OrderQueryRepository}（application 查询端口 + infra 实现），
 * 读路径绕过 domain（PO → DTO 直接投影），不经过本接口；读端口维持原生值承载（法卷锚 BP-15）。
 */
public interface OrderRepository extends Repository<Order, OrderId> {
}
