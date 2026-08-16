package com.yoursweakfoe.sampleapplication.sampleservice.application.product.event.listener;

import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockDeductedEvent;
import com.yoursweakfoe.sampleapplication.sampleservice.domain.product.model.event.StockRestoredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 商品领域事件监听器（域内反应）—— 对商品聚合的领域事件作出进程内反应。
 *
 * <p>当前为纯日志监听（无副作用）。若未来出现跨聚合业务反应（如库存告警触发补货单），
 * 遵循「接事件 → 加载聚合 → 委托 DomainService」的薄编排契约在本类扩展。
 */
@Component
public class ProductDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductDomainEventListener.class);

    @EventListener
    public void onStockDeducted(StockDeductedEvent event) {
        log.info("Stock deducted: productId={}, quantity={}",
                event.getProductId(), event.getQuantity());
    }

    @EventListener
    public void onStockRestored(StockRestoredEvent event) {
        log.info("Stock restored: productId={}, quantity={}",
                event.getProductId(), event.getRestoredQuantity());
    }
}
