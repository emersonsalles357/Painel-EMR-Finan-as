# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de Execução
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]