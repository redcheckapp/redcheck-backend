# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Build application
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: Create and use non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]