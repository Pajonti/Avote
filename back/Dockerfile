# --- Build : compile et découpe le jar en couches pour un meilleur cache d'image ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Les tests (dont Testcontainers) tournent en CI, pas dans la construction de l'image.
RUN mvn -B -q -DskipTests package \
 && java -Djarmode=tools -jar target/avote-back.jar extract --layers --destination target/extracted

# --- Exécution : JRE seul, utilisateur non-root ---
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /build/target/extracted/dependencies/ ./
COPY --from=build /build/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/target/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "avote-back.jar"]
