FROM ghcr.io/graalvm/graalvm-ce:java11-20.3.0
ARG STRUCTR_VERSION
ADD ./target/structr-$STRUCTR_VERSION-dist.zip /root/
RUN microdnf install -y unzip
RUN unzip -q /root/structr-$STRUCTR_VERSION-dist.zip -d /var/lib/ && mv /var/lib/structr-* /var/lib/structr && rm /root/structr-$STRUCTR_VERSION-dist.zip
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh
