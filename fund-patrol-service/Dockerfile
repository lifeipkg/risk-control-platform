# 资金巡查线服务（补贴薅取治理）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY risk-common/pom.xml risk-common/
COPY fund-patrol-service/pom.xml fund-patrol-service/
RUN mvn -B -q -pl fund-patrol-service -am dependency:go-offline || true

COPY risk-common/src risk-common/src/
COPY fund-patrol-service/src fund-patrol-service/src/
RUN mvn -B -q -pl fund-patrol-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r risk && useradd -r -g risk risk
COPY --from=build /build/fund-patrol-service/target/fund-patrol-service-1.0.0.jar app.jar
USER risk
EXPOSE 8084
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
