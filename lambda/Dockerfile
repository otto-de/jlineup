FROM public.ecr.aws/lambda/java:11

USER root
RUN sh -c 'echo -e "[google-chrome]\nname=google-chrome - 64-bit\nbaseurl=http://dl.google.com/linux/chrome/rpm/stable/x86_64\nenabled=1\ngpgcheck=1\ngpgkey=https://dl-ssl.google.com/linux/linux_signing_key.pub" >> /etc/yum.repos.d/google-chrome.repo'
RUN yum install tar bzip2 dbus-glib libXt google-chrome-stable -y
RUN wget -nv 'https://download.mozilla.org/?product=firefox-latest-ssl&os=linux64&lang=en-US' -O /usr/lib/firefox.tar.bz2
RUN tar -xf /usr/lib/firefox.tar.bz2 --directory /usr/lib
RUN ln -s /usr/lib/firefox/firefox /bin/firefox
RUN rm /usr/lib/firefox.tar.bz2
RUN ln -s /tmp /.cache
RUN ln -s /tmp /.mozilla
#RUN yum install -y gtk3-devel
#xorg-x11-server-Xorg
#RUN /usr/lib/firefox/firefox -headless -new-tab https://www.example.com
USER nobody

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
COPY build/dependency/* ${LAMBDA_TASK_ROOT}/lib/

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "de.otto.jlineup.lambda.JLineupHandler::handleRequest" ]