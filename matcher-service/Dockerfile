FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/matcher-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]