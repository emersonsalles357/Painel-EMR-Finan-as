# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto para o container
COPY . .

# Entra na pasta do Back-End onde está o pom.xml
WORKDIR "/app/BackEnd/Back End"

# Compila o projeto a partir dessa pasta
RUN mvn clean package -DskipTests

# Etapa de Execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar gerado para a raiz de execução
COPY --from=build ["/app/BackEnd/Back End/target/*.jar", "app.jar"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]