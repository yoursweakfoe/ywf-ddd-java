package com.yoursweakfoe.sampleapplication.sampleservice.adapter.rest;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.OrderAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderSummaryCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CompleteOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ConfirmOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.DeliverOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PayOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ShipOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.adapter.rest.OrderController;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 端点 —— 纯透传到 {@link OrderAppService}。
 *
 * <p>HTTP 映射与文档注解声明在 {@link OrderController} 契约接口上（映射经接口继承），
 * 本实现类仅以 {@code @RestController} 标记协议并透传。
 */
@RestController
public class OrderControllerImpl implements OrderController {

    private final OrderAppService orderAppService;

    public OrderControllerImpl(OrderAppService orderAppService) {
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

    @Override
    public List<OrderSummaryCO> getOrderPage(GetOrderPageQuery query) {
        return orderAppService.getOrderPage(query).records();
    }
}
