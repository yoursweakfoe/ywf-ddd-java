package com.yoursweakfoe.sampleapplication.sampleservice.contract.order.api;

import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.PlaceOrderCommand;
import java.util.UUID;

/**
 * 订单服务接口 —— 服务内部用例契约（方法签名单一事实源）。
 *
 * <p>对外 REST 面由服务端 {@code adapter.web.OrderController} 以 spring-web
 * 原生注解显式声明路径并实现本接口；东西向 RPC 面见
 * {@code src/main/proto} 下的 proto 契约（proto-first，按需最小化）。
 */
public interface OrderService {

    /**
     * 下单。
     *
     * @param command 下单命令（客户 ID + 订单项列表）
     * @return 创建后的订单信息
     */
    OrderCO placeOrder(PlaceOrderCommand command);

    /**
     * 支付订单。
     *
     * @param orderId 订单 ID
     * @return 支付后的订单信息
     */
    OrderCO payOrder(UUID orderId);

    /**
     * 确认订单。
     *
     * @param orderId 订单 ID
     * @return 确认后的订单信息
     */
    OrderCO confirmOrder(UUID orderId);

    /**
     * 发货。
     *
     * @param orderId        订单 ID
     * @param trackingNumber 物流单号
     * @return 发货后的订单信息
     */
    OrderCO shipOrder(UUID orderId, String trackingNumber);

    /**
     * 签收。
     *
     * @param orderId 订单 ID
     * @return 签收后的订单信息
     */
    OrderCO deliverOrder(UUID orderId);

    /**
     * 完成订单。
     *
     * @param orderId 订单 ID
     * @return 完成后的订单信息
     */
    OrderCO completeOrder(UUID orderId);

    /**
     * 取消订单。
     *
     * @param orderId 订单 ID
     * @param command 取消命令（取消原因）
     */
    void cancelOrder(UUID orderId, CancelOrderCommand command);

    /**
     * 查询订单详情。
     *
     * @param orderId 订单 ID
     * @return 订单完整信息
     */
    OrderCO getOrder(String orderId);
}
