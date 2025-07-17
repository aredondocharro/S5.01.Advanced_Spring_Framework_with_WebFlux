# Use a base image with Java 21
FROM eclipse-temurin:21-jdk

# Optional: metadata
LABEL maintainer="your_email@example.com"

# Set working directory inside the container
WORKDIR /app

# Copy the built JAR file to the container
COPY target/blackjack-api-0.0.1-SNAPSHOT.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
