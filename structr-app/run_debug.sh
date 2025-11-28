#!/bin/sh

# determine latest Structr version
LATEST=`ls target/structr-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`

if [ -z "$SUSPEND" ]; then
	SUSPEND=n
fi

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
LOGS_DIR=$BASE_DIR/logs

if [ ! -d "$LOGS_DIR" ]; then
				mkdir $LOGS_DIR
fi

if [ -f "structr.conf" ]; then

  # Read java heap config
	MAX_HEAP=$(grep -E "^\s*application\.heap\.max_size\s*=" structr.conf | sed 's/^[^=]*=\s*//' | tr -d '[:space:]')
	MIN_HEAP=$(grep -E "^\s*application\.heap\.min_size\s*=" structr.conf | sed 's/^[^=]*=\s*//' | tr -d '[:space:]')

	# Read timezone
	CONF_TIMEZONE=$(grep -E "^\s*application\.timezone\s*=" structr.conf | sed 's/^[^=]*=\s*//' | tr -d '[:space:]')

	if [ -n "$MAX_HEAP" ] || [ -n "$MIN_HEAP" ]; then
		MEMORY_OPTS=""
		[ -n "$MIN_HEAP" ] && MEMORY_OPTS="-Xms${MIN_HEAP}"
		[ -n "$MAX_HEAP" ] && MEMORY_OPTS="${MEMORY_OPTS} -Xmx${MAX_HEAP}"
		MEMORY_OPTS=$(echo "$MEMORY_OPTS" | xargs)  # trim whitespace
		echo "Using structr.conf memory settings: $MEMORY_OPTS"
	fi
elif [ -z "$MEMORY_OPTS" ]; then
	MEMORY_OPTS="-Xms2g -Xmx8g"
fi


if [ -n "$STRUCTR_TIMEZONE" ]; then
	export PROCESS_TZ=$STRUCTR_TIMEZONE
	echo "Using user-provided timezone '$STRUCTR_TIMEZONE'"
elif [ -n "$CONF_TIMEZONE" ]; then
	export PROCESS_TZ=$CONF_TIMEZONE
	echo "Using structr.conf timezone '$CONF_TIMEZONE'"
elif [ -z "$TZ" ]; then
	export PROCESS_TZ="UTC"
	echo "Using default timezone '$PROCESS_TZ'"
else
	export PROCESS_TZ=$TZ
	echo "Using system TZ env timezone '$TZ'"
fi

echo "Current directory: $(pwd)"
# start Structr

java -server -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=$PROCESS_TZ -Duser.country=US -Duser.language=en -Djava.net.useSystemProxies=true -Dorg.apache.sshd.registerBouncyCastle=false -Dorg.neo4j.io.pagecache.implSingleFilePageSwapper.channelStripePower=0 -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=$SUSPEND -Dfile.encoding=utf-8 $MEMORY_OPTS -XX:+UseG1GC -XX:+UseNUMA -XX:+UseCodeCacheFlushing -cp ./target/lib/*:./plugins/*:$LATEST org.structr.Server
