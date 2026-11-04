FROM alpine:latest AS build
ARG STRUCTR_VERSION
ADD ./target/$STRUCTR_VERSION-dist.zip /root/
RUN apk update && \
    apk add unzip && \
    unzip -q /root/$STRUCTR_VERSION-dist.zip -d /root && \
    rm /root/$STRUCTR_VERSION-dist.zip

FROM ghcr.io/graalvm/jdk-community:25-ol10 AS main

ARG TESSERACT_PACKAGES="tesseract tesseract-langpack-eng tesseract-langpack-deu tesseract-langpack-fra tesseract-langpack-spa tesseract-langpack-ita tesseract-langpack-nld tesseract-osd"
RUN microdnf install -y $TESSERACT_PACKAGES fontconfig dejavu-sans-fonts && \
    microdnf clean all

COPY --from=build /root/structr-* /var/lib/structr

# UTF-8 locale, so that the JVM passes non-ASCII process arguments unchanged
ENV LANG=C.UTF-8
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh
