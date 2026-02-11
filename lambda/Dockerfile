FROM public.ecr.aws/lambda/java:21-x86_64

# This is used to grep the latest full release URL of Chrome with this Major version from the Chrome for Testing website
ARG CHROME_MAJOR_VERSION="139"

# This sets the specific version of the AMI Linux 2023 release
# This is to ensure that the latest version of the AMI Linux 2023 release is used for the dnf upgrade,
# as the java:21 lambda image is not always based on the latest AMI Linux 2023 release
ARG AMI_LINUX_2023_VERSION="2023.10.20260202"

USER root

RUN echo "$AMI_LINUX_2023_VERSION" > /etc/dnf/vars/releasever

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
    jq \
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
    libva \
    nss \
    pango \
    pulseaudio-libs \
    procps \
    tar \
    util-linux \
    vulkan \
    wget \
    xorg-x11-fonts-100dpi \
    xorg-x11-fonts-75dpi \
    xorg-x11-fonts-Type1 \
    xorg-x11-fonts-misc \
    xorg-x11-utils \
    xdg-utils \
    xz

RUN mkdir ${LAMBDA_TASK_ROOT}/lib
RUN mkdir ${LAMBDA_TASK_ROOT}/lib/firefox
RUN wget --no-check-certificate -nv 'https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US' -O /tmp/firefox.tar.xz
RUN tar -xf /tmp/firefox.tar.xz --directory ${LAMBDA_TASK_ROOT}/lib/
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox ${LAMBDA_TASK_ROOT}/firefox
RUN ln -s ${LAMBDA_TASK_ROOT}/lib/firefox/firefox /usr/bin/firefox
RUN rm /tmp/firefox.tar.xz

#RUN CHROME_FOR_TESTING_RELEASE="$(curl --silent https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json | jq '.channels.Stable')"; \
RUN curl --insecure https://googlechromelabs.github.io/chrome-for-testing/latest-versions-per-milestone-with-downloads.json
RUN CHROME_FOR_TESTING_RELEASE=$(curl --insecure --silent https://googlechromelabs.github.io/chrome-for-testing/latest-versions-per-milestone-with-downloads.json | jq ".milestones.\"$CHROME_MAJOR_VERSION\"") \
    CHROME_DOWNLOAD_URL="$(echo ""${CHROME_FOR_TESTING_RELEASE}"" | jq -r '.downloads.chrome[] | select(.platform == "linux64") | .url')"; \
    curl --insecure --remote-name --silent "${CHROME_DOWNLOAD_URL}"; \
    ls -alh; \
    unzip -q chrome-linux64.zip; \
    ln --symbolic "${PWD}/chrome-linux64/chrome" /usr/local/bin/chrome; \
    CHROMEDRIVER_DOWNLOAD_URL="$(echo ""${CHROME_FOR_TESTING_RELEASE}"" | jq -r '.downloads.chromedriver[] | select(.platform == "linux64") | .url')"; \
    curl --insecure --remote-name --silent "${CHROMEDRIVER_DOWNLOAD_URL}"; \
    unzip -q chromedriver-linux64.zip; \
    install --mode=+rwx "${PWD}/chromedriver-linux64/chromedriver" /usr/local/bin/chromedriver

USER nobody
ENV HOME=/tmp/uusseerr

RUN ls -alh /usr/local/bin
RUN /usr/local/bin/chrome --version
RUN /usr/local/bin/chromedriver --version

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
#COPY build/resources/main ${LAMBDA_TASK_ROOT}
COPY build/output/lib/* ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "de.otto.jlineup.lambda.JLineupHandler::handleRequest" ]
