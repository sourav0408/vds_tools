
# First stage: Build the application
FROM maven:3.8.5-openjdk-17 AS build

# Set working directory inside the container
WORKDIR /app

# Copy all files from the project into the container
COPY . .

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Second stage: Run the built JAR file using OpenJDK
FROM openjdk:17.0.1-jdk-slim

# Set working directory inside the container
WORKDIR /app

# Copy the generated JAR file from the first stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar demo.jar

# Expose the port that Spring Boot runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "demo.jar"]
