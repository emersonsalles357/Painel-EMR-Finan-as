# Etapa de Build (compilação com Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto
COPY . .

# Procura o arquivo pom.xml em qualquer subpasta e compila direto dele
RUN mvn clean package -DskipTests -f $(find . -name pom.xml)

# Etapa de Execução
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copia o .jar gerado em qualquer subpasta target
COPY --from=build /app/**/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]