# 消息巡查线服务（飞单/私单治理）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY risk-common/pom.xml risk-common/
COPY message-patrol-service/pom.xml message-patrol-service/
RUN mvn -B -q -pl message-patrol-service -am dependency:go-offline || true

COPY risk-common/src risk-common/src/
COPY message-patrol-service/src message-patrol-service/src/
RUN mvn -B -q -pl message-patrol-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r risk && useradd -r -g risk risk
COPY --from=build /build/message-patrol-service/target/message-patrol-service-1.0.0.jar app.jar
USER risk
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
