package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderViewDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.ddd.application.handler.QueryHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 查询订单详情。 */
@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderViewDTO> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public GetOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    public OrderViewDTO handle(GetOrderQuery query) {
        Order order = orderRepository.findById(UUID.fromString(query.getOrderId()))
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        return orderAssembler.toDTO(order);
    }
}
