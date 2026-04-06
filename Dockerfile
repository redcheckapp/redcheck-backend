# Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy pom and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline
# Copy source code and compile skipping tests
COPY src ./src
RUN mvn clean package -DskipTests

# Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy generated .jar in Build
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Command to execute the app
ENTRYPOINT ["java", "-jar", "app.jar"]