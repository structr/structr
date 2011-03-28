#!/bin/sh
mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -Xms512m -Xmx1024m -classpath %classpath \
org.structr.tools.GeoDataTool \
-dbPath /opt/structr/structr-tfs2 \
-type Hotel \
-geocode" \
-Dexec.executable=/usr/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec
