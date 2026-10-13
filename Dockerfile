FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --create-home app

COPY --from=build /build/target/marketanalysis-0.1.0.jar ./app.jar

USER app

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]