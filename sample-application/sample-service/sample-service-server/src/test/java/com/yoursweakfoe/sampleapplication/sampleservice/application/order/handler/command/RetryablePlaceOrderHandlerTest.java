package com.yoursweakfoe.sampleapplication.sampleservice.application.order.handler.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.ddd.infrastructure.mybatis.persistence.OptimisticLockConflictException;
import com.yoursweakfoe.sampleapplication.sampleservice.application.order.dto.OrderDTO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 下单重试包装器测试 —— 零退避参数注入，验证冲突识别、重试边界与透传语义。
 */
@ExtendWith(MockitoExtension.class)
class RetryablePlaceOrderHandlerTest {

    private static final OptimisticLockConflictException CONFLICT =
            new OptimisticLockConflictException(
                    "UPDATE affected 0 rows for entity ID: x (optimistic lock version conflict)");

    @Mock private PlaceOrderHandler inner;

    private RetryablePlaceOrderHandler wrapper;

    @BeforeEach
    void setUp() {
        // maxRetries=3, baseDelay=0ms —— 用例不真实 sleep
        wrapper = new RetryablePlaceOrderHandler(inner, 3, 0);
    }

    private final PlaceOrderCommand command = new PlaceOrderCommand();

    @Test
    @DisplayName("前两次冲突第三次成功 → 返回结果且共调用 3 次")
    void retriesOnConflict_thenSucceeds() {
        OrderDTO expected = new OrderDTO();
        when(inner.handle(command))
                .thenThrow(CONFLICT)
                .thenThrow(CONFLICT)
                .thenReturn(expected);

        OrderDTO result = wrapper.handle(command);

        assertThat(result).isSameAs(expected);
        verify(inner, times(3)).handle(command);
    }

    @Test
    @DisplayName("非乐观锁 IllegalStateException → 立即上抛不重试")
    void nonConflictIse_notRetried() {
        IllegalStateException dataError = new IllegalStateException("Expected at most one row but found multiple");
        when(inner.handle(command)).thenThrow(dataError);

        assertThatThrownBy(() -> wrapper.handle(command))
                .isSameAs(dataError);
        verify(inner, times(1)).handle(command);
    }

    @Test
    @DisplayName("重试耗尽仍冲突 → 上抛最后一次冲突异常")
    void exhaustedRetries_rethrowsConflict() {
        RetryablePlaceOrderHandler singleRetry = new RetryablePlaceOrderHandler(inner, 2, 0);
        when(inner.handle(command)).thenThrow(CONFLICT);

        assertThatThrownBy(() -> singleRetry.handle(command))
                .isSameAs(CONFLICT);
        verify(inner, times(2)).handle(command);
    }

    @Test
    @DisplayName("业务异常（如库存不足）→ 原样穿透不重试")
    void businessException_passesThrough() {
        com.yoursweakfoe.common.exception.type.BusinessException insufficient =
                new com.yoursweakfoe.common.exception.type.BusinessException("product:err.insufficientStock");
        when(inner.handle(command)).thenThrow(insufficient);

        assertThatThrownBy(() -> wrapper.handle(command))
                .isSameAs(insufficient);
        verify(inner, times(1)).handle(command);
    }
}
