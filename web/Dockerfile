FROM debian:bullseye-slim

USER root
RUN mkdir -p /usr/share/man/man1
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y wget \
    && apt-get install -yf default-jre-headless chromium firefox-esr libjpeg-progs \
    && wget -U "jlineup-docker" -O jlineup-web.jar https://repo1.maven.org/maven2/de/otto/jlineup-web/4.3.4/jlineup-web-4.3.4.jar
ADD docker/application.yml application.yml
RUN apt-get remove --auto-remove perl -yf && apt-get purge --auto-remove perl -yf
EXPOSE 8080

ENTRYPOINT ["java","-jar","/jlineup-web.jar"]
