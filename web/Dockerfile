FROM debian:buster-slim

USER root
RUN mkdir -p /usr/share/man/man1
RUN apt-get update \
    && apt-get install -y wget \
    && apt-get install -yf default-jre-headless chromium firefox-esr libjpeg-progs \
    && wget -U "jlineup-docker" -O jlineup-web.jar http://central.maven.org/maven2/de/otto/jlineup-web/3.0.2/jlineup-web-3.0.2.jar
ADD docker/application.yml application.yml
RUN apt-get remove --auto-remove perl -yf && apt-get purge --auto-remove perl -yf
EXPOSE 8080

ENTRYPOINT ["java","-jar","/jlineup-web.jar"]
