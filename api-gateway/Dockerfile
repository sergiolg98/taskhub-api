# syntax=docker/dockerfile:1

# ---------- Stage 1: build (Maven + JDK). Nothing from this stage ends up in the final image except the files copied below. ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# The pom goes first: this layer (all the dependencies) is rebuilt only when pom.xml changes, not on every code change.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src src
# Tests are skipped on purpose: they run in their own stage of the pipeline (class 13), not while building the image.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
 && cp target/*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination layers

# ---------- Stage 2: runtime (JRE only, no Maven, no sources) ----------
FROM eclipse-temurin:21-jre

# Never run as root inside the container.
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app

# Spring Boot layers, from the least to the most frequently changing: a code change only rebuilds the last layer.
COPY --from=build /workspace/layers/dependencies/ ./
COPY --from=build /workspace/layers/spring-boot-loader/ ./
COPY --from=build /workspace/layers/snapshot-dependencies/ ./
COPY --from=build /workspace/layers/application/ ./

USER app
EXPOSE 8080

# curl exists in this image (checked on eclipse-temurin:21-jre); on an -alpine variant use wget instead.
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
