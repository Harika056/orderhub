# One Dockerfile for every service: docker build --build-arg MODULE=order-service .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN mvn -q -B -DskipTests -pl ${MODULE} -am package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
