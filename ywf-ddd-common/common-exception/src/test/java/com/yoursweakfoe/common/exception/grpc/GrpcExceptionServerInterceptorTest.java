package com.yoursweakfoe.common.exception.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yoursweakfoe.common.exception.BusinessException;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("GrpcExceptionServerInterceptor — gRPC 服务端异常映射")
@SuppressWarnings("unchecked")
class GrpcExceptionServerInterceptorTest {

    private final GrpcExceptionServerInterceptor interceptor = new GrpcExceptionServerInterceptor();

    private ServerCall<Object, Object> call;
    private ServerCallHandler<Object, Object> next;
    private ServerCall.Listener<Object> delegate;

    private static final MethodDescriptor.Marshaller<Object> NOOP_MARSHALLER =
            new MethodDescriptor.Marshaller<>() {
                @Override
                public InputStream stream(Object value) {
                    return new ByteArrayInputStream(new byte[0]);
                }

                @Override
                public Object parse(InputStream stream) {
                    return null;
                }
            };

    /** 捕获 {@code call.close} 的 Status + Trailers。 */
    private record CloseCapture(Status status, Metadata trailers) {}

    @BeforeEach
    void setUp() {
        call = mock(ServerCall.class);
        next = mock(ServerCallHandler.class);
        delegate = mock(ServerCall.Listener.class);
        when(next.startCall(any(), any(Metadata.class))).thenReturn(delegate);
        MethodDescriptor<Object, Object> method = MethodDescriptor.<Object, Object>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("test.Service/Method")
                .setRequestMarshaller(NOOP_MARSHALLER)
                .setResponseMarshaller(NOOP_MARSHALLER)
                .build();
        when(call.getMethodDescriptor()).thenReturn(method);
    }

    /** 触发一次 onHalfClose 异常映射，返回捕获的 close 载荷。 */
    private CloseCapture triggerAndCapture(RuntimeException thrown) {
        doThrow(thrown).when(delegate).onHalfClose();
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(call, new Metadata(), next);
        listener.onHalfClose();

        ArgumentCaptor<Status> statusCaptor = ArgumentCaptor.forClass(Status.class);
        ArgumentCaptor<Metadata> trailersCaptor = ArgumentCaptor.forClass(Metadata.class);
        verify(call).close(statusCaptor.capture(), trailersCaptor.capture());
        return new CloseCapture(statusCaptor.getValue(), trailersCaptor.getValue());
    }

    @Test
    @DisplayName("BusinessException → FAILED_PRECONDITION + Trailers（messageKey + params JSON）")
    void businessException_mapsToFailedPreconditionWithTrailers() {
        CloseCapture capture = triggerAndCapture(
                new BusinessException("order:err.insufficientStock",
                        Map.of("sku", "A001", "required", 10)));

        assertThat(capture.status().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(capture.status().getDescription()).isEqualTo("order:err.insufficientStock");
        assertThat(capture.trailers().get(GrpcExceptionMetadata.MESSAGE_KEY))
                .isEqualTo("order:err.insufficientStock");
        // params 以 JSON 字节写入二进制 trailer，可解码还原
        Map<String, Object> decoded =
                GrpcExceptionParamsCodec.decode(capture.trailers().get(GrpcExceptionMetadata.PARAMS));
        assertThat(decoded).containsEntry("sku", "A001").containsEntry("required", 10);
    }

    @Test
    @DisplayName("BusinessException 无 params → 不写入 params Trailer")
    void businessExceptionWithoutParams_noParamsTrailer() {
        CloseCapture capture = triggerAndCapture(new BusinessException("order:err.notFound"));

        assertThat(capture.status().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(capture.trailers().get(GrpcExceptionMetadata.MESSAGE_KEY)).isEqualTo("order:err.notFound");
        assertThat(capture.trailers().get(GrpcExceptionMetadata.PARAMS)).isNull();
    }

    @Test
    @DisplayName("IllegalStateException → FAILED_PRECONDITION（无业务 Trailers）")
    void illegalState_mapsToFailedPrecondition() {
        CloseCapture capture = triggerAndCapture(new IllegalStateException("Order already confirmed"));

        assertThat(capture.status().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(capture.status().getDescription()).isEqualTo("Order already confirmed");
        assertThat(capture.trailers().get(GrpcExceptionMetadata.MESSAGE_KEY)).isNull();
    }

    @Test
    @DisplayName("IllegalArgumentException → INVALID_ARGUMENT")
    void illegalArgument_mapsToInvalidArgument() {
        CloseCapture capture = triggerAndCapture(new IllegalArgumentException("quantity must be positive"));

        assertThat(capture.status().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(capture.status().getDescription()).isEqualTo("quantity must be positive");
    }

    @Test
    @DisplayName("未知 RuntimeException → INTERNAL（原始信息仅落日志，不进 description）")
    void unknownException_mapsToInternalWithoutDetails() {
        CloseCapture capture = triggerAndCapture(new RuntimeException("secret internal detail"));

        assertThat(capture.status().getCode()).isEqualTo(Status.Code.INTERNAL);
        assertThat(capture.status().getDescription()).isEqualTo("Internal error");
        assertThat(capture.status().getDescription()).doesNotContain("secret internal detail");
        assertThat(capture.trailers().get(GrpcExceptionMetadata.MESSAGE_KEY)).isNull();
    }

    @Test
    @DisplayName("无异常回调 → 不关闭调用")
    void noException_doesNotCloseCall() {
        ServerCall.Listener<Object> listener =
                interceptor.interceptCall(call, new Metadata(), next);
        listener.onHalfClose();

        verify(call, never()).close(any(Status.class), any(Metadata.class));
        verify(delegate).onHalfClose();
    }
}
