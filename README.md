# GGE Referee Report System

[![Docker Image build](https://github.com/Tyde/gaa-referee-report/actions/workflows/docker.yml/badge.svg)](https://github.com/Tyde/gaa-referee-report/actions/workflows/docker.yml)
[![Java CI with Gradle](https://github.com/Tyde/gaa-referee-report/actions/workflows/gradle.yml/badge.svg)](https://github.com/Tyde/gaa-referee-report/actions/workflows/gradle.yml)


This is a simple system for referees to report on games of tournaments. 
It is designed to be used by the GGE to replace the old Excel-Based approach.

## Deployment

This project is built as a docker container using Github actions. Therefore there is a Package available on the Github Package Repository. You can get the docker container with:
```
docker pull ghcr.io/tyde/gaa-referee-report:master
```
To deploy the system, this is a sample docker-compose.yml:
```yml
version: '3'
services:
  app:
    image: 'ghcr.io/tyde/gaa-referee-report:master'
    restart: unless-stopped
    ports:
      - '8095:8080'
    volumes:
      - database:/app/data
    environment:
      SERVER_URL: "https://base.url"
      MAILJET_PUBLIC: "public"
      MAILJET_SECRET: "secret"
      ADMIN_FIRST_NAME: "Your"
      ADMIN_LAST_NAME: "Name"
      ADMIN_MAIL: "mail"
      ADMIN_PASSWORD: "password"
      REDIS_HOST: "cache"
      REDIS_PORT: "6379"
      REDIS_PASSWORD: "password"
      POSTGRES_HOST: "db"
      POSTGRES_PORT: "5432"
      POSTGRES_DATABASE: "referee"
      POSTGRES_USER: "referee"
      POSTGRES_PASSWORD: "password"
  cache:
    image: 'redis/redis-stack-server:latest'
  db:
    image: 'postgres:latest'
    environment:
      POSTGRES_USER: "referee"
      POSTGRES_PASSWORD: 'password'
      POSTGRES_DB: 'referee'
    volumes:
      - pgsqldata:/var/lib/postgresql/data
volumes:
  database:
  pgsqldata:

```

## Sample video
[here](https://www.youtube.com/watch?v=gyzN0ipvogg)


## Development

The project consists of a backend written in Kotlin (Ktor framework; organized via gradle) and a frontend
written in Vue.js. To build the frontend you'll need to have node.js installed.
Build it via `npm run build` in the frontend directory. 
If you want to develop you can use `npm run watch-build` to continiously rebuild the files.
The build command then copies the files to the backend directory (resources/static folder).

## Usage

The backend needs some environment variables to be set.
Either you set them with the `gge-referee.properties` file which you 
can setup by looking at the  `gge-referee.properties.sample` file.

Otherwise you can set them as environment variables. 
The naming convention is like the following: 

`server.url` -> `SERVER_URL`

