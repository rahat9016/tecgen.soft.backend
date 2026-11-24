FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw -B -DskipTests package
FROM eclipse-temurin:26-jre-noble
RUN groupadd --system --gid 1001 gateway && useradd --system --uid 1001 --gid 1001 --shell /usr/sbin/nologin gateway
WORKDIR /app
COPY --from=build --chown=gateway:gateway /build/target/tecgensoft-gateway-*.jar app.jar
USER 1001
EXPOSE 8000
ENTRYPOINT ["java","-jar","/app/app.jar"]
