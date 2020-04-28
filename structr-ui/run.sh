#!/bin/sh

NAME=$1
HEAPSIZE=$2

if [ -z $NAME ]; then
        NAME="default"
fi

if [ -z $HEAPSIZE ]; then
	HEAPSIZE=1
fi

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd $BASE_DIR
JAVA=`which java`
LATEST=`ls target/structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`
VERSION=${LATEST#target/structr-ui-};VERSION=${VERSION%%.jar}
STRUCTR="-Djava.awt.headless=true -Djava.system.class.loader=org.structr.StructrClassLoader -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Djava.util.logging.config.file=logging.properties.debug -Dorg.apache.sshd.registerBouncyCastle=false -Dorg.neo4j.io.pagecache.implSingleFilePageSwapper.channelStripePower=0 -cp target/lib/*:$LATEST org.structr.Server"
STRUCTR_ARGS="-server -Xms${HEAPSIZE}g -Xmx${HEAPSIZE}g -XX:+UseNUMA -XX:+UseG1GC -Dinstance=$NAME"

PIDFILE=$BASE_DIR/structr-$NAME.pid
LOGS_DIR=$BASE_DIR/logs
SERVER_LOG=$BASE_DIR/logs/server.log

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

if [ -f $PIDFILE ]; then
	PID=`cat $PIDFILE`
	echo "Structr seems to be running as pid $PID, pid file $PIDFILE exists, exiting."
	exit 0
fi

if [ -f $SERVER_LOG ]; then
        NOW=$(date +%Y%m%d-%H%M%S)
        SERVER_LOG_BACKUP=$SERVER_LOG-$NOW
	echo "Rotating existing logfile $SERVER_LOG to $SERVER_LOG_BACKUP"
        mv $SERVER_LOG $SERVER_LOG_BACKUP
        touch $SERVER_LOG
fi

STRUCTR_CONF=`find $BASE_DIR -name structr.conf`
echo "Starting Structr instance '$NAME' with config file $STRUCTR_CONF"

nohup $JAVA $STRUCTR_ARGS $STRUCTR > $SERVER_LOG 2>&1 &
echo $! >$PIDFILE

( tail -q -n0 -F $SERVER_LOG 2>/dev/null & echo $! >tail.pid ) | sed -n '/Initialization complete/q'
tail -q -200 $SERVER_LOG 2> /dev/null | grep 'Starting'

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
echo "Structr instance '$NAME' started successfully (PID $!)"
kill `cat tail.pid`
rm tail.pid
