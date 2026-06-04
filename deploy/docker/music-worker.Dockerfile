FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY apps ./apps
COPY modules ./modules
RUN ./gradlew :apps:music-worker:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --create-home --uid 10001 appuser
WORKDIR /app
COPY --from=build /workspace/apps/music-worker/build/libs/*.jar /app/app.jar
USER appuser
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD curl -fsS http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
