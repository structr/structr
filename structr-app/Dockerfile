FROM alpine:latest AS build
ARG STRUCTR_VERSION
ADD ./target/$STRUCTR_VERSION-dist.zip /root/
RUN apk update && \
    apk add unzip && \
    unzip -q /root/$STRUCTR_VERSION-dist.zip -d /root && \
    rm /root/$STRUCTR_VERSION-dist.zip

FROM ghcr.io/graalvm/jdk-community:25-ol10 AS main

# Tesseract for OCR in the content extraction module (Tika calls the binary). Without it, extraction
# still works, just without OCR. Derived builds can change the language set via this build argument.
ARG TESSERACT_PACKAGES="tesseract tesseract-langpack-eng tesseract-langpack-deu tesseract-langpack-fra tesseract-langpack-spa tesseract-langpack-ita tesseract-langpack-nld tesseract-osd"
RUN microdnf install -y $TESSERACT_PACKAGES && \
    microdnf clean all

COPY --from=build /root/structr-* /var/lib/structr
WORKDIR /var/lib/structr
EXPOSE 8082
CMD bin/docker.sh
