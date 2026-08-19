package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCancelledEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderCompletedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderConfirmedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderDeliveredEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPaidEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderPlacedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model.event.OrderShippedEvent;
import com.yoursweakfoe.common.ddd.domain.model.AggregateRoot;
import com.yoursweakfoe.common.exception.type.BusinessException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单聚合根 —— 管理订单生命周期（下单→支付→确认→发货→签收→完成，可取消）。
 */
public class Order extends AggregateRoot<UUID> {

    // region 字段与构造器
    private UUID id;
    @Getter
    private OrderStatus status;
    private List<OrderItem> items;
    @Getter
    private BigDecimal totalAmount;
    @Getter
    private String customerId;
    @Getter
    private String trackingNumber;
    @Getter
    private String cancelReason;
    @Getter
    private OffsetDateTime createAt;
    @Getter
    private OffsetDateTime updateAt;
    @Getter
    private Integer version;

    /** 业务构造器（创建新订单） */
    public Order(UUID id, List<OrderItem> items, String customerId) {
        this.id = id;
        this.status = OrderStatus.PENDING;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.customerId = customerId;
        this.totalAmount = calculateTotal();
    }

    /** 重建构造器（持久化层 Converter 使用） */
    public static Order reconstitute(UUID id, OrderStatus status, List<OrderItem> items,
                                     BigDecimal totalAmount, String customerId,
                                     String trackingNumber, String cancelReason,
                                     OffsetDateTime createAt, OffsetDateTime updateAt,
                                     Integer version) {
        Order order = new Order(id, items, customerId);
        order.status = status;
        order.totalAmount = totalAmount;
        order.trackingNumber = trackingNumber;
        order.cancelReason = cancelReason;
        order.createAt = createAt;
        order.updateAt = updateAt;
        order.version = version;
        return order;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }

    // endregion

    // region 行为方法

    /**
     * 下单：校验不变量 + 注册领域事件（状态保持 PENDING）。
     */
    public void place() {
        validate();
        registerEvent(new OrderPlacedEvent(id, totalAmount, customerId));
    }

    /**
     * 支付订单。
     *
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不是 PENDING 时
     */
    public void pay() {
        requireStatus("order:err.status.pending", OrderStatus.PENDING);
        this.status = OrderStatus.PAID;
        registerEvent(new OrderPaidEvent(id));
    }

    /**
     * 商家确认订单。
     *
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不是 PAID 时
     */
    public void confirm() {
        requireStatus("order:err.status.paid", OrderStatus.PAID);
        this.status = OrderStatus.CONFIRMED;
        registerEvent(new OrderConfirmedEvent(id));
    }

    /**
     * 发货。
     *
     * @param trackingNumber 快递单号
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不是 CONFIRMED 时
     */
    public void ship(String trackingNumber) {
        requireStatus("order:err.status.confirmed", OrderStatus.CONFIRMED);
        this.status = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        registerEvent(new OrderShippedEvent(id, trackingNumber));
    }

    /**
     * 签收。
     *
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不是 SHIPPED 时
     */
    public void deliver() {
        requireStatus("order:err.status.shipped", OrderStatus.SHIPPED);
        this.status = OrderStatus.DELIVERED;
        registerEvent(new OrderDeliveredEvent(id));
    }

    /**
     * 完成订单。
     *
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不是 DELIVERED 时
     */
    public void complete() {
        requireStatus("order:err.status.delivered", OrderStatus.DELIVERED);
        this.status = OrderStatus.COMPLETED;
        registerEvent(new OrderCompletedEvent(id));
    }

    /**
     * 取消订单（仅允许从 PENDING 或 PAID 状态取消）。
     *
     * @param reason 取消原因
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 状态不允许取消时
     */
    public void cancel(String reason) {
        requireStatus("order:err.status.cancellable", OrderStatus.PENDING, OrderStatus.PAID);
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        registerEvent(new OrderCancelledEvent(id, reason));
    }
    // endregion

    // region 不变量校验

    @Override
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("order:err.itemsEmpty");
        }
        if (customerId == null) {
            throw new BusinessException("order:err.customerIdRequired");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("order:err.totalMustBePositive");
        }
    }
    // endregion

    // region 内部方法

    /**
     * 状态转换守卫 —— 确认当前状态在允许集合内，否则拒绝转换。
     *
     * <p>使用 JDK 21 模式匹配 switch：新增枚举值时编译器强制要求处理（穷尽性检查）。
     */
    private void requireStatus(String errorKey, OrderStatus... validStatuses) {
        boolean allowed = switch (status) {
            case PENDING -> contains(validStatuses, OrderStatus.PENDING);
            case PAID -> contains(validStatuses, OrderStatus.PAID);
            case CONFIRMED -> contains(validStatuses, OrderStatus.CONFIRMED);
            case SHIPPED -> contains(validStatuses, OrderStatus.SHIPPED);
            case DELIVERED -> contains(validStatuses, OrderStatus.DELIVERED);
            case COMPLETED -> contains(validStatuses, OrderStatus.COMPLETED);
            case CANCELLED -> contains(validStatuses, OrderStatus.CANCELLED);
        };
        if (!allowed) {
            throw new BusinessException(errorKey);
        }
    }

    private static boolean contains(OrderStatus[] statuses, OrderStatus target) {
        return List.of(statuses).contains(target);
    }

    private BigDecimal calculateTotal() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    // endregion
}
