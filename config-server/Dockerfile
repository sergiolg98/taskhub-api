# Stage 1: build with Maven. Nothing from this stage ends up in the final image except the JAR.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# The pom goes first: the dependencies are downloaded again only when pom.xml changes.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
# Tests run in their own stage of the pipeline (class 13), not while building the image.
RUN mvn -q -DskipTests package

# Stage 2: run with a JRE only (no Maven, no sources).
FROM eclipse-temurin:21-jre
# Never run as root inside the container.
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
# The configuration repository is NOT inside the image: mount it (docker run -v ../config-repo:/config-repo).
ENV CONFIG_REPO_PATH=/config-repo
USER app
EXPOSE 8888

# Docker asks Actuator if the service is healthy; Compose uses it to start services in order (class 13).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8888/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
