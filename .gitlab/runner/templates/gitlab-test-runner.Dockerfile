# Dockerfile for gitlab job runners to run Structr tests.
# Build the image with the following command and adjust the image tag in your gitlab job to 'gitlab-test-runner'.
# docker buildx build -t gitlab-test-runner -f gitlab-test-runner.Dockerfile .
# For multiplatform docker buildx build --platform linux/amd64,linux/arm64 -t gitlab-test-runner -f gitlab-test-runner.Dockerfile .

FROM alpine:3.22 AS setup

WORKDIR /setup

ARG TARGETARCH
ARG MAVEN_VERSION="3.9.16"
ARG GLAB_VERSION="1.122.0"
ARG FFMPEG_VERSION="7.0.2"

RUN echo "TARGETARCH is: $TARGETARCH" \
    && echo "Full URL: https://gitlab.com/gitlab-org/cli/-/releases/v${GLAB_VERSION}/downloads/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm"

# Install Maven without pulling OpenJDK for building Structr.
# Install glab to create releases and interact with gitlab from runner.
RUN apk update \
    && apk add tar gzip curl git wget \
    && wget https://gitlab.com/gitlab-org/cli/-/releases/v${GLAB_VERSION}/downloads/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm \
    && curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /setup/maven.tgz \
    && tar -xzf /setup/maven.tgz -C /opt \
    && rm -f /setup/maven.tgz

# Static ffmpeg and ffprobe for the media module tests, pinned by version and checksum.
# A release moves from releases/ to old-releases/ once a newer one is out.
RUN apk add xz \
    && case "$TARGETARCH" in \
         amd64) FFMPEG_MD5="7fa72b652e19bf84c9461e332ea1cdf3" ;; \
         arm64) FFMPEG_MD5="807afe21601db0a73e426121c7d636ea" ;; \
         *) echo "No ffmpeg build for $TARGETARCH" && exit 1 ;; \
       esac \
    && FFMPEG_FILE="ffmpeg-${FFMPEG_VERSION}-${TARGETARCH}-static.tar.xz" \
    && (curl -fsSL "https://johnvansickle.com/ffmpeg/releases/${FFMPEG_FILE}" -o "${FFMPEG_FILE}" \
        || curl -fsSL "https://johnvansickle.com/ffmpeg/old-releases/${FFMPEG_FILE}" -o "${FFMPEG_FILE}") \
    && echo "${FFMPEG_MD5}  ${FFMPEG_FILE}" | md5sum -c - \
    && mkdir -p /opt/ffmpeg \
    && tar -xJf "${FFMPEG_FILE}" -C /opt/ffmpeg --strip-components=1 \
    && rm -f "${FFMPEG_FILE}"

FROM ghcr.io/graalvm/jdk-community:25-ol9 AS main
WORKDIR /structr-runner

ARG TARGETARCH
ARG MAVEN_VERSION="3.9.16"
ARG GLAB_VERSION="1.122.0"

COPY --from=setup /setup/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm /structr-runner/
COPY --from=setup /opt/apache-maven-${MAVEN_VERSION} /opt/maven
COPY --from=setup /opt/ffmpeg/ffmpeg /opt/ffmpeg/ffprobe /usr/local/bin/

# UTF-8 locale, so that the JVM passes non-ASCII process arguments unchanged
ENV LANG=C.UTF-8 \
    MAVEN_HOME=/opt/maven \
    MAVEN_CONFIG=/root/.m2 \
    PATH=/opt/maven/bin:$PATH

# Install docker-cli for docker functions and setup glab and maven
# docker-buildx-plugin is required: the multi-arch image build and the moving tag promotion
# both shell out to "docker buildx". CLI plugins live in the client, so the mounted host
# docker socket does not provide it.
RUN microdnf install -y dnf dnf-plugins-core \
     && dnf -y install dnf-plugins-core \
     && dnf config-manager --add-repo "https://download.docker.com/linux/centos/docker-ce.repo" \
     && dnf -y install docker-ce-cli docker-buildx-plugin git \
     && dnf -y install socat \
  	 && dnf -y install jq \
     && dnf -y install tesseract tesseract-langpack-eng tesseract-langpack-deu fontconfig dejavu-sans-fonts \
     && dnf install -y glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm \
     && dnf clean all \
     && rm -f *.rpm