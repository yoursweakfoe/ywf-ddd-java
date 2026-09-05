package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query;

import com.yoursweakfoe.common.contract.dto.query.PageableQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 订单分页查询。
 *
 * @param status     订单状态过滤（可选，null 表示不过滤；值域=OrderStatus，非法字面量经
 *                   Spring binding 直接 400 typeMismatch——显式失败优于静默空页）
 * @param customerId 客户 ID 过滤（可选）
 * @param pageNum    页码（从 1 开始，默认 1）
 * @param pageSize   每页大小（默认 20，最大 1000）
 */
public record GetOrderPageQuery(
        @Schema(description = "订单状态过滤（可选，值域=OrderStatus；非法值 400）") OrderStatus status,
        @Schema(description = "客户 ID 过滤（可选）") String customerId,
        @Min(1) @Schema(description = "页码（从 1 开始，默认 1）") int pageNum,
        @Min(1) @Max(PageableQuery.MAX_PAGE_SIZE) @Schema(description = "每页大小（默认 20，最大 1000）") int pageSize
) implements PageableQuery {
}
