package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CompleteOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 完成订单。 */
@Slf4j
@Component
public class CompleteOrderHandler implements CommandHandler<CompleteOrderCommand, OrderDTO> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public CompleteOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(CompleteOrderCommand command) {
        // 入口一点定型（WC-13，法卷锚 BP-13／案卷 2026-09-typed-identifier）：CQE 裸 UUID → 币种，其后链路全为 OrderId
        OrderId orderId = OrderId.of(command.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.complete();
        orderRepository.update(order);
        log.info("Order completed: orderId={}", order.getId());
        return orderAssembler.toDTO(order);
    }
}
