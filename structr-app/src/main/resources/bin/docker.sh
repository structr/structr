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

# Check if heap settings are configured in structr.conf
CONF_MAX_HEAP=$(grep -E "^\s*application\.heap\.max_size\s*=" "${_STRUCTR_HOME}/structr.conf" | sed 's/^[^=]*=\s*//' | tr -d '[:space:]')
CONF_MIN_HEAP=$(grep -E "^\s*application\.heap\.min_size\s*=" "${_STRUCTR_HOME}/structr.conf" | sed 's/^[^=]*=\s*//' | tr -d '[:space:]')

# Configure heap settings if not already present in structr.conf
if [ -z "$CONF_MAX_HEAP" ] && [ -z "$CONF_MIN_HEAP" ]; then
  if ! [ -f "${_STRUCTR_HOME}/bin/memory.conf" ]; then
    # Check if deprecated environment variables are used
    if [ -n "$STRUCTR_MIN_HEAP" ] || [ -n "$STRUCTR_MAX_HEAP" ]; then
      echo
      echo "        Deprecation warning: STRUCTR_MIN_HEAP and STRUCTR_MAX_HEAP environment variables are deprecated."
      echo "        Please use application.heap.min_size and application.heap.max_size in structr.conf instead."
      echo "        You can set these also via environment variables STRUCTR_application_heap_min__size and STRUCTR_application_heap_max__size"
      echo
    fi

    # Set heap values (use provided values or defaults)
    HEAP_MIN="${STRUCTR_MIN_HEAP:-1g}"
    HEAP_MAX="${STRUCTR_MAX_HEAP:-4g}"

    echo "        Setting memory configuration in structr.conf with min_size=${HEAP_MIN} max_size=${HEAP_MAX}"
    addEnvironmentEntryToConfig "application.heap.min_size" "${HEAP_MIN}" "${_STRUCTR_HOME}"
    addEnvironmentEntryToConfig "application.heap.max_size" "${HEAP_MAX}" "${_STRUCTR_HOME}"

    unset STRUCTR_MIN_HEAP
    unset STRUCTR_MAX_HEAP
    unset HEAP_MIN
    unset HEAP_MAX
  fi
fi
# Run startup config script
. bin/config

LOGS_DIR="logs"

if [ ! -d $LOGS_DIR ]; then
		echo "        Creating logs directory..."
		mkdir $LOGS_DIR
		touch $LOG_FILE
fi

echo "        Starting structr server $DISPLAY_NAME"

java $RUN_OPTS $JAVA_OPTS $MAIN_CLASS