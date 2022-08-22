FROM public.ecr.aws/lambda/java:latest-x86_64

#WORKDIR /var/task

USER root
RUN sh -c 'echo -e "[google-chrome]\nname=google-chrome - 64-bit\nbaseurl=http://dl.google.com/linux/chrome/rpm/stable/x86_64\nenabled=1\ngpgcheck=1\ngpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub" >> /etc/yum.repos.d/google-chrome.repo'

RUN yum update -y
RUN yum install -y pango.x86_64
RUN yum install -y libXcomposite.x86_64
RUN yum install -y libXcursor.x86_64
RUN yum install -y libXdamage.x86_64
RUN yum install -y libXext.x86_64
RUN yum install -y libXi.x86_64
RUN yum install -y libXtst.x86_64
RUN yum install -y cups-libs.x86_64
RUN yum install -y libXScrnSaver.x86_64
RUN yum install -y libXrandr.x86_64
RUN yum install -y GConf2.x86_64
RUN yum install -y alsa-lib.x86_64
RUN yum install -y atk.x86_64
RUN yum install -y gtk3.x86_64
RUN yum install -y ipa-gothic-fonts
RUN yum install -y xorg-x11-fonts-100dpi
RUN yum install -y xorg-x11-fonts-75dpi
RUN yum install -y xorg-x11-utils
RUN yum install -y xorg-x11-fonts-cyrillic
RUN yum install -y xorg-x11-fonts-Type1
RUN yum install -y xorg-x11-fonts-misc
RUN yum install -y glibc-devel.x86_64
RUN yum install -y at-spi2-atk.x86_64
RUN yum install -y mesa-libgbm.x86_64
RUN yum install -y libxkbcommon

RUN yum install -y util-linux.x86_64
RUN yum install -y tar.x86_64
RUN yum install -y bzip2.x86_64
RUN yum install -y dbus-glib.x86_64
RUN yum install -y libXt.x86_64
RUN yum install -y google-chrome-stable
RUN yum install -y wget.x86_64
RUN yum install -y procps

RUN mkdir /tmp/lib
RUN wget --no-check-certificate -nv 'https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US' -O /tmp/lib/firefox.tar.bz2
RUN tar -xf /tmp/lib/firefox.tar.bz2 --directory /tmp/lib
RUN ln -s /tmp/lib/firefox/firefox /usr/bin/firefox
RUN chmod +x /usr/bin/firefox
RUN rm /tmp/lib/firefox.tar.bz2
#RUN ln -s /tmp /.cache
#RUN ln -s /tmp /.mozilla
#RUN /tmp/lib/firefox/firefox -headless -new-tab https://www.example.com
USER nobody

ENV HOME=/tmp/uusseerr

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
COPY build/dependency/* ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "de.otto.jlineup.lambda.JLineupHandler::handleRequest" ]