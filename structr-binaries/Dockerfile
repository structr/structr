# Dockerfile for building Structr
#
# Start with: sudo docker run -ti -p 0.0.0.0:8082:8082 structr:$STRUCTR_VERSION

FROM openjdk:8-jdk-alpine

ARG STRUCTR_VERSION

ADD ./target/structr-$STRUCTR_VERSION-dist.zip /root/

# Install Structr Debian package
RUN unzip -q /root/structr-$STRUCTR_VERSION-dist.zip -d /var/lib/ && mv /var/lib/structr-* /var/lib/structr && rm /root/structr-$STRUCTR_VERSION-dist.zip

WORKDIR /var/lib/structr

EXPOSE 8082

CMD bin/docker.sh