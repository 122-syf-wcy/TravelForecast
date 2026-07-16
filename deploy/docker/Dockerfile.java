# syntax=docker/dockerfile:1.6
#
# 通用 Java 服务 Dockerfile：多阶段构建，两行参数即可复用于
# Gateway / Backend / AI-Backend / MiniProgramBackend。
#
# 用法（docker-compose 中通过 build.args 传入）：
#   SERVICE_DIR=TravelForecastGateway
#   JAR_NAME=travel-gateway-1.0.0.jar

ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-17
ARG RUNTIME_IMAGE=eclipse-temurin:17-jre-jammy

FROM ${MAVEN_IMAGE} AS builder
ARG SERVICE_DIR
WORKDIR /workspace

# 先单独复制 pom，利用缓存预下载依赖
COPY ${SERVICE_DIR}/pom.xml ${SERVICE_DIR}/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    cd ${SERVICE_DIR} && mvn -B -ntp dependency:go-offline

COPY ${SERVICE_DIR} ${SERVICE_DIR}
RUN --mount=type=cache,target=/root/.m2 \
    cd ${SERVICE_DIR} && mvn -B -ntp -DskipTests package

FROM ${RUNTIME_IMAGE}
ARG SERVICE_DIR
ARG JAR_NAME
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=70.0"
WORKDIR /opt/app

COPY --from=builder /workspace/${SERVICE_DIR}/target/${JAR_NAME} /opt/app/app.jar

EXPOSE 8080 8081 8082 8083 8001 8888
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/app/app.jar"]
