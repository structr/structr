#!/bin/sh
_STRUCTR_HOME="/var/lib/structr"

if [ "$AGREE_TO_STRUCTR_PRIVACY_POLICY" != "yes" ]; then

	echo
	echo
	echo "In order to use Structr you must agree to the privacy policy"
	echo "at https://structr.com/privacy. To do so set the environment"
	echo "variable AGREE_TO_STRUCTR_PRIVACY_POLICY=yes"
	echo
	echo "Use the following docker command line argument to do so:"
	echo
	echo "    --env=AGREE_TO_STRUCTR_PRIVACY_POLICY=yes"
	echo
	echo

	exit 1
fi

function addDefaultConfigEntry {
	local SETTING="${1}"
	local VALUE="${2}"
	local _STRUCTR_HOME="${3}"

	if ! grep -q "^${SETTING}=" "${_STRUCTR_HOME}"/structr.conf
	then
		echo -e ${SETTING}=${VALUE} >> "${_STRUCTR_HOME}"/structr.conf
	fi
}

function addEnvironmentEntryToConfig {
	local SETTING="${1}"
	local VALUE="${2}"
	local STRUCTR_HOME="${3}"

  # remove setting from config if it already exists...
	if grep -q -F "${SETTING}=" "${_STRUCTR_HOME}"/structr.conf; then
		sed --in-place "/^${SETTING}=.*/d" "${_STRUCTR_HOME}"/structr.conf
	fi

	echo "${SETTING}=${VALUE}" >> "${_STRUCTR_HOME}"/structr.conf
}

# create memory config file before startup
touch "${_STRUCTR_HOME}/bin/memory.config"
if [ -n "$STRUCTR_MAX_HEAP" ] && ! [ -n "$STRUCTR_MIN_HEAP" ]; then
    echo "STRUCTR_MAX_HEAP set but STRUCTR_MIN_HEAP not found in docker environment"
    exit 1;
  elif [ -n "$STRUCTR_MIN_HEAP" ] && ! [ -n "$STRUCTR_MAX_HEAP" ]; then
    echo "STRUCTR_MIN_HEAP set but STRUCTR_MIN_HEAP not found in docker environment"
    exit 1;
  else
    echo "Creating memory configuration file with -Xms${STRUCTR_MIN_HEAP} -Xmx${STRUCTR_MAX_HEAP}"
    echo "-Xms${STRUCTR_MIN_HEAP} -Xmx${STRUCTR_MAX_HEAP}" > "${_STRUCTR_HOME}/bin/memory.config"
fi

unset STRUCTR_MIN_HEAP
unset STRUCTR_MAX_HEAP

# application settings
touch "${_STRUCTR_HOME}/structr.conf"
addDefaultConfigEntry "setup.wizard.completed" "true" "${_STRUCTR_HOME}"

# Add structr conf entries from environment variables
for i in $( set | grep ^STRUCTR_ | awk -F'=' '{print $1}'); do
    setting=$(echo "${i}" | sed 's|^STRUCTR_||' | sed 's|_|.|g' | sed 's|\.\.|_|g')
    value=$(echo "${!i}")
    # skip settings with no value
    if [[ -n ${value} ]]; then
        addEnvironmentEntryToConfig "${setting}" "${value}" "${_STRUCTR_HOME}"
    fi
done

# Run startup config script
. bin/config

LOGS_DIR="logs"

if [ -e $PID_FILE ]; then

	$(ps aux | grep "org.structr.Server" | grep -v grep)

	result=$?

	if [ $result -eq 1 ]; then
		echo
		echo "        Found $PID_FILE, but server is not running."
		echo "        Removing $PID_FILE and proceeding with startup."
		echo

		rm $PID_FILE
	else
		echo
		echo "        ERROR: server already running."
		echo
		echo "        Please stop any running instances before starting a"
		echo "        new one. (Remove $PID_FILE if this message appears"
		echo "        even if no server is running.)"
		echo

		exit 0
	fi

fi



if [ ! -d $LOGS_DIR ]; then
		echo "        Creating logs directory..."
		mkdir $LOGS_DIR
		touch $LOG_FILE
fi

echo "        Starting structr server $DISPLAY_NAME"

java $RUN_OPTS $JAVA_OPTS $MAIN_CLASS
