FROM gradle:7-jdk11 AS build

RUN apt-get install -y curl \
  && curl -sL https://deb.nodesource.com/setup_18.x | bash - \
  && apt-get install -y nodejs \
  && curl -L https://www.npmjs.com/install.sh | sh

COPY --chown=gradle:gradle [ "build.gradle.kts", "settings.gradle.kts" ,"gradle.properties" , "/home/gradle/src/"]
COPY --chown=gradle:gradle [ "src/" , "/home/gradle/src/src/"]
COPY --chown=gradle:gradle [ "frontend-vite/" , "/home/gradle/src/frontend-vite/"]



WORKDIR /home/gradle/src/frontend-vite
RUN npm install
RUN npm run build

WORKDIR /home/gradle/src
RUN gradle shadowJar

FROM openjdk:11
COPY --from=build /home/gradle/src/build/libs/*.jar /app/report-system.jar

VOLUME /app/data

ENV SERVER_URL=""
ENV MAILJET_PUBLIC=""
ENV MAILJET_SECRET=""

ENV ADMIN_FIRST_NAME=""
ENV ADMIN_LAST_NAME=""
ENV ADMIN_EMAIL=""
ENV ADMIN_PASSWORD=""

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/report-system.jar"]
