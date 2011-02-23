#!/bin/sh
mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -Xms2048m -Xmx2048m -classpath %classpath \
org.structr.tools.Admin \
-removeOrphanedNodes -dbPath /opt/structr/structr-tfs2" \
-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec
