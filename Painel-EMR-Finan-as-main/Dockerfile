# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto
COPY . .

# Encontra a pasta do pom.xml, entra nela, compila e move o .jar para /app/app.jar
RUN cd "$(dirname "$(find . -name pom.xml | head -n 1)")" && mvn clean package -DskipTests && cp target/*.jar /app/app.jar

# Etapa de Execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar pronto da etapa de build
COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]