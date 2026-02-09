#!/bin/bash

export BASE_URL=http://localhost:8082
export SUPERUSER_PASSWORD=structr1234

npx playwright test --debug $*
