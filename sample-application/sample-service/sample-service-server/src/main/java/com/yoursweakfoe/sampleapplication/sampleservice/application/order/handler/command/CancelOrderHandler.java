package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.Order;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.repository.domain.OrderRepository;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.shared.service.InventoryDomainService;
import com.yoursweakfoe.common.ddd.application.handler.command.CommandHandler;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 取消订单 —— 库存回补经 {@code InventoryDomainService} 同事务直调。
 *
 * <p>取消与回补同生共死（本地 ACID）：回补失败（如乐观锁冲突）异常上抛、
 * 整体回滚，客户端重试从干净状态重来。
 */
@Slf4j
@Component
public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, Void> {

    // region 依赖注入
    private final OrderRepository orderRepository;
    private final InventoryDomainService inventoryDomainService;

    public CancelOrderHandler(OrderRepository orderRepository,
                              InventoryDomainService inventoryDomainService) {
        this.orderRepository = orderRepository;
        this.inventoryDomainService = inventoryDomainService;
    }
    // endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void handle(CancelOrderCommand command) {
        Order order = orderRepository.findById(UUID.fromString(command.getOrderId()))
                .orElseThrow(() -> new BusinessException("order:err.notFound"));
        order.cancel(command.getReason());
        orderRepository.update(order);
        // 同事务补偿：与下单扣库存（PlaceOrderHandler 直调 deductStock）对称
        inventoryDomainService.replenishStock(order.getItems());
        log.info("Order cancelled with stock replenished: orderId={}", order.getId());
        return null;
    }
}
