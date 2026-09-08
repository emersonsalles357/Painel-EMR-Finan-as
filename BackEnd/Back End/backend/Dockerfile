FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp clean package

FROM eclipse-temurin:17-jre

RUN groupadd --system spring \
    && useradd --system --gid spring --create-home spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/app.jar /app/app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
