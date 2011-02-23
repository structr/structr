#!/bin/sh
mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -classpath %classpath \
org.structr.tools.GeoDataTool \
-linkCountries \
-dbPath /opt/structr/structr-tfs2" \
-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec
