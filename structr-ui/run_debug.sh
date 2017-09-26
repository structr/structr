#!/bin/sh

# determine latest Structr version
LATEST=`ls target/structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LOGS_DIR=$BASE_DIR/logs

if [ ! -d $LOGS_DIR ]; then
        mkdir $LOGS_DIR
fi

# start Structr
java -server -d64 -Djava.awt.headless=true -Djava.system.class.loader=org.structr.StructrClassLoader -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Djava.net.useSystemProxies=true -Dorg.apache.sshd.registerBouncyCastle=false -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -Dfile.encoding=utf-8 -Xms6g -Xmx8g -XX:+UseG1GC -XX:+UseNUMA -cp target/lib/*:$LATEST org.structr.Server | tee -a logs/debug.log
