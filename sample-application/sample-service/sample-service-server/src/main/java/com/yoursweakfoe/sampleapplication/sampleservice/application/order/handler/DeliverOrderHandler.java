package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.DeliverOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.ddd.application.cqrs.command.CommandHandler;
import com.yoursweakfoe.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 签收。 */
@Component
public class DeliverOrderHandler implements CommandHandler<DeliverOrderCommand, OrderDTO> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public DeliverOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(DeliverOrderCommand command) {
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.deliver();
        orderRepository.update(order);
        return orderAssembler.toDTO(order);
    }
}
