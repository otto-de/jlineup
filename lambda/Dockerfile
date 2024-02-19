FROM public.ecr.aws/lambda/java:21-x86_64

#WORKDIR /var/task

USER root

RUN echo "2023.3.20240205" > /etc/dnf/vars/releasever
RUN dnf clean all; dnf upgrade -y
RUN dnf install -y \
    alsa-lib \
    at-spi2-atk \
    atk \
    bzip2 \
    cups-libs \
    dbus-glib \
    glibc-devel \
    gtk3 \
    ipa-gothic-fonts \
    libdrm \
    libXScrnSaver \
    libXcomposite \
    libXcursor \
    libXdamage \
    liberation-fonts \
    libXext \
    libXi \
    libXrandr \
    libXt \
    libXtst \
    libxkbcommon \
    mesa-libgbm \
    libgbm \
    pango \
    procps \
    tar \
    util-linux \
    wget \
    xorg-x11-fonts-100dpi \
    xorg-x11-fonts-75dpi \
    xorg-x11-fonts-Type1 \
    xorg-x11-fonts-misc \
    xorg-x11-utils \
    xdg-utils \
    vulkan \
    libva \
    pulseaudio-libs \
    nss \
    jq


RUN mkdir ${LAMBDA_TASK_ROOT}/lib
RUN mkdir ${LAMBDA_TASK_ROOT}/lib/firefox
RUN wget --no-check-certificate -nv 'https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US' -O /tmp/firefox.tar.bz2
RUN tar -xf /tmp/firefox.tar.bz2 --directory ${LAMBDA_TASK_ROOT}/lib/
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox ${LAMBDA_TASK_ROOT}/firefox
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox /usr/bin/firefox
RUN rm /tmp/firefox.tar.bz2

#RUN CHROME_FOR_TESTING_RELEASE="$(curl --silent https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json | jq '.channels.Stable')"; \
RUN CHROME_FOR_TESTING_RELEASE=$(curl --silent https://googlechromelabs.github.io/chrome-for-testing/latest-versions-per-milestone-with-downloads.json | jq '.milestones."119"') \
    CHROME_DOWNLOAD_URL="$(echo ""${CHROME_FOR_TESTING_RELEASE}"" | jq -r '.downloads.chrome[] | select(.platform == "linux64") | .url')"; \
    curl --remote-name --silent "${CHROME_DOWNLOAD_URL}"; \
    unzip -q chrome-linux64.zip; \
    ln --symbolic "${PWD}/chrome-linux64/chrome" /usr/local/bin/chrome; \
    CHROMEDRIVER_DOWNLOAD_URL="$(echo ""${CHROME_FOR_TESTING_RELEASE}"" | jq -r '.downloads.chromedriver[] | select(.platform == "linux64") | .url')"; \
    curl --remote-name --silent "${CHROMEDRIVER_DOWNLOAD_URL}"; \
    unzip -q chromedriver-linux64.zip; \
    install --mode=+rwx "${PWD}/chromedriver-linux64/chromedriver" /usr/local/bin/chromedriver

USER nobody
ENV HOME=/tmp/uusseerr

RUN ls -alh /usr/local/bin
RUN /usr/local/bin/chrome --version
RUN /usr/local/bin/chromedriver --version

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
COPY build/resources/main ${LAMBDA_TASK_ROOT}
COPY build/output/lib/* ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "de.otto.jlineup.lambda.JLineupHandler::handleRequest" ]