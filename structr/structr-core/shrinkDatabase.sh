#!/bin/sh
rm -rf /tmp/neo4j-copy
cp /opt/structr/t5s/db/neo4j.conf /tmp
mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -Xms2048m -Xmx2048m -classpath %classpath \
org.structr.tools.Admin \
-copyDatabase -targetDbPath /tmp/neo4j-copy \
-dbPath /opt/structr/t5s/db" \
-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec
mv /opt/structr/t5s/db /opt/structr/t5s/db-$(date +%Y%m%d-%H%M%S)
mv /tmp/neo4j-copy /opt/structr/t5s/db
mv /tmp/neo4j.conf /opt/structr/t5s/db


