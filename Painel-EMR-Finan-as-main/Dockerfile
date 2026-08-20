# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto
COPY . .

# Entra na subpasta correta usando aspas para suportar o espaço no nome e compila
RUN cd "/app/BackEnd/Back End" && mvn clean package -DskipTests

# Etapa de Execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar gerado usando a sintaxe de array/colchetes
COPY --from=build ["/app/BackEnd/Back End/target/*.jar", "app.jar"]
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]