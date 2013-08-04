#!/bin/bash

# Set path to phantomjs
export PATH=$PATH:./bin/`uname`/

while test $# -gt 0; do
	case "$1" in
		-a)
			shift
			echo "Running all tests..."

			# Failed login test
			casperjs/bin/casperjs test failed_login.js

			# Successful login test
			casperjs/bin/casperjs test successful_login.js

			# Create page test
			casperjs/bin/casperjs test create_page.js

			# Rename page test
			casperjs/bin/casperjs test rename_page.js

			# Inline editing test
			casperjs/bin/casperjs test inline_editing.js

			echo "Done with all tests."

			;;
		*)
			echo "Running test $1"
			casperjs/bin/casperjs test $1.js
			shift
	esac
done

