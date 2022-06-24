FROM ghcr.io/graalvm/graalvm-ce:ol8-java11-22.1.0
RUN gu install python
ARG STRUCTR_VERSION
ADD ./target/structr-$STRUCTR_VERSION-dist.zip /root/
RUN microdnf install -y unzip
RUN unzip -q /root/structr-$STRUCTR_VERSION-dist.zip -d /var/lib/ && mv /var/lib/structr-* /var/lib/structr && rm /root/structr-$STRUCTR_VERSION-dist.zip
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh