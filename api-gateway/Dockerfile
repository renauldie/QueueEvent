# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy POM files for dependency caching
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY event-service/pom.xml event-service/
COPY user-service/pom.xml user-service/

# Cache dependencies
RUN mvn dependency:go-offline -B -pl api-gateway -am || true

# Copy source code and build api-gateway
COPY api-gateway/src api-gateway/src
RUN mvn clean package -DskipTests -pl api-gateway -am

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
