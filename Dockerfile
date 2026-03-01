# ---- Build stage ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# JVM tuning for Neo4j + Spring AI workloads
ENV JAVA_OPTS="-Xms512m -Xmx2g"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

