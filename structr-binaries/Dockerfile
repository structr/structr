FROM alpine:latest as build
ARG STRUCTR_VERSION
ADD ./target/structr-$STRUCTR_VERSION-dist.zip /root/
RUN apk update && \
    apk add unzip && \
    unzip -q /root/structr-$STRUCTR_VERSION-dist.zip -d /root && \
    rm /root/structr-$STRUCTR_VERSION-dist.zip

FROM ghcr.io/graalvm/graalvm-ce:22.3.2 as main
#RUN gu install python && \
#    gu install js
RUN gu install js
COPY --from=build /root/structr-* /var/lib/structr
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh