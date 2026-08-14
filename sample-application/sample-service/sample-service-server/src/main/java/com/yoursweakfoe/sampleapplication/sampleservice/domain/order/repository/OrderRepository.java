package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository;

import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.common.ddd.domain.repository.Repository;
import java.util.UUID;

/**
 * 订单仓储接口（领域层契约）。
 */
public interface OrderRepository extends Repository<Order, UUID> {

    /**
     * 分页查询订单。
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页大小
     * @return 分页结果
     */
    PageResult<Order> findPage(int pageNum, int pageSize);
}
