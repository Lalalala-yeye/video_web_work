# 微服务共用构建：context 必须是 services/
# docker build -f services/Dockerfile --build-arg MODULE=doinb-gateway --build-arg PORT=8081 services/
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY . .
ARG MODULE=doinb-gateway
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ARG MODULE=doinb-gateway
ARG PORT=8080
ARG APP_VERSION=dev
ENV SERVER_PORT=${PORT}
ENV APP_VERSION=${APP_VERSION}
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
COPY --from=builder /build/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar app.jar
EXPOSE ${PORT}
HEALTHCHECK --interval=15s --timeout=5s --start-period=75s --retries=5 \
  CMD curl -fs "http://127.0.0.1:${SERVER_PORT}/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
