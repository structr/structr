#!/bin/sh
#mvn -Dexec.classpathScope=runtime \
#-Dexec.args="-server -Xms512m -Xmx1024m -classpath %classpath \
#org.structr.tools.GeoDataTool \
#-dbPath /opt/structr/t5s/db \
#-file /home/axel/Downloads/TM_WORLD_BORDERS_SIMPL-0.3/TM_WORLD_BORDERS_SIMPL-0.3.shp \
#-importShapefile \
#-layer world_regions" \
#-Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec

mvn -Dexec.classpathScope=runtime -Dexec.args="-server -Xms64m -Xmx1024m -classpath %classpath org.structr.tools.GeoDataTool \
-dbPath /opt/structr/t5s/db -file /home/axel/MERGED/WORLD_REGIONS.shp \
-importShapefile -layer world_regions_final" -Dexec.executable=/usr/lib/jvm/java-6-openjdk/bin/java -Dnetbeans.execution=true process-classes org.codehaus.mojo:exec-maven-plugin:1.1.1:exec

