package com.yoursweakfoe.common.exception.grpc;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * BusinessException params 的 JSON 编解码（gRPC Trailer 载荷）。
 *
 * <p>使用 Jackson 3（Boot 4 默认 JSON 库，版本由 Boot dependency management 托管）。
 * 解码失败时返回空 Map 并记录告警——params 丢失不应阻断异常本身的还原。
 */
final class GrpcExceptionParamsCodec {

    private static final Logger log = LoggerFactory.getLogger(GrpcExceptionParamsCodec.class);

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE =
            new TypeReference<>() {};

    private GrpcExceptionParamsCodec() {}

    /** Map → JSON 字节（UTF-8）。 */
    static byte[] encode(Map<String, Object> params) {
        return MAPPER.writeValueAsBytes(params);
    }

    /** JSON 字节 → Map；入参为 null 或解析失败时返回空 Map。 */
    static Map<String, Object> decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(bytes, MAP_TYPE);
        } catch (RuntimeException e) {
            log.warn("Failed to decode business exception params from gRPC trailers", e);
            return Map.of();
        }
    }
}
