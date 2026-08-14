package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import com.yoursweakfoe.common.ddd.application.handler.QueryHandler;
import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Component;

/**
 * 订单分页查询 —— 出路径多视图演示：同一 View → Presenter 裁剪为不同 CO。
 *
 * <p>Handler 产出 OrderViewDTO（全量内部视图），AppService 经 Presenter 裁剪为
 * {@link com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderSummaryCO}（列表），
 * 区别于 {@link com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO}（详情）。
 */
@Component
public class GetOrderPageHandler implements QueryHandler<GetOrderPageQuery, PageResult<OrderViewDTO>> {

    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public GetOrderPageHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }

    @Override
    public PageResult<OrderViewDTO> handle(GetOrderPageQuery query) {
        return orderRepository.findPage(query.getPageNum(), query.getPageSize())
                .map(orderAssembler::toDTO);
    }
}