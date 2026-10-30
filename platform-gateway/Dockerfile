# 平台统一接入网关
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY risk-common/pom.xml risk-common/
COPY platform-gateway/pom.xml platform-gateway/
RUN mvn -B -q -pl platform-gateway -am dependency:go-offline || true

COPY risk-common/src risk-common/src/
COPY platform-gateway/src platform-gateway/src/
RUN mvn -B -q -pl platform-gateway -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r risk && useradd -r -g risk risk
COPY --from=build /build/platform-gateway/target/platform-gateway-1.0.0.jar app.jar
USER risk
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
