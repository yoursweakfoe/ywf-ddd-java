package com.yoursweakfoe.sampleapplication.sampleservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yoursweakfoe.common.exception.BusinessException;
import com.yoursweakfoe.common.exception.grpc.GrpcExceptions;
import com.yoursweakfoe.sampleapplication.sampleservice.Application;
import com.yoursweakfoe.sampleapplication.sampleservice.application.product.ProductAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.CreateProductCommand;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.GetProductInternalRequest;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.ProductInternalInfo;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.ProductInternalServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.test.context.ActiveProfiles;

/**
 * gRPC 东西向端点真实调用集成测试。
 *
 * <p>启动完整应用（gRPC server 随机端口，application-test.yml 配置 port=0，
 * 经 {@link GrpcServerLifecycle#getPort()} 读取实际端口），通过 Boot 自动配置的
 * {@link GrpcChannelFactory} 创建真实通道，验证完整链路：
 * gRPC Client → Netty Server → ProductInternalGrpcService → AppService → Domain → DB。
 *
 * <p>同时验证 common-exception 的 gRPC 异常双拦截器：
 * <ul>
 *   <li>服务端：BusinessException → FAILED_PRECONDITION + Trailers（messageKey/params）
 *   <li>客户端：Trailers 还原 BusinessException（挂载 Status cause）
 * </ul>
 */
@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrpcEndpointIntegrationTest {

    @Autowired
    private GrpcChannelFactory channels;

    @Autowired
    private GrpcServerLifecycle grpcServerLifecycle;

    @Autowired
    private ProductAppService productAppService;

    private static Long productId;

    private ProductInternalServiceGrpc.ProductInternalServiceBlockingStub stub() {
        return ProductInternalServiceGrpc.newBlockingStub(
                channels.createChannel("localhost:" + grpcServerLifecycle.getPort()));
    }

    @BeforeAll
    void setUpProduct() {
        // 经应用层准备测试数据（与 gRPC 端点解耦）
        ProductCO created = productAppService.createProduct(new CreateProductCommand("MacBook Pro", 50));
        productId = created.getId();
    }

    @Test
    @Order(1)
    void getProduct_viaGrpc_success() {
        assertThat(grpcServerLifecycle.getPort()).isPositive();

        ProductInternalInfo info = stub().getProduct(
                GetProductInternalRequest.newBuilder().setProductId(productId).build());

        assertThat(info.getId()).isEqualTo(productId);
        assertThat(info.getName()).isEqualTo("MacBook Pro");
        assertThat(info.getStock()).isEqualTo(50);
    }

    @Test
    @Order(2)
    void getProduct_notFound_businessExceptionRestored() {
        assertThatThrownBy(() -> stub().getProduct(
                GetProductInternalRequest.newBuilder().setProductId(99999L).build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                    // 服务端映射的状态码与 description（messageKey）
                    assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
                    assertThat(e.getStatus().getDescription()).isEqualTo("product:err.notFound");
                    // 客户端拦截器还原的 BusinessException（messageKey + params）
                    BusinessException restored = GrpcExceptions.extractBusiness(e);
                    assertThat(restored).isNotNull();
                    assertThat(restored.getMessage()).isEqualTo("product:err.notFound");
                });
    }

    @Test
    @Order(3)
    void healthService_reportsServing() {
        HealthCheckResponse response = HealthGrpc.newBlockingStub(
                channels.createChannel("localhost:" + grpcServerLifecycle.getPort()))
                .check(HealthCheckRequest.newBuilder().setService("").build());

        assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.ServingStatus.SERVING);
    }

    @Test
    @Order(4)
    void reflectionService_listsProductInternalService() throws Exception {
        List<String> serviceNames = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<ServerReflectionRequest> request = ServerReflectionGrpc.newStub(
                        channels.createChannel("localhost:" + grpcServerLifecycle.getPort()))
                .serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
                    @Override
                    public void onNext(ServerReflectionResponse value) {
                        value.getListServicesResponse().getServiceList()
                                .forEach(info -> serviceNames.add(info.getName()));
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        request.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
        request.onCompleted();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(serviceNames).contains("sampleservice.product.internal.ProductInternalService");
    }
}
