package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository;

import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.OrderReadView;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单仓储接口（领域层契约）。
 */
public interface OrderRepository extends Repository<Order, UUID> {

    /**
     * 按 ID 直接投影读模型（读侧绕过聚合根，不 reconstitute）。
     *
     * @param id 订单 ID
     * @return 订单读模型，不存在时返回 empty
     */
    Optional<OrderReadView> findReadView(UUID id);

    /**
     * 分页投影读模型（读侧绕过聚合根，不 reconstitute）。
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分页读模型
     */
    PageResult<OrderReadView> findReadViewPage(int pageNum, int pageSize);
}
