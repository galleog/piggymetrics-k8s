FROM openjdk:8-jdk-alpine
ENV PORT 8080
EXPOSE 8080

COPY build/libs/*.jar /opt/app.jar
WORKDIR /opt
CMD ["java", "-Xmx200m", "-jar", "/app.jar"]