# Dockerfile for gitlab job runners to run Structr tests.
# Build the image with the following command and adjust the image tag in your gitlab job to 'gitlab-test-runner'.
# docker buildx build -t gitlab-test-runner -f gitlab-test-runner.Dockerfile .
# For multiplatform docker buildx build --platform linux/amd64,linux/arm64 -t gitlab-test-runner -f gitlab-test-runner.Dockerfile .

FROM alpine:3.22 AS setup

WORKDIR /setup

ARG TARGETARCH
ARG MAVEN_VERSION="3.9.12"
ARG GLAB_VERSION="1.80.4"

RUN echo "TARGETARCH is: $TARGETARCH" \
    && echo "Full URL: https://gitlab.com/gitlab-org/cli/-/releases/v${GLAB_VERSION}/downloads/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm"

# Install Maven without pulling OpenJDK for building Structr.
# Install glab to create releases and interact with gitlab from runner.
RUN apk update \
    && apk add tar gzip curl git wget \
    && wget https://gitlab.com/gitlab-org/cli/-/releases/v${GLAB_VERSION}/downloads/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm \
    && curl -fsSL https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /setup/maven.tgz \
    && tar -xzf /setup/maven.tgz -C /opt \
    && rm -f /setup/maven.tgz

FROM ghcr.io/graalvm/jdk-community:25-ol9 AS main
WORKDIR /structr-runner

ARG TARGETARCH
ARG MAVEN_VERSION="3.9.12"
ARG GLAB_VERSION="1.80.4"

COPY --from=setup /setup/glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm /structr-runner/
COPY --from=setup /opt/apache-maven-${MAVEN_VERSION} /opt/maven

ENV MAVEN_HOME=/opt/maven \
    MAVEN_CONFIG=/root/.m2 \
    PATH=/opt/maven/bin:$PATH

# Install docker-cli for docker functions and setup glab and maven
RUN microdnf install -y dnf dnf-plugins-core \
     && dnf -y install dnf-plugins-core \
     && dnf config-manager --add-repo "https://download.docker.com/linux/centos/docker-ce.repo" \
     && dnf -y install docker-ce-cli git \
     && dnf -y install socat \
     && dnf install -y glab_${GLAB_VERSION}_linux_${TARGETARCH}.rpm \
     && dnf clean all \
     && rm -f *.rpm