FROM alpine:latest AS build
ARG STRUCTR_VERSION
ADD ./target/structr-app-$STRUCTR_VERSION-dist.zip /root/
RUN apk update && \
    apk add unzip && \
    unzip -q /root/structr-app-$STRUCTR_VERSION-dist.zip -d /root && \
    rm /root/structr-app-$STRUCTR_VERSION-dist.zip

FROM ghcr.io/graalvm/jdk-community:25 AS main
COPY --from=build /root/structr-* /var/lib/structr
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh