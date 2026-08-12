package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto;

import com.yoursweakfoe.common.contract.PageableQuery;

/**
 * 订单分页查询。
 *
 * @param status   订单状态过滤（可选，null 表示不过滤）
 * @param customerId 客户 ID 过滤（可选）
 * @param pageNum  页码（从 1 开始，默认 1）
 * @param pageSize 每页大小（默认 20，最大 1000）
 */
public record GetOrderPageQuery(
        String status,
        String customerId,
        int pageNum,
        int pageSize
) implements PageableQuery {
}
