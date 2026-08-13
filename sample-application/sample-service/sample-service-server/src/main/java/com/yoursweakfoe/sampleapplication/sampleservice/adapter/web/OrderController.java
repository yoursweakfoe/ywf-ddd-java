package com.yoursweakfoe.sampleapplication.sampleservice.adapter.web;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.OrderAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.api.OrderService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.CompleteOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.ConfirmOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.DeliverOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.GetOrderQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.PayOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.ShipOrderCommand;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 端点 —— 纯透传到 {@link OrderAppService}。
 *
 * <p>HTTP 映射与文档注解声明在 {@link OrderService} 契约接口上（映射经接口继承），
 * 本 Controller 仅以 {@code @RestController} 标记协议并透传。
 */
@RestController
public class OrderController implements OrderService {

    private final OrderAppService orderAppService;

    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    @Override
    public OrderCO placeOrder(PlaceOrderCommand command) {
        return orderAppService.placeOrder(command);
    }

    @Override
    public OrderCO payOrder(UUID orderId) {
        return orderAppService.payOrder(new PayOrderCommand(orderId));
    }

    @Override
    public OrderCO confirmOrder(UUID orderId) {
        return orderAppService.confirmOrder(new ConfirmOrderCommand(orderId));
    }

    @Override
    public OrderCO shipOrder(UUID orderId, String trackingNumber) {
        return orderAppService.shipOrder(new ShipOrderCommand(orderId, trackingNumber));
    }

    @Override
    public OrderCO deliverOrder(UUID orderId) {
        return orderAppService.deliverOrder(new DeliverOrderCommand(orderId));
    }

    @Override
    public OrderCO completeOrder(UUID orderId) {
        return orderAppService.completeOrder(new CompleteOrderCommand(orderId));
    }

    @Override
    public void cancelOrder(UUID orderId, CancelOrderCommand command) {
        orderAppService.cancelOrder(command);
    }

    @Override
    public OrderCO getOrder(String orderId) {
        return orderAppService.getOrder(new GetOrderQuery(orderId));
    }
}
