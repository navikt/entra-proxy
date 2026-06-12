ARG BASE_IMAGE=europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
FROM ${BASE_IMAGE}
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
