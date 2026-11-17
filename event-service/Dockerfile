# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy POM files for dependency caching
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY event-service/pom.xml event-service/
COPY user-service/pom.xml user-service/

# Cache dependencies
RUN mvn dependency:go-offline -B -pl event-service -am || true

# Copy source code and build event-service
COPY event-service/src event-service/src
RUN mvn clean package -DskipTests -pl event-service -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/event-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
