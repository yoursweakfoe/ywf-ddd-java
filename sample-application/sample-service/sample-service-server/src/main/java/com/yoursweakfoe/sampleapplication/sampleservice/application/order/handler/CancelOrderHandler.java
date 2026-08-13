package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.OrderRepository;
import com.yoursweakfoe.common.ddd.application.cqrs.command.CommandHandler;
import com.yoursweakfoe.common.exception.BusinessException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 取消订单（库存回补由 EventHandler 处理）。 */
@Component
public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, Void> {

    // region 依赖注入
    private final OrderRepository orderRepository;

    public CancelOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(UUID.fromString(command.getOrderId()))
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.cancel(command.getReason());
        orderRepository.update(order);
        return null;
    }
}
