#!/bin/sh

. bin/config

LOGS_DIR="logs"

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
        touch $LOG_FILE
fi

java $RUN_OPTS $JAVA_OPTS $MAIN_CLASS | tee -a $LOG_FILE