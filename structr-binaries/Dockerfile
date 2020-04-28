FROM openjdk:13-jdk-alpine
ARG STRUCTR_VERSION
ADD ./target/structr-$STRUCTR_VERSION-dist.zip /root/
RUN unzip -q /root/structr-$STRUCTR_VERSION-dist.zip -d /var/lib/ && mv /var/lib/structr-* /var/lib/structr && rm /root/structr-$STRUCTR_VERSION-dist.zip
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh
