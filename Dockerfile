FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY ["BackEnd/Back End/backend/pom.xml", "./pom.xml"]
COPY ["BackEnd/Back End/backend/src", "./src"]
RUN mvn --batch-mode --no-transfer-progress clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system emr && useradd --system --gid emr emr
COPY --from=build --chown=emr:emr /build/target/emr-financas-0.0.1-SNAPSHOT.jar /app/app.jar
USER emr
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
