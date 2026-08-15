package com.yoursweakfoe.sampleapplication.sampleservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.yoursweakfoe.sampleapplication.sampleservice.Application;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.co.OrderCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.CancelOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.order.dto.command.PlaceOrderCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.command.CreateProductCommand;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

/**
 * REST 端真实调用集成测试。
 *
 * <p>通过 RestClient 向 MVC 端口发起真实 HTTP 请求，验证完整链路：
 * HTTP → Spring MVC Controller → AppService → Handler → Domain → Repository → DB。
 *
 * <p>REST 路径由 Controller 上的 spring-web 注解定义（context-path /api 前缀）。
 *
 * <p>同时验证 GlobalRestExceptionHandler（@RestControllerAdvice）对异常的翻译：
 * <ul>
 *   <li>BusinessException → 422（RFC 9457，detail 为 i18n messageKey）
 *   <li>IllegalStateException → 409
 * </ul>
 */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestEndpointIntegrationTest {

    private static final ParameterizedTypeReference<Map<String, Object>> PROBLEM_BODY =
            new ParameterizedTypeReference<>() {};

    @LocalServerPort
    private int port;

    private static Long createdProductId;
    private static String createdOrderId;

    private RestClient client() {
        return RestClient.create("http://localhost:" + port + "/api");
    }

    // ==================== 商品 ====================

    @Test
    @Order(1)
    void createProduct_returns200() {
        var command = new CreateProductCommand("iPhone 15", 100);

        ProductCO dto = client().post().uri("/products")
                .body(command)
                .retrieve()
                .body(ProductCO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("iPhone 15");
        assertThat(dto.getStock()).isEqualTo(100);
        assertThat(dto.getId()).isNotNull();
        createdProductId = dto.getId();
    }

    @Test
    @Order(2)
    void getProduct_returns200() {
        assertThat(createdProductId).isNotNull();

        ProductCO dto = client().get().uri("/products/" + createdProductId)
                .retrieve()
                .body(ProductCO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("iPhone 15");
        assertThat(dto.getStock()).isEqualTo(100);
    }

    @Test
    @Order(3)
    void getProduct_notFound_returns422ProblemDetail() {
        Map<String, Object> body = client().get().uri("/products/99999")
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("title")).isEqualTo("Business Error");
        assertThat(body.get("detail")).isEqualTo("product:err.notFound");
        assertThat(body.get("type")).isEqualTo("about:blank");
    }

    // ==================== 下单 ====================

    @Test
    @Order(4)
    void placeOrder_success_returns200() {
        assertThat(createdProductId).isNotNull();
        var command = new PlaceOrderCommand("customer-001",
                List.of(new PlaceOrderCommand.OrderItemView(createdProductId, 2)));

        OrderCO dto = client().post().uri("/orders")
                .body(command)
                .retrieve()
                .body(OrderCO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getStatus()).isEqualTo("PENDING");
        assertThat(dto.getCustomerId()).isEqualTo("customer-001");
        assertThat(dto.getItems()).hasSize(1);
        createdOrderId = dto.getId();
    }

    @Test
    @Order(5)
    void placeOrder_insufficientStock_returns422() {
        assertThat(createdProductId).isNotNull();
        var command = new PlaceOrderCommand("customer-002",
                List.of(new PlaceOrderCommand.OrderItemView(createdProductId, 99999)));

        Map<String, Object> body = client().post().uri("/orders")
                .body(command)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("title")).isEqualTo("Business Error");
        assertThat(body.get("detail")).isEqualTo("product:err.insufficientStock");
    }

    @Test
    @Order(6)
    void placeOrder_productNotFound_returns422() {
        var command = new PlaceOrderCommand("customer-003",
                List.of(new PlaceOrderCommand.OrderItemView(99999L, 1)));

        Map<String, Object> body = client().post().uri("/orders")
                .body(command)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("detail")).isEqualTo("product:err.notFound");
    }

    // ==================== 查询订单 ====================

    @Test
    @Order(7)
    void getOrder_success_returns200() {
        assertThat(createdOrderId).isNotNull();

        OrderCO dto = client().get().uri("/orders/" + createdOrderId)
                .retrieve()
                .body(OrderCO.class);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(createdOrderId);
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @Order(8)
    void getOrder_notFound_returns422() {
        Map<String, Object> body = client().get()
                .uri("/orders/00000000-0000-0000-0000-000000000000")
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("detail")).isEqualTo("order:err.notFound");
    }

    // ==================== 取消订单 ====================

    @Test
    @Order(9)
    void cancelOrder_success_returns200() {
        assertThat(createdOrderId).isNotNull();
        var command = new CancelOrderCommand(createdOrderId, "customer request");

        client().put().uri("/orders/" + createdOrderId + "/cancel")
                .body(command)
                .retrieve()
                .toBodilessEntity();

        // 验证订单状态已变为 CANCELLED
        OrderCO dto = client().get().uri("/orders/" + createdOrderId)
                .retrieve()
                .body(OrderCO.class);
        assertThat(dto).isNotNull();
        assertThat(dto.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @Order(10)
    void cancelOrder_alreadyCancelled_returns422() {
        assertThat(createdOrderId).isNotNull();
        var command = new CancelOrderCommand(createdOrderId, "try again");

        Map<String, Object> body = client().put().uri("/orders/" + createdOrderId + "/cancel")
                .body(command)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("detail")).isEqualTo("order:err.status.cancellable");
    }

    // ==================== 库存回补验证 ====================

    @Test
    @Order(11)
    void afterCancelOrder_stockReplenished() {
        assertThat(createdProductId).isNotNull();
        // 初始 100，下单扣 2，取消回补 2 → 应该回到 100
        ProductCO dto = client().get().uri("/products/" + createdProductId)
                .retrieve()
                .body(ProductCO.class);
        assertThat(dto).isNotNull();
        assertThat(dto.getStock()).isEqualTo(100);
    }

    // ==================== 订单全生命周期（pay → confirm → ship → deliver → complete） ====================

    private static String lifecycleOrderId;

    @Test
    @Order(12)
    void lifecycle_placeAndPay_success() {
        assertThat(createdProductId).isNotNull();
        var command = new PlaceOrderCommand("lifecycle-customer",
                List.of(new PlaceOrderCommand.OrderItemView(createdProductId, 1)));

        OrderCO placed = client().post().uri("/orders")
                .body(command)
                .retrieve()
                .body(OrderCO.class);
        assertThat(placed).isNotNull();
        assertThat(placed.getStatus()).isEqualTo("PENDING");
        lifecycleOrderId = placed.getId();

        OrderCO paid = client().put().uri("/orders/" + lifecycleOrderId + "/pay")
                .retrieve()
                .body(OrderCO.class);
        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo("PAID");
    }

    @Test
    @Order(13)
    void lifecycle_payAgain_returns422() {
        assertThat(lifecycleOrderId).isNotNull();

        Map<String, Object> body = client().put().uri("/orders/" + lifecycleOrderId + "/pay")
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })
                .body(PROBLEM_BODY);

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("detail")).isEqualTo("order:err.status.pending");
    }

    @Test
    @Order(14)
    void lifecycle_confirmShipDeliverComplete_reachesCompleted() {
        assertThat(lifecycleOrderId).isNotNull();

        OrderCO confirmed = client().put().uri("/orders/" + lifecycleOrderId + "/confirm")
                .retrieve()
                .body(OrderCO.class);
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.getStatus()).isEqualTo("CONFIRMED");

        OrderCO shipped = client().put()
                .uri("/orders/" + lifecycleOrderId + "/ship?trackingNumber=SF-123456")
                .retrieve()
                .body(OrderCO.class);
        assertThat(shipped).isNotNull();
        assertThat(shipped.getStatus()).isEqualTo("SHIPPED");

        OrderCO delivered = client().put().uri("/orders/" + lifecycleOrderId + "/deliver")
                .retrieve()
                .body(OrderCO.class);
        assertThat(delivered).isNotNull();
        assertThat(delivered.getStatus()).isEqualTo("DELIVERED");

        OrderCO completed = client().put().uri("/orders/" + lifecycleOrderId + "/complete")
                .retrieve()
                .body(OrderCO.class);
        assertThat(completed).isNotNull();
        assertThat(completed.getStatus()).isEqualTo("COMPLETED");
    }
}
