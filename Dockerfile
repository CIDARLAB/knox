# Build the JAR
FROM maven:3.5-jdk-8 AS build
COPY src /usr/src/knox/src
COPY pom.xml /usr/src/knox
RUN mvn -f /usr/src/knox/pom.xml clean package

# Base image containing Java runtime
FROM openjdk:8-jdk-alpine
COPY --from=build /usr/src/knox/target/knox-0.0.1-SNAPSHOT.jar /usr/knox/knox-0.0.1-SNAPSHOT.jar
# Make port 8080 available to the world outside this container
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/knox/knox-0.0.1-SNAPSHOT.jar"]
