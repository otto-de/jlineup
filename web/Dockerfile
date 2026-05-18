FROM eclipse-temurin:25@sha256:c2b7ea21649875fb9052237ac4e3cd4ef63968a2a389a0a1b1a72a5e53e5c93f

USER root
RUN mkdir -p /usr/share/man/man1
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y wget xvfb \
    && apt-get install -yf chromium-browser firefox libjpeg-progs \
           webkit2gtk-driver epiphany-browser \
           gstreamer1.0-plugins-base gstreamer1.0-plugins-good \
           libgl1-mesa-dri \
    && wget -U "jlineup-docker" -O jlineup-web.jar https://repo1.maven.org/maven2/de/otto/jlineup-web/5.3.0/jlineup-web-5.3.0.jar
ADD docker/application.yml application.yml
RUN apt-get remove --auto-remove perl -yf && apt-get purge --auto-remove perl -yf
EXPOSE 8080

ENTRYPOINT ["java","-jar","/jlineup-web.jar"]
