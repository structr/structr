#!/bin/bash
JAVA=`which java`
STRUCTR="-cp target/lib/*:target/structr-ui-1.0-SNAPSHOT.jar org.structr.Server"
STRUCTR_ARGS="-server -d64 -Xms512m -Xmx512m -XX:+UseNUMA -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Dinstance=your_instance_name"

BASE_DIR=.
PIDFILE=$BASE_DIR/structr-ui.pid
LOGS_DIR=$BASE_DIR/logs
SERVER_LOG=$BASE_DIR/logs/server.log

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

$JAVA $STRUCTR $STRUCTR_ARGS > $SERVER_LOG 2>&1 &
echo $! >$PIDFILE
