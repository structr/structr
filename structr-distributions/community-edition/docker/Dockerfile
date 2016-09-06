# Dockerizing Structr: Dockerfile for building Structr images
# Based on ubuntu:latest, installs Structr following the instructions from:
# http://docs.structr.org/installation-and-configuration-guide
#
# Start with:
#
# sudo docker run --name="structr-community-edition-2.1.0-SNAPSHOT" -p 0.0.0.0:8082:8082 -p 0.0.0.0:8021:8021 -t -i structr/structr-community-edition:2.1.0-SNAPSHOT

FROM ubuntu:latest

ENV distribution structr-community-edition
ENV baseversion 2.1.0
ENV version $baseversion-SNAPSHOT


MAINTAINER Axel Morgner <axel.morgner@structr.com>

# Install wget and Java 8 (OpenJDK)
RUN apt-get update && apt-get -y install wget openjdk-8-jdk

# Alternatively, install Oracle Java JDK 8
#RUN echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
#RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
#RUN apt-get update
#RUN apt-get -y install software-properties-common
#RUN add-apt-repository ppa:webupd8team/java
#RUN apt-get update
#RUN apt-get -y install wget oracle-java8-installer

# Download latest Structr snapshot
# RUN wget "https://oss.sonatype.org/service/local/artifact/maven/content?r=snapshots&g=org.structr&a=$distribution&v=$version&e=deb" --content-disposition
RUN wget "https://structr.org/download/structr-community-edition-2.1.0-SNAPSHOT-201609061251.3702b.deb" --content-disposition

# Install Structr Debian package
RUN dpkg -i $distribution-$baseversion-*.deb

# Expose ports 7474 (Neo4j http), 8082 (Structr http) and 8021 (Structr ftp) from the container to the host (run container with -P to expose all ports to the host)
EXPOSE 8021 8082

# Set start command
CMD cd /usr/lib/structr && bin/run
