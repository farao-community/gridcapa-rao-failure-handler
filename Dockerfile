FROM farao/farao-computation-base:1.8.0

ARG JAR_FILE=rao-failure-handler/target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]