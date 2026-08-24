# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY shared-contracts/pom.xml shared-contracts/
COPY modules/identity/pom.xml modules/identity/
COPY modules/catalog/pom.xml modules/catalog/
COPY modules/orders/pom.xml modules/orders/
COPY modules/payments/pom.xml modules/payments/
COPY app/pom.xml app/
RUN mvn -q -B dependency:go-offline || true
COPY shared-contracts/src shared-contracts/src
COPY modules/identity/src modules/identity/src
COPY modules/catalog/src modules/catalog/src
COPY modules/orders/src modules/orders/src
COPY modules/payments/src modules/payments/src
COPY app/src app/src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /build/app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
