FROM gradle:7-jdk17 AS build

RUN apt-get install -y curl \
  && curl -sL https://deb.nodesource.com/setup_18.x | bash - \
  && apt-get install -y nodejs \
  && curl -L https://www.npmjs.com/install.sh | sh

COPY --chown=gradle:gradle [ "build.gradle.kts", "settings.gradle.kts" ,"gradle.properties" , "/home/gradle/src/"]
COPY --chown=gradle:gradle [ "src/" , "/home/gradle/src/src/"]
COPY --chown=gradle:gradle [ "gaa-referee-report-common/" , "/home/gradle/src/gaa-referee-report-common/"]
COPY --chown=gradle:gradle [ "frontend-vite/" , "/home/gradle/src/frontend-vite/"]



WORKDIR /home/gradle/src/frontend-vite
RUN npm install
RUN npm run build

WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM openjdk:17
COPY --from=build /home/gradle/src/build/libs/*.jar /app/report-system.jar
WORKDIR /app
RUN mkdir data
VOLUME /app/data

ENV SERVER_URL=""
ENV MAILJET_PUBLIC=""
ENV MAILJET_SECRET=""

ENV ADMIN_FIRST_NAME=""
ENV ADMIN_LAST_NAME=""
ENV ADMIN_EMAIL=""
ENV ADMIN_PASSWORD=""

ENV REDIS_HOST="cache"
ENV REDIS_PORT="6379"
ENV REDIS_PASSWORD=""

ENV POSTGRES_HOST="db"
ENV POSTGRES_PORT="5432"
ENV POSTGRES_DB="referee"
ENV POSTGRES_USER="referee"
ENV POSTGRES_PASSWORD=""

EXPOSE 8080
ENTRYPOINT ["java","-jar","report-system.jar"]
