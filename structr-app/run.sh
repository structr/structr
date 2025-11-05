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
LATEST=`ls target/structr-app-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`
VERSION=${LATEST#target/structr-app-};VERSION=${VERSION%%.jar}

STRUCTR="-Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Djava.util.logging.config.file=logging.properties.debug -Dorg.apache.sshd.registerBouncyCastle=false -Dorg.neo4j.io.pagecache.implSingleFilePageSwapper.channelStripePower=0 -cp ./target/lib/*:./plugins/*:$LATEST org.structr.Server"
STRUCTR_ARGS="-server -Xms${HEAPSIZE}g -Xmx${HEAPSIZE}g -XX:+UseNUMA -XX:+UseG1GC -XX:+UseCodeCacheFlushing -Dinstance=$NAME"

PIDFILE=$BASE_DIR/structr-$NAME.pid
LOGS_DIR=$BASE_DIR/logs

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

if [ -f $PIDFILE ]; then
	PID=`cat $PIDFILE`
	echo "Structr seems to be running as pid $PID, pid file $PIDFILE exists, exiting."
	exit 0
fi

STRUCTR_CONF=`find $BASE_DIR -name structr.conf`
echo "Starting Structr instance '$NAME' with config file $STRUCTR_CONF"

nohup $JAVA $STRUCTR_ARGS $STRUCTR >/dev/null 2>&1 &
echo $! >$PIDFILE

echo "        _                          _         "
echo " ____  | |_   ___   _   _   ____  | |_   ___ "
echo "(  __| | __| |  _| | | | | |  __| | __| |  _|"
echo " \ \   | |   | |   | | | | | |    | |   | |  "
echo " _\ \  | |_  | |   | |_| | | |__  | |_  | |  "
echo "|____) |___| |_|   |_____| |____| |___| |_|  "
echo
echo "$VERSION"
