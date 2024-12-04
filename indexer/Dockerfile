FROM openjdk:21-jdk-slim AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY ../pom.xml /app/pom.xml
COPY . .
ARG PROFILE=expandedIndexer
RUN mvn -pl indexer clean package -DskipTests -P$PROFILE

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/indexer/target/indexer-1.0-SNAPSHOT.jar indexer.jar
COPY --from=build /app/indexer/src/main/resources /app/resources
CMD ["java", "-jar", "indexer.jar"]
