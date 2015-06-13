#!/bin/bash

# Force HTTPS by adding a redirect rule to urlrewrite.xml

# 1. Copy the original urlrewrite.xml to the target/ directory:

   cd target
   cp ../src/main/resources/urlrewrite.xml urlrewrite.xml-orig

# 2. Add the following rule to urlrewrite.xml:

    cat urlrewrite.xml-orig | grep -v '</urlrewrite>' > urlrewrite.xml
    cat <<EOF >> urlrewrite.xml
    <rule>
        <condition type="scheme" operator="notequal">https</condition>
        <from>^/(.*)</from>
        <to type="permanent-redirect" last="true">https://%{server-name}/$1</to>
    </rule>

</urlrewrite>
EOF

# 3. Update jar file with the modified urlrewrite.xml:

JAR=`which jar`
LATEST=`ls structr-ui-*.jar | grep -v 'sources.jar' | grep -v 'javadoc.jar' | sort | tail -1`

$JAR uvf $LATEST urlrewrite.xml

# The modification will take effect upon next (re)start. To undo the changes, just rebuild without applying this script again.
