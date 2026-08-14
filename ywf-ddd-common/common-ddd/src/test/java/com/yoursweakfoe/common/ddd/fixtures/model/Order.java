package com.yoursweakfoe.common.ddd.fixtures.model;

import com.yoursweakfoe.common.ddd.fixtures.event.OrderCancelledEvent;
import com.yoursweakfoe.common.ddd.fixtures.event.OrderPlacedEvent;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.type.BusinessException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order extends AggregateRoot<UUID> {

    private UUID id;
    private OrderStatus status;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private String customerId;

    /** 新建订单（业务构造器） */
    public Order(UUID id, OrderStatus status, List<OrderItem> items,
                 BigDecimal totalAmount, String customerId) {
        this.id = id;
        this.status = status;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalAmount = totalAmount;
        this.customerId = customerId;
    }

    /** 重建订单（持久化层 Converter 使用，跳过校验与事件注册） */
    public static Order reconstitute(UUID id, OrderStatus status, List<OrderItem> items,
                                     BigDecimal totalAmount, String customerId) {
        return new Order(id, status, items, totalAmount, customerId);
    }

    /** 下单：校验不变量 + 注册领域事件 */
    public void place() {
        validate();
        registerEvent(new OrderPlacedEvent(id, totalAmount));
    }

    /** 取消订单 */
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessException("order:err.alreadyCancelled");
        }
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCancelledEvent(id, reason));
    }

    @Override
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order:err.itemsEmpty");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("order:err.totalMustBePositive");
        }
    }
}
