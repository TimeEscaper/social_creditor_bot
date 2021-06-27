FROM gradle:6.9.0-jdk11 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ls -lah
RUN ./gradlew clean build --no-daemon

FROM openjdk:11-jre-slim

RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

ENTRYPOINT java -jar /app/app.jar --token $BOT_TOKEN --db-path /db/database.db