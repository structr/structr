#!/bin/bash
JAVA=`which java`
LATEST=`ls target/structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`
VERSION=${LATEST#target/structr-ui-};VERSION=${VERSION%%.jar}
STRUCTR="-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -cp target/lib/*:$LATEST org.structr.Server"
STRUCTR_ARGS="-d64 -Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxPermSize=128m -XX:+UseNUMA -Dinstance=your_instance_name"

if [ -z $NAME ]; then
        NAME="default"
fi

if [ -z $HEAPSIZE ]; then
	HEAPSIZE=1
fi

JAVA="/opt/jdk1.7.0_45/bin/java"
STRUCTR="-Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -cp target/lib/*:target/structr-ui-1.1-SNAPSHOT-$(git rev-parse --short=5 HEAD).jar org.structr.Server"
STRUCTR_ARGS="-server -d64 -Xms${HEAPSIZE}g -Xmx${HEAPSIZE}g -XX:MaxPermSize=128m -XX:+UseNUMA -XX:+UseG1GC -Dinstance=$NAME"

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
