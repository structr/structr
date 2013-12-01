#!/bin/bash
java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -Dfile.encoding=utf-8 -Xms2g -Xmx2g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseNUMA -cp target/lib/*:target/structr-ui-0.9-SNAPSHOT.jar -Djava.util.logging.config.file=logging.properties -Dmode=test org.structr.Server
