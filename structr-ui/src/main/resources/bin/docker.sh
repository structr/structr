#!/bin/sh

. bin/config

LOGS_DIR="logs"

if [ -e $PID_FILE ]; then

	$(ps aux | grep "org.structr.Server" | grep -v grep)

	result=$?

	if [ $result -eq 1 ]; then
		echo
		echo "        Found $PID_FILE, but server is not running."
		echo "        Removing $PID_FILE and proceeding with startup."
		echo

		rm $PID_FILE
	else
		echo
		echo "        ERROR: server already running."
		echo
		echo "        Please stop any running instances before starting a"
		echo "        new one. (Remove $PID_FILE if this message appears"
		echo "        even if no server is running.)"
		echo

		exit 0
	fi

fi

if [ ! -d $LOGS_DIR ]; then
		echo "        Creating logs directory..."
        mkdir $LOGS_DIR
        touch $LOG_FILE
fi

echo "        Starting structr server $DISPLAY_NAME"

java $RUN_OPTS $JAVA_OPTS $MAIN_CLASS