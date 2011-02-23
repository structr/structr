#!/bin/sh
mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -Xms64m -Xmx128m -classpath %classpath \
org.structr.tools.GeoDataTool \
-dbPath /opt/structr/structr-tfs2 \
-file /home/axel/Downloads/TM_WORLD_BORDERS_SIMPL-0.3/TM_WORLD_BORDERS_SIMPL-0.3.shp \
-importShapefile \
-layer world_regions" \
-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec

mvn -Dexec.classpathScope=runtime \
-Dexec.args="-server -Xms64m -Xmx128m -classpath %classpath \
org.structr.tools.GeoDataTool \
-dbPath /opt/structr/structr-tfs2 \
-file /home/axel/Downloads/TM_WORLD_BORDERS-0.3/TM_WORLD_BORDERS-0.3.shp \
-importShapefile \
-layer t5s_geodata" \
-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec
