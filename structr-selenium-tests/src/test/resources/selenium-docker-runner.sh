#!/bin/bash
#
# test.sh - run Structr deployment and selenium tests in a dockerized setup
#
# prerequisites:
# - docker
# - curl
#

BASE_URL=http://localhost
PORT=11223
USERNAME=admin
PASSWORD=admin
VERSION=5.1-SNAPSHOT
NETWORK=selenium-tests
MAX_TRIES=60
WAIT=5
LOG=/dev/null
REUSE_EXISTING=true
IMAGE_EXISTS=false
DUMP_CONFIG=false
RECORD=false
NAME=structr:selenium
SUMMARY=.
EXIT_CODE=0

function started() {
	curl -si $BASE_URL:$PORT/structr/rest >/dev/null
}

function cancel() {
	echo "Error: Structr not responding, cancelling server set up"
	docker stop $CONTAINER
}

function usage() {

	echo
	echo "Usage"
	echo "    test.sh [-c] [-d <webapp>] [-h] [-n <image name>] [-l <logfile>] [-r] [-u] [-v <version> [-t <testsuite>]"
	echo
	echo "Options"
	echo "    -c                 - dump configuration and exit"
	echo "    -d <webapp>        - deploy the webapp from the given directory"
	echo "    -h                 - print this message and exit"
	echo "    -k <license file>  - use given license file"
	echo "    -l <logfile>       - use given log file (default: /dev/null)"
	echo "    -n <image name>    - use given test image name (default: $NAME)"
	echo "    -r                 - recording mode (dont run tests, just start the instance)"
	echo "    -s <summary>       - create the given summary file"
	echo "    -t <testsuite>     - run tests in the given directory"
	echo "    -u                 - update test image (don't resuse existing images)"
	echo "    -v <version>       - use given Structr version (default: $VERSION)"
	echo
	echo "Docker reference"
	echo "    list containers         - docker container ls [-a]"
	echo "    run shell on container  - docker exec -ti <name> /bin/sh"
	echo "    list images             - docker image ls [-a]"
	echo "    manage containers       - docker container rm <name> | create <name> | start <name> | stop <name>"
	echo "    manage images           - docker image rm <id> | create | start <id> | stop <id>"
	echo "    manage networks         - docker network ls | rm | create | start <name> | stop <name>"
	echo "    fetch logs / stdout     - docker logs [-f] <name>"
	echo

	exit 0
}

# process command line options
while [ "$#" -gt 0 ]; do

	case "$1" in
		-c) DUMP_CONFIG="true"; shift 1;;
		-d) SOURCE="$2"; shift 2;;
		-h) usage; shift 1;;
		-k) LICENSE="$(cat $2)"; shift 2;;
		-l) LOG="$2"; shift 2;;
		-n) NAME="$2"; shift 2;;
		-r) RECORD="true"; shift 1;;
		-s) SUMMARY="$2"; shift 2;;
		-t) TESTSUITE="$2"; shift 2;;
		-u) REUSE_EXISTING="false"; shift 1;;
		-v) VERSION="$2"; shift 2;;
		*)
			echo "Error: unknown option $1"
			usage
			;;

	esac
done

if [ "$DUMP_CONFIG" == "true" ]; then

	echo
	echo "Configuration:"
	echo
	echo "    deployment source:     $SOURCE"
	echo "    test suite:            $TESTSUITE"
	echo "    Structr version:       $VERSION"
	echo "    License key:           $LICENSE"
	echo "    Test image name:       $NAME"
	echo "    base URL:              $BASE_URL"
	echo "    HTTP port:             $PORT"
	echo "    REST username:         $USERNAME"
	echo "    REST password:         $PASSWORD"
	echo "    network:               $NETWORK"
	echo "    log file:              $LOG"
	echo "    health check interval: $WAIT"
	echo "    health check retries:  $MAX_TRIES"
	echo "    recording mode:        $RECORD"
	echo "    test summary file:     $SUMMARY"
	echo

	exit 0
fi

if [ "$REUSE_EXISTING" == "false" ]; then

	echo "Removing test image" |tee -a $LOG
	docker image rm selenium-test >>$LOG

fi

if [ "$RECORD" == "true" ]; then

	echo "#"
	echo "# running in RECORDING mode"
	echo "# $(date)"
	echo "#"

else

	if [ -z "$TESTSUITE" ]; then
		echo "Error: missing testsuite parameter"
		usage
	fi

	echo "#"
	echo "# running in TESTING mode"
	echo "# $(date)"
	echo "#"
fi

# check for existing image, reuse if it exists (and not forced to update)
if docker inspect --type=image $NAME >/dev/null 2>&1; then
	IMAGE_EXISTS=true
fi

echo "Creating isolated network.." |tee -a $LOG
docker network create $NETWORK >>$LOG

if [ "$IMAGE_EXISTS" == "true" ] && [ "$REUSE_EXISTING" == "true" ]; then

	#
	# start Structr using an existing image from previous runs
	#

	echo "Re-using existing image $NAME" |tee -a $LOG
	CONTAINER=$(docker create -e AGREE_TO_STRUCTR_PRIVACY_POLICY=yes -e STRUCTR_license_key=$LICENSE -e STRUCTR_application_schema_automigration=true --name structr-selenium-tests --network $NETWORK --net-alias structr -p $PORT:8082 $NAME)

	echo "Starting container.." |tee -a $LOG
	docker start $CONTAINER >>$LOG || exit 1

	PORT=$(docker inspect --format='{{(index (index .NetworkSettings.Ports "8082/tcp") 0).HostPort}}' structr-selenium-tests)

	echo -n "Waiting for Structr to become available.." |tee -a $LOG
	sleep $WAIT
	until started || [ $MAX_TRIES -eq 0 ]; do
		echo -n "."
		sleep $WAIT
	done

	echo "."

	if [ $MAX_TRIES -eq 0 ]; then
		cancel
	fi

	if [ -x /usr/bin/notify-send ]; then
		notify-send "Structr Selenium Test Runner" "Test instance is ready"
	fi

else

	#
	# start Structr using a new image
	#

	echo "Creating new container from version $VERSION" |tee -a $LOG
	CONTAINER=$(docker create -e AGREE_TO_STRUCTR_PRIVACY_POLICY=yes -e STRUCTR_license_key=$LICENSE -e STRUCTR_application_schema_automigration=true --name structr-selenium-tests --network $NETWORK --net-alias structr -p $PORT:8082 structr/structr:$VERSION)

	echo "Container: $CONTAINER"

	echo "Copying resources.." |tee -a $LOG

	if [ -e license.key ]; then
		docker cp license.key $CONTAINER:/var/lib/structr/license.key >>$LOG || exit 1
	fi

	if [ -n "$SOURCE" ]; then
		docker cp $SOURCE $CONTAINER:/tmp/webapp >>$LOG || exit 1
	fi

	docker cp structr.conf $CONTAINER:/var/lib/structr/structr.conf >>$LOG || exit 1

	echo "Starting container.." |tee -a $LOG
	docker start $CONTAINER >>$LOG || exit 1
	PORT=$(docker inspect --format='{{(index (index .NetworkSettings.Ports "8082/tcp") 0).HostPort}}' structr-selenium-tests)

	echo "Waiting for Structr to become available..." |tee -a $LOG
	sleep $WAIT
	until started || [ $MAX_TRIES -eq 0 ]; do
		echo -n "."
		sleep $WAIT
	done
	
	echo "."

	if [ $MAX_TRIES -eq 0 ]; then
		cancel
	fi

	if [ -n "$SOURCE" ]; then
		echo "Deploying webapp from $SOURCE.." |tee -a $LOG
		curl -si -HX-User:$USERNAME -HX-Password:$PASSWORD $BASE_URL:$PORT/structr/rest/maintenance/deploy -d '{ mode: import, source: "/tmp/webapp" }' >>$LOG || exit 1
	fi

	echo "Storing Structr image for later use.." |tee -a $LOG
	docker commit $CONTAINER $NAME >>$LOG || exit 1

fi

if [ "$RECORD" == "true" ]; then

	#
	# wait for user interaction and stop container afterwards, no tests are executed
	#

	echo "Structr instance started in recording mode."
	echo "URL: $BASE_URL:$PORT"
	echo
	read -n 1 -s -r -p "Press any key to stop.."
	echo

	echo "Stopping container.." |tee -a $LOG
	docker stop $CONTAINER >>$LOG || exit 1

	echo "Removing container.." |tee -a $LOG
	docker container rm $CONTAINER >>$LOG || exit 1

else

	#
	# create or re-use test image and run tests
	#

	echo "Creating test image.." |tee -a $LOG
	#docker image build -q -t selenium-test . >>$LOG || exit 1
	docker image build -q -t selenium-test .

	echo "Creating test container.." |tee -a $LOG
	TESTCONTAINER=$(docker create --network $NETWORK --shm-size=2g selenium-test)
	docker cp $TESTSUITE $TESTCONTAINER:/tmp/testsuite >>$LOG || exit 1
	
	echo "Running tests.." |tee -a $LOG
	docker start -a $TESTCONTAINER |tee -a $LOG || exit 1

	EXIT_CODE=$(docker inspect $TESTCONTAINER --format='{{.State.ExitCode}}')
	#if [ $EXIT_CODE -gt 0 ]; then

		echo "Downloading server.log and screenshots"
		docker exec -i $CONTAINER /bin/sh -c 'cat /var/lib/structr/logs/server.log' >server.log
		docker cp $TESTCONTAINER:/tmp/screenshots . >>$LOG
	#fi

	echo "Downloading test summary file.."
	if [ "$SUMMARY" != "." ]; then
		TARGET=`dirname $SUMMARY`
		if [ ! -e $TARGET ]; then
			mkdir $TARGET
		fi
	fi
	docker cp $TESTCONTAINER:/tmp/selenium-test-summary.xml $SUMMARY

	echo "Stopping containers.." |tee -a $LOG
	docker stop $CONTAINER >>$LOG || exit 1
	docker stop $TESTCONTAINER >>$LOG || exit 1

	echo "Removing containers.." |tee -a $LOG
	docker container rm $CONTAINER >>$LOG || exit 1
	docker container rm $TESTCONTAINER >>$LOG || exit 1

fi

echo "Removing isolated network" |tee -a $LOG
docker network rm $NETWORK >>$LOG || exit 1

exit $EXIT_CODE
