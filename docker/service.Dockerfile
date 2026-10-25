# syntax=docker/dockerfile:1
# 通用微服务构建镜像（多阶段：Maven 构建 + JRE 运行）
# 构建参数 MODULE 指定要构建的模块名，如 ai-oj-gateway
# alpine 变体镜像更小，拉取更快
FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG MODULE
WORKDIR /build

# 使用阿里云 Maven 镜像加速依赖下载
COPY docker/maven/settings.xml /root/.m2/settings.xml

# 先拷贝全部模块的 pom.xml，使父 POM 的 <modules> 能解析（只拷目标模块会导致 reactor 报错）
COPY pom.xml .
COPY ai-oj-common/pom.xml ai-oj-common/pom.xml
COPY ai-oj-common/ai-oj-common-core/pom.xml ai-oj-common/ai-oj-common-core/pom.xml
COPY ai-oj-common/ai-oj-common-web/pom.xml ai-oj-common/ai-oj-common-web/pom.xml
COPY ai-oj-api/pom.xml ai-oj-api/pom.xml
COPY ai-oj-gateway/pom.xml ai-oj-gateway/pom.xml
COPY ai-oj-auth/pom.xml ai-oj-auth/pom.xml
COPY ai-oj-problem/pom.xml ai-oj-problem/pom.xml
COPY ai-oj-judge/pom.xml ai-oj-judge/pom.xml
COPY ai-oj-contest/pom.xml ai-oj-contest/pom.xml
COPY ai-oj-file/pom.xml ai-oj-file/pom.xml
COPY ai-oj-ai/pom.xml ai-oj-ai/pom.xml

# 再拷贝构建所需的源码（common + api + 目标模块）
COPY ai-oj-common/ ./ai-oj-common/
COPY ai-oj-api/ ./ai-oj-api/
COPY ${MODULE}/ ./${MODULE}/

# --mount cache 让 10 个模块、跨多次构建共享同一个本地仓库（第一个模块下载，其余命中）；
# -Dmaven.artifact.threads=16 构件并行下载；源码变化时本层只重新编译
RUN --mount=type=cache,target=/root/.m2/repository,sharing=shared \
    mvn -q -B -Dmaven.artifact.threads=16 -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /build/${MODULE}/target/*.jar app.jar
# 默认容器内 Nacos 地址，可被 compose environment 覆盖
ENV NACOS_ADDR=nacos:8848
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
