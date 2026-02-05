#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.env"

cd "$RUNNER_BASE_DIR/templates"
docker build -t "$RUNNER_IMAGE" -f gitlab-test-runner.Dockerfile .