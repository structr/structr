# Dockerizing Structr: Dockerfile for building Structr images
# Based on ubuntu:latest, installs Structr following the instructions from:
# http://docs.structr.org/installation-and-configuration-guide
#
# Start with:
#
# sudo docker run --name="structr" -ti -p 0.0.0.0:8082:8082 structr:2.1.4-SNAPSHOT

FROM ubuntu:latest

ARG STRUCTR_VERSION

#ENV STRUCTR_VERSION 2.1.4-SNAPSHOT
MAINTAINER Axel Morgner <axel.morgner@structr.com>

# Install wget and Java 8 (OpenJDK)
RUN	apt-get update && apt-get -y install wget openjdk-8-jdk

ADD ./target/structr-$STRUCTR_VERSION.deb /root/

# Download Structr
#RUN wget https://structr.org/repositories/snapshots/org/structr/structr/$STRUCTR_VERSION/structr-$STRUCTR_VERSION.deb

# Install Structr Debian package
RUN	dpkg -i /root/structr-$STRUCTR_VERSION.deb

# Expose port 8082 (Structr http) from the container to the host (run container with -P to expose all ports to the host)
EXPOSE 8082

# Set start command
CMD cd /usr/lib/structr/ && . bin/config && if [ ! -e "logs" ]; then mkdir logs; fi && if [ ! -e "structr" ]; then jar -xf structr-*.jar structr ; fi && java -cp lib/*:structr-ui.jar -server -d64 -Xms1g -Xmx1g -XX:+UseG1GC -XX:+UseNUMA org.structr.Server > /var/log/structr.log
