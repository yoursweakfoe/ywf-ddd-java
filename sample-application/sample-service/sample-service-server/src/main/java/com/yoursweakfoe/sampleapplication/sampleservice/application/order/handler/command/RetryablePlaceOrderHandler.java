package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.OptimisticLockConflictException;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 下单重试包装器 —— 乐观锁冲突自动重试
 * （{@code docs/application/cookbook/optimistic-lock-retry.md} §2 模板的落地实现）。
 *
 * <p><b>适用场景</b>：高并发下单扣库存。并发事务加载同一 Product（version=N）后先后 UPDATE，
 * 后者影响 0 行 → {@code MybatisPersistence.updateDomain()} 抛
 * {@link OptimisticLockConflictException} → 本包装器按<b>异常类型</b>识别为乐观锁冲突，
 * 指数退避后整单重试。
 *
 * <p><b>为什么整单重试安全</b>：内部 {@link PlaceOrderHandler#handle} 标注
 * {@code @Transactional}——每次 attempt 经代理开启<b>独立事务</b>，冲突抛异常时该次事务已整体回滚，
 * 无部分扣减/无订单残留；重试从头重新加载商品（拿最新 version）与构建订单项。
 *
 * <p><b>不重试的异常</b>：其余一切异常直接上抛——业务规则违反（如库存不足 422）不应被重试掩盖；
 * 「实体已被删除/不存在」由框架抛普通 {@link IllegalStateException}（非冲突类型），同样不重试。
 * 重试耗尽仍冲突 → 上抛，由全局异常处理返回 409（前端提示刷新重试）。
 *
 * <p><b>契约说明</b>：冲突识别依赖框架类型 {@link OptimisticLockConflictException}
 * （编译期绑定，无消息文本耦合）；历史版本曾以消息含 {@code affected 0 rows} 判定，
 * 该字样在框架侧保留仅为兼容期过渡，新代码一律按类型捕获。
 */
@Slf4j
@Component
public class RetryablePlaceOrderHandler {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 100;

    private final PlaceOrderHandler placeOrderHandler;
    private final int maxRetries;
    private final long baseDelayMs;

    /** Spring 装配入口（类含多个构造器，须显式标注装配选择）。 */
    @Autowired
    public RetryablePlaceOrderHandler(PlaceOrderHandler placeOrderHandler) {
        this(placeOrderHandler, DEFAULT_MAX_RETRIES, DEFAULT_BASE_DELAY_MS);
    }

    /** 测试专用：注入更小退避参数，避免用例真实 sleep。 */
    RetryablePlaceOrderHandler(PlaceOrderHandler placeOrderHandler, int maxRetries, long baseDelayMs) {
        this.placeOrderHandler = placeOrderHandler;
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    /**
     * 执行下单（带乐观锁冲突重试）。
     *
     * @throws OptimisticLockConflictException 重试耗尽仍冲突
     * @throws IllegalStateException 发生「实体消失」类数据异常时原样上抛
     */
    public OrderDTO handle(PlaceOrderCommand command) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return placeOrderHandler.handle(command);
            } catch (OptimisticLockConflictException e) {
                if (attempt == maxRetries) {
                    throw e;
                }
                long delay = baseDelayMs * (1L << (attempt - 1));
                log.warn("Optimistic lock conflict on place order, retry {}/{} after {}ms: customerId={}",
                        attempt, maxRetries, delay, command.getCustomerId());
                sleep(delay);
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    /** 虚拟线程下 Thread.sleep 不占用载体线程，退避等待无性能顾虑。 */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", e);
        }
    }
}
