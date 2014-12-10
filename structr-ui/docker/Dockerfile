# Dockerizing Structr: Dockerfile for building Structr images
# Based on ubuntu:latest, installs Structr following the instructions from:
# http://docs.structr.org/installation-and-configuration-guide
#
# Start with:
#
# sudo docker run --name="structr-1.1-SNAPSHOT" -p 0.0.0.0:8082:8082 -p 0.0.0.0:8021:8021 -t -i structr/structr:1.1-SNAPSHOT

FROM ubuntu:latest
MAINTAINER Axel Morgner <axel@morgner.de>

# Install wget and Java 7 (OpenJDK)
RUN	apt-get update && apt-get -y install wget openjdk-7-jdk

# Download Structr
RUN wget "http://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=org.structr&a=structr-ui&v=1.1-SNAPSHOT&e=deb" --content-disposition

# Install Structr Debian package
RUN	dpkg -i structr-ui-1.1-*.deb

# Expose ports 8082 (http) and 8021 (ftp) from the container to the host (run container with -P to expose all ports to the host)
EXPOSE 8021 8082

# Set start command
CMD cd /usr/lib/structr-ui && if [ ! -e "logs" ]; then mkdir logs; fi && if [ ! -e "structr" ]; then jar -xf structr-*.jar structr ; fi && if [ ! -f "structr.conf" ] ; then cat structr.conf_templ | sed 's/src\/main\/resources\///' > structr.conf ; fi && java -cp lib/*:structr-ui.jar -server -d64 -Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxPermSize=128m -XX:+UseNUMA org.structr.Server > /var/log/structr-ui.log
