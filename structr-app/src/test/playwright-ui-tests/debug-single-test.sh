#!/bin/bash

export BASE_URL=http://localhost:8082
export SUPERUSER_PASSWORD=structr1234
export SETUP_TOKEN=ui-test-setup-token

npx playwright test --debug $*
