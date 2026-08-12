package com.yoursweakfoe.sampleapplication.sampleservice.adapter.grpc;

import com.yoursweakfoe.sampleapplication.sampleservice.application.product.ProductAppService;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.co.ProductCO;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.dto.GetProductQuery;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.GetProductInternalRequest;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.ProductInternalInfo;
import com.yoursweakfoe.sampleapplication.sampleservice.contract.product.proto.ProductInternalServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

/**
 * 商品东西向 gRPC 端点 —— 纯透传到 {@link ProductAppService}。
 *
 * <p>实现 proto 契约 {@code ProductInternalService}（contract 模块 src/main/proto）。
 * 业务异常（如商品不存在）直接上抛，由 common-exception 的
 * {@code GrpcExceptionServerInterceptor} 统一映射为 Status + Trailers。
 */
@GrpcService
public class ProductInternalGrpcService extends ProductInternalServiceGrpc.ProductInternalServiceImplBase {

    // region 依赖注入
    private final ProductAppService productAppService;

    public ProductInternalGrpcService(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }
    // endregion

    @Override
    public void getProduct(GetProductInternalRequest request,
                           StreamObserver<ProductInternalInfo> responseObserver) {
        ProductCO product = productAppService.getProduct(new GetProductQuery(request.getProductId()));

        ProductInternalInfo info = ProductInternalInfo.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setStock(product.getStock())
                .build();
        responseObserver.onNext(info);
        responseObserver.onCompleted();
    }
}
