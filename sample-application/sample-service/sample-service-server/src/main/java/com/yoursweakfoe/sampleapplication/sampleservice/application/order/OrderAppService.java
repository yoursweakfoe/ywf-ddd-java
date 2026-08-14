package com.yoursweakfoe.sampleapplication.sampleservice.application.order;

import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.CancelOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.CompleteOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.ConfirmOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.DeliverOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query.GetOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.query.GetOrderPageHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.PayOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.PlaceOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command.ShipOrderHandler;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.presenter.OrderPresenter;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderSummaryCO;
import com.yoursweakfoe.common.ddd.domain.model.PageResult;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CompleteOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ConfirmOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.DeliverOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.query.GetOrderPageQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PayOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.ShipOrderCommand;
import org.springframework.stereotype.Service;

/**
 * 订单应用服务 —— 聚合协调入口。
 *
 * <p>所有用例委托 Handler 执行（返回 DTO），本类仅做 DTO → CO 呈现。
 * 具备编排多个 Handler 的能力（通过 DTO 流转，不涉及 CO）。
 */
@Service
public class OrderAppService {

    // region 依赖注入
    private final OrderPresenter orderPresenter;
    private final PlaceOrderHandler placeOrderHandler;
    private final PayOrderHandler payOrderHandler;
    private final ConfirmOrderHandler confirmOrderHandler;
    private final ShipOrderHandler shipOrderHandler;
    private final DeliverOrderHandler deliverOrderHandler;
    private final CompleteOrderHandler completeOrderHandler;
    private final CancelOrderHandler cancelOrderHandler;
    private final GetOrderHandler getOrderHandler;
    private final GetOrderPageHandler getOrderPageHandler;

    public OrderAppService(OrderPresenter orderPresenter,
                           PlaceOrderHandler placeOrderHandler,
                           PayOrderHandler payOrderHandler,
                           ConfirmOrderHandler confirmOrderHandler,
                           ShipOrderHandler shipOrderHandler,
                           DeliverOrderHandler deliverOrderHandler,
                           CompleteOrderHandler completeOrderHandler,
                           CancelOrderHandler cancelOrderHandler,
                           GetOrderHandler getOrderHandler,
                           GetOrderPageHandler getOrderPageHandler) {
        this.orderPresenter = orderPresenter;
        this.placeOrderHandler = placeOrderHandler;
        this.payOrderHandler = payOrderHandler;
        this.confirmOrderHandler = confirmOrderHandler;
        this.shipOrderHandler = shipOrderHandler;
        this.deliverOrderHandler = deliverOrderHandler;
        this.completeOrderHandler = completeOrderHandler;
        this.cancelOrderHandler = cancelOrderHandler;
        this.getOrderHandler = getOrderHandler;
        this.getOrderPageHandler = getOrderPageHandler;
    }
    // endregion

    // region 写操作用例
    public OrderCO placeOrder(PlaceOrderCommand command) {
        return orderPresenter.present(placeOrderHandler.handle(command));
    }

    public OrderCO payOrder(PayOrderCommand command) {
        return orderPresenter.present(payOrderHandler.handle(command));
    }

    public OrderCO confirmOrder(ConfirmOrderCommand command) {
        return orderPresenter.present(confirmOrderHandler.handle(command));
    }

    public OrderCO shipOrder(ShipOrderCommand command) {
        return orderPresenter.present(shipOrderHandler.handle(command));
    }

    public OrderCO deliverOrder(DeliverOrderCommand command) {
        return orderPresenter.present(deliverOrderHandler.handle(command));
    }

    public OrderCO completeOrder(CompleteOrderCommand command) {
        return orderPresenter.present(completeOrderHandler.handle(command));
    }

    public void cancelOrder(CancelOrderCommand command) {
        cancelOrderHandler.handle(command);
    }
    // endregion

    // region 读操作用例
    public OrderCO getOrder(GetOrderQuery query) {
        return orderPresenter.present(getOrderHandler.handle(query));
    }

    /**
     * 分页查询订单 —— 出路径多视图演示。
     *
     * <p>Handler 返回 {@code PageResult<OrderViewDTO>}，Presenter 裁剪为 {@link OrderSummaryCO}。
     * 同一 View 可呈现为不同 CO（详情 {@link OrderCO} / 概览 {@link OrderSummaryCO}），
     * 视调用方场景而定。
     */
    public PageResult<OrderSummaryCO> getOrderPage(GetOrderPageQuery query) {
        return getOrderPageHandler.handle(query).map(orderPresenter::presentSummary);
    }
    // endregion
}
