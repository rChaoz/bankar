FROM gradle:8-jdk17 AS build-backend
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle backend:buildFatJar --no-daemon


FROM openjdk:17 as backend
WORKDIR /app
COPY --from=build-backend /home/gradle/src/backend/build/libs/backend-all.jar .
EXPOSE 8080:8080
ENTRYPOINT ["java", "-jar", "backend-all.jar"]
