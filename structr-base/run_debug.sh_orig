#!/bin/sh

# determine latest Structr version
LATEST=`ls target/structr-base-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`

if [ -z "$SUSPEND" ]; then
	SUSPEND=n
fi

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LOGS_DIR=$BASE_DIR/logs

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

if [ -z "$MEMORY_OPTS" ]; then
	MEMORY_OPTS="-Xms2g -Xmx8g"
fi

# start Structr
java -server -Djava.awt.headless=true -Djava.system.class.loader=org.structr.StructrClassLoader -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Djava.net.useSystemProxies=true -Dorg.apache.sshd.registerBouncyCastle=false -Dorg.neo4j.io.pagecache.implSingleFilePageSwapper.channelStripePower=0 -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=$SUSPEND -Dfile.encoding=utf-8 $MEMORY_OPTS -XX:+UseG1GC -XX:+UseNUMA -cp target/lib/*:$LATEST org.structr.Server
