package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.api;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.PlaceOrderCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务接口 —— 完整 REST 契约（方法签名 + 能力语义 + HTTP 映射的单一事实源）。
 *
 * <p>本接口承载 HTTP 映射（路径、方法、参数绑定）与文档注解（{@code @Operation} / {@code @Tag}），
 * 契约 = 完整 REST 定义。服务端 {@code adapter.web.OrderController} 仅以 {@code @RestController}
 * 标记并实现本接口（映射经接口继承，不重复声明）；东西向服务间调用同样经 HTTP 消费本契约
 * （消费方 RestClient 直连服务端 REST 端点）。
 */
@Tag(name = "订单服务", description = "订单生命周期管理：下单、支付、发货、签收、完成、取消")
@RequestMapping("/orders")
public interface OrderService {

    /**
     * 下单。
     *
     * @param command 下单命令（客户 ID + 订单项列表）
     * @return 创建后的订单信息
     */
    @Operation(summary = "下单", description = "创建新订单，初始状态为 PENDING")
    @PostMapping("")
    OrderCO placeOrder(@Valid @RequestBody PlaceOrderCommand command);

    /**
     * 支付订单。
     *
     * @param orderId 订单 ID
     * @return 支付后的订单信息
     */
    @Operation(summary = "支付订单", description = "将 PENDING 订单标记为已支付")
    @PutMapping("/{orderId}/pay")
    OrderCO payOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId);

    /**
     * 确认订单。
     *
     * @param orderId 订单 ID
     * @return 确认后的订单信息
     */
    @Operation(summary = "确认订单", description = "商家确认已支付订单")
    @PutMapping("/{orderId}/confirm")
    OrderCO confirmOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId);

    /**
     * 发货。
     *
     * @param orderId        订单 ID
     * @param trackingNumber 物流单号
     * @return 发货后的订单信息
     */
    @Operation(summary = "发货", description = "商家发货并填写物流单号")
    @PutMapping("/{orderId}/ship")
    OrderCO shipOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId,
            @RequestParam("trackingNumber") @Parameter(description = "物流单号") String trackingNumber);

    /**
     * 签收。
     *
     * @param orderId 订单 ID
     * @return 签收后的订单信息
     */
    @Operation(summary = "签收", description = "买家确认签收")
    @PutMapping("/{orderId}/deliver")
    OrderCO deliverOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId);

    /**
     * 完成订单。
     *
     * @param orderId 订单 ID
     * @return 完成后的订单信息
     */
    @Operation(summary = "完成订单", description = "订单最终完成")
    @PutMapping("/{orderId}/complete")
    OrderCO completeOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId);

    /**
     * 取消订单。
     *
     * @param orderId 订单 ID
     * @param command 取消命令（取消原因）
     */
    @Operation(summary = "取消订单", description = "取消未支付/未发货订单")
    @PutMapping("/{orderId}/cancel")
    void cancelOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") UUID orderId,
            @Valid @RequestBody CancelOrderCommand command);

    /**
     * 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单完整信息
     */
    @Operation(summary = "查询订单详情", description = "根据 ID 获取订单完整信息")
    @GetMapping("/{orderId}")
    OrderCO getOrder(
            @PathVariable("orderId") @Parameter(description = "订单 ID") String orderId);
}
