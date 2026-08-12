package com.yoursweakfoe.common.exception.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.exception.BusinessException;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("GrpcExceptionClientInterceptor — gRPC 客户端 BusinessException 还原")
@SuppressWarnings("unchecked")
class GrpcExceptionClientInterceptorTest {

    private final GrpcExceptionClientInterceptor interceptor = new GrpcExceptionClientInterceptor();

    private Channel channel;
    private ClientCall<Object, Object> rawCall;
    private ClientCall.Listener<Object> appListener;

    @BeforeEach
    void setUp() {
        channel = mock(Channel.class);
        rawCall = mock(ClientCall.class);
        appListener = mock(ClientCall.Listener.class);
        when(channel.newCall(any(), any())).thenReturn(rawCall);
    }

    /** 经客户端拦截器启动调用，并向还原监听器投递 onClose。 */
    private void deliverOnClose(Status status, Metadata trailers) {
        ClientCall<Object, Object> wrapped =
                interceptor.interceptCall(mock(MethodDescriptor.class), CallOptions.DEFAULT, channel);

        ArgumentCaptor<ClientCall.Listener<Object>> listenerCaptor =
                ArgumentCaptor.forClass(ClientCall.Listener.class);
        wrapped.start(appListener, new Metadata());
        verify(rawCall).start(listenerCaptor.capture(), any(Metadata.class));

        listenerCaptor.getValue().onClose(status, trailers);
    }

    @Test
    @DisplayName("业务 Trailers 存在 → Status cause 还原为 BusinessException（messageKey + params）")
    void onClose_withBusinessTrailers_restoresBusinessExceptionCause() {
        Metadata trailers = new Metadata();
        trailers.put(GrpcExceptionMetadata.MESSAGE_KEY, "order:err.insufficientStock");
        trailers.put(GrpcExceptionMetadata.PARAMS,
                GrpcExceptionParamsCodec.encode(Map.of("sku", "A001", "required", 10)));

        deliverOnClose(Status.FAILED_PRECONDITION.withDescription("order:err.insufficientStock"), trailers);

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(appListener).onClose(statusCaptor.capture(), any(Metadata.class));
        Status delivered = statusCaptor.getValue();

        assertThat(delivered.getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        BusinessException restored = GrpcExceptions.extractBusiness(
                delivered.asRuntimeException(trailers));
        assertThat(restored).isNotNull();
        assertThat(restored.getMessage()).isEqualTo("order:err.insufficientStock");
        assertThat(restored.getParams()).containsEntry("sku", "A001").containsEntry("required", 10);
    }

    @Test
    @DisplayName("无业务 Trailers → Status 原样透传（cause 为空）")
    void onClose_withoutBusinessTrailers_statusPassThrough() {
        deliverOnClose(Status.UNAVAILABLE.withDescription("server down"), new Metadata());

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(appListener).onClose(statusCaptor.capture(), any(Metadata.class));
        Status delivered = statusCaptor.getValue();

        assertThat(delivered.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(delivered.getCause()).isNull();
        assertThat(GrpcExceptions.extractBusiness(delivered.asRuntimeException(new Metadata()))).isNull();
    }

    @Test
    @DisplayName("正常完成（OK）→ 不触发还原逻辑")
    void onClose_okStatus_passThrough() {
        deliverOnClose(Status.OK, new Metadata());

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(appListener).onClose(statusCaptor.capture(), any(Metadata.class));
        assertThat(statusCaptor.getValue().isOk()).isTrue();
    }

    @Test
    @DisplayName("服务端 → 客户端全链路：服务端映射的 Trailers 可被客户端完整还原")
    void serverToClient_businessExceptionRoundTrip() {
        // 服务端侧：BusinessException 经服务端拦截器映射为 close(Status, Trailers)
        GrpcExceptionServerInterceptor serverInterceptor = new GrpcExceptionServerInterceptor();
        ServerCall<Object, Object> serverCall = mock(ServerCall.class);
        ServerCallHandler<Object, Object> next = mock(ServerCallHandler.class);
        ServerCall.Listener<Object> delegate = mock(ServerCall.Listener.class);
        when(next.startCall(any(), any(Metadata.class))).thenReturn(delegate);
        MethodDescriptor.Marshaller<Object> noopMarshaller = new MethodDescriptor.Marshaller<>() {
            @Override
            public java.io.InputStream stream(Object value) {
                return new java.io.ByteArrayInputStream(new byte[0]);
            }

            @Override
            public Object parse(java.io.InputStream stream) {
                return null;
            }
        };
        when(serverCall.getMethodDescriptor()).thenReturn(MethodDescriptor.<Object, Object>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("test.Service/Method")
                .setRequestMarshaller(noopMarshaller)
                .setResponseMarshaller(noopMarshaller)
                .build());
        doThrow(new BusinessException("product:err.notFound", Map.of("sku", "B002")))
                .when(delegate).onHalfClose();

        ServerCall.Listener<Object> serverListener =
                serverInterceptor.interceptCall(serverCall, new Metadata(), next);
        serverListener.onHalfClose();

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<Metadata> trailersCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(serverCall).close(statusCaptor.capture(), trailersCaptor.capture());

        // 客户端侧：收到的 Status + Trailers 经客户端拦截器还原
        deliverOnClose(statusCaptor.getValue(), trailersCaptor.getValue());

        ArgumentCaptor<Status> clientStatusCaptor = ArgumentCaptor.forClass(Status.class);
        verify(appListener).onClose(clientStatusCaptor.capture(), any(Metadata.class));
        BusinessException restored = GrpcExceptions.extractBusiness(
                clientStatusCaptor.getValue().asRuntimeException(trailersCaptor.getValue()));

        assertThat(restored).isNotNull();
        assertThat(restored.getMessage()).isEqualTo("product:err.notFound");
        assertThat(restored.getParams()).containsEntry("sku", "B002");
    }

    @Test
    @DisplayName("GrpcExceptions.extractBusiness 直接接收 BusinessException 时原样返回")
    void extractBusiness_directBusinessException_returned() {
        BusinessException direct = new BusinessException("direct:key");
        assertThat(GrpcExceptions.extractBusiness(direct)).isSameAs(direct);
        assertThat(GrpcExceptions.extractBusiness(new IllegalStateException("x"))).isNull();
        assertThat(GrpcExceptions.extractBusiness(
                Status.UNAVAILABLE.asRuntimeException(new Metadata()))).isNull();
    }
}
