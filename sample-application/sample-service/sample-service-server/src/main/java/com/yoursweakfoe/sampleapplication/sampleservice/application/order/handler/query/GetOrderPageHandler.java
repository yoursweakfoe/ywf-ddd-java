package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import com.yoursweakfoe.common.ddd.application.handler.QueryHandler;
import com.yoursweakfoe.common.contract.dto.query.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.repository.query.OrderQueryRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import org.springframework.stereotype.Component;

/**
 * 订单分页查询 —— 读侧绕过 domain，PO → DTO 直接投影。
 *
 * <p>Handler 产出 {@link OrderViewDTO}（读 DTO），AppService 经读侧 Presenter 裁剪为
 * {@link com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderSummaryCO}（列表），
 * 区别于 {@link com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO}（详情）。
 */
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {

    private final OrderQueryRepository orderQueryRepository;

    public GetOrderPageHandler(OrderQueryRepository orderQueryRepository) {
        this.orderQueryRepository = orderQueryRepository;
    }

    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        // 读侧绕过 domain：查询端口直接 PO → 读 DTO 分页投影，不 reconstitute 聚合
        return orderQueryRepository.findPage(
                query.status(), query.customerId(), query.pageNum(), query.pageSize());
    }
}
