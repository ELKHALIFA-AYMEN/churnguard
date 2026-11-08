# --- Étape 1 : compilation ---
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# --- Étape 2 : image d'exécution, légère et sans droits particuliers ---
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home --uid 10001 appuser
WORKDIR /app
COPY --from=build /build/target/backend-*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]