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
# Sized for a 512 MB host (Render's free plan): small heap, thread stacks and code cache, so the whole process
# stays well under the container limit. If the heap ever runs out the JVM exits and Render restarts it, which is
# better than a half-dead process.
ENV JAVA_TOOL_OPTIONS="-Xmx192m -Xss512k -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
