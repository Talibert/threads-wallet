# Multi-stage Build

# Estágio 1: Build (Usando Maven e Java 21)
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Estágio 2: Runtime (O container que vai rodar de fato)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/threads-wallet-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]