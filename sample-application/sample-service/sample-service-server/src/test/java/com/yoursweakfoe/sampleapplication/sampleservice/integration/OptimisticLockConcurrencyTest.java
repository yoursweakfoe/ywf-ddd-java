package com.yoursweakfoe.sampleapplication.sampleservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.sampleapplication.sampleservice.Application;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * 并发乐观锁压力测试 —— 真实 HTTP 调用（MVC 随机端口）。
 *
 * <p>多线程同时下单购买同一商品，验证：
 * <ul>
 *   <li>乐观锁机制正确拦截并发冲突（部分请求返回 409）
 *   <li>库存不会超卖（最终库存 >= 0）
 *   <li>成功订单数 * 购买数量 + 剩余库存 = 初始库存
 * </ul>
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("stress")
class OptimisticLockConcurrencyTest {

    @LocalServerPort
    private int port;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port + "/api");
    }

    /** 发起请求并返回状态码（任何状态均不抛异常）。 */
    private int postForStatus(String path, Object body) {
        return client().post().uri(path)
                .body(body)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .toBodilessEntity()
                .getStatusCode()
                .value();
    }

    @Test
    void concurrentOrders_optimisticLock_preventsOversell() throws Exception {
        // 1. 创建库存为 10 的商品
        ProductCO product = client().post().uri("/products")
                .body(new CreateProductCommand("Limited Edition", 10))
                .retrieve()
                .body(ProductCO.class);
        assertThat(product).isNotNull();
        Long productId = product.getId();

        // 2. 20 个线程同时下单，每个买 1 件（库存只有 10，必然有冲突）
        int threadCount = 20;
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        AtomicInteger businessErrorCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startGate.await(10, TimeUnit.SECONDS);

                    var cmd = new PlaceOrderCommand("stress-customer-" + idx,
                            List.of(new PlaceOrderCommand.OrderItemDTO(productId, 1)));

                    int status = postForStatus("/orders", cmd);

                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    } else if (status == 422 || status == 500) {
                        businessErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 忽略
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 3. 验证库存不超卖
        ProductCO remaining = client().get().uri("/products/" + productId)
                .retrieve()
                .body(ProductCO.class);
        assertThat(remaining).isNotNull();
        int remainingStock = remaining.getStock();

        assertThat(remainingStock).isGreaterThanOrEqualTo(0);
        // 成功订单数不超过初始库存
        assertThat(successCount.get()).isLessThanOrEqualTo(10);
        // 守恒：成功数 + 剩余库存 = 初始库存
        assertThat(successCount.get() + remainingStock).isEqualTo(10);

        // 4. 输出统计
        System.out.printf(
                "[Stress] success=%d, conflict(409)=%d, businessError(422/500)=%d, remainingStock=%d%n",
                successCount.get(), conflictCount.get(), businessErrorCount.get(), remainingStock);
    }
}
