FROM eclipse-temurin:21-jre-alpine-3.23

WORKDIR /usr/src/app

# OpenTelemetry Java Agent（零侵入自动链路追踪）
# 构建时从 GitHub Releases 下载，确保新克隆的仓库也能直接 docker build
# 版本升级：修改 OTEL_AGENT_VERSION 即可
ARG OTEL_AGENT_VERSION=2.14.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /app/otel-javaagent.jar

# 应用 jar
COPY target/service-*.jar app.jar

# ----- 稳定默认配置（环境无关，如需覆盖请在 docker run / compose 中 -e 显式传入）-----
# Spring Profile：默认 prod；本地/测试环境通过 -e SPRING_PROFILES_ACTIVE=dev 覆盖
ENV SPRING_PROFILES_ACTIVE=prod

# OpenTelemetry 配置（traces + logs 统一接入阿里云 ARMS）
# OTEL_SERVICE_NAME / OTEL_EXPORTER_OTLP_ENDPOINT 由运行时环境注入（docker-compose / K8s）
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_INSTRUMENTATION_LOGBACK_APPENDER_ENABLED=true
ENV OTEL_INSTRUMENTATION_LOGBACK_MDC_ENABLED=true
ENV OTEL_TRACES_SAMPLER=parentbased_always_on

# 8080：REST 面（Spring MVC，经 Higress 网关入口）
# 50051：gRPC 东西向内部通道（仅集群内部，不得直暴公网）
EXPOSE 8080 50051

# 健康检查：依赖 Spring Boot Actuator /actuator/health 端点
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

# JVM 参数说明：
#   -XX:MaxRAMPercentage=75.0   —— 容器感知，堆上限取 cgroup 内存的 75%（留 25% 给元空间/线程栈/堆外）
#   -XX:InitialRAMPercentage=50.0 —— 初始堆取 50%，避免冷启动过度分配
#   -Djdk.tracePinnedThreads=short —— 虚拟线程 pinning 检测（发现 synchronized 导致载体线程被钉住时打印短堆栈）
CMD ["java", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=50.0", \
     "-Djdk.tracePinnedThreads=short", \
     "-javaagent:/app/otel-javaagent.jar", \
     "-jar", \
     "app.jar"]
