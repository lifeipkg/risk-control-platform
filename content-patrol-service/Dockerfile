# 内容巡查线服务（岗位真实性巡查）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom 充分利用构建缓存
COPY pom.xml .
COPY risk-common/pom.xml risk-common/
COPY content-patrol-service/pom.xml content-patrol-service/
RUN mvn -B -q -pl content-patrol-service -am dependency:go-offline || true

# 拷贝源码并构建
COPY risk-common/src risk-common/src/
COPY content-patrol-service/src content-patrol-service/src/
RUN mvn -B -q -pl content-patrol-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r risk && useradd -r -g risk risk
COPY --from=build /build/content-patrol-service/target/content-patrol-service-1.0.0.jar app.jar
USER risk
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
