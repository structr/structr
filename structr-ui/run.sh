#!/bin/bash
JAVA=`which java`
STRUCTR="-cp target/lib/*:target/structr-ui-1.0.RC3.jar org.structr.Server"
STRUCTR_ARGS="-server -d64 -Xms2g -Xmx2g -XX:+UseNUMA -XX:+UseG1GC -Dinstance=your_instance_name"

STRUCTR_CONF=`find . -name structr.conf`
echo "Starting Structr with config file $STRUCTR_CONF"
BASE_DIR=`grep -v '^#' $STRUCTR_CONF | grep -m1 "base\.path" | awk '{ print $3 }' | tr -d [:cntrl:]`

PIDFILE=$BASE_DIR/structr-ui.pid
LOGS_DIR=$BASE_DIR/logs
SERVER_LOG=$BASE_DIR/logs/server.log

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

nohup $JAVA $STRUCTR $STRUCTR_ARGS > $SERVER_LOG 2>&1 &
echo $! >$PIDFILE

{ tail -q -n0 --pid=$! -F $SERVER_LOG 2>/dev/null & } | sed -n '/Initialization complete/q'
tail -200 $SERVER_LOG 2> /dev/null | grep 'Starting'

# If your console font is rather slim, you can change the ascii art message to
# better fit the structr logo ;-) (you know, details matter...)

#echo "       _                          _         "
#echo " ___  | |_   ___   _   _   ____  | |_   ___ "
#echo "(  _| | __| |  _| | | | | |  __| | __| |  _|"
#echo " \ \  | |_  | |   | |_| | | |__  | |_  | |  "
#echo "|___) |___| |_|   |_____| |____| |___| |_|  "


echo "        _                          _         "
echo " ____  | |_   ___   _   _   ____  | |_   ___ "
echo "(  __| | __| |  _| | | | | |  __| | __| |  _|"
echo " \ \   | |   | |   | | | | | |    | |   | |  "
echo " _\ \  | |_  | |   | |_| | | |__  | |_  | |  "
echo "|____) |___| |_|   |_____| |____| |___| |_|  "

echo
echo "Structr started successfully (PID $!)"
