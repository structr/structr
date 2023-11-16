FROM ubuntu:latest
COPY structr-selenium-dsl-0.1-SNAPSHOT.jar /root
RUN apt-get update && \
    apt-get install -y vim openjdk-17-jdk wget bzip2 libgtk-3-0 libdbus-glib-1-2 && \
    wget "https://download.mozilla.org/?product=firefox-latest&os=linux64&lang=en-US" -O /root/firefox.tar.bz2 && \
    tar xvjf /root/firefox.tar.bz2 && \
    wget https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux64.tar.gz -O /root/geckodriver-v0.33.0-linux64.tar.gz && \
    #wget https://github.com/mozilla/geckodriver/releases/download/v0.33.0/geckodriver-v0.33.0-linux-aarch64.tar.gz -O /root/geckodriver-v0.33.0-linux64.tar.gz && \
    tar xvzf /root/geckodriver-v0.33.0-linux64.tar.gz && \
    cp geckodriver /usr/local/bin && \
    cp -a firefox/* /usr/bin

ENV MOZ_HEADLESS 1
WORKDIR /root
CMD java -jar structr-selenium-dsl-0.1-SNAPSHOT.jar -e firefox /tmp/testsuite -u "http://structr:8082" -s /tmp/selenium-test-summary.xml
