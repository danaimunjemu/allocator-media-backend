FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
COPY geoip ./geoip
RUN chown -R app:app /app
USER app
EXPOSE 8080
# Render's free tier caps the container at 512MB. MaxRAMPercentage alone isn't
# enough — it only bounds the heap, while metaspace (a large Spring Boot
# monolith with this many dependencies loads a lot of classes), the JIT code
# cache, and per-thread stacks are separate, unbounded regions that pushed
# total usage past the container limit and got the process SIGKILLed (exit
# 137). Capping every region explicitly and using SerialGC (far lower
# baseline overhead than G1, the default) keeps total usage predictable on a
# single small core.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=50 -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -Xss256k -XX:+UseSerialGC -jar app.jar"]
