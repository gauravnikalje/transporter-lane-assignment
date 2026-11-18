# ---- build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI (see .github/workflows/ci.yml), not in the image build.
RUN ./mvnw -B -q -DskipTests package

# ---- run
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 app
USER app
WORKDIR /app
COPY --from=build /build/target/app.jar app.jar
# Small heap for a 512 MB host (Render's free plan).
ENV JAVA_TOOL_OPTIONS="-Xmx320m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
