package com.yoursweakfoe.sampleapplication.sampleservice.domain.product.id;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.exception.type.BusinessException;
import org.junit.jupiter.api.Test;

/**
 * 币种 null 闸守卫 —— 断言走统一 BusinessException 位点通道（非 NPE 中文硬编码）。
 * 案卷 2026-09-id-guard-site（BP-12 同法收编币种教学件）。
 */
class ProductIdTest {

    @Test
    void nullBaseValue_goesThroughBusinessExceptionSite() {
        assertThatThrownBy(() -> ProductId.of(null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(BusinessException.class);
    }
}
