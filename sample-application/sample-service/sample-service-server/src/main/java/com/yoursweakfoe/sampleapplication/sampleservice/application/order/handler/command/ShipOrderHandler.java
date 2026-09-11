package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.assembler.OrderAssembler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ShipOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.id.OrderId;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 发货。 */
@Slf4j
@Component
public class ShipOrderHandler implements CommandHandler<ShipOrderCommand, OrderDTO> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final OrderAssembler orderAssembler;

    public ShipOrderHandler(OrderRepository orderRepository, OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderAssembler = orderAssembler;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDTO handle(ShipOrderCommand command) {
        // 入口一点定型（WC-13，法卷锚 BP-13／案卷 2026-09-typed-identifier）：CQE 裸 UUID → 币种，其后链路全为 OrderId
        OrderId orderId = OrderId.of(command.getOrderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.ship(command.getTrackingNumber());
        orderRepository.update(order);
        log.info("Order shipped: orderId={}, trackingNumber={}", order.getId(), command.getTrackingNumber());
        return orderAssembler.toDTO(order);
    }
}
