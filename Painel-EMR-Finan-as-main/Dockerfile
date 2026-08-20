# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto
COPY . .

# Entra na subpasta onde o arquivo pom.xml realmente está e gera o .jar
WORKDIR "/app/BackEnd/Back End"
RUN mvn clean package -DskipTests

# Etapa de Execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar gerado na etapa de build para a pasta atual
COPY --from=build "/app/BackEnd/Back End/target/*.jar" app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]