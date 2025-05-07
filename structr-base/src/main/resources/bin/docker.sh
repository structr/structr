#!/bin/sh
_STRUCTR_HOME="/var/lib/structr"

if [ "$AGREE_TO_STRUCTR_PRIVACY_POLICY" != "yes" ]; then

	echo
	echo
	echo "        In order to use Structr you must agree to the privacy policy"
	echo "        at https://structr.com/privacy. To do so set the environment"
	echo "        variable AGREE_TO_STRUCTR_PRIVACY_POLICY=yes"
	echo
	echo "        Use the following docker command line argument to do so:"
	echo
	echo "            --env=AGREE_TO_STRUCTR_PRIVACY_POLICY=yes"
	echo
	echo

	exit 1
fi

function fileEndsWithNewline() {
    [[ $(tail -c1 "$1" | wc -l) -gt 0 ]]
}

addEmptyLineToConfig() {
  local _STRUCTR_HOME="${1}"

  if ! fileEndsWithNewline "${_STRUCTR_HOME}/structr.conf"
  then
    echo -e "\n" >> "${_STRUCTR_HOME}/structr.conf"
  fi
}

addDefaultConfigEntry() {
	local SETTING="${1}"
	local VALUE="${2}"
	local _STRUCTR_HOME="${3}"

	if ! grep -qE "^${SETTING}\s*=.*" "${_STRUCTR_HOME}/structr.conf"
	then
		echo ${SETTING} = ${VALUE} >> "${_STRUCTR_HOME}/structr.conf"
	fi
}

addEnvironmentEntryToConfig() {
	local SETTING="${1}"
	local VALUE="${2}"
	local STRUCTR_HOME="${3}"

  # remove setting from config if it already exists...
  sed --in-place "/^${SETTING}\s*=.*/d" "${_STRUCTR_HOME}/structr.conf"

	# write entry to conf file.
	echo "${SETTING} = ${VALUE}" >> "${_STRUCTR_HOME}/structr.conf"
}

# Add a new filesystem group for deployment file/directory permissions
NEW_GROUP_NAME="$(set | grep STRUCTR_deploymentservlet_filegroup_name | awk -F '=' '{print $2}')"
NEW_GROUP_ID="$(set | grep STRUCTR_deploymentservlet_filegroup_id | awk -F '=' '{print $2}')"

if [ -n "${NEW_GROUP_NAME}" ] && [ -z "${NEW_GROUP_ID}" ]; then
  echo "        Group name for deployment file permissions found, but no group id given. Please also add 'STRUCTR_deploymentservlet_filegroup_id' to the environment, that corresponds with the id of the hosts group with the same name."
  exit 1;
elif [ -n "${NEW_GROUP_ID}" ] && [ -z "${NEW_GROUP_NAME}" ]; then
  echo "        Group id for deployment file permissions found, but no group name given. Please also add 'STRUCTR_deploymentservlet_filegroup_name' to the environment, that corresponds with the id of the hosts group with the same name."
  exit 1;
elif [ -n "${NEW_GROUP_ID}" ] && [ -n "${NEW_GROUP_NAME}" ]; then
  echo "        Adding new group to docker container: ${NEW_GROUP_NAME} with gid ${NEW_GROUP_ID}"
  groupadd -f --gid ${NEW_GROUP_ID} ${NEW_GROUP_NAME}
  USER=$(id -u -n)
  usermod -a -G ${NEW_GROUP_NAME} ${USER}
fi

# check and create memory config file before startup
if ! [ -f "${_STRUCTR_HOME}/bin/memory.conf" ]; then
  touch "${_STRUCTR_HOME}/bin/memory.conf"
  if [ -n "$STRUCTR_MAX_HEAP" ] &&  [ -z "$STRUCTR_MIN_HEAP" ]; then
    echo "        STRUCTR_MAX_HEAP set but STRUCTR_MIN_HEAP not found in docker environment"
    exit 1;
  elif [ -n "$STRUCTR_MIN_HEAP" ] && [ -z "$STRUCTR_MAX_HEAP" ]; then
    echo "        STRUCTR_MIN_HEAP set but STRUCTR_MIN_HEAP not found in docker environment"
    exit 1;
  elif ! [ -n "$STRUCTR_MIN_HEAP" ] && [ -z "$STRUCTR_MAX_HEAP" ]; then
      echo "        Creating memory configuration file with default configuration of -Xms1g -Xmx4g"
      echo "-Xms1g -Xmx4g" > "${_STRUCTR_HOME}/bin/memory.conf"
  else
    echo "        Creating memory configuration file with -Xms${STRUCTR_MIN_HEAP} -Xmx${STRUCTR_MAX_HEAP}"
    echo "-Xms${STRUCTR_MIN_HEAP} -Xmx${STRUCTR_MAX_HEAP}" > "${_STRUCTR_HOME}/bin/memory.conf"
  fi
fi

unset STRUCTR_MIN_HEAP
unset STRUCTR_MAX_HEAP

# application settings
touch "${_STRUCTR_HOME}/structr.conf"
addEmptyLineToConfig "${_STRUCTR_HOME}"
addDefaultConfigEntry "setup.wizard.completed" "true" "${_STRUCTR_HOME}"

# Add structr conf entries from environment variables
for i in $( set | grep ^STRUCTR_ | awk -F'=' '{print $1}'); do
    setting=$(echo "${i}" | sed 's|^STRUCTR_||' | sed 's|_|.|g' | sed 's|\.\.|_|g')
    value=$(echo "${!i}")

    # skip settings with no value
    if [ -n "${value}" ]; then
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

		rm "$PID_FILE"
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