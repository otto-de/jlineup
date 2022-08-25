FROM public.ecr.aws/lambda/java:latest-x86_64

#WORKDIR /var/task

USER root
RUN sh -c 'echo -e "[google-chrome]\nname=google-chrome - 64-bit\nbaseurl=http://dl.google.com/linux/chrome/rpm/stable/x86_64\nenabled=1\ngpgcheck=1\ngpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub" >> /etc/yum.repos.d/google-chrome.repo'

RUN yum update -y

RUN yum install -y \
    GConf2.x86_64 \
    alsa-lib.x86_64 \
    at-spi2-atk.x86_64 \
    atk.x86_64 \
    bzip2.x86_64 \
    cups-libs.x86_64 \
    dbus-glib.x86_64 \
    glibc-devel.x86_64 \
    google-chrome-stable \
    gtk3.x86_64 \
    ipa-gothic-fonts \
    libXScrnSaver.x86_64 \
    libXcomposite.x86_64 \
    libXcursor.x86_64 \
    libXdamage.x86_64 \
    libXext.x86_64 \
    libXi.x86_64 \
    libXrandr.x86_64 \
    libXt.x86_64 \
    libXtst.x86_64 \
    libxkbcommon \
    mesa-libgbm.x86_64 \
    pango.x86_64 \
    procps \
    tar.x86_64 \
    util-linux.x86_64 \
    wget.x86_64 \
    xorg-x11-fonts-100dpi \
    xorg-x11-fonts-75dpi \
    xorg-x11-fonts-Type1 \
    xorg-x11-fonts-cyrillic \
    xorg-x11-fonts-misc \
    xorg-x11-utils

RUN mkdir ${LAMBDA_TASK_ROOT}/lib
RUN mkdir ${LAMBDA_TASK_ROOT}/lib/firefox
RUN wget --no-check-certificate -nv 'https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US' -O /tmp/firefox.tar.bz2
RUN tar -xf /tmp/firefox.tar.bz2 --directory ${LAMBDA_TASK_ROOT}/lib/
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox ${LAMBDA_TASK_ROOT}/firefox
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox /usr/bin/firefox
#RUN chmod +x ${LAMBDA_TASK_ROOT}/firefox
RUN rm /tmp/firefox.tar.bz2

#RUN ln -s /tmp /.cache
#RUN ln -s /tmp /.mozilla
#RUN /tmp/lib/firefox/firefox -headless -new-tab https://www.example.com
USER nobody

ENV HOME=/tmp/uusseerr

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
COPY build/resources/main ${LAMBDA_TASK_ROOT}
COPY build/dependency/* ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "de.otto.jlineup.lambda.JLineupHandler::handleRequest" ]