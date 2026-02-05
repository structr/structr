#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.env"

docker run -d --name gitlab-runner --restart always \
    -v "$RUNNER_BASE_DIR/volumes/config:/etc/gitlab-runner" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    gitlab/gitlab-runner:latest

echo "GitLab runner started."