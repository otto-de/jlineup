FROM eclipse-temurin:17

USER root
RUN mkdir -p /usr/share/man/man1
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y wget \
    && apt-get install -yf chromium-browser firefox libjpeg-progs \
    && wget -U "jlineup-docker" -O jlineup-web.jar https://repo1.maven.org/maven2/de/otto/jlineup-web/4.6.0/jlineup-web-4.6.0.jar
ADD docker/application.yml application.yml
RUN apt-get remove --auto-remove perl -yf && apt-get purge --auto-remove perl -yf
EXPOSE 8080

ENTRYPOINT ["java","-jar","/jlineup-web.jar"]
