FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests package
FROM eclipse-temurin:26-jre-noble
WORKDIR /app
COPY --from=build /build/target/tecgensoft-turf-service-*.jar app.jar
USER 1001
ENTRYPOINT ["java","-jar","app.jar"]
