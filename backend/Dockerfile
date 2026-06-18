FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle* settings.gradle* ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

EXPOSE 8080

CMD ["./gradlew", "bootRun"]