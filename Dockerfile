FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app
WORKDIR /app
COPY --chown=app:app build/libs/*.jar app.jar
USER app
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
