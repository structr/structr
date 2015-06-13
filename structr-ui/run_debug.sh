#!/bin/bash
LATEST=`ls target/structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`

java -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Djava.net.useSystemProxies=true -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -Dfile.encoding=utf-8 -Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxPermSize=128m -XX:+UseNUMA -cp target/lib/*:$LATEST -Djava.util.logging.config.file=logging.properties org.structr.Server | tee logs/debug.log
