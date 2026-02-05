#!/bin/bash

docker stop gitlab-runner 2>/dev/null || true
docker container rm gitlab-runner 2>/dev/null || true
echo "GitLab runner stopped."