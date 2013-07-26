#!/bin/bash

# Set path to phantomjs
export PATH=$PATH:./bin/`uname`/

# Failed login test
casperjs/bin/casperjs test failed_login.js

# Successful login test
casperjs/bin/casperjs test successful_login.js

# Create page test
casperjs/bin/casperjs test create_page.js

# Rename page test
casperjs/bin/casperjs test rename_page.js
