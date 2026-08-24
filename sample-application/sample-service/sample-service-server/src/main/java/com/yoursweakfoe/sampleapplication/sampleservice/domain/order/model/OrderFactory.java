package com.yoursweakfoe.sampleapplication.sampleservice.domain.order.model;

import com.yoursweakfoe.common.ddd.domain.factory.Factory;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 订单工厂 —— 聚合新建路径的唯一入口（Factory 标记接口的落地示范）。
 *
 * <p>创建即合法：{@code create(...)} 返回的订单必然通过了不变量校验并携带
 * {@code OrderPlacedEvent}，调用方不存在「构造了但忘了下单」的中间态。
 * 入参只收领域概念（customerId + 已定价订单项），禁止感知 Command/DTO——
 * 把外部输入翻译为 {@code OrderItem} 是应用层 Handler 的职责。
 *
 * <p>与重建路径的分工：持久化恢复走 {@link Order#reconstitute}（惰性，无事件无校验），
 * 本工厂只负责「从无到有」。ID 铸造策略（UUID）收口于此，业务侧无需关心。
 *
 * <p>本类与 {@link Order} 同包：聚合的业务构造器为包私有，仅工厂可访问——
 * 「谁能 new 一个订单」由包结构在编译期锁死。
 */
@Component
public class OrderFactory implements Factory {

    /**
     * 创建已下单订单。
     *
     * @param customerId 客户 ID
     * @param items      已定价订单项（单价来自商品聚合）
     * @return 通过校验、状态 PENDING、携带 OrderPlacedEvent 的订单
     * @throws com.yoursweakfoe.common.exception.type.BusinessException 不变量违反时
     *         （订单项为空 / 客户 ID 缺失 / 总金额非正）
     */
    public Order create(String customerId, List<OrderItem> items) {
        Order order = new Order(UUID.randomUUID(), items, customerId);
        order.place();
        return order;
    }
}
