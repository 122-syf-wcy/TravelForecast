FROM eclipse-temurin:17-jre

LABEL maintainer="TravelForecast Gateway"
LABEL description="智教黔行 - API网关服务"

WORKDIR /app

# 复制本地已打包好的 JAR
COPY target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露网关端口
EXPOSE 8888

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8888/actuator/health || exit 1

# 启动命令 - 使用 docker profile
ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=docker", \
  "-jar", "app.jar"]
