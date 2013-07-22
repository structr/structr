#!/bin/bash

# Clear old screenthos
rm -rf screenshots/

# Set path to phantomjs
export PATH=$PATH:./bin/

# Failed login test
casperjs/bin/casperjs test failed_login.js
chromium-browser failed_login.html &

# Successful login test
casperjs/bin/casperjs test successful_login.js
chromium-browser successful_login.html &

# Create page test
casperjs/bin/casperjs test create_page.js
chromium-browser create_page.html &
