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
# 与 CI / compose 同一套变量（见 application-ci.yml）。密码与 JWT 必须运行期覆盖。
# compose 会把 MYSQL_HOST 设为 mysql；单独 docker run 时默认连宿主机 MySQL。
ENV SPRING_PROFILES_ACTIVE=ci \
    MYSQL_HOST=host.docker.internal \
    MYSQL_PORT=3306 \
    MYSQL_DATABASE=doinb \
    MYSQL_USER=root \
    MYSQL_PASSWORD=must-set-at-run \
    JWT_SECRET=must-set-at-run-at-least-32-chars!!! \
    UPLOAD_PATH=/app/uploads
# 视频与头像的上传目录挂卷持久化
VOLUME ["/app/uploads"]
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fs http://127.0.0.1:8081/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
