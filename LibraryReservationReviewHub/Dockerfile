# Build the Spring Boot jar, then run it on a slim JRE image
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar -x test && cp "$(ls build/libs/*.jar | grep -v plain)" app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
# the app runs as a user of its own, not root; it can write only tmp/ (the chat memory), not the jar
RUN groupadd --system app && useradd --system --gid app --home-dir /app --no-create-home app \
    && mkdir -p /app/tmp/chat-memory && chown -R app:app /app/tmp
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8432
# healthy once the API answers on a public route that also reaches the database
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:8432/api/book/stats > /dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
