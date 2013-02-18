#!/bin/bash
java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n -Dfile.encoding=utf-8 -Xms1g -Xmx1g -XX:+UseNUMA -cp target/lib/*:target/structr-ui-0.7-SNAPSHOT.jar org.structr.Ui
