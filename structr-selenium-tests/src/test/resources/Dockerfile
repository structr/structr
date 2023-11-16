FROM ubuntu:latest
RUN apt-get update
RUN apt-get install -y vim openjdk-17-jdk wget bzip2 libgtk-3-0 libdbus-glib-1-2
RUN wget "https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US" -O /root/firefox.tar.bz2
RUN tar xvjf /root/firefox.tar.bz2
RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux64.tar.gz -O /root/geckodriver-v0.33.0-linux64.tar.gz
RUN tar xvzf /root/geckodriver-v0.33.0-linux64.tar.gz
RUN cp geckodriver /usr/local/bin
RUN cp -a firefox/* /usr/bin
COPY structr-selenium-dsl-0.1-SNAPSHOT.jar /root
ENV MOZ_HEADLESS 1
WORKDIR /root
CMD java -jar structr-selenium-dsl-0.1-SNAPSHOT.jar -e firefox /tmp/testsuite -u "http://structr:8082" -s /tmp/selenium-test-summary.xml
