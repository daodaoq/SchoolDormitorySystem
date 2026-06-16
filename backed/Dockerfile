# ===================== 构建阶段 =====================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# 复制 Maven wrapper 和 pom.xml (利用Docker缓存层)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# 复制源码并编译
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ===================== 运行阶段 =====================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非root用户
RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser
USER appuser

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
