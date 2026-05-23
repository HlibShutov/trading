FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/wallet-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081
EXPOSE 9091

ENTRYPOINT ["java", "-jar", "app.jar"]