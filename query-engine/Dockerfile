FROM openjdk:21-jdk-slim AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY ../pom.xml /app/pom.xml
COPY . .
RUN mvn -pl query-engine clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/query-engine/target/query-engine-1.0-SNAPSHOT.jar query-engine.jar
CMD ["java", "-jar", "query-engine.jar"]
