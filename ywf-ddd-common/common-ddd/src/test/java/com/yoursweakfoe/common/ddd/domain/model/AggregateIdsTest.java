package com.yoursweakfoe.common.ddd.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AggregateIds 测试 —— v7 定型（版本/变体位）、同进程唯一性、时间有序契约。
 *
 * <p>本测试锁住的是<strong>装配宣言对外承诺的行为面</strong>（见 AggregateIds 类 javadoc）：
 * version()==7、variant==IETF(2)、批量铸造无碰撞、48 位毫秒时间戳前缀随铸造顺序不减/跨毫秒递增。
 * 断言只依赖 RFC 9562 位布局与毫秒时间前缀随时间不减，不依赖具体时钟读数、不依赖毫秒内数值序。
 */
@DisplayName("AggregateIds — 聚合身份铸造契约")
class AggregateIdsTest {

    /** v7 位布局：msb 前 48 位为 Unix 毫秒时间戳前缀（跨毫秒断言用的「粗序」键）。 */
    private static long epochMillisPrefix(UUID id) {
        return id.getMostSignificantBits() >>> 16;
    }

    @Test
    void mint_conformsToRfc9562V7Layout() {
        UUID id = AggregateIds.mint();

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void mint_uniqueOverTenThousandCalls() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(AggregateIds.mint());
        }

        assertThat(seen).hasSize(10_000);
    }

    @Test
    void mint_timeOrdered_withinProcess() {
        // 同毫秒对：时间戳前缀不减（Random 变体同毫秒重摇独立熵，数值不保证单调但前缀相等属预期）
        UUID a = AggregateIds.mint();
        UUID b = AggregateIds.mint();
        assertThat(epochMillisPrefix(b)).isGreaterThanOrEqualTo(epochMillisPrefix(a));

        // 跨毫秒对（睡过至少一个毫秒刻度）：时间戳前缀严格递增
        UUID earlier = AggregateIds.mint();
        sleepAtLeastTwoMillis();
        UUID later = AggregateIds.mint();
        assertThat(epochMillisPrefix(later)).isGreaterThan(epochMillisPrefix(earlier));
    }

    private void sleepAtLeastTwoMillis() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("测试线程被中断", e);
        }
    }
}
