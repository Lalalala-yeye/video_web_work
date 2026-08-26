# ---------- 构建阶段 ----------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
# 先只拷 pom 与 maven wrapper，利用镜像层缓存加速依赖下载
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline || true
COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
# 敏感配置一律运行期注入（-e / K8s Secret），镜像内不落任何密钥
ENV SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/doinb?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai \
    SPRING_DATASOURCE_USERNAME=root \
    SPRING_DATASOURCE_PASSWORD=must-set-at-run \
    JWT_SECRET=must-set-at-run-at-least-32-chars!!! \
    UPLOAD_PATH=/app/uploads
# 视频与头像的上传目录挂卷持久化
VOLUME ["/app/uploads"]
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fs http://127.0.0.1:8081/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
