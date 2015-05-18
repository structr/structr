#!/bin/bash
BASE_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $BASE_DIR
JAVA=`which java`
LATEST=`ls target/structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`
VERSION=${LATEST#target/structr-ui-};VERSION=${VERSION%%.jar}
STRUCTR="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -cp target/lib/*:$LATEST org.structr.Server"
STRUCTR_ARGS="-d64 -Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxPermSize=128m -XX:+UseNUMA -Dinstance=your_instance_name"

STRUCTR_CONF=`find . -name structr.conf`
echo "Starting Structr with config file $STRUCTR_CONF"

PIDFILE=./structr-ui.pid
LOGS_DIR=./logs
SERVER_LOG=./logs/server.log

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
echo "$VERSION"

echo
echo "Structr started successfully (PID $!)"
