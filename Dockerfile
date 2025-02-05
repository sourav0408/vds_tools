# Use a Maven image with JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Use a JDK 21 runtime for running the application
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built JAR from the first stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar demo.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "demo.jar"]
