#!/bin/bash

NAME=$1
HEAPSIZE=$2

if [ -z $NAME ]; then
        NAME="default"
fi

if [ -z $HEAPSIZE ]; then
	HEAPSIZE=1
fi

JAVA="/opt/jdk1.7.0_45/bin/java"
STRUCTR="-cp lib/*:target/lib/*:target/structr-ui-1.0.0.jar org.structr.Server"
STRUCTR_ARGS="-server -d64 -Xms${HEAPSIZE}g -Xmx${HEAPSIZE}g -XX:+UseNUMA -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Dinstance=$NAME"

BASE_DIR=/opt/structrdb
PIDFILE=$BASE_DIR/structrdb-$NAME.pid
LOGS_DIR=$BASE_DIR/logs
SERVER_LOG=$BASE_DIR/logs/server.log

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

cd $BASE_DIR
$JAVA $STRUCTR $STRUCTR_ARGS > $SERVER_LOG 2>&1 &
echo $! >$PIDFILE
