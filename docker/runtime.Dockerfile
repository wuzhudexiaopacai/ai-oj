# 运行时镜像：jar 由宿主机本地 mvn package 产出（本地 ~/.m2 缓存热，秒级完成）
# 构建参数 MODULE 指定模块名，如 ai-oj-gateway
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/*.jar app.jar
# 默认容器内 Nacos 地址，可被 compose environment 覆盖
ENV NACOS_ADDR=nacos:8848
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
