#!/bin/bash
java -Dfile.encoding=utf-8 -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+UseNUMA -cp target/lib/*:target/structr-ui-1.0-SNAPSHOT.jar org.structr.Server
