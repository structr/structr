#!/bin/bash

# Set path to phantomjs
export PATH=$PATH:./bin/`uname`/

while test $# -gt 0; do
	case "$1" in
		-a)
			shift
			echo "Running all tests..."

			casperjs/bin/casperjs test 001_failed_login.js
			casperjs/bin/casperjs test 002_successful_login.js
			casperjs/bin/casperjs test 003_create_user.js
			casperjs/bin/casperjs test 004_create_page.js
			casperjs/bin/casperjs test 005_rename_page.js
			casperjs/bin/casperjs test 006_inline_editing.js
			casperjs/bin/casperjs test 007_add_element_to_page.js
			casperjs/bin/casperjs test 008_create_folder.js
			casperjs/bin/casperjs test 009_create_and_edit_file.js
			casperjs/bin/casperjs test 010_page_visibility.js
		
			echo "Done with all tests."

			;;
		*)
			echo "Running test $1"
			casperjs/bin/casperjs test $1
			shift
	esac
done

