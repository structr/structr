#!/bin/bash

export BASE_URL=http://localhost:8082
export SUPERUSER_PASSWORD=sehrgeheim

npx playwright test --debug $*
