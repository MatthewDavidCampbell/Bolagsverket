# Build from the repository root, since the service is built together with the libraries:
#   docker build -f service/Dockerfile -t countries-api .

# Build stage: compiles all modules with the Maven wrapper (tests run in CI, not here).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY . .
RUN ./mvnw -B -q -DskipTests package

# Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /src/service/target/service.jar service.jar
USER app
EXPOSE 8080
# Pass -e RESTCOUNTRIES_API_KEY=... to import live data; otherwise the bundled snapshot is used.
# Kubernetes probes: /actuator/health/liveness and /actuator/health/readiness.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "service.jar"]
