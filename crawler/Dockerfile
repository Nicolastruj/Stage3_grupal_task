FROM openjdk:21-jdk-slim AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY ../pom.xml /app/pom.xml
COPY . .
RUN mvn -pl crawler clean package -DskipTests

FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/crawler/target/crawler-1.0-SNAPSHOT.jar crawler.jar
CMD ["java", "-jar", "crawler.jar"]
