package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import java.util.UUID;

/**
 * 订单仓储接口（领域层契约）。
 */
public interface OrderRepository extends Repository<Order, UUID> {
}
