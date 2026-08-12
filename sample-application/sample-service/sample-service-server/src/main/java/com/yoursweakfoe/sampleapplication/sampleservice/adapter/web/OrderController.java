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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 REST 端点 —— 纯透传到 {@link OrderAppService}。
 *
 * <p>对外 REST 面以本 Controller 为准（spring-web 原生注解显式声明路径），
 * 实现 {@link OrderService} 契约接口以保持方法签名与契约一致。
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "订单服务", description = "订单生命周期管理：下单、支付、发货、签收、完成、取消")
public class OrderController implements OrderService {

    // region 依赖注入
    private final OrderAppService orderAppService;

    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }
    // endregion

    @Override
    @PostMapping("")
    @Operation(summary = "下单", description = "创建新订单，初始状态为 PENDING")
    public OrderCO placeOrder(@RequestBody PlaceOrderCommand command) {
        return orderAppService.placeOrder(command);
    }

    @Override
    @PutMapping("/{orderId}/pay")
    @Operation(summary = "支付订单", description = "将 PENDING 订单标记为已支付")
    public OrderCO payOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId) {
        return orderAppService.payOrder(new PayOrderCommand(orderId));
    }

    @Override
    @PutMapping("/{orderId}/confirm")
    @Operation(summary = "确认订单", description = "商家确认已支付订单")
    public OrderCO confirmOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId) {
        return orderAppService.confirmOrder(new ConfirmOrderCommand(orderId));
    }

    @Override
    @PutMapping("/{orderId}/ship")
    @Operation(summary = "发货", description = "商家发货并填写物流单号")
    public OrderCO shipOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId,
            @RequestParam("trackingNumber") @Parameter(description = "物流单号") String trackingNumber) {
        return orderAppService.shipOrder(new ShipOrderCommand(orderId, trackingNumber));
    }

    @Override
    @PutMapping("/{orderId}/deliver")
    @Operation(summary = "签收", description = "买家确认签收")
    public OrderCO deliverOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId) {
        return orderAppService.deliverOrder(new DeliverOrderCommand(orderId));
    }

    @Override
    @PutMapping("/{orderId}/complete")
    @Operation(summary = "完成订单", description = "订单最终完成")
    public OrderCO completeOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId) {
        return orderAppService.completeOrder(new CompleteOrderCommand(orderId));
    }

    @Override
    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "取消订单", description = "取消未支付/未发货订单")
    public void cancelOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId,
            @RequestBody CancelOrderCommand command) {
        orderAppService.cancelOrder(command);
    }

    @Override
    @GetMapping("/{orderId}")
    @Operation(summary = "查询订单详情", description = "根据 ID 获取订单完整信息")
    public OrderCO getOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") String orderId) {
        return orderAppService.getOrder(new GetOrderQuery(orderId));
    }
}
