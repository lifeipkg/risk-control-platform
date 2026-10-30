# 账号巡查线服务（接码注册/养号治理）
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY risk-common/pom.xml risk-common/
COPY account-patrol-service/pom.xml account-patrol-service/
RUN mvn -B -q -pl account-patrol-service -am dependency:go-offline || true

COPY risk-common/src risk-common/src/
COPY account-patrol-service/src account-patrol-service/src/
RUN mvn -B -q -pl account-patrol-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r risk && useradd -r -g risk risk
COPY --from=build /build/account-patrol-service/target/account-patrol-service-1.0.0.jar app.jar
USER risk
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
